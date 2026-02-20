package com.chastity.diary.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chastity.diary.ui.components.*
import com.chastity.diary.viewmodel.DashboardState
import com.chastity.diary.viewmodel.DashboardViewModel
import com.chastity.diary.viewmodel.TimeRange
import java.time.format.DateTimeFormatter

/**
 * Dashboard screen with statistics
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel()
) {
    val dashboardState by viewModel.dashboardState.collectAsState()
    val currentStreak by viewModel.currentStreak.collectAsState()
    val longestStreak by viewModel.longestStreak.collectAsState()
    val timeRange by viewModel.timeRange.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("儀表板") }
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
                        text = "錯誤: ${state.message}",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is DashboardState.Success -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
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
                                                TimeRange.WEEK -> "本週"
                                                TimeRange.MONTH -> "本月"
                                                TimeRange.THREE_MONTHS -> "3個月"
                                                TimeRange.ALL -> "全部"
                                            }
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        
                        // Statistics cards
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatCard(
                                title = "總配戴天數",
                                value = "${state.totalDays}",
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                title = "記錄完成率",
                                value = String.format("%.1f%%", state.completionRate),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatCard(
                                title = "連續打卡",
                                value = "$currentStreak 天 🔥",
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                title = "最長連續",
                                value = "$longestStreak 天",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        // Streak Achievement Badges
                        StreakBadgesSection(
                            currentStreak = currentStreak,
                            longestStreak = longestStreak
                        )
                        
                        // Calendar Heatmap
                        CalendarHeatmap(
                            title = "記錄完成度",
                            dates = state.entries
                                .takeLast(7)
                                .associate { 
                                    it.date.format(DateTimeFormatter.ISO_DATE) to 3 
                                }
                        )
                        
                        // Mood Trend Chart
                        if (state.moodTrend.isNotEmpty()) {
                            TrendLineChart(
                                title = "心情趨勢 (1=挫折 → 5=開心)",
                                data = state.moodTrend.takeLast(14)
                            )
                        }
                        
                        // Desire Level Trend
                        TrendLineChart(
                            title = "性慾強度趨勢 (1-10)",
                            data = state.entries
                                .takeLast(14)
                                .mapNotNull { it.desireLevel?.toFloat() }
                        )
                        
                        // Comfort Rating Trend
                        TrendLineChart(
                            title = "舒適度趨勢 (1-5)",
                            data = state.entries
                                .takeLast(14)
                                .mapNotNull { it.comfortRating?.toFloat() }
                        )
                        
                        // Exercise Statistics
                        val exerciseStats = state.entries
                            .filter { it.exercised }
                            .flatMap { it.exerciseTypes }
                            .groupingBy { it }
                            .eachCount()
                            .mapValues { it.value.toFloat() }
                        
                        StatColumnChart(
                            title = "運動類型統計",
                            data = exerciseStats
                        )
                        
                        // Summary Statistics
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "統計摘要",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Divider()
                                StatRow("平均性慾強度", String.format("%.1f / 10", state.averageDesireLevel))
                                StatRow("平均舒適度", String.format("%.1f / 5", state.averageComfortRating))
                                StatRow("色情內容接觸", "${state.pornViewCount} 次")
                                StatRow("自慰次數", "${state.masturbationCount} 次")
                                StatRow("運動次數", "${state.exerciseCount} 次")
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
