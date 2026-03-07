package com.example.headachetracker.ui.analysis

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun AnalysisScreen(
    isExpanded: Boolean = false,
    viewModel: AnalysisViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Prediction card
        item {
            PredictionCard(
                predictionState = state.prediction,
                onRefresh = { viewModel.refreshPrediction() }
            )
        }

        // Summary stats
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Entries",
                    value = state.totalEntries.toString(),
                    subtitle = "in ${state.selectedRange.label}",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Avg Pain",
                    value = if (state.totalEntries > 0) {
                        String.format("%.1f", state.averagePain)
                    } else "–",
                    subtitle = "out of 5",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Streak",
                    value = state.daysSinceLastHeadache?.toString() ?: "–",
                    subtitle = "days clear",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Time range selector
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TimeRange.entries.forEach { range ->
                    FilterChip(
                        selected = state.selectedRange == range,
                        onClick = { viewModel.setTimeRange(range) },
                        label = { Text(range.label) }
                    )
                }
            }
        }

        // Series tab selector
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.availableSeriesNames) { name ->
                    FilterChip(
                        selected = state.selectedSeriesName == name,
                        onClick = { viewModel.selectSeries(name) },
                        label = { Text(name) }
                    )
                }
                if (state.isLoadingOverlays) {
                    item {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(32.dp)
                                .padding(4.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }
        }

        // Chart for selected series
        val selectedSeries = state.allSeries[state.selectedSeriesName]
        if (isExpanded) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (selectedSeries != null) {
                        PainChart(
                            series = selectedSeries,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    CalendarHeatmap(
                        currentMonth = state.currentMonth,
                        calendarData = state.calendarData,
                        onNavigateMonth = { viewModel.navigateMonth(it) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        } else {
            if (selectedSeries != null) {
                item {
                    PainChart(series = selectedSeries)
                }
            }
            item {
                CalendarHeatmap(
                    currentMonth = state.currentMonth,
                    calendarData = state.calendarData,
                    onNavigateMonth = { viewModel.navigateMonth(it) }
                )
            }
        }

        // Correlations
        item {
            CorrelationCard(
                correlations = state.correlations,
                totalEntries = state.totalEntries
            )
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = value,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}
