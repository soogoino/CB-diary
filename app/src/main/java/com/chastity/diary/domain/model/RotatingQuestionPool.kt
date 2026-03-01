package com.chastity.diary.domain.model

import androidx.annotation.StringRes
import com.chastity.diary.R

/**
 * Shared lookup table: rotating question key (e.g. "R1") → display title string resource.
 *
 * Mirrors the private RotatingQuestion enum in DailyEntryScreen. Keep in sync if questions change.
 */
private val KEY_TO_TITLE_RES: Map<String, Int> = mapOf(
    "R1"  to R.string.rq_r1_title,
    "R2"  to R.string.rq_r2_title,
    "R3"  to R.string.rq_r3_title,
    "R4"  to R.string.rq_r4_title,
    "R6"  to R.string.rq_r6_title,
    "R7"  to R.string.rq_r7_title,
    "R8"  to R.string.rq_r8_title,
    "R9"  to R.string.rq_r9_title,
    "R10" to R.string.rq_r10_title,
    "R11" to R.string.rq_r11_title,
    "R12" to R.string.rq_r12_title,
    "R13" to R.string.rq_r13_title,
    "R14" to R.string.rq_r14_title,
    "R15" to R.string.rq_r15_title,
    "R16" to R.string.rq_r16_title,
    "R17" to R.string.rq_r17_title,
    "R18" to R.string.rq_r18_title,
    "R19" to R.string.rq_r19_title,
    "R20" to R.string.rq_r20_title,
    "R21" to R.string.rq_r21_title,
    "R22" to R.string.rq_r22_title,
    "R23" to R.string.rq_r23_title,
    "R24" to R.string.rq_r24_title,
    "R25" to R.string.rq_r25_title,
    "R26" to R.string.rq_r26_title,
    "R27" to R.string.rq_r27_title,
    "R28" to R.string.rq_r28_title,
    "R29" to R.string.rq_r29_title,
    "R30" to R.string.rq_r30_title,
    "R31" to R.string.rq_r31_title,
    "R32" to R.string.rq_r32_title,
    "R33" to R.string.rq_r33_title,
)

/**
 * Returns the [StringRes] for the given rotating question key, or null if not found.
 */
@StringRes
fun rotatingQuestionTitleRes(key: String): Int? = KEY_TO_TITLE_RES[key]
