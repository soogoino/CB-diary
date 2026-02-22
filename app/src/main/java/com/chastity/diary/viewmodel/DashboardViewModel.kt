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
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * ViewModel for dashboard/statistics
 *
 * D-2: Reactive Flow pipeline — getAllEntries() is a Room-backed Flow that auto-emits
 * whenever the database changes. combine() with timeRange means DashboardState updates
 * instantly after any insert/update/delete, with no manual refresh needed.
 * Stats (avgDesire, pornCount, etc.) are computed in-memory from the entry list,
 * eliminating 6 redundant DB round-trips from the old suspend-based loadStatistics().
 */
class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = AppDatabase.getInstance(application)
    private val repository = EntryRepository(database.dailyEntryDao(), database.dailyEntryAttributeDao())
    private val preferencesManager = PreferencesManager(application)
    private val streakRepository = StreakRepository(preferencesManager)
    
    private val _timeRange = MutableStateFlow(TimeRange.WEEK)
    val timeRange: StateFlow<TimeRange> = _timeRange.asStateFlow()

    // H-1: All entries as reactive stream — used by HistoryScreen calendar (needs full history,
    // not just the selected time range).
    val allEntries: StateFlow<List<DailyEntry>> = repository.getAllEntries()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // D-2: Reactive dashboard state — auto-refreshes whenever DB changes OR time range changes.
    // Stats are derived in-memory; no extra DB queries needed.
    val dashboardState: StateFlow<DashboardState> = combine(
        _timeRange,
        repository.getAllEntries()
    ) { range, allEntries ->
        val (startDate, endDate) = getDateRange(range)
        val rangeEntries = allEntries.filter {
            !it.date.isBefore(startDate) && !it.date.isAfter(endDate)
        }
        val totalDaysInRange = ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1
        val completionRate = if (totalDaysInRange > 0)
            rangeEntries.size.toFloat() / totalDaysInRange * 100f else 0f

        // 型別明確宣告為 DashboardState 供 Kotlin 推斷 combine 泛型 R = DashboardState，
        // 否則推斷為 DashboardState.Success 導致 .catch { emit(Error) } 型別不符。
        val result: DashboardState = DashboardState.Success(
            totalDays        = allEntries.size,
            completionRate   = completionRate,
            entries          = rangeEntries,
            averageDesireLevel  = rangeEntries.mapNotNull { it.desireLevel?.toFloat() }
                .average().let { if (it.isNaN()) 0f else it.toFloat() },
            averageComfortRating = rangeEntries.mapNotNull { it.comfortRating?.toFloat() }
                .average().let { if (it.isNaN()) 0f else it.toFloat() },
            pornViewCount       = rangeEntries.count { it.viewedPorn },
            masturbationCount   = rangeEntries.count { it.masturbated },
            exerciseCount       = rangeEntries.count { it.exercised }
        )
        result
    }
    .catch { e -> emit(DashboardState.Error(e.message ?: "載入失敗")) }
    .stateIn(viewModelScope, SharingStarted.Lazily, DashboardState.Loading)
    
    val currentStreak = streakRepository.currentStreak
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)
    
    val longestStreak = streakRepository.longestStreak
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)
    
    fun setTimeRange(range: TimeRange) {
        _timeRange.value = range
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
        val exerciseCount: Int
    ) : DashboardState()
    data class Error(val message: String) : DashboardState()
}
