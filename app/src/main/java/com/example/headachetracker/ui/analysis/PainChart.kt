package com.example.headachetracker.ui.analysis

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.headachetracker.data.model.TimeSeriesData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PainChart(
    seriesData: List<TimeSeriesData>,
    modifier: Modifier = Modifier
) {
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    val gridColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Pain Over Time",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (seriesData.all { it.points.isEmpty() }) {
                Text(
                    text = "No data for this period",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 32.dp)
                )
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    val leftPadding = 40f
                    val bottomPadding = 40f
                    val chartWidth = size.width - leftPadding
                    val chartHeight = size.height - bottomPadding
                    val maxY = 5f

                    // Find global time range across all series
                    val allPoints = seriesData.flatMap { it.points }
                    if (allPoints.isEmpty()) return@Canvas

                    val minTime = allPoints.minOf { it.timestamp }
                    val maxTime = allPoints.maxOf { it.timestamp }
                    val timeRange = (maxTime - minTime).coerceAtLeast(1)

                    // Draw grid lines
                    for (i in 0..5) {
                        val y = chartHeight - (i / maxY) * chartHeight
                        drawLine(
                            color = gridColor,
                            start = Offset(leftPadding, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1f
                        )
                        // Y-axis labels
                        drawContext.canvas.nativeCanvas.drawText(
                            i.toString(),
                            10f,
                            y + 5f,
                            android.graphics.Paint().apply {
                                color = axisColor.hashCode()
                                textSize = 28f
                                textAlign = android.graphics.Paint.Align.LEFT
                            }
                        )
                    }

                    // Draw each series
                    seriesData.forEach { series ->
                        if (series.points.size < 2) {
                            // Draw dots for single points
                            series.points.forEach { point ->
                                val x = leftPadding + ((point.timestamp - minTime).toFloat() / timeRange) * chartWidth
                                val y = chartHeight - (point.value / maxY) * chartHeight
                                drawCircle(
                                    color = series.color,
                                    radius = 6f,
                                    center = Offset(x, y)
                                )
                            }
                            return@forEach
                        }

                        val path = Path()
                        series.points.sortedBy { it.timestamp }.forEachIndexed { index, point ->
                            val x = leftPadding + ((point.timestamp - minTime).toFloat() / timeRange) * chartWidth
                            val y = chartHeight - (point.value / maxY) * chartHeight

                            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }

                        drawPath(
                            path = path,
                            color = series.color,
                            style = Stroke(width = 3f, cap = StrokeCap.Round)
                        )

                        // Draw data points
                        series.points.forEach { point ->
                            val x = leftPadding + ((point.timestamp - minTime).toFloat() / timeRange) * chartWidth
                            val y = chartHeight - (point.value / maxY) * chartHeight
                            drawCircle(
                                color = series.color,
                                radius = 4f,
                                center = Offset(x, y)
                            )
                        }
                    }

                    // X-axis date labels
                    val dateFormat = SimpleDateFormat("M/d", Locale.getDefault())
                    val labelCount = 5
                    for (i in 0 until labelCount) {
                        val time = minTime + (timeRange * i / (labelCount - 1))
                        val x = leftPadding + (i.toFloat() / (labelCount - 1)) * chartWidth
                        drawContext.canvas.nativeCanvas.drawText(
                            dateFormat.format(Date(time)),
                            x,
                            size.height - 5f,
                            android.graphics.Paint().apply {
                                color = axisColor.hashCode()
                                textSize = 24f
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Legend
                Row(verticalAlignment = Alignment.CenterVertically) {
                    seriesData.forEach { series ->
                        Canvas(modifier = Modifier.size(12.dp)) {
                            drawCircle(color = series.color)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = series.seriesName,
                            style = MaterialTheme.typography.labelSmall
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                    }
                }
            }
        }
    }
}
