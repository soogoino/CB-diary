package com.chastity.diary.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * C-2: Room POJO combining DailyEntryEntity + its EAV attributes in a single @Transaction query.
 * Replaces the previous two-step sequential lookup (getByDate → getForEntry) with one DB round-trip.
 */
data class DailyEntryWithAttributes(
    @Embedded val entry: DailyEntryEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "entryId"
    )
    val attributes: List<DailyEntryAttributeEntity>
)
