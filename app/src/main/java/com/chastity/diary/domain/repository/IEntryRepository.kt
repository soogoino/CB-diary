package com.chastity.diary.domain.repository

import com.chastity.diary.domain.model.DailyEntry
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Domain-layer contract for daily entry persistence.
 * Concrete implementation: [com.chastity.diary.data.repository.EntryRepository]
 */
interface IEntryRepository {

    fun getAllEntries(): Flow<List<DailyEntry>>

    suspend fun getAllEntriesSync(): List<DailyEntry>

    fun getEntriesInRange(startDate: LocalDate, endDate: LocalDate): Flow<List<DailyEntry>>

    suspend fun getEntriesInRangeSync(startDate: LocalDate, endDate: LocalDate): List<DailyEntry>

    suspend fun getEntryByDate(date: LocalDate): DailyEntry?

    fun getEntryByDateFlow(date: LocalDate): Flow<DailyEntry?>

    suspend fun insertEntry(entry: DailyEntry): Long

    suspend fun updateEntry(entry: DailyEntry)

    suspend fun deleteEntry(entry: DailyEntry)

    suspend fun getTotalCount(): Int

    suspend fun getCountInRange(startDate: LocalDate, endDate: LocalDate): Int

    suspend fun getLatestEntry(): DailyEntry?

    // Statistics

    suspend fun getAverageDesireLevel(startDate: LocalDate, endDate: LocalDate): Float?

    suspend fun getAverageComfortRating(startDate: LocalDate, endDate: LocalDate): Float?

    suspend fun getPornViewCount(startDate: LocalDate, endDate: LocalDate): Int

    suspend fun getMasturbationCount(startDate: LocalDate, endDate: LocalDate): Int

    suspend fun getExerciseCount(startDate: LocalDate, endDate: LocalDate): Int
}
