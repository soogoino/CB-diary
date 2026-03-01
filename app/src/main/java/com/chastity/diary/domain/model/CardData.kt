package com.chastity.diary.domain.model

import java.time.LocalDate

/**
 * Aggregated data for a shareable summary card.
 *
 * Combines today's [DailyEntry] fields with 7-day rolling averages from the dashboard
 * and streak info from [StreakRepository].
 *
 * Fields marked "sensitive" are hidden by default and only included when
 * [showSensitiveData] = true (user opt-in in card bottom-sheet).
 */
data class CardData(
    val date: LocalDate,

    // ── Streak ───────────────────────────────────────────────────────────────
    val currentStreak: Int,
    val longestStreak: Int,

    // ── Today fields ─────────────────────────────────────────────────────────
    val morningMood: String?,        // emoji / label
    val morningEnergy: Int?,         // 1-10
    val selfRating: Int?,            // 1-5 (Q23)
    val exercised: Boolean,
    val exposedDevice: Boolean,      // sensitive – only shown when opt-in

    // ── Rotating question answer (最新一題輪換題） ────────────────────────────
    val rotatingQuestionLabel: String?,
    val rotatingQuestionAnswer: String?,

    // ── 7-day rolling averages ────────────────────────────────────────────────
    val avg7Desire: Float,           // Q8
    val avg7Comfort: Float,          // Q9
    val avg7Focus: Float,            // Q18
    val avg7Sleep: Float,            // Q15

    // ── Privacy toggle ────────────────────────────────────────────────────────
    /** When false, [exposedDevice] is masked in the rendered card. */
    val showSensitiveData: Boolean = false
)
