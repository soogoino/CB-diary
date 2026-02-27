package com.chastity.diary.util

import com.chastity.diary.domain.model.DailyEntry
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.random.Random

/**
 * Generate test data for development and testing
 */
object TestDataGenerator {
    
    private val moods = listOf("Happy", "Calm", "Neutral", "Sad", "Anxious", "Frustrated")
    private val exerciseTypes = listOf("Running", "Swimming", "Gym", "Yoga", "Walking")
    private val emotions = listOf("Excited", "Calm", "Nervous", "Satisfied", "Longing", "Resigned")
    
    /**
     * Generate sample entries for the past N days
     */
    fun generateSampleEntries(days: Int = 30): List<DailyEntry> {
        val today = LocalDate.now()
        return (0 until days).map { offset ->
            val date = today.minusDays(offset.toLong())
            generateEntryForDate(date)
        }
    }
    
    private fun generateEntryForDate(date: LocalDate): DailyEntry {
        return DailyEntry(
            id = 0, // Will be auto-generated
            date = date,
            createdAt = date.atStartOfDay(),
            updatedAt = LocalDateTime.now(),
            
            // Randomize mood with tendency towards positive
            mood = moods[Random.nextInt(moods.size)],
            
            // Desire level tends to vary
            desireLevel = Random.nextInt(1, 11),
            
            // Comfort rating usually decent
            comfortRating = Random.nextInt(2, 6),
            
            // Sleep quality
            sleepQuality = Random.nextInt(2, 6),
            wokeUpDueToDevice = Random.nextBoolean(),
            
            // Focus level
            focusLevel = Random.nextInt(3, 10),
            
            // Device check usually passes
            deviceCheckPassed = Random.nextDouble() > 0.2,
            
            // Self rating
            selfRating = Random.nextInt(2, 6),
            
            // Emotions
            emotions = emotions.shuffled().take(Random.nextInt(1, 4)),
            
            // Random activities
            viewedPorn = Random.nextDouble() < 0.3,
            pornDuration = if (Random.nextBoolean()) Random.nextInt(5, 60) else null,
            
            hadErection = Random.nextDouble() < 0.4,
            erectionCount = if (Random.nextBoolean()) Random.nextInt(1, 6) else null,
            
            exercised = Random.nextDouble() < 0.6,
            exerciseTypes = if (Random.nextBoolean()) {
                exerciseTypes.shuffled().take(Random.nextInt(1, 3))
            } else emptyList(),
            exerciseDuration = if (Random.nextBoolean()) Random.nextInt(15, 90) else null,
            
            unlocked = Random.nextDouble() < 0.1,
            masturbated = Random.nextDouble() < 0.05,
            masturbationDuration = null,
            
            exposedLock = Random.nextDouble() < 0.2,
            exposedLocations = if (Random.nextBoolean()) listOf("Gym") else emptyList(),
            
            photoPath = null,
            
            hasDiscomfort = Random.nextDouble() < 0.3,
            discomfortAreas = if (Random.nextBoolean()) listOf("Groin") else emptyList(),
            discomfortLevel = if (Random.nextBoolean()) Random.nextInt(1, 6) else null,
            
            cleaningType = if (Random.nextBoolean()) "Shower rinse" else null,
            
            hadLeakage = Random.nextDouble() < 0.2,
            leakageAmount = if (Random.nextBoolean()) "Small amount" else null,
            
            hadEdging = Random.nextDouble() < 0.15,
            edgingDuration = null,
            edgingMethods = emptyList(),
            
            keyholderInteraction = Random.nextDouble() < 0.3,
            interactionTypes = if (Random.nextBoolean()) listOf("Text chat") else emptyList(),
            
            temporarilyRemoved = Random.nextDouble() < 0.05,
            removalDuration = null,
            removalReasons = emptyList(),
            
            nightErections = if (Random.nextBoolean()) Random.nextInt(0, 4) else null,
            wokeUpFromErection = Random.nextBoolean(),
            
            completedTasks = emptyList(),
            
            socialActivities = if (Random.nextBoolean()) listOf("Hangout with friends") else emptyList(),
            socialAnxiety = if (Random.nextBoolean()) Random.nextInt(1, 6) else null,
            
            notes = if (Random.nextDouble() < 0.3) "Feeling good today" else null
        )
    }
}
