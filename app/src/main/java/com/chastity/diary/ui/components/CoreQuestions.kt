package com.chastity.diary.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
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
                    text = "核心問題",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "10 題 • 約 3 分鐘",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
        
        // Q1: Mood
        QuestionSection(
            title = "1. 今天的整體心情如何？",
            subtitle = "選擇最貼近你今天感受的心情"
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
            title = "2. 今天的性慾強度？",
            subtitle = "0 = 完全沒有, 10 = 非常強烈"
        ) {
            SliderWithLabel(
                value = entry.desireLevel?.toFloat() ?: 5f,
                onValueChange = { value ->
                    onEntryUpdate(entry.copy(desireLevel = value.toInt()))
                },
                valueRange = 0f..10f,
                steps = 9,
                label = "性慾強度"
            )
        }
        
        // Q3: Comfort Rating
        QuestionSection(
            title = "3. 佩戴鎖的舒適度？",
            subtitle = "0 = 非常不舒適, 10 = 非常舒適"
        ) {
            SliderWithLabel(
                value = entry.comfortRating?.toFloat() ?: 5f,
                onValueChange = { value ->
                    onEntryUpdate(entry.copy(comfortRating = value.toInt()))
                },
                valueRange = 0f..10f,
                steps = 9,
                label = "舒適度"
            )
        }
        
        // Q4: Sleep Quality
        QuestionSection(
            title = "4. 昨晚的睡眠品質？",
            subtitle = "鎖是否影響你的睡眠"
        ) {
            StarRating(
                rating = entry.sleepQuality ?: 3,
                onRatingChange = { rating ->
                    onEntryUpdate(entry.copy(sleepQuality = rating))
                },
                maxStars = 5,
                label = "昨晚有睡好嗎？"
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            YesNoToggle(
                value = entry.wokeUpDueToDevice,
                onValueChange = { woke ->
                    onEntryUpdate(entry.copy(wokeUpDueToDevice = woke))
                },
                label = "因為鎖而醒來"
            )
        }
        
        // Q5: Focus Level
        QuestionSection(
            title = "5. 今天的專注度如何？",
            subtitle = "1 = 完全無法專注, 10 = 非常專注"
        ) {
            SliderWithLabel(
                value = entry.focusLevel?.toFloat() ?: 5f,
                onValueChange = { value ->
                    onEntryUpdate(entry.copy(focusLevel = value.toInt()))
                },
                valueRange = 1f..10f,
                steps = 8,
                label = "專注度"
            )
        }
        
        // Q6: Device Check
        QuestionSection(
            title = "6. 貞操鎖鎖檢查",
            subtitle = "檢查貞操鎖是否正常佩戴、無損壞"
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
                    Text("✓ 正常")
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
                    Text("✗ 異常")
                }
            }
        }
        
        // Q7: Self Rating
        QuestionSection(
            title = "7. 今天的自我評價？",
            subtitle = "你認為自己今天表現如何"
        ) {
            StarRating(
                rating = entry.selfRating ?: 3,
                onRatingChange = { rating ->
                    onEntryUpdate(entry.copy(selfRating = rating))
                },
                maxStars = 5,
                label = "自我評價"
            )
        }
        
        // Q8: Photo (Optional)
        QuestionSection(
            title = "8. 照片打卡",
            subtitle = "可選：記錄今天的視覺紀念"
        ) {
            if (entry.photoPath != null) {
                Text(
                    text = "已拍攝照片",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                OutlinedButton(
                    onClick = { onEntryUpdate(entry.copy(photoPath = null)) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("移除照片")
                }
            } else {
                OutlinedButton(
                    onClick = onTakePhoto,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Camera, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("拍攝照片")
                }
            }
        }
        
        // Q9: Emotions
        QuestionSection(
            title = "9. 今天的細緻情緒？",
            subtitle = "可多選：更精確地描述你的情緒狀態"
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
            title = "10. 備註",
            subtitle = "可選：記錄任何想補充的想法或事件"
        ) {
            OutlinedTextField(
                value = entry.notes ?: "",
                onValueChange = { notes ->
                    onEntryUpdate(entry.copy(notes = notes))
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("輸入備註...") },
                minLines = 3,
                maxLines = 5
            )
        }
    }
}
