package com.chastity.diary.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chastity.diary.data.datastore.PreferencesManager
import com.chastity.diary.data.local.database.AppDatabase
import com.chastity.diary.data.repository.EntryRepository
import com.chastity.diary.data.repository.StreakRepository
import com.chastity.diary.domain.model.DailyEntry
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * ViewModel for dashboard/statistics
 */
class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = AppDatabase.getInstance(application)
    private val repository = EntryRepository(database.dailyEntryDao(), database.dailyEntryAttributeDao())
    private val preferencesManager = PreferencesManager(application)
    private val streakRepository = StreakRepository(preferencesManager)
    
    private val _timeRange = MutableStateFlow(TimeRange.WEEK)
    val timeRange: StateFlow<TimeRange> = _timeRange.asStateFlow()
    
    private val _dashboardState = MutableStateFlow<DashboardState>(DashboardState.Loading)
    val dashboardState: StateFlow<DashboardState> = _dashboardState.asStateFlow()
    
    val currentStreak = streakRepository.currentStreak
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)
    
    val longestStreak = streakRepository.longestStreak
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)
    
    init {
        loadStatistics()
    }
    
    fun setTimeRange(range: TimeRange) {
        _timeRange.value = range
        loadStatistics()
    }
    
    private fun loadStatistics() {
        viewModelScope.launch {
            try {
                val (startDate, endDate) = getDateRange(_timeRange.value)
                
                val entries = repository.getEntriesInRangeSync(startDate, endDate)
                val totalEntries = repository.getTotalCount()
                val totalDaysInRange = ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1
                val completionRate = if (totalDaysInRange > 0) {
                    (entries.size.toFloat() / totalDaysInRange) * 100
                } else 0f
                
                //  Statistics
                val avgDesire = repository.getAverageDesireLevel(startDate, endDate) ?: 0f
                val avgComfort = repository.getAverageComfortRating(startDate, endDate) ?: 0f
                val pornCount = repository.getPornViewCount(startDate, endDate)
                val masturbationCount = repository.getMasturbationCount(startDate, endDate)
                val exerciseCount = repository.getExerciseCount(startDate, endDate)
                
                _dashboardState.value = DashboardState.Success(
                    totalDays = totalEntries,
                    completionRate = completionRate,
                    entries = entries,
                    averageDesireLevel = avgDesire,
                    averageComfortRating = avgComfort,
                    pornViewCount = pornCount,
                    masturbationCount = masturbationCount,
                    exerciseCount = exerciseCount,
                    moodTrend = entries.mapNotNull { getMoodScore(it.mood) }
                )
            } catch (e: Exception) {
                _dashboardState.value = DashboardState.Error(e.message ?: "載入失敗")
            }
        }
    }
    
    private fun getDateRange(range: TimeRange): Pair<LocalDate, LocalDate> {
        val end = LocalDate.now()
        val start = when (range) {
            TimeRange.WEEK -> end.minusWeeks(1)
            TimeRange.MONTH -> end.minusMonths(1)
            TimeRange.THREE_MONTHS -> end.minusMonths(3)
            TimeRange.ALL -> end.minusYears(10) // Arbitrary old date
        }
        return start to end
    }
    
    /**
     * Convert mood string to numeric score for charting
     */
    private fun getMoodScore(mood: String?): Float? {
        if (mood == null) return null
        return when (mood) {
            "開心" -> 5f
            "平靜" -> 4f
            "普通" -> 3f
            "沮喪" -> 2f
            "焦慮" -> 1.5f
            "挫折" -> 1f
            else -> null
        }
    }
}

enum class TimeRange {
    WEEK, MONTH, THREE_MONTHS, ALL
}

sealed class DashboardState {
    object Loading : DashboardState()
    data class Success(
        val totalDays: Int,
        val completionRate: Float,
        val entries: List<DailyEntry>,
        val averageDesireLevel: Float,
        val averageComfortRating: Float,
        val pornViewCount: Int,
        val masturbationCount: Int,
        val exerciseCount: Int,
        val moodTrend: List<Float>
    ) : DashboardState()
    data class Error(val message: String) : DashboardState()
}
