package com.chastity.diary.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chastity.diary.R
import com.chastity.diary.domain.model.DailyEntry
import com.chastity.diary.util.Constants

/**
 * Core Questions (10) - Always shown, takes ~3 minutes
 */

@Composable
fun CoreQuestionsSection(
    entry: DailyEntry,
    onEntryUpdate: (DailyEntry) -> Unit,
    onTakePhoto: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section header
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.core_section_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = stringResource(R.string.core_section_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
        
        // Q1: Mood
        QuestionSection(
            title = stringResource(R.string.core_q1_title),
            subtitle = stringResource(R.string.core_q1_subtitle)
        ) {
            MoodSelector(
                selectedMood = entry.mood,
                moods = stringArrayResource(R.array.moods_array).toList(),
                moodKeys = Constants.MOODS,
                onMoodSelected = { mood ->
                    onEntryUpdate(entry.copy(mood = mood))
                }
            )
        }
        
        // Q2: Desire Level
        QuestionSection(
            title = stringResource(R.string.core_q2_title),
            subtitle = stringResource(R.string.core_q2_subtitle)
        ) {
            SliderWithLabel(
                value = entry.desireLevel?.toFloat() ?: 5f,
                onValueChange = { value ->
                    onEntryUpdate(entry.copy(desireLevel = value.toInt()))
                },
                valueRange = 0f..10f,
                steps = 9,
                label = stringResource(R.string.core_label_desire)
            )
        }
        
        // Q3: Comfort Rating
        QuestionSection(
            title = stringResource(R.string.core_q3_title),
            subtitle = stringResource(R.string.core_q3_subtitle)
        ) {
            SliderWithLabel(
                value = entry.comfortRating?.toFloat() ?: 5f,
                onValueChange = { value ->
                    onEntryUpdate(entry.copy(comfortRating = value.toInt()))
                },
                valueRange = 0f..10f,
                steps = 9,
                label = stringResource(R.string.core_label_comfort)
            )
        }
        
        // Q4: Sleep Quality
        QuestionSection(
            title = stringResource(R.string.core_q4_title),
            subtitle = stringResource(R.string.core_q4_subtitle)
        ) {
            StarRating(
                rating = entry.sleepQuality ?: 3,
                onRatingChange = { rating ->
                    onEntryUpdate(entry.copy(sleepQuality = rating))
                },
                maxStars = 5,
                label = stringResource(R.string.core_label_sleeped_well)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            YesNoToggle(
                value = entry.wokeUpDueToDevice,
                onValueChange = { woke ->
                    onEntryUpdate(entry.copy(wokeUpDueToDevice = woke))
                },
                label = stringResource(R.string.core_label_woke_due_to_lock)
            )
        }
        
        // Q5: Focus Level
        QuestionSection(
            title = stringResource(R.string.core_q5_title),
            subtitle = stringResource(R.string.core_q5_subtitle)
        ) {
            SliderWithLabel(
                value = entry.focusLevel?.toFloat() ?: 5f,
                onValueChange = { value ->
                    onEntryUpdate(entry.copy(focusLevel = value.toInt()))
                },
                valueRange = 1f..10f,
                steps = 8,
                label = stringResource(R.string.core_label_focus)
            )
        }
        
        // Q6: Device Check
        QuestionSection(
            title = stringResource(R.string.core_q6_title),
            subtitle = stringResource(R.string.core_q6_subtitle)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onEntryUpdate(entry.copy(deviceCheckPassed = true)) },
                    modifier = Modifier.weight(1f),
                    colors = if (entry.deviceCheckPassed == true) {
                        ButtonDefaults.buttonColors()
                    } else {
                        ButtonDefaults.outlinedButtonColors()
                    }
                ) {
                    Text(stringResource(R.string.core_status_ok))
                }
                Button(
                    onClick = { onEntryUpdate(entry.copy(deviceCheckPassed = false)) },
                    modifier = Modifier.weight(1f),
                    colors = if (entry.deviceCheckPassed == false) {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    } else {
                        ButtonDefaults.outlinedButtonColors()
                    }
                ) {
                    Text(stringResource(R.string.core_status_bad))
                }
            }
        }
        
        // Q7: Self Rating
        QuestionSection(
            title = stringResource(R.string.core_q7_title),
            subtitle = stringResource(R.string.core_q7_subtitle)
        ) {
            StarRating(
                rating = entry.selfRating ?: 3,
                onRatingChange = { rating ->
                    onEntryUpdate(entry.copy(selfRating = rating))
                },
                maxStars = 5,
                label = stringResource(R.string.core_label_self_rating)
            )
        }
        
        // Q8: Photo (Optional)
        QuestionSection(
            title = stringResource(R.string.core_q8_title),
            subtitle = stringResource(R.string.core_q8_subtitle)
        ) {
            if (entry.photoPath != null) {
                Text(
                    text = stringResource(R.string.core_photo_taken),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                OutlinedButton(
                    onClick = { onEntryUpdate(entry.copy(photoPath = null)) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.core_remove_photo))
                }
            } else {
                OutlinedButton(
                    onClick = onTakePhoto,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Camera, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.core_take_photo))
                }
            }
        }
        
        // Q9: Emotions
        QuestionSection(
            title = stringResource(R.string.core_q9_title),
            subtitle = stringResource(R.string.core_q9_subtitle)
        ) {
            MultiSelectChipGroup(
                options = stringArrayResource(R.array.emotions_array).toList(),
                selectedOptions = entry.emotions,
                storageKeys = Constants.EMOTIONS,
                onSelectionChange = { emotions ->
                    onEntryUpdate(entry.copy(emotions = emotions))
                }
            )
        }
        
        // Q10: Notes (Optional)
        QuestionSection(
            title = stringResource(R.string.core_q10_title),
            subtitle = stringResource(R.string.core_q10_subtitle)
        ) {
            OutlinedTextField(
                value = entry.notes ?: "",
                onValueChange = { notes ->
                    onEntryUpdate(entry.copy(notes = notes))
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.core_notes_placeholder)) },
                minLines = 3,
                maxLines = 5
            )
        }
    }
}
