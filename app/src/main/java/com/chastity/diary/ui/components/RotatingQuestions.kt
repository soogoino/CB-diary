package com.chastity.diary.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.res.stringArrayResource
import com.chastity.diary.R
import kotlin.math.abs
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chastity.diary.domain.model.DailyEntry
import com.chastity.diary.domain.model.QuestionId
import com.chastity.diary.util.Constants

/**
 * Rotating Questions (5) - 1 random question shown per day
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RotatingQuestionSection(
    questionOfDay: QuestionId,
    entry: DailyEntry,
    onEntryUpdate: (DailyEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section header
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "今日特別問題 🎲",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    text = "每天輪替一個不同的問題",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                )
            }
        }
        
        // Render the rotating question based on questionOfDay
        when (questionOfDay) {
            QuestionId.EXERCISE -> ExerciseQuestion(entry, onEntryUpdate)
            QuestionId.EXPOSED_LOCK -> ExposedLockQuestion(entry, onEntryUpdate)
            QuestionId.KEYHOLDER_INTERACTION -> KeyholderInteractionQuestion(entry, onEntryUpdate)
            QuestionId.CLEANING -> CleaningQuestion(entry, onEntryUpdate)
            QuestionId.SOCIAL_ACTIVITIES -> SocialActivitiesQuestion(entry, onEntryUpdate)
            else -> {
                // Should not happen
                Text("無效的輪替問題")
            }
        }

        // Unified feedback: show a playful, slightly-embarrassed message regardless of yes/no
        val answered = when (questionOfDay) {
            QuestionId.EXERCISE -> entry.exercised || entry.exerciseTypes.isNotEmpty() || entry.exerciseDuration != null
            QuestionId.EXPOSED_LOCK -> entry.exposedLock || entry.exposedLocations.isNotEmpty()
            QuestionId.KEYHOLDER_INTERACTION -> entry.keyholderInteraction || entry.interactionTypes.isNotEmpty()
            QuestionId.CLEANING -> entry.cleaningType != null
            QuestionId.SOCIAL_ACTIVITIES -> entry.socialActivities.isNotEmpty()
            else -> false
        }

        RotatingQuestionFeedback(key = questionOfDay.name, answered = answered)
    }
}

@Composable
private fun RotatingQuestionFeedback(key: String, answered: Boolean) {
    val feedbacks = stringArrayResource(R.array.daily_rotating_feedback_generic)
    val unified = remember(key) { feedbacks[abs(key.hashCode()) % feedbacks.size] }
    AnimatedVisibility(visible = answered) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.12f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = unified,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
private fun ExerciseQuestion(
    entry: DailyEntry,
    onEntryUpdate: (DailyEntry) -> Unit
) {
    QuestionSection(
        title = "今天有運動嗎？",
        subtitle = "記錄運動類型與時長"
    ) {
        YesNoToggle(
            value = entry.exercised,
            onValueChange = { exercised ->
                onEntryUpdate(entry.copy(exercised = exercised))
            },
            label = "是否運動"
        )
        
        if (entry.exercised) {
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "運動類型",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            MultiSelectChipGroup(
                options = Constants.EXERCISE_TYPES,
                selectedOptions = entry.exerciseTypes,
                onSelectionChange = { types ->
                    onEntryUpdate(entry.copy(exerciseTypes = types))
                }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            DurationPicker(
                selectedMinutes = entry.exerciseDuration,
                onDurationSelected = { duration ->
                    onEntryUpdate(entry.copy(exerciseDuration = duration))
                },
                quickOptions = listOf(15, 30, 45, 60, 90, 120),
                label = "運動時長"
            )
        }
    }
}

@Composable
private fun ExposedLockQuestion(
    entry: DailyEntry,
    onEntryUpdate: (DailyEntry) -> Unit
) {
    QuestionSection(
        title = "鎖有在公共場合露出嗎？",
        subtitle = "記錄露出的地點"
    ) {
        YesNoToggle(
            value = entry.exposedLock,
            onValueChange = { exposed ->
                onEntryUpdate(entry.copy(exposedLock = exposed))
            },
            label = "是否露出"
        )
        
        if (entry.exposedLock) {
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "露出地點",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            MultiSelectChipGroup(
                options = Constants.EXPOSED_LOCATIONS,
                selectedOptions = entry.exposedLocations,
                onSelectionChange = { locations ->
                    onEntryUpdate(entry.copy(exposedLocations = locations))
                }
            )
        }
    }
}

@Composable
private fun KeyholderInteractionQuestion(
    entry: DailyEntry,
    onEntryUpdate: (DailyEntry) -> Unit
) {
    QuestionSection(
        title = "今天與 Keyholder 有互動嗎？",
        subtitle = "記錄互動類型"
    ) {
        YesNoToggle(
            value = entry.keyholderInteraction,
            onValueChange = { interaction ->
                onEntryUpdate(entry.copy(keyholderInteraction = interaction))
            },
            label = "是否互動"
        )
        
        if (entry.keyholderInteraction) {
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "互動類型",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            MultiSelectChipGroup(
                options = Constants.INTERACTION_TYPES,
                selectedOptions = entry.interactionTypes,
                onSelectionChange = { types ->
                    onEntryUpdate(entry.copy(interactionTypes = types))
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CleaningQuestion(
    entry: DailyEntry,
    onEntryUpdate: (DailyEntry) -> Unit
) {
    QuestionSection(
        title = "今天如何清潔鎖？",
        subtitle = "選擇清潔方式"
    ) {
        val cleaningTypes = Constants.CLEANING_TYPES
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            cleaningTypes.chunked(2).forEach { rowTypes ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowTypes.forEach { type ->
                        FilterChip(
                            selected = entry.cleaningType == type,
                            onClick = { onEntryUpdate(entry.copy(cleaningType = type)) },
                            label = { Text(type) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = MaterialTheme.colorScheme.outline,
                                selectedBorderColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                    }
                    // Fill remaining
                    if (rowTypes.size < 2) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun SocialActivitiesQuestion(
    entry: DailyEntry,
    onEntryUpdate: (DailyEntry) -> Unit
) {
    QuestionSection(
        title = "今天有參與社交活動嗎？",
        subtitle = "記錄活動類型與焦慮程度"
    ) {
        Text(
            text = "社交活動類型",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        MultiSelectChipGroup(
            options = Constants.SOCIAL_ACTIVITIES,
            selectedOptions = entry.socialActivities,
            onSelectionChange = { activities ->
                onEntryUpdate(entry.copy(socialActivities = activities))
            }
        )
        
        if (entry.socialActivities.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            
            SliderWithLabel(
                value = entry.socialAnxiety?.toFloat() ?: 1f,
                onValueChange = { value ->
                    onEntryUpdate(entry.copy(socialAnxiety = value.toInt()))
                },
                valueRange = 1f..10f,
                steps = 8,
                label = "焦慮程度 (1=無焦慮, 10=極度焦慮)"
            )
        }
    }
}
