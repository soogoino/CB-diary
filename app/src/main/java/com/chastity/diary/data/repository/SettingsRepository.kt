package com.chastity.diary.data.repository

import com.chastity.diary.data.datastore.PreferencesManager
import com.chastity.diary.domain.model.DarkMode
import com.chastity.diary.domain.model.Gender
import com.chastity.diary.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Repository for user settings
 */
class SettingsRepository(private val preferencesManager: PreferencesManager) {
    
    val userSettings: Flow<UserSettings> = preferencesManager.userSettingsFlow
    
    suspend fun updateGender(gender: Gender) {
        preferencesManager.updateGender(gender)
    }
    
    suspend fun updateStartDate(date: LocalDate) {
        preferencesManager.updateStartDate(date)
    }
    
    suspend fun updateReminderSettings(enabled: Boolean, hour: Int, minute: Int) {
        preferencesManager.updateReminderSettings(enabled, hour, minute)
    }
    
    suspend fun updateBiometricEnabled(enabled: Boolean) {
        preferencesManager.updateBiometricEnabled(enabled)
    }
    
    suspend fun updatePinEnabled(enabled: Boolean) {
        preferencesManager.updatePinEnabled(enabled)
    }
    
    suspend fun updateDarkMode(mode: DarkMode) {
        preferencesManager.updateDarkMode(mode)
    }
    
    suspend fun updateCloudSyncEnabled(enabled: Boolean) {
        preferencesManager.updateCloudSyncEnabled(enabled)
    }
    
    suspend fun updateLastSyncTime(timestamp: Long) {
        preferencesManager.updateLastSyncTime(timestamp)
    }
    
    suspend fun updateCustomTasks(tasks: List<String>) {
        preferencesManager.updateCustomTasks(tasks)
    }
    
    // Personal profile update methods
    
    suspend fun updateHeight(height: Int?) {
        preferencesManager.updateHeight(height)
    }
    
    suspend fun updateWeight(weight: Float?) {
        preferencesManager.updateWeight(weight)
    }
    
    suspend fun updateCurrentDeviceName(name: String?) {
        preferencesManager.updateCurrentDeviceName(name)
    }

    suspend fun updateCurrentDeviceSize(size: String?) {
        preferencesManager.updateCurrentDeviceSize(size)
    }

    suspend fun updateNickname(name: String?) {
        preferencesManager.updateNickname(name)
    }

    val isOnboardingCompleted: Flow<Boolean> = preferencesManager.isOnboardingCompleted

    suspend fun setOnboardingCompleted(completed: Boolean) {
        preferencesManager.setOnboardingCompleted(completed)
    }

    suspend fun updateMorningReminderSettings(enabled: Boolean, hour: Int, minute: Int) {
        preferencesManager.updateMorningReminderSettings(enabled, hour, minute)
    }

    suspend fun updatePhotoBlurEnabled(enabled: Boolean) {
        preferencesManager.updatePhotoBlurEnabled(enabled)
    }
}
