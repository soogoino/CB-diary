package com.chastity.diary.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.chastity.diary.R

/**
 * Milestone definition for streak achievements
 */
data class StreakMilestone(
    val days: Int,
    val emoji: String,
    val labelRes: Int,
    val color: Color
)

/**
 * All achievement milestones in order
 */
val STREAK_MILESTONES = listOf(
    StreakMilestone(7,   "🌱", R.string.streak_label_beginner,   Color(0xFF4CAF50.toInt())),
    StreakMilestone(14,  "⚡", R.string.streak_label_progressing, Color(0xFF2196F3.toInt())),
    StreakMilestone(30,  "🔥", R.string.streak_label_one_month,  Color(0xFFFF9800.toInt())),
    StreakMilestone(60,  "💪", R.string.streak_label_two_months, Color(0xFF9C27B0.toInt())),
    StreakMilestone(100, "🏆", R.string.streak_label_hundred_days, Color(0xFFFFD700.toInt())),
    StreakMilestone(365, "👑", R.string.streak_label_one_year,   Color(0xFFE91E63.toInt()))
)

/**
 * Streak achievement badges section displayed in the dashboard
 */
@Composable
fun StreakBadgesSection(
    currentStreak: Int,
    longestStreak: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.streak_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.streak_longest_days, longestStreak),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Progress bar showing next milestone
            val nextMilestone = STREAK_MILESTONES.firstOrNull { it.days > longestStreak }
            val prevMilestone = STREAK_MILESTONES.lastOrNull { it.days <= longestStreak }

            if (nextMilestone != null) {
                val startDays = prevMilestone?.days ?: 0
                val progress = (longestStreak - startDays).toFloat() /
                        (nextMilestone.days - startDays).toFloat()
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(
                                R.string.streak_distance_to,
                                nextMilestone.emoji,
                                stringResource(nextMilestone.labelRes)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.streak_days_remaining, nextMilestone.days - longestStreak),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    LinearProgressIndicator(
                        progress = progress.coerceIn(0f, 1f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = nextMilestone.color,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            } else {
                // All milestones achieved!
                Text(
                    text = stringResource(R.string.streak_all_unlocked),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            // Badges row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(STREAK_MILESTONES) { milestone ->
                    val achieved = longestStreak >= milestone.days
                    val isCurrent = currentStreak >= milestone.days && longestStreak >= milestone.days
                    StreakBadge(
                        milestone = milestone,
                        achieved = achieved,
                        isCurrentActive = isCurrent
                    )
                }
            }
        }
    }
}

/**
 * Individual badge item
 */
@Composable
fun StreakBadge(
    milestone: StreakMilestone,
    achieved: Boolean,
    isCurrentActive: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .alpha(if (achieved) 1f else 0.35f)
            .width(64.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(
                    if (achieved)
                        milestone.color.copy(alpha = 0.15f)
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                )
                .then(
                    if (isCurrentActive)
                        Modifier.border(2.dp, milestone.color, CircleShape)
                    else if (achieved)
                        Modifier.border(1.dp, milestone.color.copy(alpha = 0.5f), CircleShape)
                    else
                        Modifier
                )
        ) {
            Text(
                text = if (achieved) milestone.emoji else "🔒",
                fontSize = 24.sp,
                textAlign = TextAlign.Center
            )
        }

        Text(
            text = stringResource(R.string.streak_days_count, milestone.days),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (achieved) FontWeight.Bold else FontWeight.Normal,
            color = if (achieved)
                MaterialTheme.colorScheme.onSurface
            else
                MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Text(
            text = stringResource(milestone.labelRes),
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            color = if (achieved) milestone.color else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}
