package com.chastity.diary.data.local.entity

import androidx.room.Entity

/**
 * EAV (Entity-Attribute-Value) table for rotating question answers.
 * Keys match [com.chastity.diary.domain.model.RotatingQuestion.key].
 * Values are stored as Strings ("true"/"false" for boolean, numeric for counts, etc.).
 */
@Entity(
    tableName = "daily_entry_attributes",
    primaryKeys = ["entryId", "attributeKey"]
)
data class DailyEntryAttributeEntity(
    val entryId: Long,
    val attributeKey: String,
    val attributeValue: String
)
