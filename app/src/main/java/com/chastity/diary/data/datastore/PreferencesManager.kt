package com.chastity.diary.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.chastity.diary.domain.model.DarkMode
import com.chastity.diary.domain.model.Gender
import com.chastity.diary.domain.model.UserSettings
import com.chastity.diary.util.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.LocalDate

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = Constants.DATASTORE_NAME
)

/**
 * DataStore manager for user preferences
 */
class PreferencesManager(private val context: Context) {
    
    private object PreferencesKeys {
        val GENDER = stringPreferencesKey("gender")
        val START_DATE = stringPreferencesKey("start_date")
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val PIN_ENABLED = booleanPreferencesKey("pin_enabled")
        val DARK_MODE = stringPreferencesKey("dark_mode")
        val CLOUD_SYNC_ENABLED = booleanPreferencesKey("cloud_sync_enabled")
        val LAST_SYNC_TIME = longPreferencesKey("last_sync_time")
        val CUSTOM_TASKS = stringPreferencesKey("custom_tasks")
        val CURRENT_STREAK = intPreferencesKey("current_streak")
        val LONGEST_STREAK = intPreferencesKey("longest_streak")
        val LAST_ENTRY_DATE = stringPreferencesKey("last_entry_date")
        
        // Personal profile
        val HEIGHT = intPreferencesKey("height")
        val WEIGHT = floatPreferencesKey("weight")
        val CURRENT_DEVICE_NAME = stringPreferencesKey("current_device_name")
        val NICKNAME = stringPreferencesKey("nickname")

        // Morning reminder
        val MORNING_REMINDER_ENABLED = booleanPreferencesKey("morning_reminder_enabled")
        val MORNING_REMINDER_HOUR = intPreferencesKey("morning_reminder_hour")
        val MORNING_REMINDER_MINUTE = intPreferencesKey("morning_reminder_minute")

        // Onboarding
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }
    
    val userSettingsFlow: Flow<UserSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            UserSettings(
                gender = Gender.valueOf(
                    preferences[PreferencesKeys.GENDER] ?: Gender.MALE.name
                ),
                startDate = preferences[PreferencesKeys.START_DATE]?.let { 
                    LocalDate.parse(it) 
                },
                reminderEnabled = preferences[PreferencesKeys.REMINDER_ENABLED] ?: true,
                reminderHour = preferences[PreferencesKeys.REMINDER_HOUR] 
                    ?: Constants.DEFAULT_REMINDER_HOUR,
                reminderMinute = preferences[PreferencesKeys.REMINDER_MINUTE] 
                    ?: Constants.DEFAULT_REMINDER_MINUTE,
                biometricEnabled = preferences[PreferencesKeys.BIOMETRIC_ENABLED] ?: false,
                pinEnabled = preferences[PreferencesKeys.PIN_ENABLED] ?: false,
                darkMode = DarkMode.valueOf(
                    preferences[PreferencesKeys.DARK_MODE] ?: DarkMode.SYSTEM.name
                ),
                cloudSyncEnabled = preferences[PreferencesKeys.CLOUD_SYNC_ENABLED] ?: false,
                lastSyncTime = preferences[PreferencesKeys.LAST_SYNC_TIME],
                customTasks = preferences[PreferencesKeys.CUSTOM_TASKS]?.split("|") 
                    ?: emptyList(),
                
                // Personal profile
                height = preferences[PreferencesKeys.HEIGHT],
                weight = preferences[PreferencesKeys.WEIGHT],
                currentDeviceName = preferences[PreferencesKeys.CURRENT_DEVICE_NAME],
                nickname = preferences[PreferencesKeys.NICKNAME],

                // Morning reminder
                morningReminderEnabled = preferences[PreferencesKeys.MORNING_REMINDER_ENABLED] ?: false,
                morningReminderHour = preferences[PreferencesKeys.MORNING_REMINDER_HOUR] ?: 7,
                morningReminderMinute = preferences[PreferencesKeys.MORNING_REMINDER_MINUTE] ?: 30
            )
        }
    
    suspend fun updateGender(gender: Gender) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.GENDER] = gender.name
        }
    }
    
    suspend fun updateStartDate(date: LocalDate) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.START_DATE] = date.toString()
        }
    }
    
    suspend fun updateReminderSettings(enabled: Boolean, hour: Int, minute: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.REMINDER_ENABLED] = enabled
            preferences[PreferencesKeys.REMINDER_HOUR] = hour
            preferences[PreferencesKeys.REMINDER_MINUTE] = minute
        }
    }
    
    suspend fun updateBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BIOMETRIC_ENABLED] = enabled
        }
    }
    
    suspend fun updatePinEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PIN_ENABLED] = enabled
        }
    }
    
    suspend fun updateDarkMode(mode: DarkMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DARK_MODE] = mode.name
        }
    }
    
    suspend fun updateCloudSyncEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CLOUD_SYNC_ENABLED] = enabled
        }
    }
    
    suspend fun updateLastSyncTime(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_SYNC_TIME] = timestamp
        }
    }
    
    suspend fun updateCustomTasks(tasks: List<String>) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CUSTOM_TASKS] = tasks.joinToString("|")
        }
    }
    
    suspend fun updateStreak(currentStreak: Int, longestStreak: Int, lastEntryDate: LocalDate) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CURRENT_STREAK] = currentStreak
            preferences[PreferencesKeys.LONGEST_STREAK] = longestStreak
            preferences[PreferencesKeys.LAST_ENTRY_DATE] = lastEntryDate.toString()
        }
    }
    
    val currentStreakFlow: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.CURRENT_STREAK] ?: 0
        }
    
    val longestStreakFlow: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.LONGEST_STREAK] ?: 0
        }
    
    // Personal profile update methods
    
    suspend fun updateHeight(height: Int?) {
        context.dataStore.edit { preferences ->
            if (height != null) {
                preferences[PreferencesKeys.HEIGHT] = height
            } else {
                preferences.remove(PreferencesKeys.HEIGHT)
            }
        }
    }
    
    suspend fun updateWeight(weight: Float?) {
        context.dataStore.edit { preferences ->
            if (weight != null) {
                preferences[PreferencesKeys.WEIGHT] = weight
            } else {
                preferences.remove(PreferencesKeys.WEIGHT)
            }
        }
    }
    
    suspend fun updateCurrentDeviceName(name: String?) {
        context.dataStore.edit { preferences ->
            if (!name.isNullOrBlank()) {
                preferences[PreferencesKeys.CURRENT_DEVICE_NAME] = name
            } else {
                preferences.remove(PreferencesKeys.CURRENT_DEVICE_NAME)
            }
        }
    }

    suspend fun updateNickname(name: String?) {
        context.dataStore.edit { preferences ->
            if (!name.isNullOrBlank()) {
                preferences[PreferencesKeys.NICKNAME] = name
            } else {
                preferences.remove(PreferencesKeys.NICKNAME)
            }
        }
    }

    val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false
        }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun updateMorningReminderSettings(enabled: Boolean, hour: Int, minute: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.MORNING_REMINDER_ENABLED] = enabled
            preferences[PreferencesKeys.MORNING_REMINDER_HOUR] = hour
            preferences[PreferencesKeys.MORNING_REMINDER_MINUTE] = minute
        }
    }
}
