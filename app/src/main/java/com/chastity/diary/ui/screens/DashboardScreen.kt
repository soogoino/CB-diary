package com.chastity.diary.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chastity.diary.domain.model.DailyEntry
import com.chastity.diary.ui.components.*
import com.chastity.diary.viewmodel.DashboardState
import com.chastity.diary.viewmodel.DashboardViewModel
import com.chastity.diary.viewmodel.TimeRange
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.sin

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
                                                TimeRange.WEEK -> "本週"
                                                TimeRange.MONTH -> "本月"
                                                TimeRange.THREE_MONTHS -> "3個月"
                                                TimeRange.ALL -> "全部"
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
                        
                        //  Discipline Radar
                        DisciplineRadarSection(entries = state.entries)

                        // 📊 Behavior Timeline (14 days × 7 behaviors)
                        BehaviorTimelineSection(entries = state.entries)
                        
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

// ─── 🕸 Discipline Radar ──────────────────────────────────────────────────────
@Composable
fun DisciplineRadarSection(entries: List<DailyEntry>) {
    if (entries.isEmpty()) return
    val n = entries.size.toFloat()

    val avgDesire    = entries.mapNotNull { it.desireLevel }.average().toFloat().takeIf { !it.isNaN() } ?: 5f
    val avgFocus     = entries.mapNotNull { it.focusLevel }.average().toFloat().takeIf { !it.isNaN() } ?: 5f
    val avgSleep     = entries.mapNotNull { it.sleepQuality }.average().toFloat().takeIf { !it.isNaN() } ?: 3f
    val avgComfort   = entries.mapNotNull { it.comfortRating }.average().toFloat().takeIf { !it.isNaN() } ?: 3f
    val exerciseRate = entries.count { it.exercised } / n
    val integrityRate= entries.count { it.cleaningType != null && it.cleaningType != "未清潔" && !it.unlocked } / n

    // Normalize all to 0..1
    val values = listOf(
        (10f - avgDesire) / 10f,  // 慾望控制：低慾高分
        avgFocus / 10f,            // 專注力
        (avgSleep * 2f) / 10f,    // 睡眠品質 (1-5 → 2-10)
        (avgComfort * 2f) / 10f,  // 身體舒適 (1-5 → 2-10)
        exerciseRate,              // 運動習慣
        integrityRate              // 自律誠信
    )
    val labels = listOf("慾望控制", "專注力", "睡眠品質", "身體舒適", "運動習慣", "自律誠信")

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("自律雷達", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            val primaryColor = MaterialTheme.colorScheme.primary
            val outlineColor = MaterialTheme.colorScheme.outlineVariant
            val fillColor    = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)

            Box(
                Modifier.fillMaxWidth().height(220.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.size(200.dp)) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val R  = size.width / 2f * 0.72f
                    val axisCount = 6
                    val angleStep = (2 * Math.PI / axisCount).toFloat()
                    val startAngle = (-Math.PI / 2).toFloat() // top

                    // Background grid (3 levels)
                    for (level in 1..3) {
                        val r = R * level / 3f
                        val path = Path()
                        for (i in 0 until axisCount) {
                            val angle = startAngle + i * angleStep
                            val x = cx + r * cos(angle)
                            val y = cy + r * sin(angle)
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        path.close()
                        drawPath(path, outlineColor, style = Stroke(width = 1.dp.toPx()))
                    }

                    // Axis lines
                    for (i in 0 until axisCount) {
                        val angle = startAngle + i * angleStep
                        drawLine(
                            outlineColor,
                            Offset(cx, cy),
                            Offset(cx + R * cos(angle), cy + R * sin(angle)),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // Data polygon
                    val dataPath = Path()
                    values.forEachIndexed { i, v ->
                        val angle = startAngle + i * angleStep
                        val r = R * v.coerceIn(0f, 1f)
                        val x = cx + r * cos(angle)
                        val y = cy + r * sin(angle)
                        if (i == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
                    }
                    dataPath.close()
                    drawPath(dataPath, fillColor)
                    drawPath(dataPath, primaryColor, style = Stroke(width = 2.dp.toPx()))

                    // Data dots
                    values.forEachIndexed { i, v ->
                        val angle = startAngle + i * angleStep
                        val r = R * v.coerceIn(0f, 1f)
                        drawCircle(primaryColor, radius = 4.dp.toPx(),
                            center = Offset(cx + r * cos(angle), cy + r * sin(angle)))
                    }
                }
            }

            // Labels grid
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                values.zip(labels).chunked(2).forEach { pair ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEach { (v, label) ->
                            Row(
                                Modifier.weight(1f),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(label, style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                Text(
                                    "${(v * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── 📊 Behavior Timeline ─────────────────────────────────────────────────────
@Composable
fun BehaviorTimelineSection(entries: List<DailyEntry>) {
    val today = LocalDate.now()
    val days = (13 downTo 0).map { today.minusDays(it.toLong()) }
    val entryMap = entries.associateBy { it.date }

    data class Behavior(val emoji: String, val label: String, val check: (DailyEntry) -> Boolean)
    val behaviors = listOf(
        Behavior("🏃", "運動")   { it.exercised },
        Behavior("🧹", "清潔")   { it.cleaningType != null && it.cleaningType != "未清潔" },
        Behavior("📷", "打卡")   { it.photoPath != null },
        Behavior("💬", "KH互動") { it.keyholderInteraction },
        Behavior("🔓", "解鎖")   { it.unlocked },
        Behavior("😈", "邊緣")   { it.hadEdging },
        Behavior("😴", "因鎖醒") { it.wokeUpDueToDevice },
    )

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("行為熱力圖（近 14 天）", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            // Day headers
            Row(Modifier.fillMaxWidth()) {
                Spacer(Modifier.width(44.dp))
                days.forEach { date ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            date.dayOfMonth.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            color = if (date == today) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontWeight = if (date == today) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // Behavior rows
            behaviors.forEach { behavior ->
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        Modifier.width(44.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(behavior.emoji, fontSize = 11.sp)
                    }
                    days.forEach { date ->
                        val entry = entryMap[date]
                        val isFuture = date.isAfter(today)
                        val dotColor = when {
                            isFuture || entry == null -> MaterialTheme.colorScheme.surfaceVariant
                            behavior.check(entry) -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.outlineVariant
                        }
                        val alpha = if (isFuture || entry == null) 0.2f else 1f
                        Box(
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(1.5.dp)
                                .clip(CircleShape)
                                .background(dotColor.copy(alpha = alpha))
                        )
                    }
                }
            }

            // Legend
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendDot(MaterialTheme.colorScheme.primary, "有")
                LegendDot(MaterialTheme.colorScheme.outlineVariant, "沒有")
                LegendDot(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), "未記錄")
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
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
