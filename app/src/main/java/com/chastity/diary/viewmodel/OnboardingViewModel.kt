package com.chastity.diary.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chastity.diary.data.datastore.PreferencesManager
import com.chastity.diary.data.repository.SettingsRepository
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

    val biometricEnabled = MutableStateFlow(false)
    val pinEnabled = MutableStateFlow(false)

    val reminderEnabled = MutableStateFlow(true)
    val reminderHour = MutableStateFlow(Constants.DEFAULT_REMINDER_HOUR)
    val reminderMinute = MutableStateFlow(Constants.DEFAULT_REMINDER_MINUTE)

    // ── Actions ────────────────────────────────────────────────────────────────

    /** Write all collected data to DataStore and mark onboarding complete. */
    fun completeOnboarding() {
        viewModelScope.launch {
            if (nickname.value.isNotBlank()) repository.updateNickname(nickname.value)
            repository.updateGender(gender.value)
            startDate.value?.let { repository.updateStartDate(it) }
            if (deviceName.value.isNotBlank()) repository.updateCurrentDeviceName(deviceName.value)
            height.value?.let { repository.updateHeight(it) }
            weight.value?.let { repository.updateWeight(it) }
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
