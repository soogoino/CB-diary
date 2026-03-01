package com.chastity.diary.data.repository

import com.chastity.diary.data.datastore.PreferencesManager
import com.chastity.diary.domain.model.AppLanguage
import com.chastity.diary.domain.model.DarkMode
import com.chastity.diary.domain.model.Gender
import com.chastity.diary.domain.model.UserSettings
import com.chastity.diary.domain.repository.ISettingsRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Repository for user settings
 */
class SettingsRepository(private val preferencesManager: PreferencesManager) : ISettingsRepository {
    
    override val userSettings: Flow<UserSettings> = preferencesManager.userSettingsFlow
    
    override suspend fun updateGender(gender: Gender) {
        preferencesManager.updateGender(gender)
    }
    
    override suspend fun updateStartDate(date: LocalDate) {
        preferencesManager.updateStartDate(date)
    }
    
    override suspend fun updateReminderSettings(enabled: Boolean, hour: Int, minute: Int) {
        preferencesManager.updateReminderSettings(enabled, hour, minute)
    }
    
    override suspend fun updateBiometricEnabled(enabled: Boolean) {
        preferencesManager.updateBiometricEnabled(enabled)
    }
    
    override suspend fun updatePinEnabled(enabled: Boolean) {
        preferencesManager.updatePinEnabled(enabled)
    }
    
    override suspend fun updateDarkMode(mode: DarkMode) {
        preferencesManager.updateDarkMode(mode)
    }
    
    override suspend fun updateCloudSyncEnabled(enabled: Boolean) {
        preferencesManager.updateCloudSyncEnabled(enabled)
    }
    
    override suspend fun updateLastSyncTime(timestamp: Long) {
        preferencesManager.updateLastSyncTime(timestamp)
    }
    
    override suspend fun updateCustomTasks(tasks: List<String>) {
        preferencesManager.updateCustomTasks(tasks)
    }
    
    // Personal profile update methods
    
    override suspend fun updateHeight(height: Int?) {
        preferencesManager.updateHeight(height)
    }
    
    override suspend fun updateWeight(weight: Float?) {
        preferencesManager.updateWeight(weight)
    }
    
    override suspend fun updateCurrentDeviceName(name: String?) {
        preferencesManager.updateCurrentDeviceName(name)
    }

    override suspend fun updateCurrentDeviceSize(size: String?) {
        preferencesManager.updateCurrentDeviceSize(size)
    }

    override suspend fun updateNickname(name: String?) {
        preferencesManager.updateNickname(name)
    }

    override val isOnboardingCompleted: Flow<Boolean> = preferencesManager.isOnboardingCompleted

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        preferencesManager.setOnboardingCompleted(completed)
    }

    override suspend fun updateMorningReminderSettings(enabled: Boolean, hour: Int, minute: Int) {
        preferencesManager.updateMorningReminderSettings(enabled, hour, minute)
    }

    override suspend fun updatePhotoBlurEnabled(enabled: Boolean) {
        preferencesManager.updatePhotoBlurEnabled(enabled)
    }

    override suspend fun updateLanguage(language: AppLanguage) {
        preferencesManager.updateLanguage(language)
    }

    override suspend fun updateCardThemeId(themeId: String) {
        preferencesManager.updateCardThemeId(themeId)
    }

    override suspend fun setSponsorUnlocked(unlocked: Boolean) {
        preferencesManager.setSponsorUnlocked(unlocked)
    }
}
