package com.chastity.diary.data.local.dao

import androidx.room.*
import com.chastity.diary.data.local.entity.DailyEntryEntity
import com.chastity.diary.data.local.entity.DailyEntryWithAttributes
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Data Access Object for daily entries
 */
@Dao
interface DailyEntryDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: DailyEntryEntity): Long
    
    @Update
    suspend fun update(entry: DailyEntryEntity)
    
    @Delete
    suspend fun delete(entry: DailyEntryEntity)
    
    @Query("SELECT * FROM daily_entries WHERE id = :id")
    suspend fun getById(id: Long): DailyEntryEntity?
    
    @Query("SELECT * FROM daily_entries WHERE date = :date")
    suspend fun getByDate(date: LocalDate): DailyEntryEntity?

    // C-2: @Relation fetches entry + attributes in one @Transaction, replacing two sequential queries.
    @Transaction
    @Query("SELECT * FROM daily_entries WHERE date = :date")
    suspend fun getByDateWithAttributes(date: LocalDate): DailyEntryWithAttributes?

    @Query("SELECT * FROM daily_entries WHERE date = :date")
    fun getByDateFlow(date: LocalDate): Flow<DailyEntryEntity?>
    
    @Query("SELECT * FROM daily_entries ORDER BY date DESC")
    fun getAllEntries(): Flow<List<DailyEntryEntity>>

    @Query("SELECT * FROM daily_entries ORDER BY date ASC")
    suspend fun getAllEntriesSync(): List<DailyEntryEntity>
    
    @Query("SELECT * FROM daily_entries WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getEntriesInRange(startDate: LocalDate, endDate: LocalDate): Flow<List<DailyEntryEntity>>
    
    @Query("SELECT * FROM daily_entries WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    suspend fun getEntriesInRangeSync(startDate: LocalDate, endDate: LocalDate): List<DailyEntryEntity>
    
    @Query("SELECT COUNT(*) FROM daily_entries")
    suspend fun getTotalCount(): Int
    
    @Query("SELECT COUNT(*) FROM daily_entries WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getCountInRange(startDate: LocalDate, endDate: LocalDate): Int
    
    @Query("SELECT * FROM daily_entries ORDER BY date DESC LIMIT 1")
    suspend fun getLatestEntry(): DailyEntryEntity?
    
    @Query("DELETE FROM daily_entries")
    suspend fun deleteAll()
    
    // Statistics queries
    @Query("SELECT AVG(desireLevel) FROM daily_entries WHERE desireLevel IS NOT NULL AND date BETWEEN :startDate AND :endDate")
    suspend fun getAverageDesireLevel(startDate: LocalDate, endDate: LocalDate): Float?
    
    @Query("SELECT AVG(comfortRating) FROM daily_entries WHERE comfortRating IS NOT NULL AND date BETWEEN :startDate AND :endDate")
    suspend fun getAverageComfortRating(startDate: LocalDate, endDate: LocalDate): Float?
    
    @Query("SELECT COUNT(*) FROM daily_entries WHERE viewedPorn = 1 AND date BETWEEN :startDate AND :endDate")
    suspend fun getPornViewCount(startDate: LocalDate, endDate: LocalDate): Int
    
    @Query("SELECT COALESCE(SUM(CASE WHEN masturbationCount IS NOT NULL THEN masturbationCount WHEN masturbated = 1 THEN 1 ELSE 0 END), 0) FROM daily_entries WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getMasturbationCount(startDate: LocalDate, endDate: LocalDate): Int
    
    @Query("SELECT COUNT(*) FROM daily_entries WHERE exercised = 1 AND date BETWEEN :startDate AND :endDate")
    suspend fun getExerciseCount(startDate: LocalDate, endDate: LocalDate): Int
}
