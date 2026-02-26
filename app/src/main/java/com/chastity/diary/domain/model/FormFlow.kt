package com.chastity.diary.domain.model

/**
 * Form flow management for progressive disclosure
 */

/**
 * Question types in the form
 */
enum class QuestionType {
    CORE,           // 10 core questions (always shown)
    CONDITIONAL,    // 8 conditional questions (shown based on answers)
    ROTATING        // 5 rotating questions (1 random per day)
}

/**
 * Form step stages
 */
enum class FormStep {
    CORE,           // Step 1: Core questions
    CONDITIONAL,    // Step 2: Conditional questions
    ROTATING,       // Step 3: Random question of the day
    REVIEW          // Step 4: Review and submit
}

/**
 * Question IDs for tracking
 */
enum class QuestionId {
    // Core questions (10)
    MOOD,
    DESIRE_LEVEL,
    COMFORT_RATING,
    SLEEP_QUALITY,
    FOCUS_LEVEL,
    DEVICE_CHECK,
    SELF_RATING,
    PHOTO,
    EMOTIONS,
    NOTES,
    
    // Conditional questions (8)
    PORN_DETAILS,           // Triggered by viewedPorn
    ERECTION_DETAILS,       // Triggered by gender=MALE && hadErection
    UNLOCK_DETAILS,         // Triggered by unlocked
    DISCOMFORT_DETAILS,     // Triggered by hasDiscomfort
    LEAKAGE_DETAILS,        // Triggered by hadLeakage
    EDGING_DETAILS,         // Triggered by hadEdging
    REMOVAL_DETAILS,        // Triggered by temporarilyRemoved
    NIGHT_ERECTION_DETAILS, // Triggered by gender=MALE && nightErections > 0
    
    // Rotating questions (5 - 1 shown per day)
    EXERCISE,
    EXPOSED_LOCK,
    KEYHOLDER_INTERACTION,
    CLEANING,
    SOCIAL_ACTIVITIES
}

/**
 * Question definition
 */
data class Question(
    val id: QuestionId,
    val type: QuestionType,
    val title: String,
    val subtitle: String? = null,
    val isRequired: Boolean = false,
    
    /**
     * Condition for showing this question
     * Returns true if question should be shown
     */
    val condition: ((DailyEntry) -> Boolean)? = null
)

/**
 * Form flow state
 */
