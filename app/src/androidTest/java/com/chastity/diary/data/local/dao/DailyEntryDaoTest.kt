package com.chastity.diary.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chastity.diary.data.local.database.AppDatabase
import com.chastity.diary.data.local.entity.DailyEntryEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.time.LocalDate

/**
 * Instrumented tests for [DailyEntryDao].
 *
 * Uses an in-memory Room database so no data is persisted to disk
 * and each test starts with a clean slate.
 */
@RunWith(AndroidJUnit4::class)
class DailyEntryDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: DailyEntryDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.dailyEntryDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun entry(
        date: LocalDate = LocalDate.now(),
        mood: String? = "😊 開心"
    ) = DailyEntryEntity(date = date, mood = mood)

    // ── Insert / Read ─────────────────────────────────────────────────────────

    @Test
    fun insertAndRetrieveByDate() = runTest {
        val today = LocalDate.now()
        dao.insert(entry(date = today, mood = "😌 平靜"))

        val result = dao.getByDate(today)
        assertNotNull("Expected entry for today", result)
        assertEquals("😌 平靜", result!!.mood)
    }

    @Test
    fun insertReturnsPositiveId() = runTest {
        val id = dao.insert(entry())
        assertTrue("Auto-generated ID should be > 0", id > 0)
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Test
    fun updateModifiesExistingEntry() = runTest {
        val today = LocalDate.now()
        val id = dao.insert(entry(date = today, mood = "😐 普通"))

        val updated = entry(date = today, mood = "💪 充實").copy(id = id)
        dao.update(updated)

        val result = dao.getByDate(today)
        assertEquals("💪 充實", result!!.mood)
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @Test
    fun deleteRemovesEntry() = runTest {
        val today = LocalDate.now()
        val id = dao.insert(entry(date = today))
        val toDel = entry(date = today).copy(id = id)
        dao.delete(toDel)

        val result = dao.getByDate(today)
        assertNull("Entry should be deleted", result)
    }

    // ── getAllEntries Flow ─────────────────────────────────────────────────────

    @Test
    fun getAllEntriesReflectsInserts() = runTest {
        dao.insert(entry(LocalDate.now().minusDays(1)))
        dao.insert(entry(LocalDate.now()))

        val all = dao.getAllEntries().first()
        assertEquals(2, all.size)
    }

    // ── getTotalCount ─────────────────────────────────────────────────────────

    @Test
    fun totalCountMatchesInsertedRows() = runTest {
        repeat(5) { i ->
            dao.insert(entry(date = LocalDate.now().minusDays(i.toLong())))
        }
        assertEquals(5, dao.getTotalCount())
    }

    // ── getLatestEntry ────────────────────────────────────────────────────────

    @Test
    fun latestEntryReturnsNewestDate() = runTest {
        val older  = LocalDate.now().minusDays(3)
        val newest = LocalDate.now()

        dao.insert(entry(date = older,  mood = "😴 無聊"))
        dao.insert(entry(date = newest, mood = "🤩 期待"))

        val latest = dao.getLatestEntry()
        assertNotNull(latest)
        assertEquals(newest, latest!!.date)
        assertEquals("🤩 期待", latest.mood)
    }

    // ── getEntriesInRange ─────────────────────────────────────────────────────

    @Test
    fun rangeQueryReturnsOnlyDatesWithinRange() = runTest {
        val base = LocalDate.of(2026, 1, 1)
        for (i in 0..9) dao.insert(entry(date = base.plusDays(i.toLong())))

        val start = base.plusDays(3)
        val end   = base.plusDays(6)
        val inRange = dao.getEntriesInRangeSync(start, end)

        assertEquals(4, inRange.size)
        assertTrue(inRange.all { it.date in start..end })
    }
}
