package com.chastity.diary.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.chastity.diary.ui.theme.HeatmapLevel0
import com.chastity.diary.ui.theme.HeatmapLevel1
import com.chastity.diary.ui.theme.HeatmapLevel2
import com.chastity.diary.ui.theme.HeatmapLevel3
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chastity.diary.domain.model.HeatmapQuestion
import com.chastity.diary.domain.model.HeatmapTimeRange
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.chart.line.lineSpec
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.patrykandpatrick.vico.core.entry.entryOf
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.ui.res.stringResource
import com.chastity.diary.R

/**
 * Line chart for trends (mood, desire, etc.)
 */
@Composable
fun TrendLineChart(
    title: String,
    data: List<Float>,
    labels: List<String> = emptyList(),
    intYAxis: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            
            if (data.isNotEmpty()) {
                // C-3: Fixed producer reference — Vico's Chart does not react to a replaced
                // ChartEntryModelProducer instance. Keep the same reference and update
                // content via setEntries() inside LaunchedEffect instead.
                val producer = remember { ChartEntryModelProducer() }
                LaunchedEffect(data) {
                    producer.setEntries(
                        data.mapIndexed { index, value -> entryOf(index.toFloat(), value) }
                    )
                }

                // X-axis: show date labels when supplied, fall back to numeric index
                val bottomFormatter = remember(labels) {
                    AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
                        labels.getOrNull(value.toInt()) ?: value.toInt().toString()
                    }
                }

                ProvideChartStyle {
                    val intFormatter = AxisValueFormatter<AxisPosition.Vertical.Start> { value, _ ->
                        value.toInt().toString()
                    }
                    Chart(
                        chart = lineChart(),
                        chartModelProducer = producer,
                        startAxis = if (intYAxis) rememberStartAxis(valueFormatter = intFormatter)
                                    else rememberStartAxis(),
                        bottomAxis = rememberBottomAxis(valueFormatter = bottomFormatter),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                }
            } else {
                Text(
                    text = "暫無資料",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 32.dp)
                )
            }
        }
    }
}

/**
 * Column chart for statistics (exercise count, etc.)
 */
@Composable
fun StatColumnChart(
    title: String,
    data: Map<String, Float>,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            
            if (data.isNotEmpty()) {
                val entries = remember(data) {
                    data.entries.mapIndexed { index, entry ->
                        entryOf(index.toFloat(), entry.value)
                    }
                }
                
                val chartEntryModelProducer = remember(entries) {
                    ChartEntryModelProducer(entries)
                }
                
                ProvideChartStyle {
                    Chart(
                        chart = columnChart(),
                        chartModelProducer = chartEntryModelProducer,
                        startAxis = rememberStartAxis(),
                        bottomAxis = rememberBottomAxis(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                }
                
                // Labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    data.keys.take(5).forEach { label ->
                        Text(
                            text = label.take(4), // 截短標籤避免過長
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            } else {
                Text(
                    text = "暫無資料",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 32.dp)
                )
            }
        }
    }
}

/**
 * Simple calendar heatmap using Cards
 */
