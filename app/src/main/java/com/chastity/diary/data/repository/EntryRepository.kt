package com.chastity.diary.data.repository

import com.chastity.diary.data.local.dao.DailyEntryDao
import com.chastity.diary.data.local.entity.toDomainModel
import com.chastity.diary.data.local.entity.toEntity
import com.chastity.diary.domain.model.DailyEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * Repository for daily entries
 */
class EntryRepository(private val dao: DailyEntryDao) {
    
    fun getAllEntries(): Flow<List<DailyEntry>> {
        return dao.getAllEntries().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    suspend fun getAllEntriesSync(): List<DailyEntry> {
        return dao.getAllEntriesSync().map { it.toDomainModel() }
    }
    
    fun getEntriesInRange(startDate: LocalDate, endDate: LocalDate): Flow<List<DailyEntry>> {
        return dao.getEntriesInRange(startDate, endDate).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    suspend fun getEntriesInRangeSync(
        startDate: LocalDate, 
        endDate: LocalDate
    ): List<DailyEntry> {
        return dao.getEntriesInRangeSync(startDate, endDate)
            .map { it.toDomainModel() }
    }
    
    suspend fun getEntryByDate(date: LocalDate): DailyEntry? {
        return dao.getByDate(date)?.toDomainModel()
    }
    
    fun getEntryByDateFlow(date: LocalDate): Flow<DailyEntry?> {
        return dao.getByDateFlow(date).map { it?.toDomainModel() }
    }
    
    suspend fun insertEntry(entry: DailyEntry): Long {
        return dao.insert(entry.toEntity())
    }
    
    suspend fun updateEntry(entry: DailyEntry) {
        dao.update(entry.toEntity())
    }
    
    suspend fun deleteEntry(entry: DailyEntry) {
        dao.delete(entry.toEntity())
    }
    
    suspend fun getTotalCount(): Int {
        return dao.getTotalCount()
    }
    
    suspend fun getCountInRange(startDate: LocalDate, endDate: LocalDate): Int {
        return dao.getCountInRange(startDate, endDate)
    }
    
    suspend fun getLatestEntry(): DailyEntry? {
        return dao.getLatestEntry()?.toDomainModel()
    }
    
    // Statistics
    suspend fun getAverageDesireLevel(startDate: LocalDate, endDate: LocalDate): Float? {
        return dao.getAverageDesireLevel(startDate, endDate)
    }
    
    suspend fun getAverageComfortRating(startDate: LocalDate, endDate: LocalDate): Float? {
        return dao.getAverageComfortRating(startDate, endDate)
    }
    
    suspend fun getPornViewCount(startDate: LocalDate, endDate: LocalDate): Int {
        return dao.getPornViewCount(startDate, endDate)
    }
    
    suspend fun getMasturbationCount(startDate: LocalDate, endDate: LocalDate): Int {
        return dao.getMasturbationCount(startDate, endDate)
    }
    
    suspend fun getExerciseCount(startDate: LocalDate, endDate: LocalDate): Int {
        return dao.getExerciseCount(startDate, endDate)
    }
}