data class FormFlowState(
    val currentStep: FormStep = FormStep.CORE,
    val completedSteps: Set<FormStep> = emptySet(),
    val rotatingQuestionOfDay: QuestionId? = null,
    val visibleConditionalQuestions: Set<QuestionId> = emptySet()
) {
    /**
     * Calculate overall progress (0.0 to 1.0)
     */
    fun calculateProgress(entry: DailyEntry, userGender: Gender): Float {
        val totalQuestions = getTotalQuestionCount(entry, userGender)
        val answeredQuestions = getAnsweredQuestionCount(entry)
        return if (totalQuestions > 0) answeredQuestions.toFloat() / totalQuestions else 0f
    }
    
    /**
     * Get total number of questions user needs to answer
     */
    private fun getTotalQuestionCount(entry: DailyEntry, userGender: Gender): Int {
        var count = 10 // Core questions
        
        // Add conditional questions that are triggered
        if (entry.viewedPorn) count++
        if (userGender == Gender.MALE && entry.hadErection) count++
        if (entry.unlocked) count++
        if (entry.hasDiscomfort) count++
        if (entry.hadLeakage) count++
        if (entry.hadEdging) count++
        if (entry.temporarilyRemoved) count++
        if (userGender == Gender.MALE && (entry.nightErections ?: 0) > 0) count++
        
        // Add rotating question
        count++
        
        return count
    }
    
    /**
     * Get number of answered questions
     */
    private fun getAnsweredQuestionCount(entry: DailyEntry): Int {
        var count = 0
        
        // Core questions
        if (entry.mood != null) count++
        if (entry.desireLevel != null) count++
        if (entry.comfortRating != null) count++
        if (entry.sleepQuality != null) count++
        if (entry.focusLevel != null) count++
        if (entry.deviceCheckPassed != null) count++
        if (entry.selfRating != null) count++
        // Photo is optional
        if (entry.emotions.isNotEmpty()) count++
        // Notes is optional
        
        // Always count these as "answered" for progress calculation
        count += 2 // Photo + Notes
        
        // Conditional questions (if triggered and answered)
        if (entry.viewedPorn && entry.pornDuration != null) count++
        if (entry.hadErection && entry.erectionCount != null) count++
        if (entry.unlocked && entry.masturbated != null) count++
        if (entry.hasDiscomfort && entry.discomfortAreas.isNotEmpty()) count++
        if (entry.hadLeakage && entry.leakageAmount != null) count++
        if (entry.hadEdging && entry.edgingDuration != null) count++
        if (entry.temporarilyRemoved && entry.removalDuration != null) count++
        if ((entry.nightErections ?: 0) > 0 && entry.wokeUpFromErection != null) count++
        
        // Rotating question (check based on rotatingQuestionOfDay)
        when (rotatingQuestionOfDay) {
            QuestionId.EXERCISE -> if (entry.exercised || entry.exerciseDuration != null) count++
            QuestionId.EXPOSED_LOCK -> if (entry.exposedLock || entry.exposedLocations.isNotEmpty()) count++
            QuestionId.KEYHOLDER_INTERACTION -> if (entry.keyholderInteraction || entry.interactionTypes.isNotEmpty()) count++
            QuestionId.CLEANING -> if (entry.cleaningType != null) count++
            QuestionId.SOCIAL_ACTIVITIES -> if (entry.socialActivities.isNotEmpty()) count++
            else -> {}
        }
        
        return count
    }
    
    /**
     * Check if can proceed to next step
     */
    fun canProceedToNextStep(entry: DailyEntry): Boolean {
        return when (currentStep) {
            FormStep.CORE -> {
                // Must answer all required core questions
                entry.mood != null &&
                entry.desireLevel != null &&
                entry.comfortRating != null &&
                entry.sleepQuality != null &&
                entry.focusLevel != null &&
                entry.deviceCheckPassed != null &&
                entry.selfRating != null
            }
            FormStep.CONDITIONAL -> true // Conditional questions are optional
            FormStep.ROTATING -> true // Rotating question is optional
            FormStep.REVIEW -> false // Last step
        }
    }
    
    /**
     * Get next step
     */
    fun nextStep(): FormStep? {
        return when (currentStep) {
            FormStep.CORE -> FormStep.CONDITIONAL
            FormStep.CONDITIONAL -> FormStep.ROTATING
            FormStep.ROTATING -> FormStep.REVIEW
            FormStep.REVIEW -> null
        }
    }
    
    /**
     * Get previous step
     */
    fun previousStep(): FormStep? {
        return when (currentStep) {
            FormStep.CORE -> null
            FormStep.CONDITIONAL -> FormStep.CORE
            FormStep.ROTATING -> FormStep.CONDITIONAL
            FormStep.REVIEW -> FormStep.ROTATING
        }
    }
}

/**
 * Generate rotating question for the day
 * Uses date as seed for deterministic randomness
 */
fun generateRotatingQuestionOfDay(date: java.time.LocalDate): QuestionId {
    val rotatingQuestions = listOf(
        QuestionId.EXERCISE,
        QuestionId.EXPOSED_LOCK,
        QuestionId.KEYHOLDER_INTERACTION,
        QuestionId.CLEANING,
        QuestionId.SOCIAL_ACTIVITIES
    )
    
    // Use day of year as seed for consistent question per day
    val dayOfYear = date.dayOfYear
    val index = dayOfYear % rotatingQuestions.size
    
    return rotatingQuestions[index]
}
