package com.chastity.diary.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chastity.diary.R
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
                    text = stringResource(R.string.cq_followup_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = stringResource(R.string.cq_followup_desc),
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
        title = stringResource(R.string.cq_q1_title),
        subtitle = stringResource(R.string.cq_q1_subtitle)
    ) {
        YesNoToggle(
            value = entry.viewedPorn,
            onValueChange = { viewed ->
                onEntryUpdate(entry.copy(viewedPorn = viewed))
            },
            label = stringResource(R.string.cq_q1_label)
        )
        
        if (entry.viewedPorn) {
            Spacer(modifier = Modifier.height(12.dp))
            DurationPicker(
                selectedMinutes = entry.pornDuration,
                onDurationSelected = { duration ->
                    onEntryUpdate(entry.copy(pornDuration = duration))
                },
                quickOptions = Constants.DURATION_QUICK_OPTIONS,
                label = stringResource(R.string.cq_q1_duration_label)
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
        title = stringResource(R.string.cq_q2_title),
        subtitle = stringResource(R.string.cq_q2_subtitle)
    ) {
        YesNoToggle(
            value = entry.hadErection,
            onValueChange = { had ->
                onEntryUpdate(entry.copy(hadErection = had))
            },
            label = stringResource(R.string.cq_q2_label)
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
                label = { Text(stringResource(R.string.cq_q2_count_label)) },
                singleLine = true,
                placeholder = { Text(stringResource(R.string.cq_q2_count_placeholder)) }
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
        title = stringResource(R.string.cq_q3_title),
        subtitle = stringResource(R.string.cq_q3_subtitle)
    ) {
        YesNoToggle(
            value = entry.unlocked,
            onValueChange = { unlocked ->
                onEntryUpdate(entry.copy(unlocked = unlocked))
            },
            label = stringResource(R.string.cq_q3_label)
        )
        
        if (entry.unlocked) {
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(stringResource(R.string.cq_q3_after_unlock_question), style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(6.dp))
            YesNoToggle(
                value = entry.masturbated,
                onValueChange = { masturbated ->
                    onEntryUpdate(entry.copy(masturbated = masturbated))
                },
                label = stringResource(R.string.cq_q3_after_unlock_label)
            )
            
            if (entry.masturbated) {
                Spacer(modifier = Modifier.height(12.dp))
                DurationPicker(
                    selectedMinutes = entry.masturbationDuration,
                    onDurationSelected = { duration ->
                        onEntryUpdate(entry.copy(masturbationDuration = duration))
                    },
                    quickOptions = Constants.DURATION_QUICK_OPTIONS,
                    label = stringResource(R.string.cq_q3_masturbation_duration_label)
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
        title = stringResource(R.string.cq_q4_title),
        subtitle = stringResource(R.string.cq_q4_subtitle)
    ) {
        YesNoToggle(
            value = entry.hasDiscomfort,
            onValueChange = { has ->
                onEntryUpdate(entry.copy(hasDiscomfort = has))
            },
            label = stringResource(R.string.cq_q4_label)
        )
        
        if (entry.hasDiscomfort) {
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = stringResource(R.string.cq_q4_discomfort_area_label),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            MultiSelectChipGroup(
                options = stringArrayResource(R.array.discomfort_areas_array).toList(),
                selectedOptions = entry.discomfortAreas,
                storageKeys = Constants.DISCOMFORT_AREAS,
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
                    label = stringResource(R.string.cq_q4_pain_level_label)
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
        title = stringResource(R.string.cq_q5_title),
        subtitle = stringResource(R.string.cq_q5_subtitle)
    ) {
        YesNoToggle(
            value = entry.hadLeakage,
            onValueChange = { had ->
                onEntryUpdate(entry.copy(hadLeakage = had))
            },
            label = stringResource(R.string.cq_q5_label)
        )
        
        if (entry.hadLeakage) {
            Spacer(modifier = Modifier.height(12.dp))

            val displayAmounts = stringArrayResource(R.array.leakage_amounts_array)
            val keyAmounts = Constants.LEAKAGE_AMOUNTS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                displayAmounts.forEachIndexed { index, label ->
                    val key = keyAmounts.getOrElse(index) { label }
                    FilterChip(
                        selected = entry.leakageAmount == key,
                        onClick = { onEntryUpdate(entry.copy(leakageAmount = key)) },
                        label = { Text(label) },
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
        title = stringResource(R.string.cq_q6_title),
        subtitle = stringResource(R.string.cq_q6_subtitle)
    ) {
        YesNoToggle(
            value = entry.hadEdging,
            onValueChange = { had ->
                onEntryUpdate(entry.copy(hadEdging = had))
            },
            label = stringResource(R.string.cq_q6_label)
        )
        
        if (entry.hadEdging) {
            Spacer(modifier = Modifier.height(12.dp))
            
            DurationPicker(
                selectedMinutes = entry.edgingDuration,
                onDurationSelected = { duration ->
                    onEntryUpdate(entry.copy(edgingDuration = duration))
                },
                quickOptions = Constants.DURATION_QUICK_OPTIONS,
                label = stringResource(R.string.cq_q6_duration_label)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = stringResource(R.string.cq_q6_methods_label),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            MultiSelectChipGroup(
                options = stringArrayResource(R.array.edging_methods_array).toList(),
                selectedOptions = entry.edgingMethods,
                storageKeys = Constants.EDGING_METHODS,
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
        title = stringResource(R.string.cq_q7_title),
        subtitle = stringResource(R.string.cq_q7_subtitle)
    ) {
        YesNoToggle(
            value = entry.temporarilyRemoved,
            onValueChange = { removed ->
                onEntryUpdate(entry.copy(temporarilyRemoved = removed))
            },
            label = stringResource(R.string.cq_q7_label)
        )
        
        if (entry.temporarilyRemoved) {
            Spacer(modifier = Modifier.height(12.dp))
            
            DurationPicker(
                selectedMinutes = entry.removalDuration,
                onDurationSelected = { duration ->
                    onEntryUpdate(entry.copy(removalDuration = duration))
                },
                quickOptions = Constants.DURATION_QUICK_OPTIONS,
                label = stringResource(R.string.cq_q7_duration_label)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = stringResource(R.string.cq_q7_reason_label),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            MultiSelectChipGroup(
                options = stringArrayResource(R.array.removal_reasons_array).toList(),
                selectedOptions = entry.removalReasons,
                storageKeys = Constants.REMOVAL_REASONS,
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
        title = stringResource(R.string.cq_q8_title),
        subtitle = stringResource(R.string.cq_q8_subtitle)
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
            label = { Text(stringResource(R.string.cq_q8_count_label)) },
            singleLine = true,
            placeholder = { Text(stringResource(R.string.cq_q8_count_placeholder)) }
        )
        
        if ((entry.nightErections ?: 0) > 0) {
            Spacer(modifier = Modifier.height(12.dp))
            
            YesNoToggle(
                value = entry.wokeUpFromErection,
                onValueChange = { woke ->
                    onEntryUpdate(entry.copy(wokeUpFromErection = woke))
                },
                label = stringResource(R.string.cq_q8_woke_label)
            )
        }
    }
}
