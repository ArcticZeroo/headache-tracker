package com.example.headachetracker.data.model

import androidx.compose.ui.graphics.Color

data class TimeSeriesPoint(
    val timestamp: Long,
    val value: Float,
    val label: String? = null
)

data class TimeSeriesData(
    val seriesName: String,
    val color: Color,
    val points: List<TimeSeriesPoint>
)
