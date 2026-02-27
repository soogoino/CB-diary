package com.chastity.diary.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.work.*
import com.chastity.diary.R
import com.chastity.diary.data.datastore.PreferencesManager
import com.chastity.diary.data.local.database.AppDatabase
import com.chastity.diary.data.repository.EntryRepository
import com.chastity.diary.data.repository.SettingsRepository
import com.chastity.diary.domain.model.AppLanguage
import com.chastity.diary.domain.model.DarkMode
import com.chastity.diary.domain.model.Gender
import com.chastity.diary.domain.model.UserSettings
import com.chastity.diary.util.Constants
import com.chastity.diary.util.CsvHelper
import com.chastity.diary.util.TestDataGenerator
import com.chastity.diary.util.NotificationHelper
import com.chastity.diary.worker.DailyReminderWorker
import com.chastity.diary.worker.MorningReminderWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * ViewModel for settings screen
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesManager = PreferencesManager(application)
    private val repository = SettingsRepository(preferencesManager)
    private val database = AppDatabase.getInstance(application)
    private val entryRepository = EntryRepository(database.dailyEntryDao(), database.dailyEntryAttributeDao())
    private val workManager = WorkManager.getInstance(application)
    
    private val encryptedPrefs: android.content.SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(application)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
            
        EncryptedSharedPreferences.create(
            application,
            Constants.ENCRYPTED_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    init {
        // Create notification channel on initialization
        NotificationHelper.createNotificationChannel(application)
    }
    
    // Q4: Eagerly so the first frame always reads persisted settings (avoids isMale flicker)
    val userSettings: StateFlow<UserSettings> = repository.userSettings
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            UserSettings()
        )
    
    private val _testDataMessage = MutableStateFlow<String?>(null)
    val testDataMessage: StateFlow<String?> = _testDataMessage

    private val _exportImportMessage = MutableStateFlow<String?>(null)
    val exportImportMessage: StateFlow<String?> = _exportImportMessage
    
    fun updateGender(gender: Gender) {
        viewModelScope.launch {
            repository.updateGender(gender)
        }
    }
    
    fun updateStartDate(date: LocalDate) {
        viewModelScope.launch {
            repository.updateStartDate(date)
        }
    }
    
    fun updateReminderSettings(enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch {
            repository.updateReminderSettings(enabled, hour, minute)
            
            if (enabled) {
                scheduleDailyReminder(hour, minute)
            } else {
                cancelDailyReminder()
            }
        }
    }

    fun updateMorningReminderSettings(enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch {
            repository.updateMorningReminderSettings(enabled, hour, minute)
            if (enabled) {
                scheduleMorningReminder(hour, minute)
            } else {
                workManager.cancelUniqueWork(Constants.WORK_MORNING_REMINDER)
            }
        }
    }
    
    private fun scheduleDailyReminder(hour: Int, minute: Int) {
        // Calculate initial delay
        val now = LocalTime.now()
        val targetTime = LocalTime.of(hour, minute)
        var initialDelayMinutes = java.time.Duration.between(now, targetTime).toMinutes()
        
        // If target time has passed today, schedule for tomorrow
        if (initialDelayMinutes < 0) {
            initialDelayMinutes += 24 * 60
        }
        
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(false)
            .build()
        
        val dailyWorkRequest = PeriodicWorkRequestBuilder<DailyReminderWorker>(
            1, TimeUnit.DAYS
        )
            .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            Constants.WORK_DAILY_REMINDER,
            ExistingPeriodicWorkPolicy.UPDATE,
            dailyWorkRequest
        )
    }
    
    private fun cancelDailyReminder() {
        workManager.cancelUniqueWork(Constants.WORK_DAILY_REMINDER)
    }

    private fun scheduleMorningReminder(hour: Int, minute: Int) {
        val now = LocalTime.now()
        val targetTime = LocalTime.of(hour, minute)
        var initialDelayMinutes = java.time.Duration.between(now, targetTime).toMinutes()
        if (initialDelayMinutes < 0) initialDelayMinutes += 24 * 60

        val request = PeriodicWorkRequestBuilder<MorningReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
            .build()

        workManager.enqueueUniquePeriodicWork(
            Constants.WORK_MORNING_REMINDER,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
    
    fun updateBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateBiometricEnabled(enabled)
            // Update encrypted preferences
            encryptedPrefs.edit().apply {
                putBoolean(Constants.KEY_LOCK_ENABLED, enabled || userSettings.value.pinEnabled)
                apply()
            }
        }
    }
    
    fun updatePinEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updatePinEnabled(enabled)
            // Update encrypted preferences
            encryptedPrefs.edit().apply {
                putBoolean(Constants.KEY_LOCK_ENABLED, enabled || userSettings.value.biometricEnabled)
                apply()
            }
        }
    }
    
    fun updatePinCode(pin: String) {
        viewModelScope.launch {
            encryptedPrefs.edit().apply {
                putString(Constants.KEY_PIN_CODE, pin)
                apply()
            }
        }
    }
    
    fun updateDarkMode(mode: DarkMode) {
        viewModelScope.launch {
            repository.updateDarkMode(mode)
        }
    }

    fun updateLanguage(language: AppLanguage) {
        viewModelScope.launch {
            repository.updateLanguage(language)
        }
    }

    fun updateCloudSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateCloudSyncEnabled(enabled)
        }
    }
    
    fun updateCustomTasks(tasks: List<String>) {
        viewModelScope.launch {
            repository.updateCustomTasks(tasks)
        }
    }
    
    // Personal profile update methods
    
    fun updateHeight(height: Int?) {
        viewModelScope.launch {
            repository.updateHeight(height)
        }
    }
    
    fun updateWeight(weight: Float?) {
        viewModelScope.launch {
            repository.updateWeight(weight)
        }
    }
    
    fun updateCurrentDeviceName(name: String?) {
        viewModelScope.launch {
            repository.updateCurrentDeviceName(name)
        }
    }

    fun updateCurrentDeviceSize(size: String?) {
        viewModelScope.launch {
            repository.updateCurrentDeviceSize(size)
        }
    }

    fun updateNickname(name: String?) {
        viewModelScope.launch {
            repository.updateNickname(name)
        }
    }

    /**
     * Generate test data for development
     */
    fun generateTestData() {
        viewModelScope.launch {
            try {
                _testDataMessage.value = getApplication<Application>().getString(R.string.status_generating_test_data)
                val testEntries = TestDataGenerator.generateSampleEntries(30)
                var successCount = 0
                testEntries.forEach { entry ->
                    try {
                        entryRepository.insertEntry(entry)
                        successCount++
                    } catch (e: Exception) {
                        // Skip duplicates or errors
                    }
                }
                _testDataMessage.value = getApplication<Application>().getString(R.string.status_generated_test_data, successCount)
            } catch (e: Exception) {
                _testDataMessage.value = getApplication<Application>().getString(R.string.status_generate_failed, e.message ?: "")
            }
        }
    }
    
    fun clearTestDataMessage() {
        _testDataMessage.value = null
    }

    fun clearExportImportMessage() {
        _exportImportMessage.value = null
    }

    fun updatePhotoBlurEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updatePhotoBlurEnabled(enabled)
        }
    }

    /**
     * Export all diary entries to a CSV file at the given URI (from SAF).
     */
    fun exportCsv(uri: Uri) {
        viewModelScope.launch {
            try {
                _exportImportMessage.value = getApplication<Application>().getString(R.string.status_exporting)
                val entries = entryRepository.getAllEntriesSync()
                val csvContent = CsvHelper.toCsv(entries)
                getApplication<Application>().contentResolver.openOutputStream(uri)?.use { stream ->
                    OutputStreamWriter(stream, Charsets.UTF_8).use { writer ->
                        writer.write(csvContent)
                    }
                }
                _exportImportMessage.value = getApplication<Application>().getString(R.string.status_export_success, entries.size)
            } catch (e: Exception) {
                _exportImportMessage.value = getApplication<Application>().getString(R.string.status_export_failed, e.message ?: "")
            }
        }
    }

    /**
     * Import diary entries from a CSV file at the given URI (from SAF).
     * Uses REPLACE conflict strategy — existing same-date records will be overwritten.
     */
    fun importCsv(uri: Uri) {
        viewModelScope.launch {
            try {
                _exportImportMessage.value = getApplication<Application>().getString(R.string.status_importing)
                val csvContent = getApplication<Application>().contentResolver
                    .openInputStream(uri)?.use { stream ->
                        BufferedReader(InputStreamReader(stream, Charsets.UTF_8))
                            .readText()
                    } ?: throw IllegalStateException(getApplication<Application>().getString(R.string.error_file_open))
                val entries = CsvHelper.fromCsv(csvContent)
                if (entries.isEmpty()) {
                    _exportImportMessage.value = getApplication<Application>().getString(R.string.error_import_invalid)
                    return@launch
                }
                var successCount = 0
                entries.forEach { entry ->
                    try {
                        // Reset id to 0 so Room decides insert vs update based on date via REPLACE
                        entryRepository.insertEntry(entry.copy(id = 0))
                        successCount++
                    } catch (e: Exception) { /* skip */ }
                }
                _exportImportMessage.value = getApplication<Application>().getString(R.string.status_import_success, successCount)
            } catch (e: Exception) {
                _exportImportMessage.value = getApplication<Application>().getString(R.string.status_import_failed, e.message ?: "")
            }
        }
    }
}
