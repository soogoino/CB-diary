package com.chastity.diary.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.patrykandpatrick.vico.core.entry.entryOf

/**
 * Line chart for trends (mood, desire, etc.)
 */
@Composable
fun TrendLineChart(
    title: String,
    data: List<Float>,
    labels: List<String> = emptyList(),
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
                    data.mapIndexed { index, value ->
                        entryOf(index.toFloat(), value)
                    }
                }
                
                val chartEntryModelProducer = remember(entries) {
                    ChartEntryModelProducer(entries)
                }
                
                ProvideChartStyle {
                    Chart(
                        chart = lineChart(),
                        chartModelProducer = chartEntryModelProducer,
                        startAxis = rememberStartAxis(),
                        bottomAxis = rememberBottomAxis(),
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
                        0 -> Color.LightGray
                        1 -> Color(0xFFB3E5FC)
                        2 -> Color(0xFF4FC3F7)
                        else -> Color(0xFF0277BD)
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
