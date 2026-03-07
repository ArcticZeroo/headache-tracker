package com.example.headachetracker.ui.analysis

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.headachetracker.data.local.DailyWeather
import com.example.headachetracker.data.ml.MIN_TRAINING_ENTRIES
import com.example.headachetracker.data.repository.DayPrediction
import com.example.headachetracker.data.repository.ModelStatus
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun PredictionCard(
    predictionState: PredictionUiState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Headache Forecast",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (predictionState is PredictionUiState.Ready || predictionState is PredictionUiState.Untrained) {
                    IconButton(onClick = onRefresh, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh prediction",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (predictionState) {
                is PredictionUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
                    }
                }

                is PredictionUiState.NotEnoughData -> {
                    Text(
                        text = "Need more data to make predictions",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { predictionState.entryCount.toFloat() / predictionState.threshold },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp)),
                        strokeCap = StrokeCap.Round
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${predictionState.entryCount} / ${predictionState.threshold} entries logged",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                is PredictionUiState.Untrained -> {
                    Text(
                        text = "Model training in progress…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap refresh after your next entry save.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                is PredictionUiState.Ready -> {
                    val result = predictionState.result
                    val hasPredictions = result.today != null || result.tomorrow != null

                    if (!hasPredictions) {
                        Text(
                            text = "No location data available for forecast.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            result.today?.let { day ->
                                DayPredictionColumn(
                                    label = "Today",
                                    prediction = day,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            result.tomorrow?.let { day ->
                                DayPredictionColumn(
                                    label = "Tomorrow",
                                    prediction = day,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    val trainedAgo = result.trainedAtMillis?.let { millis ->
                        val days = ((System.currentTimeMillis() - millis) / 86_400_000L).toInt()
                        when {
                            days == 0 -> "today"
                            days == 1 -> "1 day ago"
                            else -> "$days days ago"
                        }
                    }
                    Text(
                        text = buildString {
                            append("Based on ${result.trainingSampleCount} samples")
                            if (trainedAgo != null) append(" · trained $trainedAgo")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DayPredictionColumn(
    label: String,
    prediction: DayPrediction,
    modifier: Modifier = Modifier
) {
    val pct = (prediction.probability * 100).roundToInt()
    val targetProgress = prediction.probability.toFloat().coerceIn(0f, 1f)
    var animationStarted by remember { mutableStateOf(false) }
    val animatedProgress by animateFloatAsState(
        targetValue = if (animationStarted) targetProgress else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "PredictionProgress"
    )
    LaunchedEffect(Unit) { animationStarted = true }

    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val indicatorColor = probabilityColor(prediction.probability)

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.size(72.dp),
                    color = trackColor,
                    strokeWidth = 7.dp
                )
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.size(72.dp),
                    color = indicatorColor,
                    strokeWidth = 7.dp,
                    strokeCap = StrokeCap.Round
                )
                Text(
                    text = "$pct%",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = indicatorColor
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = probabilityLabel(prediction.probability),
                style = MaterialTheme.typography.labelSmall,
                color = indicatorColor,
                fontWeight = FontWeight.Medium
            )
            prediction.forecastSummary?.let { weather ->
                Spacer(modifier = Modifier.height(6.dp))
                WeatherSummaryRow(weather)
            }
        }
    }
}

@Composable
private fun WeatherSummaryRow(weather: DailyWeather) {
    val tempMin = weather.temperatureMin?.let { "%.0f°".format(it) } ?: "–"
    val tempMax = weather.temperatureMax?.let { "%.0f°".format(it) } ?: "–"
    val pressure = weather.pressureMean?.let { "%.0f hPa".format(it) } ?: ""
    val rain = weather.rainSum?.takeIf { it > 0.1 }?.let { "%.1f mm".format(it) } ?: ""

    Text(
        text = "$tempMin–$tempMax",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    )
    if (pressure.isNotEmpty()) {
        Text(
            text = pressure,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
    if (rain.isNotEmpty()) {
        Text(
            text = "🌧 $rain",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}

private fun probabilityColor(p: Double): Color = when {
    p < 0.30 -> Color(0xFF43A047)  // green
    p < 0.55 -> Color(0xFFFB8C00)  // orange
    p < 0.75 -> Color(0xFFEF6C00)  // deep orange
    else     -> Color(0xFFE53935)  // red
}

private fun probabilityLabel(p: Double): String = when {
    p < 0.30 -> "Low risk"
    p < 0.55 -> "Moderate risk"
    p < 0.75 -> "High risk"
    else     -> "Very high risk"
}
