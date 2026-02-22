package com.chastity.diary.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chastity.diary.domain.model.DailyEntry
import com.chastity.diary.util.Constants

/**
 * Conditional Questions (8) - Shown based on previous answers
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConditionalQuestionsSection(
    entry: DailyEntry,
    onEntryUpdate: (DailyEntry) -> Unit,
    userGender: com.chastity.diary.domain.model.Gender,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section header
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "後續問題",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "根據你的情況回答，某些問題可能不適用",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
        }
        
        // Q1: Porn (always ask trigger)
        PornQuestion(entry, onEntryUpdate)
        
        // Q2: Erection (male only)
        if (userGender == com.chastity.diary.domain.model.Gender.MALE) {
            ErectionQuestion(entry, onEntryUpdate)
        }
        
        // Q3: Unlock
        UnlockQuestion(entry, onEntryUpdate)
        
        // Q4: Discomfort
        DiscomfortQuestion(entry, onEntryUpdate)
        
        // Q5: Leakage
        LeakageQuestion(entry, onEntryUpdate)
        
        // Q6: Edging
        EdgingQuestion(entry, onEntryUpdate)
        
        // Q7: Removal
        RemovalQuestion(entry, onEntryUpdate)
        
        // Q8: Night Erection (male only)
        if (userGender == com.chastity.diary.domain.model.Gender.MALE) {
            NightErectionQuestion(entry, onEntryUpdate)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PornQuestion(
    entry: DailyEntry,
    onEntryUpdate: (DailyEntry) -> Unit
) {
    QuestionSection(
        title = "1. 今天是否觀看色情內容？",
        subtitle = "如果有,記錄觀看時長"
    ) {
        YesNoToggle(
            value = entry.viewedPorn,
            onValueChange = { viewed ->
                onEntryUpdate(entry.copy(viewedPorn = viewed))
            },
            label = "觀看色情內容"
        )
        
        if (entry.viewedPorn) {
            Spacer(modifier = Modifier.height(12.dp))
            DurationPicker(
                selectedMinutes = entry.pornDuration,
                onDurationSelected = { duration ->
                    onEntryUpdate(entry.copy(pornDuration = duration))
                },
                quickOptions = Constants.DURATION_QUICK_OPTIONS,
                label = "觀看時長"
            )
        }
    }
}

@Composable
private fun ErectionQuestion(
    entry: DailyEntry,
    onEntryUpdate: (DailyEntry) -> Unit
) {
    QuestionSection(
        title = "2. 今天是否勃起？",
        subtitle = "如果有,記錄勃起次數"
    ) {
        YesNoToggle(
            value = entry.hadErection,
            onValueChange = { had ->
                onEntryUpdate(entry.copy(hadErection = had))
            },
            label = "有勃起"
        )
        
        if (entry.hadErection) {
            Spacer(modifier = Modifier.height(12.dp))
            
            var countText by remember(entry.erectionCount) { 
                mutableStateOf(entry.erectionCount?.toString() ?: "") 
            }
            
            OutlinedTextField(
                value = countText,
                onValueChange = { text ->
                    countText = text
                    text.toIntOrNull()?.let { count ->
                        onEntryUpdate(entry.copy(erectionCount = count))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("勃起次數") },
                singleLine = true,
                placeholder = { Text("輸入次數...") }
            )
        }
    }
}

@Composable
private fun UnlockQuestion(
    entry: DailyEntry,
    onEntryUpdate: (DailyEntry) -> Unit
) {
    QuestionSection(
        title = "3. 今天是否解鎖？",
        subtitle = "如果解鎖後自慰,記錄詳情"
    ) {
        YesNoToggle(
            value = entry.unlocked,
            onValueChange = { unlocked ->
                onEntryUpdate(entry.copy(unlocked = unlocked))
            },
            label = "有解鎖"
        )
        
        if (entry.unlocked) {
            Spacer(modifier = Modifier.height(12.dp))
            
            Text("解鎖後有自慰？", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(6.dp))
            YesNoToggle(
                value = entry.masturbated,
                onValueChange = { masturbated ->
                    onEntryUpdate(entry.copy(masturbated = masturbated))
                },
                label = "解鎖後自慰"
            )
            
            if (entry.masturbated) {
                Spacer(modifier = Modifier.height(12.dp))
                DurationPicker(
                    selectedMinutes = entry.masturbationDuration,
                    onDurationSelected = { duration ->
                        onEntryUpdate(entry.copy(masturbationDuration = duration))
                    },
                    quickOptions = Constants.DURATION_QUICK_OPTIONS,
                    label = "自慰時長"
                )
            }
        }
    }
}

@Composable
private fun DiscomfortQuestion(
    entry: DailyEntry,
    onEntryUpdate: (DailyEntry) -> Unit
) {
    QuestionSection(
        title = "4. 今天是否感到不適或疼痛？",
        subtitle = "如果有,記錄部位與程度"
    ) {
        YesNoToggle(
            value = entry.hasDiscomfort,
            onValueChange = { has ->
                onEntryUpdate(entry.copy(hasDiscomfort = has))
            },
            label = "有不適或疼痛"
        )
        
        if (entry.hasDiscomfort) {
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "不適部位",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            MultiSelectChipGroup(
                options = Constants.DISCOMFORT_AREAS,
                selectedOptions = entry.discomfortAreas,
                onSelectionChange = { areas ->
                    onEntryUpdate(entry.copy(discomfortAreas = areas))
                }
            )
            
            if (entry.discomfortAreas.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                SliderWithLabel(
                    value = entry.discomfortLevel?.toFloat() ?: 3f,
                    onValueChange = { value ->
                        onEntryUpdate(entry.copy(discomfortLevel = value.toInt()))
                    },
                    valueRange = 1f..10f,
                    steps = 8,
                    label = "疼痛程度 (1=輕微, 10=劇烈)"
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LeakageQuestion(
    entry: DailyEntry,
    onEntryUpdate: (DailyEntry) -> Unit
) {
    QuestionSection(
        title = "5. 今天是否有洩漏情況？",
        subtitle = "如果有,評估洩漏程度"
    ) {
        YesNoToggle(
            value = entry.hadLeakage,
            onValueChange = { had ->
                onEntryUpdate(entry.copy(hadLeakage = had))
            },
            label = "有洩漏"
        )
        
        if (entry.hadLeakage) {
            Spacer(modifier = Modifier.height(12.dp))
            
            val amounts = listOf("少量", "中等", "大量")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                amounts.forEach { amount ->
                    FilterChip(
                        selected = entry.leakageAmount == amount,
                        onClick = { onEntryUpdate(entry.copy(leakageAmount = amount)) },
                        label = { Text(amount) },
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
            }
        }
    }
}

@Composable
private fun EdgingQuestion(
    entry: DailyEntry,
    onEntryUpdate: (DailyEntry) -> Unit
) {
    QuestionSection(
        title = "6. 今天是否進行邊緣訓練？",
        subtitle = "如果有,記錄訓練詳情"
    ) {
        YesNoToggle(
            value = entry.hadEdging,
            onValueChange = { had ->
                onEntryUpdate(entry.copy(hadEdging = had))
            },
            label = "有邊緣訓練"
        )
        
        if (entry.hadEdging) {
            Spacer(modifier = Modifier.height(12.dp))
            
            DurationPicker(
                selectedMinutes = entry.edgingDuration,
                onDurationSelected = { duration ->
                    onEntryUpdate(entry.copy(edgingDuration = duration))
                },
                quickOptions = Constants.DURATION_QUICK_OPTIONS,
                label = "訓練時長"
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "訓練方式",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            MultiSelectChipGroup(
                options = Constants.EDGING_METHODS,
                selectedOptions = entry.edgingMethods,
                onSelectionChange = { methods ->
                    onEntryUpdate(entry.copy(edgingMethods = methods))
                }
            )
        }
    }
}

@Composable
private fun RemovalQuestion(
    entry: DailyEntry,
    onEntryUpdate: (DailyEntry) -> Unit
) {
    QuestionSection(
        title = "7. 今天是否臨時取下鎖？",
        subtitle = "如果有,記錄取下詳情"
    ) {
        YesNoToggle(
            value = entry.temporarilyRemoved,
            onValueChange = { removed ->
                onEntryUpdate(entry.copy(temporarilyRemoved = removed))
            },
            label = "有臨時取下"
        )
        
        if (entry.temporarilyRemoved) {
            Spacer(modifier = Modifier.height(12.dp))
            
            DurationPicker(
                selectedMinutes = entry.removalDuration,
                onDurationSelected = { duration ->
                    onEntryUpdate(entry.copy(removalDuration = duration))
                },
                quickOptions = Constants.DURATION_QUICK_OPTIONS,
                label = "取下時長"
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "取下原因",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            MultiSelectChipGroup(
                options = Constants.REMOVAL_REASONS,
                selectedOptions = entry.removalReasons,
                onSelectionChange = { reasons ->
                    onEntryUpdate(entry.copy(removalReasons = reasons))
                }
            )
        }
    }
}

@Composable
private fun NightErectionQuestion(
    entry: DailyEntry,
    onEntryUpdate: (DailyEntry) -> Unit
) {
    QuestionSection(
        title = "8. 昨晚是否有夜間勃起？",
        subtitle = "如果有,記錄詳情"
    ) {
        var countText by remember(entry.nightErections) { 
            mutableStateOf(entry.nightErections?.toString() ?: "0") 
        }
        
        OutlinedTextField(
            value = countText,
            onValueChange = { text ->
                countText = text
                text.toIntOrNull()?.let { count ->
                    onEntryUpdate(entry.copy(nightErections = count))
                }
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("夜間勃起次數") },
            singleLine = true,
            placeholder = { Text("輸入次數 (0 表示沒有)...") }
        )
        
        if ((entry.nightErections ?: 0) > 0) {
            Spacer(modifier = Modifier.height(12.dp))
            
            YesNoToggle(
                value = entry.wokeUpFromErection,
                onValueChange = { woke ->
                    onEntryUpdate(entry.copy(wokeUpFromErection = woke))
                },
                label = "是否因勃起醒來"
            )
        }
    }
}
