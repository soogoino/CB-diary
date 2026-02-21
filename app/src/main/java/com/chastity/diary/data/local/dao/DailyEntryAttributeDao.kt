package com.chastity.diary.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.chastity.diary.data.local.entity.DailyEntryAttributeEntity

/**
 * DAO for the EAV daily_entry_attributes table.
 */
@Dao
interface DailyEntryAttributeDao {

    /** Upsert a single attribute (insert or replace on PK collision). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(attr: DailyEntryAttributeEntity)

    /** Upsert many attributes at once (e.g., after saving an entry). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(attrs: List<DailyEntryAttributeEntity>)

    /** Retrieve all attribute rows for a given entry. */
    @Query("SELECT * FROM daily_entry_attributes WHERE entryId = :entryId")
    suspend fun getForEntry(entryId: Long): List<DailyEntryAttributeEntity>

    /** Delete all attributes tied to an entry (called before deleting the entry). */
    @Query("DELETE FROM daily_entry_attributes WHERE entryId = :entryId")
    suspend fun deleteForEntry(entryId: Long)
}
