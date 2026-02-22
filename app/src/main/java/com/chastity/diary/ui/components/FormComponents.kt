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
 *
 * PERF-FIX: 使用本地緩衝 state 分離「拖動中顯示值」與「提交至 ViewModel 的值」。
 * 原本每次拖動都呼叫 onValueChange → updateEntry → _entryState 改變 →
 * 整個 DailyEntryTabContent（含另一個 Tab）觸發 Recompose，一秒可能發生數十次。
 * 改為：
 *   • 拖動中：僅更新 localValue（只影響此 Composable，成本極低）
 *   • 手指放開：透過 onValueChangeFinished 才提交一次到 ViewModel
 * remember(value) 確保從外部載入新日記時 localValue 會同步重置。
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
    var localValue by remember(value) { mutableStateOf(value) }

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
                text = valueFormatter(localValue),   // 顯示即時本地值，回饋無延遲
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = localValue,
            onValueChange = { localValue = it },                    // 僅更新本地 UI，不觸發外部狀態
            onValueChangeFinished = { onValueChange(localValue) },  // 放開後才提交一次
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
                    label = { Text("⭐") },
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
    // PERF-FIX: 用 remember(moods) 快取 chunked 結果，避免每次 recompose 都建立新 List
    val rows = remember(moods) { moods.chunked(4) }
    Column(modifier = modifier) {
        rows.forEach { rowMoods ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowMoods.forEach { mood ->
                    FilterChip(
                        selected = selectedMood == mood,
                        onClick = { onMoodSelected(mood) },
                        label = { Text(mood) },
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
    // PERF-FIX: 快取 chunked 結果，避免每次 recompose 都建立新 List 物件
    val rows = remember(options) { options.chunked(3) }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        rows.forEach { rowOptions ->
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
            repeat(3 - (quickOptions.size - 3).coerceAtMost(3)) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

/**
 * Yes/No chip pair — replaces Switch to make selection state explicit.
 * [label] is kept for call-site compatibility but not displayed (parent QuestionSection provides context).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YesNoToggle(
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = value,
            onClick = { onValueChange(true) },
            label = { Text("是") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            ),
            border = FilterChipDefaults.filterChipBorder(
                borderColor = MaterialTheme.colorScheme.outline,
                selectedBorderColor = MaterialTheme.colorScheme.primary,
            ),
        )
        FilterChip(
            selected = !value,
            onClick = { onValueChange(false) },
            label = { Text("否") },
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
