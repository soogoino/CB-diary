package com.chastity.diary.data.repository

import com.chastity.diary.data.local.dao.DailyEntryAttributeDao
import com.chastity.diary.data.local.dao.DailyEntryDao
import com.chastity.diary.data.local.entity.DailyEntryAttributeEntity
import com.chastity.diary.data.local.entity.toDomainModel
import com.chastity.diary.data.local.entity.toEntity
import com.chastity.diary.domain.model.DailyEntry
import com.chastity.diary.domain.repository.IEntryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * Repository for daily entries
 */
class EntryRepository(
    private val dao: DailyEntryDao,
    private val attributeDao: DailyEntryAttributeDao
) : IEntryRepository {
    
    override fun getAllEntries(): Flow<List<DailyEntry>> {
        return dao.getAllEntries().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getAllEntriesSync(): List<DailyEntry> {
        return dao.getAllEntriesSync().map { it.toDomainModel() }
    }
    
    override fun getEntriesInRange(startDate: LocalDate, endDate: LocalDate): Flow<List<DailyEntry>> {
        return dao.getEntriesInRange(startDate, endDate).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    override suspend fun getEntriesInRangeSync(
        startDate: LocalDate, 
        endDate: LocalDate
    ): List<DailyEntry> {
        return dao.getEntriesInRangeSync(startDate, endDate)
            .map { it.toDomainModel() }
    }
    
    override suspend fun getEntryByDate(date: LocalDate): DailyEntry? {
        // C-2: Single @Transaction query replaces two sequential round-trips (getByDate + getForEntry)
        val result = dao.getByDateWithAttributes(date) ?: return null
        val rotatingAnswers = result.attributes.associate { it.attributeKey to it.attributeValue }
        return result.entry.toDomainModel().copy(rotatingAnswers = rotatingAnswers)
    }
    
    override fun getEntryByDateFlow(date: LocalDate): Flow<DailyEntry?> {
        return dao.getByDateFlow(date).map { it?.toDomainModel() }
    }
    
    override suspend fun insertEntry(entry: DailyEntry): Long {
        val id = dao.insert(entry.toEntity())
        if (entry.rotatingAnswers.isNotEmpty()) {
            attributeDao.upsertAll(entry.rotatingAnswers.map { (k, v) ->
                DailyEntryAttributeEntity(entryId = id, attributeKey = k, attributeValue = v)
            })
        }
        return id
    }
    
    override suspend fun updateEntry(entry: DailyEntry) {
        dao.update(entry.toEntity())
        attributeDao.deleteForEntry(entry.id)
        if (entry.rotatingAnswers.isNotEmpty()) {
            attributeDao.upsertAll(entry.rotatingAnswers.map { (k, v) ->
                DailyEntryAttributeEntity(entryId = entry.id, attributeKey = k, attributeValue = v)
            })
        }
    }
    
    override suspend fun deleteEntry(entry: DailyEntry) {
        attributeDao.deleteForEntry(entry.id)
        dao.delete(entry.toEntity())
    }
    
    override suspend fun getTotalCount(): Int {
        return dao.getTotalCount()
    }
    
    override suspend fun getCountInRange(startDate: LocalDate, endDate: LocalDate): Int {
        return dao.getCountInRange(startDate, endDate)
    }
    
    override suspend fun getLatestEntry(): DailyEntry? {
        return dao.getLatestEntry()?.toDomainModel()
    }
    
    // Statistics
    override suspend fun getAverageDesireLevel(startDate: LocalDate, endDate: LocalDate): Float? {
        return dao.getAverageDesireLevel(startDate, endDate)
    }
    
    override suspend fun getAverageComfortRating(startDate: LocalDate, endDate: LocalDate): Float? {
        return dao.getAverageComfortRating(startDate, endDate)
    }
    
    override suspend fun getPornViewCount(startDate: LocalDate, endDate: LocalDate): Int {
        return dao.getPornViewCount(startDate, endDate)
    }
    
    override suspend fun getMasturbationCount(startDate: LocalDate, endDate: LocalDate): Int {
        return dao.getMasturbationCount(startDate, endDate)
    }
    
    override suspend fun getExerciseCount(startDate: LocalDate, endDate: LocalDate): Int {
        return dao.getExerciseCount(startDate, endDate)
    }
}
