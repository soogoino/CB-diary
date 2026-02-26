package com.chastity.diary

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Pure JVM unit tests for streak calculation logic.
 *
 * The production implementation lives in data/repository/StreakRepository.kt.
 * These tests verify the core algorithmic behaviour without any Android or
 * DataStore dependencies.
 */
class StreakCalculationTest {

    /**
     * Extract of the streak computation logic from StreakRepository.updateStreak().
     * Keeping it here allows fast JVM tests without mocking DataStore.
     */
    private fun computeNewStreak(
        currentStreak: Int,
        lastEntryDate: LocalDate?,
        entryDate: LocalDate
    ): Int = when {
        lastEntryDate == null -> 1
        lastEntryDate == entryDate -> currentStreak
        ChronoUnit.DAYS.between(lastEntryDate, entryDate) == 1L -> currentStreak + 1
        else -> 1
    }

    // ── First entry ────────────────────────────────────────────────────────────

    @Test
    fun `first entry with no history starts streak at 1`() {
        val streak = computeNewStreak(
            currentStreak = 0,
            lastEntryDate = null,
            entryDate = LocalDate.now()
        )
        assertEquals(1, streak)
    }

    // ── Same-day re-save ───────────────────────────────────────────────────────

    @Test
    fun `re-saving on the same day does not change streak`() {
        val today = LocalDate.now()
        val streak = computeNewStreak(
            currentStreak = 5,
            lastEntryDate = today,
            entryDate = today
        )
        assertEquals(5, streak)
    }

    // ── Consecutive days ──────────────────────────────────────────────────────

    @Test
    fun `consecutive day increments streak`() {
        val yesterday = LocalDate.now().minusDays(1)
        val streak = computeNewStreak(
            currentStreak = 7,
            lastEntryDate = yesterday,
            entryDate = LocalDate.now()
        )
        assertEquals(8, streak)
    }

    @Test
    fun `streak grows correctly over multiple consecutive days`() {
        val base = LocalDate.of(2026, 1, 1)
        var streak = 0
        var lastDate: LocalDate? = null
        repeat(30) { i ->
            val d = base.plusDays(i.toLong())
            streak = computeNewStreak(streak, lastDate, d)
            lastDate = d
        }
        assertEquals(30, streak)
    }

    // ── Broken streak ─────────────────────────────────────────────────────────

    @Test
    fun `missing one day resets streak to 1`() {
        val twoDaysAgo = LocalDate.now().minusDays(2)
        val streak = computeNewStreak(
            currentStreak = 15,
            lastEntryDate = twoDaysAgo,
            entryDate = LocalDate.now()
        )
        assertEquals(1, streak)
    }

    @Test
    fun `large gap resets streak to 1`() {
        val longAgo = LocalDate.now().minusDays(30)
        val streak = computeNewStreak(
            currentStreak = 100,
            lastEntryDate = longAgo,
            entryDate = LocalDate.now()
        )
        assertEquals(1, streak)
    }

    // ── Longest streak invariant ──────────────────────────────────────────────

    @Test
    fun `longest streak is always max of new and previous longest`() {
        val yesterday = LocalDate.now().minusDays(1)
        val newStreak = computeNewStreak(49, yesterday, LocalDate.now())
        val longestStreak = maxOf(newStreak, 50)
        assertEquals(50, longestStreak) // 50 stays because new is only 50 == 50
    }
}
