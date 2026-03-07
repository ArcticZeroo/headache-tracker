package com.example.headachetracker.ui.analysis

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.headachetracker.data.correlation.CorrelationEngine
import com.example.headachetracker.data.correlation.CorrelationResult
import com.example.headachetracker.ui.components.ContentCard
import com.example.headachetracker.ui.theme.Dimensions
import kotlin.math.abs

@Composable
fun CorrelationCard(
    correlations: List<CorrelationResult>,
    totalEntries: Int,
    modifier: Modifier = Modifier
) {
    ContentCard(modifier = modifier, containerColor = MaterialTheme.colorScheme.surfaceVariant) {
        Text(
            text = "Correlations",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(Dimensions.GapTight))
        if (totalEntries < CorrelationEngine.MIN_SAMPLE_SIZE) {
            Text(
                text = "Need at least ${CorrelationEngine.MIN_SAMPLE_SIZE} entries to show correlations. " +
                        "You have $totalEntries so far.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else if (correlations.isEmpty()) {
            Text(
                text = "Not enough paired data yet. Make sure location is enabled for weather " +
                        "correlations and Health Connect is connected for sleep/fitness correlations.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Spacer(modifier = Modifier.height(Dimensions.GapSmall))
            correlations.forEach { result ->
                CorrelationRow(result)
                Spacer(modifier = Modifier.height(Dimensions.GapSmall))
            }
        }
    }
}

@Composable
private fun CorrelationRow(result: CorrelationResult) {
    val absCoeff = abs(result.coefficient)
    val color = when {
        absCoeff < 0.1 -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        result.coefficient > 0 -> Color(0xFFE53935) // positive = more pain
        else -> Color(0xFF43A047) // negative = less pain
    }
    val icon = when {
        absCoeff < 0.1 -> Icons.AutoMirrored.Filled.TrendingFlat
        result.coefficient > 0 -> Icons.AutoMirrored.Filled.TrendingUp
        else -> Icons.AutoMirrored.Filled.TrendingDown
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = result.factorName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = result.interpretation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            text = String.format("%.2f", result.coefficient),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
