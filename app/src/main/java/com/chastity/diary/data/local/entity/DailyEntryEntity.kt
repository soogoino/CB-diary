package com.chastity.diary.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.chastity.diary.domain.model.DailyEntry
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Room entity for daily entries
 */
@Entity(tableName = "daily_entries")
@TypeConverters(Converters::class)
data class DailyEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: LocalDate,
    
    // Basic questions
    val mood: String? = null,
    val viewedPorn: Boolean = false,
    val pornDuration: Int? = null,
    val hadErection: Boolean = false,
    val erectionCount: Int? = null,
    val exercised: Boolean = false,
    val exerciseTypes: List<String>? = null,
    val exerciseDuration: Int? = null,
    val unlocked: Boolean = false,
    val masturbated: Boolean = false,
    val masturbationDuration: Int? = null,
    val exposedLock: Boolean = false,
    val exposedLocations: List<String>? = null,
    val photoPath: String? = null,
    
    // Extended questions
    val desireLevel: Int? = null,
    val comfortRating: Int? = null,
    val hasDiscomfort: Boolean = false,
    val discomfortAreas: List<String>? = null,
    val discomfortLevel: Int? = null,
    val cleaningType: String? = null,
    val hadLeakage: Boolean = false,
    val leakageAmount: String? = null,
    val hadEdging: Boolean = false,
    val edgingDuration: Int? = null,
    val edgingMethods: List<String>? = null,
    val keyholderInteraction: Boolean = false,
    val interactionTypes: List<String>? = null,
    val sleepQuality: Int? = null,
    val wokeUpDueToDevice: Boolean = false,
    val temporarilyRemoved: Boolean = false,
    val removalDuration: Int? = null,
    val removalReasons: List<String>? = null,
    val nightErections: Int? = null,
    val wokeUpFromErection: Boolean = false,
    val focusLevel: Int? = null,
    val completedTasks: List<String>? = null,
    val emotions: List<String>? = null,
    val deviceCheckPassed: Boolean = true,
    val socialActivities: List<String>? = null,
    val socialAnxiety: Int? = null,
    val selfRating: Int? = null,
    
    // Optional
    val notes: String? = null,
    
    // Metadata
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

/**
 * Extension function to convert Entity to Domain Model
 */
fun DailyEntryEntity.toDomainModel(): DailyEntry {
    return DailyEntry(
        id = id,
        date = date,
        mood = mood,
        viewedPorn = viewedPorn,
        pornDuration = pornDuration,
        hadErection = hadErection,
        erectionCount = erectionCount,
        exercised = exercised,
        exerciseTypes = exerciseTypes ?: emptyList(),
        exerciseDuration = exerciseDuration,
        unlocked = unlocked,
        masturbated = masturbated,
        masturbationDuration = masturbationDuration,
        exposedLock = exposedLock,
        exposedLocations = exposedLocations ?: emptyList(),
        photoPath = photoPath,
        desireLevel = desireLevel,
        comfortRating = comfortRating,
        hasDiscomfort = hasDiscomfort,
        discomfortAreas = discomfortAreas ?: emptyList(),
        discomfortLevel = discomfortLevel,
        cleaningType = cleaningType,
        hadLeakage = hadLeakage,
        leakageAmount = leakageAmount,
        hadEdging = hadEdging,
        edgingDuration = edgingDuration,
        edgingMethods = edgingMethods ?: emptyList(),
        keyholderInteraction = keyholderInteraction,
        interactionTypes = interactionTypes ?: emptyList(),
        sleepQuality = sleepQuality,
        wokeUpDueToDevice = wokeUpDueToDevice,
        temporarilyRemoved = temporarilyRemoved,
        removalDuration = removalDuration,
        removalReasons = removalReasons ?: emptyList(),
        nightErections = nightErections,
        wokeUpFromErection = wokeUpFromErection,
        focusLevel = focusLevel,
        completedTasks = completedTasks ?: emptyList(),
        emotions = emotions ?: emptyList(),
        deviceCheckPassed = deviceCheckPassed,
        socialActivities = socialActivities ?: emptyList(),
        socialAnxiety = socialAnxiety,
        selfRating = selfRating,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

/**
 * Extension function to convert Domain Model to Entity
 */
fun DailyEntry.toEntity(): DailyEntryEntity {
    return DailyEntryEntity(
        id = id,
        date = date,
        mood = mood,
        viewedPorn = viewedPorn,
        pornDuration = pornDuration,
        hadErection = hadErection,
        erectionCount = erectionCount,
        exercised = exercised,
        exerciseTypes = exerciseTypes.takeIf { it.isNotEmpty() },
        exerciseDuration = exerciseDuration,
        unlocked = unlocked,
        masturbated = masturbated,
        masturbationDuration = masturbationDuration,
        exposedLock = exposedLock,
        exposedLocations = exposedLocations.takeIf { it.isNotEmpty() },
        photoPath = photoPath,
        desireLevel = desireLevel,
        comfortRating = comfortRating,
        hasDiscomfort = hasDiscomfort,
        discomfortAreas = discomfortAreas.takeIf { it.isNotEmpty() },
        discomfortLevel = discomfortLevel,
        cleaningType = cleaningType,
        hadLeakage = hadLeakage,
        leakageAmount = leakageAmount,
        hadEdging = hadEdging,
        edgingDuration = edgingDuration,
        edgingMethods = edgingMethods.takeIf { it.isNotEmpty() },
        keyholderInteraction = keyholderInteraction,
        interactionTypes = interactionTypes.takeIf { it.isNotEmpty() },
        sleepQuality = sleepQuality,
        wokeUpDueToDevice = wokeUpDueToDevice,
        temporarilyRemoved = temporarilyRemoved,
        removalDuration = removalDuration,
        removalReasons = removalReasons.takeIf { it.isNotEmpty() },
        nightErections = nightErections,
        wokeUpFromErection = wokeUpFromErection,
        focusLevel = focusLevel,
        completedTasks = completedTasks.takeIf { it.isNotEmpty() },
        emotions = emotions.takeIf { it.isNotEmpty() },
        deviceCheckPassed = deviceCheckPassed,
        socialActivities = socialActivities.takeIf { it.isNotEmpty() },
        socialAnxiety = socialAnxiety,
        selfRating = selfRating,
        notes = notes,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
