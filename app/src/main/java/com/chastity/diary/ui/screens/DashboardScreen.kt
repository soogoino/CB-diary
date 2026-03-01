package com.chastity.diary.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chastity.diary.R
import com.chastity.diary.domain.model.DailyEntry
import com.chastity.diary.domain.model.HeatmapTimeRange
import com.chastity.diary.ui.components.*
import com.chastity.diary.util.Constants
import com.chastity.diary.viewmodel.DashboardState
import com.chastity.diary.viewmodel.DashboardViewModel
import com.chastity.diary.viewmodel.TimeRange
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * Dashboard screen with statistics
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel(),
    outerPadding: PaddingValues = PaddingValues()
) {
    val dashboardState by viewModel.dashboardState.collectAsState()
    val currentStreak by viewModel.currentStreak.collectAsState()
    val longestStreak by viewModel.longestStreak.collectAsState()
    val timeRange by viewModel.timeRange.collectAsState()
    val heatmapTimeRange by viewModel.heatmapTimeRange.collectAsState()
    val heatmapData by viewModel.heatmapData.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.dashboard_title)) }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = dashboardState) {
                is DashboardState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is DashboardState.Error -> {
                    Text(
                        text = stringResource(R.string.error_prefix, state.message),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is DashboardState.Success -> {
                    // P2: Cache derived pairs (date, value) for charts
                    val dateFmt = remember { java.time.format.DateTimeFormatter.ofPattern("M/d") }
                    val last14 = remember(state.entries) { state.entries.takeLast(14) }
                    // Hoist stringResource calls — composable calls are not allowed inside remember's crossinline lambda
                    val labelDesire    = stringResource(R.string.metric_desire_label)
                    val labelComfort   = stringResource(R.string.metric_comfort_label)
                    val labelFocus     = stringResource(R.string.metric_focus_label)
                    val labelSleep     = stringResource(R.string.metric_sleep_label)
                    val labelEnergy    = stringResource(R.string.metric_energy_label)
                    val moodHappy      = stringResource(R.string.mood_happy)
                    val moodCalm       = stringResource(R.string.mood_calm)
                    val moodNeutral    = stringResource(R.string.mood_neutral)
                    val moodDepressed  = stringResource(R.string.mood_depressed)
                    val moodAnxious    = stringResource(R.string.mood_anxious)
                    val moodFrustrated = stringResource(R.string.mood_frustrated)
                    val metricSeries = remember(last14) {
                        listOf(
                            Triple(labelDesire,   Color(0xFF6650A4), last14.mapNotNull { e -> e.desireLevel?.let { e.date to it.toFloat() } }),
                            Triple(labelComfort,  Color(0xFF0288D1), last14.mapNotNull { e -> e.comfortRating?.let { e.date to it.toFloat() } }),
                            Triple(labelFocus,    Color(0xFF2E7D32), last14.mapNotNull { e -> e.focusLevel?.let { e.date to it.toFloat() } }),
                            Triple(labelSleep,    Color(0xFFF57C00), last14.mapNotNull { e -> e.sleepQuality?.let { e.date to it.toFloat() } }),
                            Triple(labelEnergy,   Color(0xFFE53935), last14.mapNotNull { e -> e.morningEnergy?.let { e.date to it.toFloat() } }),
                        )
                    }
                    val moodPairs = remember(last14, moodHappy, moodCalm, moodNeutral, moodDepressed, moodAnxious, moodFrustrated) {
                        last14.mapNotNull { e ->
                            val score = when (e.mood) {
                                moodHappy      -> 5f
                                moodCalm       -> 4f
                                moodNeutral    -> 3f
                                moodDepressed  -> 2f
                                moodAnxious    -> 1.5f
                                moodFrustrated -> 1f
                                else           -> null
                            }
                            score?.let { e.date to it }
                        }
                    }
                    val moodLabels = remember(moodPairs) { moodPairs.map { it.first.format(dateFmt) } }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(
                                start = 16.dp, end = 16.dp, top = 16.dp,
                                bottom = outerPadding.calculateBottomPadding() + 16.dp
                            ),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Time range selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TimeRange.values().forEach { range ->
                                FilterChip(
                                    selected = timeRange == range,
                                    onClick = { viewModel.setTimeRange(range) },
                                    label = {
                                        Text(
                                            when (range) {
                                                TimeRange.WEEK -> stringResource(R.string.time_range_week)
                                                TimeRange.MONTH -> stringResource(R.string.time_range_month)
                                                TimeRange.THREE_MONTHS -> stringResource(R.string.time_range_3months)
                                                TimeRange.ALL -> stringResource(R.string.time_range_all)
                                            }
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
                        
                        // Statistics cards
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatCard(
                                title = stringResource(R.string.stat_total_days),
                                value = "${state.totalDays}",
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                title = stringResource(R.string.stat_completion_rate),
                                value = String.format("%.1f%%", state.completionRate),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatCard(
                                title = stringResource(R.string.stat_streak),
                                value = stringResource(R.string.dashboard_streak_format, currentStreak),
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                title = stringResource(R.string.stat_longest_streak),
                                value = stringResource(R.string.dashboard_longest_format, longestStreak),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        // Streak Achievement Badges
                        StreakBadgesSection(
                            currentStreak = currentStreak,
                            longestStreak = longestStreak
                        )
                        
                        // Mood Trend Chart
                        if (moodPairs.isNotEmpty()) {
                            TrendLineChart(
                                title = stringResource(R.string.chart_mood_trend),
                                data = moodPairs.map { it.second },
                                labels = moodLabels
                            )
                        }

                        // Combined 1-10 metric trend chart
                        MultiTrendLineChart(
                            title = stringResource(R.string.chart_metric_trend),
                            series = metricSeries
                        )

                        // Action Heatmap — Yes/No 題目橫軸日期熱力圖
                        ActionHeatmapSection(
                            heatmapData = heatmapData,
                            selectedTimeRange = heatmapTimeRange,
                            onTimeRangeChange = viewModel::setHeatmapTimeRange
                        )

                        // Summary Statistics（放在最後）
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.section_summary),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Divider()
                                StatRow(stringResource(R.string.summary_avg_desire), String.format("%.1f / 10", state.averageDesireLevel))
                                StatRow(stringResource(R.string.summary_avg_comfort), String.format("%.1f / 10", state.averageComfortRating))
                                StatRow(stringResource(R.string.summary_avg_focus), String.format("%.1f / 10", state.averageFocusLevel))
                                StatRow(stringResource(R.string.summary_avg_sleep), String.format("%.1f / 10", state.averageSleepQuality))
                                StatRow(stringResource(R.string.summary_avg_energy), String.format("%.1f / 5", state.averageMorningEnergy))
                                    StatRow(stringResource(R.string.summary_masturbation_count), stringResource(R.string.count_times, state.masturbationCount))
                                    StatRow(stringResource(R.string.summary_exercise_count), stringResource(R.string.count_times, state.exerciseCount))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
@Composable
fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}
