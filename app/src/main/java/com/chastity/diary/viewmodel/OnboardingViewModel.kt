package com.chastity.diary.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.chastity.diary.data.datastore.PreferencesManager
import com.chastity.diary.data.repository.SettingsRepository
import com.chastity.diary.domain.model.AppLanguage
import com.chastity.diary.domain.model.Gender
import com.chastity.diary.util.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class OnboardingViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesManager = PreferencesManager(application)
    private val repository = SettingsRepository(preferencesManager)

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

    // ── Detect existing user ────────────────────────────────────────────────────
    val isOnboardingCompleted: StateFlow<Boolean?> = repository.isOnboardingCompleted
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** True if user has pre-existing data (upgrading from older version) */
    val isExistingUser: StateFlow<Boolean> = repository.userSettings
        .map { it.startDate != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // ── Onboarding form state (temp, written only on completion) ────────────────
    val nickname = MutableStateFlow("")
    val gender = MutableStateFlow(Gender.MALE)
    val startDate = MutableStateFlow<LocalDate?>(null)
    val deviceName = MutableStateFlow("")
    val height = MutableStateFlow<Int?>(null)
    val weight = MutableStateFlow<Float?>(null)

    val language = MutableStateFlow(AppLanguage.SYSTEM)

    val biometricEnabled = MutableStateFlow(false)
    val pinEnabled = MutableStateFlow(false)
    val pinCode = MutableStateFlow("")   // set via PinSetupDialog in SecurityPage

    val reminderEnabled = MutableStateFlow(true)
    val reminderHour = MutableStateFlow(Constants.DEFAULT_REMINDER_HOUR)
    val reminderMinute = MutableStateFlow(Constants.DEFAULT_REMINDER_MINUTE)

    // ── Actions ────────────────────────────────────────────────────────────────

    /**
     * Persist the chosen language and immediately apply it via AppCompatDelegate.
     */
    fun updateLanguage(lang: AppLanguage) {
        language.value = lang
        viewModelScope.launch {
            repository.updateLanguage(lang)
            val tag = lang.tag
            val locales = if (tag.isEmpty())
                androidx.core.os.LocaleListCompat.getEmptyLocaleList()
            else
                androidx.core.os.LocaleListCompat.forLanguageTags(tag)
            androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(locales)
        }
    }

    /** Write all collected data to DataStore and mark onboarding complete. */
    fun completeOnboarding() {
        // Write encrypted prefs synchronously so MainActivity can read the lock state
        // immediately in its onComplete lambda (before the DataStore coroutine finishes).
        encryptedPrefs.edit().apply {
            val lockEnabled = biometricEnabled.value || pinEnabled.value
            putBoolean(Constants.KEY_LOCK_ENABLED, lockEnabled)
            if (pinEnabled.value && pinCode.value.isNotEmpty()) {
                putString(Constants.KEY_PIN_CODE, pinCode.value)
            }
            apply()
        }
        viewModelScope.launch {
            repository.updateLanguage(language.value)
            if (nickname.value.isNotBlank()) repository.updateNickname(nickname.value)
            repository.updateGender(gender.value)
            startDate.value?.let { repository.updateStartDate(it) }
            if (deviceName.value.isNotBlank()) repository.updateCurrentDeviceName(deviceName.value)
            height.value?.let { repository.updateHeight(it) }
            weight.value?.let { repository.updateWeight(it) }
            repository.updateBiometricEnabled(biometricEnabled.value)
            repository.updatePinEnabled(pinEnabled.value)
            repository.updateReminderSettings(reminderEnabled.value, reminderHour.value, reminderMinute.value)
            repository.setOnboardingCompleted(true)
        }
    }

    /** Skip = just mark complete without writing optional data. */
    fun skip() {
        viewModelScope.launch {
            repository.setOnboardingCompleted(true)
        }
    }
}
