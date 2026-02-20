package com.chastity.diary.data.repository

import com.chastity.diary.data.datastore.PreferencesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Repository for streak tracking
 */
class StreakRepository(private val preferencesManager: PreferencesManager) {
    
    val currentStreak: Flow<Int> = preferencesManager.currentStreakFlow
    val longestStreak: Flow<Int> = preferencesManager.longestStreakFlow
    
    suspend fun updateStreak(entryDate: LocalDate) {
        val currentStreakValue = preferencesManager.currentStreakFlow.first()
        val longestStreakValue = preferencesManager.longestStreakFlow.first()
        
        val lastEntryDateStr = try {
            preferencesManager.userSettingsFlow.first().let { settings ->
                // Get last entry date from settings or use a placeholder
                null as String? // We'll need to add this to PreferencesManager
            }
        } catch (e: Exception) {
            null
        }
        
        val lastEntryDate = lastEntryDateStr?.let { LocalDate.parse(it) }
        
        val newStreak = when {
            lastEntryDate == null -> 1 // First entry
            lastEntryDate == entryDate -> currentStreakValue // Same day, no change
            ChronoUnit.DAYS.between(lastEntryDate, entryDate) == 1L -> {
                // Consecutive day
                currentStreakValue + 1
            }
            else -> 1 // Streak broken
        }
        
        val newLongestStreak = maxOf(newStreak, longestStreakValue)
        
        preferencesManager.updateStreak(newStreak, newLongestStreak, entryDate)
    }
    
    suspend fun resetStreak() {
        val longestStreakValue = preferencesManager.longestStreakFlow.first()
        preferencesManager.updateStreak(0, longestStreakValue, LocalDate.now())
    }
}
