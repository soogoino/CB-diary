package com.chastity.diary.domain.repository

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Domain-layer contract for streak tracking.
 * Concrete implementation: [com.chastity.diary.data.repository.StreakRepository]
 */
interface IStreakRepository {

    val currentStreak: Flow<Int>

    val longestStreak: Flow<Int>

    suspend fun updateStreak(entryDate: LocalDate)

    suspend fun resetStreak()
}
