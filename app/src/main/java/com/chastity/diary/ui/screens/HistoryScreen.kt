package com.chastity.diary.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
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
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chastity.diary.R
import com.chastity.diary.domain.model.DailyEntry
import com.chastity.diary.viewmodel.DailyEntryViewModel
import com.chastity.diary.viewmodel.DashboardState
import com.chastity.diary.viewmodel.DashboardViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: DashboardViewModel = viewModel(),
    dailyEntryViewModel: DailyEntryViewModel = viewModel(),
    onNavigateToDailyEntry: () -> Unit = {},
    outerPadding: PaddingValues = PaddingValues()
) {
    val dashboardState by viewModel.dashboardState.collectAsState()
    // H-1: Use allEntries for the calendar so every recorded date is visible regardless
    // of the currently selected time range (which defaults to WEEK on DashboardScreen).
    val allEntries by viewModel.allEntries.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.history_title)) })
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = dashboardState) {
                is DashboardState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                is DashboardState.Error -> Text(stringResource(R.string.error_prefix, state.message), Modifier.align(Alignment.Center))
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
                        MoodCalendarSection(
                            entries = allEntries,
                            onDateClick = { date ->
                                dailyEntryViewModel.selectDate(date)
                                onNavigateToDailyEntry()
                            }
                        )
                        // H-2: Use allEntries so every saved entry appears regardless of
                        // the timeRange selected on DashboardScreen. Pagination replaces take(30).
                        RecentEntriesSection(entries = allEntries)
                    }
                }
            }
        }
    }
}

// ─── 🗓 Mood Calendar ─────────────────────────────────────────────────────────
@Composable
fun MoodCalendarSection(entries: List<DailyEntry>, onDateClick: ((LocalDate) -> Unit)? = null) {
    // E-2: Wrap all derived values in remember{} — prevents O(n) HashMap rebuild on every recompose
    val today       = remember { LocalDate.now() }
    val yearMonth   = remember { YearMonth.from(today) }
    val firstDay    = remember { yearMonth.atDay(1) }
    val daysInMonth = remember { yearMonth.lengthOfMonth() }
    val startOffset = remember { firstDay.dayOfWeek.value % 7 } // 0=Sun
    val entryMap    = remember(entries) { entries.associateBy { it.date } }
    // Hoist out of remember — stringArrayResource is @Composable and cannot be called inside crossinline lambda
    val weekDays    = stringArrayResource(R.array.weekdays_short).toList()

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val monthPattern = if (Locale.getDefault().language.startsWith("zh"))
                stringResource(R.string.history_month_pattern_zh)
            else
                stringResource(R.string.history_month_pattern_en)

            val monthLabel = remember(monthPattern) {
                today.format(
                    DateTimeFormatter.ofPattern(monthPattern, Locale.getDefault())
                )
            }
            Text(
                stringResource(R.string.history_calendar_title, monthLabel),
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
                            // C-4: Dark-mode-safe colours — surfaceVariant (fully opaque) has
                            // enough luminance delta from surface in both light and dark themes,
                            // whereas surfaceVariant.copy(alpha=0.4f) blends into the background.
                            val bgColor = when {
                                isFuture -> Color.Transparent
                                entry == null -> MaterialTheme.colorScheme.surfaceVariant
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
                                    )
                                    .then(
                                        if (!isFuture && onDateClick != null)
                                            Modifier.clickable { onDateClick(date) }
                                        else Modifier
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
                LegendDot(MaterialTheme.colorScheme.primaryContainer, stringResource(R.string.history_has_record))
                LegendDot(MaterialTheme.colorScheme.surfaceVariant, stringResource(R.string.history_no_record))
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
private const val PAGE_SIZE = 10

@Composable
private fun RecentEntriesSection(entries: List<DailyEntry>) {
    // E-1: sort once per list change
    val sorted = remember(entries) { entries.sortedByDescending { it.date } }
    if (sorted.isEmpty()) return

    // Pagination state — persists across recompositions but resets when list changes
    var displayCount by remember(sorted) { mutableIntStateOf(PAGE_SIZE) }
    val display     = remember(sorted, displayCount) { sorted.take(displayCount) }
    val remaining   = sorted.size - displayCount

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.history_all_records), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    stringResource(R.string.history_total_records, sorted.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Divider()
            display.forEachIndexed { index, entry ->
                EntryRow(entry)
                if (index < display.lastIndex)
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            }
            // "Load more" button
            if (remaining > 0) {
                OutlinedButton(
                    onClick = { displayCount += PAGE_SIZE },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ExpandMore, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.history_load_more, remaining))
                }
            }
        }
    }
}

// E-3: Top-level constant avoids rebuilding DateTimeFormatter on every EntryRow recompose
private val ENTRY_DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd (EEE)", java.util.Locale.getDefault())

@Composable
private fun EntryRow(entry: DailyEntry) {
    // E-3: remember(entry) re-runs buildString only when the entry object changes
    val score = remember(entry) {
        buildString {
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
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // ── Header row: date + score ───────────────────────────────────────────
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    entry.date.format(ENTRY_DATE_FORMATTER),
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
                    if (entry.masturbated) Text("💧", fontSize = 13.sp)
                }
            }
            Text(
                score,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        // ── Metric chips: key numbers from the daily summary ──────────────────
        val hasMetrics = entry.desireLevel != null || entry.comfortRating != null ||
                         entry.sleepQuality != null || entry.selfRating != null
        if (hasMetrics) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                entry.desireLevel?.let  { MetricChip(stringResource(R.string.metric_desire_format, it)) }
                entry.comfortRating?.let { MetricChip(stringResource(R.string.metric_comfort_format, it)) }
                entry.sleepQuality?.let  { MetricChip(stringResource(R.string.metric_sleep_format, it)) }
                entry.selfRating?.let    { MetricChip(stringResource(R.string.metric_self_rating_format, it)) }
            }
        }
        // ── Notes excerpt ─────────────────────────────────────────────────────
        if (!entry.notes.isNullOrBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(6.dp),
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

@Composable
private fun MetricChip(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(50),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
