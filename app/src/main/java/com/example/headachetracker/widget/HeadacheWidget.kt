package com.example.headachetracker.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

class HeadacheWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                WidgetContent()
            }
        }
    }

    @Composable
    private fun WidgetContent() {
        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.background)
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            (0..5).forEach { level ->
                val bgColor = when (level) {
                    0 -> ColorProvider(Color(0xFF4CAF50))
                    1 -> ColorProvider(Color(0xFF7CB342))
                    2 -> ColorProvider(Color(0xFFF9A825))
                    3 -> ColorProvider(Color(0xFFEF6C00))
                    4 -> ColorProvider(Color(0xFFE64A19))
                    5 -> ColorProvider(Color(0xFFC62828))
                    else -> ColorProvider(Color(0xFF4CAF50))
                }

                Box(
                    modifier = GlanceModifier
                        .size(40.dp)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .background(bgColor)
                        .clickable(
                            actionRunCallback<WidgetClickAction>(
                                actionParametersOf(WidgetClickAction.PAIN_LEVEL_KEY to level)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = level.toString(),
                        style = TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorProvider(Color.White)
                        )
                    )
                }
            }
        }
    }
}