@Composable
fun CalendarHeatmap(
    title: String,
    dates: Map<String, Int>, // date to completion level (0-3)
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            
            Text(
                text = "過去 7 天記錄完成情況",
                style = MaterialTheme.typography.bodySmall
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(7) { index ->
                    val level = dates.values.elementAtOrNull(index) ?: 0
                    val color = when (level) {
                        0 -> HeatmapLevel0
                        1 -> HeatmapLevel1
                        2 -> HeatmapLevel2
                        else -> HeatmapLevel3
                    }
                    
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = color
                        )
                    ) {}
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("少", style = MaterialTheme.typography.labelSmall)
                Text("多", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Action Heatmap
// ─────────────────────────────────────────────────────────────────────────────

private val CELL_WIDTH = 40.dp
private val CELL_HEIGHT = 36.dp
private val LABEL_WIDTH = 72.dp
private val CELL_GAP = 2.dp
private val dateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("M/d")

/**
 * 行動熱力圖 Section（題目固定，不可自訂）
 *
 * 橫軸：日期（預設 7 天，可切換 7 / 14 / 30 天），右側可水平捲動。
 * 縱軸：固定題目，依性別自動過濾（晨勃僅男性顯示）。
 * 格子：有做 → 主題色塊；未做 → surfaceVariant 灰色。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionHeatmapSection(
    heatmapData: Map<LocalDate, Map<HeatmapQuestion, Boolean>>,
    selectedTimeRange: HeatmapTimeRange,
    onTimeRangeChange: (HeatmapTimeRange) -> Unit,
    modifier: Modifier = Modifier
) {
    val sortedDates = remember(heatmapData) { heatmapData.keys.sorted() }
    val questions = remember(heatmapData) {
        heatmapData.values.firstOrNull()?.keys?.toList() ?: emptyList()
    }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── 標題列 ──
            Text(
                text = stringResource(R.string.heatmap_section_title),
                style = MaterialTheme.typography.titleMedium
            )

            // ── 時間範圍切換 ──
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                HeatmapTimeRange.entries.forEach { range ->
                    FilterChip(
                        selected = selectedTimeRange == range,
                        onClick = { onTimeRangeChange(range) },
                        label = { Text(stringResource(range.labelResId), fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = MaterialTheme.colorScheme.outline,
                            selectedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            // ── 矩陣主體 ──
            Row(modifier = Modifier.fillMaxWidth()) {

                // 左側：固定縱軸標籤
                Column(
                    modifier = Modifier.width(LABEL_WIDTH),
                    verticalArrangement = Arrangement.spacedBy(CELL_GAP)
                ) {
                    Spacer(modifier = Modifier.height(CELL_HEIGHT))
                    questions.forEach { question ->
                        Box(
                            modifier = Modifier.height(CELL_HEIGHT).fillMaxWidth(),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Text(
                                text = stringResource(question.labelResId),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // 右側：日期橫軸 + 格子，可水平捲動
                Column(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(CELL_GAP)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(CELL_GAP)) {
                        sortedDates.forEach { date ->
                            Text(
                                text = date.format(dateFmt),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.width(CELL_WIDTH).height(CELL_HEIGHT)
                            )
                        }
                    }
                    questions.forEach { question ->
                        Row(horizontalArrangement = Arrangement.spacedBy(CELL_GAP)) {
                            sortedDates.forEach { date ->
                                val done = heatmapData[date]?.get(question) ?: false
                                Box(
                                    modifier = Modifier
                                        .size(width = CELL_WIDTH, height = CELL_HEIGHT)
                                        .background(
                                            color = if (done)
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
                                            else
                                                MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                )
                            }
                        }
                    }
                }
            }

            // ── 圖例 ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Box(modifier = Modifier.size(12.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(2.dp)))
                Spacer(modifier = Modifier.width(4.dp))
                Text("未做", style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.size(12.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.75f), RoundedCornerShape(2.dp)))
                Spacer(modifier = Modifier.width(4.dp))
                Text("完成", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

/**
 * Multi-series line chart for comparing numeric metrics on the same 1–10 scale.
 * [series]: list of Triple(label, color, list of (LocalDate, Float) pairs)
 */
@Composable
fun MultiTrendLineChart(
    title: String,
    series: List<Triple<String, Color, List<Pair<LocalDate, Float>>>>,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)

            val hasSomeData = series.any { it.third.isNotEmpty() }
            if (hasSomeData) {
                // Unified sorted date index across all active series
                val allDates = remember(series) {
                    series.flatMap { (_, _, pts) -> pts.map { it.first } }
                        .distinct().sorted()
                }
                val dateFmt = remember { DateTimeFormatter.ofPattern("M/d") }
                val dateLabels = remember(allDates) { allDates.map { it.format(dateFmt) } }

                val producer = remember { ChartEntryModelProducer() }
                LaunchedEffect(series) {
                    val seriesEntries = series.map { (_, _, pts) ->
                        val dateMap = pts.toMap()
                        allDates.mapIndexedNotNull { i, date ->
                            dateMap[date]?.let { entryOf(i.toFloat(), it) }
                        }
                    }
                    producer.setEntries(*seriesEntries.toTypedArray())
                }

                val bottomFormatter = remember(dateLabels) {
                    AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
                        dateLabels.getOrNull(value.toInt()) ?: ""
                    }
                }
                val intFormatter = AxisValueFormatter<AxisPosition.Vertical.Start> { value, _ ->
                    value.toInt().toString()
                }

                ProvideChartStyle {
                    Chart(
                        chart = lineChart(
                            lines = series.map { (_, color, _) -> lineSpec(lineColor = color) }
                        ),
                        chartModelProducer = producer,
                        startAxis = rememberStartAxis(valueFormatter = intFormatter),
                        bottomAxis = rememberBottomAxis(valueFormatter = bottomFormatter),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                    )
                }

                // Legend row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    series.forEach { (label, color, _) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(color, CircleShape)
                            )
                            Text(text = label, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            } else {
                Text(
                    text = "暫無資料",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 32.dp)
                )
            }
        }
    }
}
