package com.chastity.diary.domain.model

import java.time.LocalDate

/**
 * User settings and preferences
 */
data class UserSettings(
    val gender: Gender = Gender.MALE,
    val startDate: LocalDate? = null,
    val reminderEnabled: Boolean = true,
    val reminderHour: Int = 21,
    val reminderMinute: Int = 0,
    val biometricEnabled: Boolean = false,
    val pinEnabled: Boolean = false,
    val darkMode: DarkMode = DarkMode.SYSTEM,
    val cloudSyncEnabled: Boolean = false,
    val lastSyncTime: Long? = null,
    val customTasks: List<String> = emptyList(),
    
    // Personal profile (optional)
    val nickname: String? = null,         // Display name
    val height: Int? = null,              // Height in cm (100-250)
    val weight: Float? = null,            // Weight in kg (30-200)
    val currentDeviceName: String? = null, // Chastity device name
    val currentDeviceSize: String? = null,  // Chastity device size (free text, e.g. "S", "38mm")

    // Morning check-in reminder
    val morningReminderEnabled: Boolean = false,
    val morningReminderHour: Int = 7,
    val morningReminderMinute: Int = 30,

    // Photo
    val photoBlurEnabled: Boolean = true
) {
    /**
     * Calculate BMI if height and weight are available
     * BMI = weight(kg) / (height(m))²
     */
    val bmi: Float?
        get() = if (height != null && weight != null && height > 0) {
            weight / ((height / 100f) * (height / 100f))
        } else null
    
    /**
     * Get BMI status category
     */
    val bmiStatus: String?
        get() = bmi?.let {
            when {
                it < 18.5 -> "過輕"
                it < 24.0 -> "正常"
                it < 27.0 -> "過重"
                it < 30.0 -> "輕度肥胖"
                it < 35.0 -> "中度肥胖"
                else -> "重度肥胖"
            }
        }
}

enum class Gender {
    MALE, FEMALE, OTHER
}

enum class DarkMode {
    LIGHT, DARK, SYSTEM
}
