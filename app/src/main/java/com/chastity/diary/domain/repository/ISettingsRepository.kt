package com.chastity.diary.domain.repository

import com.chastity.diary.domain.model.AppLanguage
import com.chastity.diary.domain.model.DarkMode
import com.chastity.diary.domain.model.Gender
import com.chastity.diary.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Domain-layer contract for user settings persistence.
 * Concrete implementation: [com.chastity.diary.data.repository.SettingsRepository]
 */
interface ISettingsRepository {

    val userSettings: Flow<UserSettings>

    val isOnboardingCompleted: Flow<Boolean>

    suspend fun updateGender(gender: Gender)

    suspend fun updateStartDate(date: LocalDate)

    suspend fun updateReminderSettings(enabled: Boolean, hour: Int, minute: Int)

    suspend fun updateBiometricEnabled(enabled: Boolean)

    suspend fun updatePinEnabled(enabled: Boolean)

    suspend fun updateDarkMode(mode: DarkMode)

    suspend fun updateCloudSyncEnabled(enabled: Boolean)

    suspend fun updateLastSyncTime(timestamp: Long)

    suspend fun updateCustomTasks(tasks: List<String>)

    suspend fun updateHeight(height: Int?)

    suspend fun updateWeight(weight: Float?)

    suspend fun updateCurrentDeviceName(name: String?)

    suspend fun updateCurrentDeviceSize(size: String?)

    suspend fun updateNickname(name: String?)

    suspend fun setOnboardingCompleted(completed: Boolean)

    suspend fun updateMorningReminderSettings(enabled: Boolean, hour: Int, minute: Int)

    suspend fun updatePhotoBlurEnabled(enabled: Boolean)

    suspend fun updateLanguage(language: AppLanguage)

    suspend fun updateCardThemeId(themeId: String)

    suspend fun setSponsorUnlocked(unlocked: Boolean)
}
