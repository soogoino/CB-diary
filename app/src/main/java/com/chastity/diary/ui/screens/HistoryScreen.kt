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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chastity.diary.domain.model.DailyEntry
import com.chastity.diary.viewmodel.DashboardState
import com.chastity.diary.viewmodel.DashboardViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: DashboardViewModel = viewModel(),
    outerPadding: PaddingValues = PaddingValues()
) {
    val dashboardState by viewModel.dashboardState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("歷史紀錄") })
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = dashboardState) {
                is DashboardState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                is DashboardState.Error -> Text("錯誤: ${state.message}", Modifier.align(Alignment.Center))
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
                        // 月份心情日曆
                        MoodCalendarSection(entries = state.entries)

                        // 近期記錄列表
                        RecentEntriesSection(entries = state.entries)
                    }
                }
            }
        }
    }
}

// ─── 🗓 Mood Calendar ─────────────────────────────────────────────────────────
@Composable
fun MoodCalendarSection(entries: List<DailyEntry>) {
    val today = LocalDate.now()
    val yearMonth = YearMonth.from(today)
    val firstDay = yearMonth.atDay(1)
    val daysInMonth = yearMonth.lengthOfMonth()
    val startOffset = firstDay.dayOfWeek.value % 7 // 0=Sun
    val entryMap = entries.associateBy { it.date }
    val weekDays = listOf("日", "一", "二", "三", "四", "五", "六")

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "${today.year}年${today.monthValue}月 心情日曆",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            // 星期標題列
            Row(Modifier.fillMaxWidth()) {
                weekDays.forEach { d ->
                    Text(
                        d, modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
            // 日曆格
            val totalCells = startOffset + daysInMonth
            val rows = (totalCells + 6) / 7
            (0 until rows).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    (0 until 7).forEach { col ->
                        val cellIndex = row * 7 + col
                        val day = cellIndex - startOffset + 1
                        if (day < 1 || day > daysInMonth) {
                            Spacer(Modifier.weight(1f).aspectRatio(1f))
                        } else {
                            val date = yearMonth.atDay(day)
                            val entry = entryMap[date]
                            val isToday = date == today
                            val isFuture = date.isAfter(today)
                            val moodEmoji = entry?.mood?.take(2) ?: ""
                            val bgColor = when {
                                isFuture -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                entry == null -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                else -> MaterialTheme.colorScheme.primaryContainer
                            }
                            Box(
                                Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(bgColor)
                                    .then(
                                        if (isToday) Modifier.border(
                                            2.dp, MaterialTheme.colorScheme.primary,
                                            RoundedCornerShape(6.dp)
                                        ) else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    if (moodEmoji.isNotEmpty()) {
                                        Text(moodEmoji, fontSize = 12.sp, lineHeight = 14.sp)
                                    }
                                    Text(
                                        "$day",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isFuture)
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                                        else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }
            // 圖例
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendDot(MaterialTheme.colorScheme.primaryContainer, "有記錄")
                LegendDot(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), "未記錄")
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Text(
            label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

// ─── 📋 Recent Entries List ───────────────────────────────────────────────────
@Composable
private fun RecentEntriesSection(entries: List<DailyEntry>) {
    val sorted = entries.sortedByDescending { it.date }
    if (sorted.isEmpty()) return

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("近期記錄", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Divider()
            sorted.take(30).forEach { entry ->
                EntryRow(entry)
                if (entry != sorted.take(30).last()) Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            }
        }
    }
}

@Composable
private fun EntryRow(entry: DailyEntry) {
    val formatter = DateTimeFormatter.ofPattern("MM/dd (EEE)", java.util.Locale.TAIWAN)
    val score = buildString {
        var s = 0; var t = 0
        t++; if (entry.deviceCheckPassed) s++
        entry.mood?.let { t++; s++ }
        entry.desireLevel?.let { t++; s++ }
        if (entry.deviceCheckPassed) { entry.comfortRating?.let { t++; s++ } }
        entry.focusLevel?.let { t++; s++ }
        t++; if (entry.exercised) s++
        entry.cleaningType?.let { t++; s++ }
        append("$s/$t")
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    entry.date.format(formatter),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    entry.mood?.let { Text(it.take(2), fontSize = 14.sp) }
                    if (entry.deviceCheckPassed) Text("🔒", fontSize = 13.sp)
                    if (entry.exercised) Text("🏃", fontSize = 13.sp)
                    if (entry.keyholderInteraction) Text("💬", fontSize = 13.sp)
                    if (entry.photoPath != null) Text("📷", fontSize = 13.sp)
                    if (entry.unlocked) Text("🔓", fontSize = 13.sp)
                }
            }
            Text(
                score,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        // 教事文字
        if (!entry.notes.isNullOrBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = entry.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    maxLines = 5,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}
