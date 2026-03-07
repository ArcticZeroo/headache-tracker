package com.example.headachetracker.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.headachetracker.ui.theme.Dimensions

/**
 * Standard card shell used throughout the app: a full-width Card with the app corner radius and
 * a Column with standard content padding. Pass [containerColor] to change the background.
 * Pass [columnHorizontalAlignment] when the content needs to be centered (e.g. stat cards).
 */
@Composable
fun ContentCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    columnHorizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(Dimensions.CardCornerRadius)
    ) {
        Column(
            modifier = Modifier.padding(Dimensions.CardContentPadding),
            horizontalAlignment = columnHorizontalAlignment,
            content = content
        )
    }
}
