package com.chastity.diary.domain.model

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Domain model for daily entry
 */
data class DailyEntry(
    val id: Long = 0,
    val date: LocalDate,
    
    // Q1: Mood
    val mood: String? = null,
    
    // Q2: Pornography
    val viewedPorn: Boolean = false,
    val pornDuration: Int? = null, // minutes
    
    // Q3: Erection (male only)
    val hadErection: Boolean = false,
    val erectionCount: Int? = null,
    
    // Q4: Exercise
    val exercised: Boolean = false,
    val exerciseTypes: List<String> = emptyList(),
    val exerciseDuration: Int? = null, // minutes
    
    // Q5: Unlock/Masturbation
    val unlocked: Boolean = false,
    val masturbated: Boolean = false,
    val masturbationDuration: Int? = null, // minutes
    
    // Q6: Exposed lock
    val exposedLock: Boolean = false,
    val exposedLocations: List<String> = emptyList(),
    
    // Q7: Photo
    val photoPath: String? = null,
    
    // Q8: Desire level
    val desireLevel: Int? = null, // 1-10
    
    // Q9: Comfort rating
    val comfortRating: Int? = null, // 1-5
    
    // Q10: Discomfort
    val hasDiscomfort: Boolean = false,
    val discomfortAreas: List<String> = emptyList(),
    val discomfortLevel: Int? = null, // 1-10
    
    // Q11: Cleaning
    val cleaningType: String? = null,
    
    // Q12: Leakage
    val hadLeakage: Boolean = false,
    val leakageAmount: String? = null,
    
    // Q13: Edging
    val hadEdging: Boolean = false,
    val edgingDuration: Int? = null, // minutes
    val edgingMethods: List<String> = emptyList(),
    
    // Q14: Keyholder interaction
    val keyholderInteraction: Boolean = false,
    val interactionTypes: List<String> = emptyList(),
    
    // Q15: Sleep quality
    val sleepQuality: Int? = null, // 1-5
    val wokeUpDueToDevice: Boolean = false,
    
    // Q16: Temporarily removed
    val temporarilyRemoved: Boolean = false,
    val removalDuration: Int? = null, // minutes
    val removalReasons: List<String> = emptyList(),
    
    // Q17: Night erections (male only)
    val nightErections: Int? = null, // count
    val wokeUpFromErection: Boolean = false,
    
    // Q18: Focus level
    val focusLevel: Int? = null, // 1-10 (1=very distracted, 10=very focused)
    
    // Q19: Completed tasks
    val completedTasks: List<String> = emptyList(),
    
    // Q20: Emotions
    val emotions: List<String> = emptyList(),
    
    // Q21: Device check
    val deviceCheckPassed: Boolean = true,
    
    // Q22: Social activities
    val socialActivities: List<String> = emptyList(),
    val socialAnxiety: Int? = null, // 1-10
    
    // Q23: Self rating
    val selfRating: Int? = null, // 1-5
    
    // Optional notes
    val notes: String? = null,
    
    // Metadata
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
