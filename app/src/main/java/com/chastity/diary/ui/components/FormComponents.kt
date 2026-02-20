package com.chastity.diary.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Question section wrapper
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionSection(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            content()
        }
    }
}

/**
 * Labeled slider
 */
@Composable
fun SliderWithLabel(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 1f..10f,
    steps: Int = 8,
    label: String,
    valueFormatter: (Float) -> String = { it.toInt().toString() },
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = valueFormatter(value),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps
        )
    }
}

/**
 * Star rating
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StarRating(
    rating: Int,
    onRatingChange: (Int) -> Unit,
    maxStars: Int = 5,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(maxStars) { index ->
                val starValue = index + 1
                FilterChip(
                    selected = rating >= starValue,
                    onClick = { onRatingChange(starValue) },
                    label = { Text("⭐") }
                )
            }
        }
    }
}

/**
 * Mood selector with emoji
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodSelector(
    selectedMood: String?,
    moods: List<String>,
    onMoodSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        moods.chunked(4).forEach { rowMoods ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowMoods.forEach { mood ->
                    FilterChip(
                        selected = selectedMood == mood,
                        onClick = { onMoodSelected(mood) },
                        label = { Text(mood) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // Fill remaining spaces
                repeat(4 - rowMoods.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * Multi-select chip group
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiSelectChipGroup(
    options: List<String>,
    selectedOptions: List<String>,
    onSelectionChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.chunked(3).forEach { rowOptions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowOptions.forEach { option ->
                    FilterChip(
                        selected = option in selectedOptions,
                        onClick = {
                            val newSelection = if (option in selectedOptions) {
                                selectedOptions - option
                            } else {
                                selectedOptions + option
                            }
                            onSelectionChange(newSelection)
                        },
                        label = { Text(option) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // Fill remaining spaces
                repeat(3 - rowOptions.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * Duration picker with quick options
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DurationPicker(
    selectedMinutes: Int?,
    onDurationSelected: (Int) -> Unit,
    quickOptions: List<Int> = listOf(5, 10, 15, 30, 60, 120),
    label: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickOptions.take(3).forEach { minutes ->
                FilterChip(
                    selected = selectedMinutes == minutes,
                    onClick = { onDurationSelected(minutes) },
                    label = { 
                        Text(
                            if (minutes < 60) "${minutes}分" 
                            else "${minutes / 60}小時"
                        ) 
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickOptions.drop(3).forEach { minutes ->
                FilterChip(
                    selected = selectedMinutes == minutes,
                    onClick = { onDurationSelected(minutes) },
                    label = { 
                        Text(
                            if (minutes < 60) "${minutes}分" 
                            else "${minutes / 60}小時"
                        ) 
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            // Fill remaining
            repeat(3 - (quickOptions.size - 3).coerceAtMost(3)) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

/**
 * Yes/No toggle
 */
@Composable
fun YesNoToggle(
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Switch(
            checked = value,
            onCheckedChange = onValueChange
        )
    }
}
