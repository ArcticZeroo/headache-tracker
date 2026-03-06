package com.example.headachetracker.ui.theme

import androidx.compose.ui.graphics.Color

// Pain level colors (green to red gradient, darkened for white text readability)
val PainLevel0 = Color(0xFF4CAF50)
val PainLevel1 = Color(0xFF7CB342)
val PainLevel2 = Color(0xFFF9A825)
val PainLevel3 = Color(0xFFEF6C00)
val PainLevel4 = Color(0xFFE64A19)
val PainLevel5 = Color(0xFFC62828)

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
