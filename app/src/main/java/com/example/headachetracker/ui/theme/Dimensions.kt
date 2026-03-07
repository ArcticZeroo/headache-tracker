package com.example.headachetracker.ui.theme

import androidx.compose.ui.unit.dp

object Dimensions {
    /** Corner radius used on all primary cards throughout the app. */
    val CardCornerRadius = 16.dp

    /** Inner padding applied to the Column inside every primary card. */
    val CardContentPadding = 16.dp

    /** Corner radius for nested/secondary cards (e.g. day prediction columns). */
    val NestedCardCornerRadius = 12.dp

    /** Horizontal/vertical padding applied to full-screen scrollable content. */
    val ScreenContentPadding = 16.dp

    /** Tight gap between closely related items (e.g. title → description). */
    val GapTight = 4.dp

    /** Standard gap between distinct elements within a card. */
    val GapSmall = 8.dp

    /** Larger gap before action buttons or major content sections. */
    val GapMedium = 12.dp
}
