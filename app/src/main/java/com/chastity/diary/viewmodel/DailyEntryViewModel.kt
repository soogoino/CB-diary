package com.chastity.diary.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chastity.diary.data.datastore.PreferencesManager
import com.chastity.diary.data.local.database.AppDatabase
import com.chastity.diary.data.repository.EntryRepository
import com.chastity.diary.data.repository.StreakRepository
import com.chastity.diary.data.repository.SettingsRepository
import com.chastity.diary.domain.model.DailyEntry
import com.chastity.diary.domain.model.FormFlowState
import com.chastity.diary.domain.model.FormStep
import com.chastity.diary.domain.model.generateRotatingQuestionOfDay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * ViewModel for daily entry form
 */
class DailyEntryViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = AppDatabase.getInstance(application)
    private val repository = EntryRepository(database.dailyEntryDao(), database.dailyEntryAttributeDao())
    private val preferencesManager = PreferencesManager(application)
    private val settingsRepository = SettingsRepository(preferencesManager)
    private val streakRepository = StreakRepository(preferencesManager)
    
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()
    
    private val _entryState = MutableStateFlow<EntryFormState>(EntryFormState.Empty)
    val entryState: StateFlow<EntryFormState> = _entryState.asStateFlow()
    
    private val _formFlowState = MutableStateFlow(
        FormFlowState(
            rotatingQuestionOfDay = generateRotatingQuestionOfDay(LocalDate.now())
        )
    )
    val formFlowState: StateFlow<FormFlowState> = _formFlowState.asStateFlow()
    
    val userSettings = settingsRepository.userSettings
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()
    
    private val _deleteSuccess = MutableStateFlow(false)
    val deleteSuccess: StateFlow<Boolean> = _deleteSuccess.asStateFlow()

    // 0 = Morning ☀️, 1 = Evening 🌙
    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    private val _morningSaveSuccess = MutableStateFlow(false)
    val morningSaveSuccess: StateFlow<Boolean> = _morningSaveSuccess.asStateFlow()

    init {
        loadEntryForDate(LocalDate.now())
    }
    
    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        loadEntryForDate(date)
        // Update rotating question for the new date
        _formFlowState.value = _formFlowState.value.copy(
            rotatingQuestionOfDay = generateRotatingQuestionOfDay(date)
        )
    }
    
    /**
     * Navigate to next step in form flow
     */
    fun nextStep() {
        val currentEntry = getCurrentEntry()
        if (_formFlowState.value.canProceedToNextStep(currentEntry)) {
            _formFlowState.value.nextStep()?.let { nextStep ->
                _formFlowState.value = _formFlowState.value.copy(
                    currentStep = nextStep,
                    completedSteps = _formFlowState.value.completedSteps + _formFlowState.value.currentStep
                )
            }
        }
    }
    
    /**
     * Navigate to previous step in form flow
     */
    fun previousStep() {
        _formFlowState.value.previousStep()?.let { prevStep ->
            _formFlowState.value = _formFlowState.value.copy(
                currentStep = prevStep
            )
        }
    }
    
    /**
     * Jump to specific step
     */
    fun goToStep(step: FormStep) {
        _formFlowState.value = _formFlowState.value.copy(
            currentStep = step
        )
    }
    
    /**
     * Get current entry or create new one
     */
    private fun getCurrentEntry(): DailyEntry {
        return when (val state = _entryState.value) {
            is EntryFormState.Loaded -> state.entry
            is EntryFormState.Empty -> DailyEntry(date = _selectedDate.value)
        }
    }
    
    private fun loadEntryForDate(date: LocalDate) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val entry = repository.getEntryByDate(date)
                _entryState.value = if (entry != null) {
                    EntryFormState.Loaded(entry)
                } else {
                    EntryFormState.Empty
                }
            } catch (e: Exception) {
                _errorMessage.value = "載入失敗: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun updateEntry(updater: (DailyEntry) -> DailyEntry) {
        val currentEntry = when (val state = _entryState.value) {
            is EntryFormState.Loaded -> state.entry
            is EntryFormState.Empty -> DailyEntry(date = _selectedDate.value)
        }
        _entryState.value = EntryFormState.Loaded(updater(currentEntry))
    }
    
    fun saveEntry() {
        viewModelScope.launch {
            _isLoading.value = true
            _saveSuccess.value = false
            try {
                val state = _entryState.value
                if (state is EntryFormState.Loaded) {
                    val entry = state.entry.copy(
                        updatedAt = LocalDateTime.now()
                    )
                    
                    if (entry.id == 0L) {
                        repository.insertEntry(entry)
                    } else {
                        repository.updateEntry(entry)
                    }
                    
                    // Update streak
                    streakRepository.updateStreak(entry.date)
                    
                    _saveSuccess.value = true
                }
            } catch (e: Exception) {
                _errorMessage.value = "儲存失敗: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
    
    fun clearSaveSuccess() {
        _saveSuccess.value = false
    }

    fun selectTab(index: Int) {
        _currentTab.value = index
    }

    fun clearMorningSaveSuccess() {
        _morningSaveSuccess.value = false
    }

    /** Save only the morning check-in fields and mark morningCheckDone = true */
    fun saveMorningCheck() {
        viewModelScope.launch {
            _isLoading.value = true
            _morningSaveSuccess.value = false
            try {
                val currentEntry = getCurrentEntry()
                val entry = currentEntry.copy(
                    morningCheckDone = true,
                    updatedAt = LocalDateTime.now()
                )
                if (entry.id == 0L) {
                    val newId = repository.insertEntry(entry)
                    _entryState.value = EntryFormState.Loaded(entry.copy(id = newId))
                } else {
                    repository.updateEntry(entry)
                    _entryState.value = EntryFormState.Loaded(entry)
                }
                streakRepository.updateStreak(entry.date)
                _morningSaveSuccess.value = true
            } catch (e: Exception) {
                _errorMessage.value = "早晨記錄失敗: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun deleteEntry() {
        viewModelScope.launch {
            _isLoading.value = true
            _deleteSuccess.value = false
            try {
                val state = _entryState.value
                if (state is EntryFormState.Loaded) {
                    val entry = state.entry
                    if (entry.id != 0L) {
                        repository.deleteEntry(entry)
                        // Reset to empty state and reload today's date
                        _entryState.value = EntryFormState.Empty
                        _selectedDate.value = LocalDate.now()
                        loadEntryForDate(LocalDate.now())
                        _deleteSuccess.value = true
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "刪除失敗: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun clearDeleteSuccess() {
        _deleteSuccess.value = false
    }
}

sealed class EntryFormState {
    object Empty : EntryFormState()
    data class Loaded(val entry: DailyEntry) : EntryFormState()
}
