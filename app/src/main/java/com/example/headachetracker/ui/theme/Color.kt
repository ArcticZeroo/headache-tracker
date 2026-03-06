package com.example.headachetracker.ui.theme

import androidx.compose.ui.graphics.Color

// Pain level colors (green to red gradient)
val PainLevel0 = Color(0xFF4CAF50)
val PainLevel1 = Color(0xFF8BC34A)
val PainLevel2 = Color(0xFFFFEB3B)
val PainLevel3 = Color(0xFFFF9800)
val PainLevel4 = Color(0xFFFF5722)
val PainLevel5 = Color(0xFFF44336)

fun painLevelColor(level: Int): Color = when (level) {
    0 -> PainLevel0
    1 -> PainLevel1
    2 -> PainLevel2
    3 -> PainLevel3
    4 -> PainLevel4
    5 -> PainLevel5
    else -> PainLevel0
}

fun painLevelColorWithAlpha(level: Int, alpha: Float = 1f): Color =
    painLevelColor(level).copy(alpha = alpha)
