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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.headachetracker.data.model.TimeSeriesData
import com.example.headachetracker.data.model.TimeSeriesPoint
import com.example.headachetracker.ui.components.ContentCard
import com.example.headachetracker.ui.theme.Dimensions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil

private const val GRID_LINES = 5
private const val X_LABEL_COUNT = 5
private const val LEFT_PADDING = 48f
private const val BOTTOM_PADDING = 40f

@Composable
fun PainChart(
    series: TimeSeriesData,
    modifier: Modifier = Modifier
) {
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    val gridColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)

    ContentCard(modifier = modifier, containerColor = MaterialTheme.colorScheme.surface) {
        Text(
            text = series.seriesName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(Dimensions.GapSmall))

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
                val chartWidth = size.width - LEFT_PADDING
                val chartHeight = size.height - BOTTOM_PADDING
                val points = series.points
                val axisRange = calculateAxisRange(points.map { it.value })
                val minTime = points.minOf { it.timestamp }
                val timeRange = (points.maxOf { it.timestamp } - minTime).coerceAtLeast(1)

                drawYAxisGridAndLabels(axisRange, chartHeight, gridColor, axisColor)
                drawChartData(
                    sortedPoints = points.sortedBy { it.timestamp },
                    axisRange = axisRange,
                    minTime = minTime,
                    timeRange = timeRange,
                    chartWidth = chartWidth,
                    chartHeight = chartHeight,
                    color = series.color
                )
                drawXAxisLabels(minTime, timeRange, chartWidth, axisColor)
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

private data class AxisRange(val min: Float, val max: Float) {
    val range: Float get() = (max - min).coerceAtLeast(0.001f)
}

private fun calculateAxisRange(values: List<Float>): AxisRange {
    val dataMax = values.max()
    val dataMin = values.min()
    return if (dataMax <= 5f && dataMin >= 0f) {
        AxisRange(min = 0f, max = 5f)
    } else {
        val spread = (dataMax - dataMin).coerceAtLeast(0.001f)
        val padding = spread * 0.1f
        val minY = (dataMin - padding).coerceAtLeast(0f)
        val rawMax = dataMax + padding
        val step = niceStep((rawMax - minY) / GRID_LINES)
        val maxY = (ceil((rawMax / step).toDouble()) * step).toFloat()
        AxisRange(min = minY, max = maxY)
    }
}

private fun formatAxisLabel(value: Float): String = when {
    value == value.toLong().toFloat() && value < 10000 -> value.toLong().toString()
    value >= 100 -> value.toInt().toString()
    else -> String.format("%.1f", value)
}

private fun DrawScope.drawYAxisGridAndLabels(
    axisRange: AxisRange,
    chartHeight: Float,
    gridColor: Color,
    axisColor: Color
) {
    for (i in 0..GRID_LINES) {
        val y = chartHeight - (i.toFloat() / GRID_LINES) * chartHeight
        drawLine(
            color = gridColor,
            start = Offset(LEFT_PADDING, y),
            end = Offset(size.width, y),
            strokeWidth = 1f
        )
        val labelValue = axisRange.min + (axisRange.range * i / GRID_LINES)
        drawContext.canvas.nativeCanvas.drawText(
            formatAxisLabel(labelValue),
            4f,
            y + 5f,
            android.graphics.Paint().apply {
                color = axisColor.hashCode()
                textSize = 26f
                textAlign = android.graphics.Paint.Align.LEFT
            }
        )
    }
}

private fun DrawScope.drawChartData(
    sortedPoints: List<TimeSeriesPoint>,
    axisRange: AxisRange,
    minTime: Long,
    timeRange: Long,
    chartWidth: Float,
    chartHeight: Float,
    color: Color
) {
    fun pointToOffset(point: TimeSeriesPoint): Offset {
        val x = LEFT_PADDING + ((point.timestamp - minTime).toFloat() / timeRange) * chartWidth
        val y = chartHeight - ((point.value - axisRange.min) / axisRange.range) * chartHeight
        return Offset(x, y)
    }

    if (sortedPoints.size < 2) {
        sortedPoints.forEach { drawCircle(color = color, radius = 6f, center = pointToOffset(it)) }
    } else {
        val path = Path()
        sortedPoints.forEachIndexed { index, point ->
            val offset = pointToOffset(point)
            if (index == 0) path.moveTo(offset.x, offset.y) else path.lineTo(offset.x, offset.y)
        }
        drawPath(path, color = color, style = Stroke(width = 3f, cap = StrokeCap.Round))
        sortedPoints.forEach { drawCircle(color = color, radius = 4f, center = pointToOffset(it)) }
    }
}

private fun DrawScope.drawXAxisLabels(
    minTime: Long,
    timeRange: Long,
    chartWidth: Float,
    axisColor: Color
) {
    val dateFormat = SimpleDateFormat("M/d", Locale.getDefault())
    for (i in 0 until X_LABEL_COUNT) {
        val time = minTime + (timeRange * i / (X_LABEL_COUNT - 1))
        val x = LEFT_PADDING + (i.toFloat() / (X_LABEL_COUNT - 1)) * chartWidth
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
