package com.chastity.diary.domain.model

import java.time.LocalDate

/**
 * Aggregated data for a shareable summary card.
 *
 * Combines today's [DailyEntry] fields with 7-day rolling averages from the dashboard
 * and streak info from [StreakRepository].
 */
data class CardData(
    val date: LocalDate,

    // ── Streak ───────────────────────────────────────────────────────────────
    val currentStreak: Int,
    val longestStreak: Int,

    // ── Today fields ─────────────────────────────────────────────────────────
    val morningMood: String?,        // emoji / label
    val morningEnergy: Int?,         // 1-10
    val exercised: Boolean,

    // ── Photo (optional, user opt-in) ─────────────────────────────────────────
    /** Absolute file path of today's photo, null if no photo was taken. */
    val photoPath: String?,
    /** Whether the user has opted to show the photo on the card. */
    val showPhoto: Boolean = false,

    // ── Rotating question answers (all answered questions for today) ──────────────
    /**
     * Raw (key, rawAnswer) pairs from [DailyEntry.rotatingAnswers].
     * key = "R1" etc., rawAnswer = "true" / "false" / free text.
     * String resolution to the current locale is done in the Composable layer.
     */
    val rotatingQuestions: List<Pair<String, String>> = emptyList(),

    // ── Today's individual ratings ──────────────────────────────────────────
    val todayDesire: Int?,           // Q8
    val todayComfort: Int?,          // Q9
    val todayFocus: Int?,            // Q18
    val todaySleep: Int?,            // Q15

    // ── 7-day rolling averages ────────────────────────────────────────────────
    val avg7Desire: Float,           // Q8
    val avg7Comfort: Float,          // Q9
    val avg7Focus: Float,            // Q18
    val avg7Sleep: Float,            // Q15
)
