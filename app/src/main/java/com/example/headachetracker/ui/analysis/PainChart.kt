package com.example.headachetracker.ui.analysis

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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
import kotlin.math.ceil

@Composable
fun PainChart(
    series: TimeSeriesData,
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
                text = series.seriesName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (series.points.isEmpty()) {
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
                    val leftPadding = 48f
                    val bottomPadding = 40f
                    val chartWidth = size.width - leftPadding
                    val chartHeight = size.height - bottomPadding

                    val points = series.points
                    val dataMax = points.maxOf { it.value }
                    val dataMin = points.minOf { it.value }

                    // Compute nice y-axis range
                    val maxY: Float
                    val minY: Float
                    val gridLines = 5

                    if (dataMax <= 5f && dataMin >= 0f) {
                        // Pain-level scale
                        maxY = 5f
                        minY = 0f
                    } else {
                        val range = (dataMax - dataMin).coerceAtLeast(0.001f)
                        val padding = range * 0.1f
                        minY = (dataMin - padding).coerceAtLeast(0f)
                        val rawMax = dataMax + padding
                        val step = niceStep((rawMax - minY) / gridLines)
                        maxY = (ceil((rawMax / step).toDouble()) * step).toFloat()
                    }
                    val yRange = (maxY - minY).coerceAtLeast(0.001f)

                    val minTime = points.minOf { it.timestamp }
                    val maxTime = points.maxOf { it.timestamp }
                    val timeRange = (maxTime - minTime).coerceAtLeast(1)

                    // Draw grid lines and y-axis labels
                    for (i in 0..gridLines) {
                        val y = chartHeight - (i.toFloat() / gridLines) * chartHeight
                        drawLine(
                            color = gridColor,
                            start = Offset(leftPadding, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1f
                        )
                        val labelValue = minY + (yRange * i / gridLines)
                        val labelText = if (labelValue == labelValue.toLong().toFloat() && labelValue < 10000) {
                            labelValue.toLong().toString()
                        } else if (labelValue >= 100) {
                            labelValue.toInt().toString()
                        } else {
                            String.format("%.1f", labelValue)
                        }
                        drawContext.canvas.nativeCanvas.drawText(
                            labelText,
                            4f,
                            y + 5f,
                            android.graphics.Paint().apply {
                                color = axisColor.hashCode()
                                textSize = 26f
                                textAlign = android.graphics.Paint.Align.LEFT
                            }
                        )
                    }

                    // Draw data
                    val sortedPoints = points.sortedBy { it.timestamp }

                    if (sortedPoints.size < 2) {
                        sortedPoints.forEach { point ->
                            val x = leftPadding + ((point.timestamp - minTime).toFloat() / timeRange) * chartWidth
                            val y = chartHeight - ((point.value - minY) / yRange) * chartHeight
                            drawCircle(color = series.color, radius = 6f, center = Offset(x, y))
                        }
                    } else {
                        val path = Path()
                        sortedPoints.forEachIndexed { index, point ->
                            val x = leftPadding + ((point.timestamp - minTime).toFloat() / timeRange) * chartWidth
                            val y = chartHeight - ((point.value - minY) / yRange) * chartHeight
                            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(path, color = series.color, style = Stroke(width = 3f, cap = StrokeCap.Round))

                        sortedPoints.forEach { point ->
                            val x = leftPadding + ((point.timestamp - minTime).toFloat() / timeRange) * chartWidth
                            val y = chartHeight - ((point.value - minY) / yRange) * chartHeight
                            drawCircle(color = series.color, radius = 4f, center = Offset(x, y))
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
            }
        }
    }
}

private fun niceStep(rawStep: Float): Float {
    val magnitude = Math.pow(10.0, Math.floor(Math.log10(rawStep.toDouble())))
    val fraction = rawStep / magnitude
    val niceFraction = when {
        fraction <= 1.5 -> 1.0
        fraction <= 3.0 -> 2.0
        fraction <= 7.0 -> 5.0
        else -> 10.0
    }
    return (niceFraction * magnitude).toFloat()
}
