package de.hipp.app.taskcards.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.size
import androidx.glance.unit.ColorProvider
import de.hipp.app.taskcards.R
import de.hipp.app.taskcards.widget.actions.OpenQuickAddAction

/**
 * A minimal 1x1 widget with a "+" button to quickly add tasks.
 * Tapping opens a dialog to enter task text.
 */
class QuickAddWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                QuickAddWidgetContent()
            }
        }
    }
}

@Composable
fun QuickAddWidgetContent() {
    val context = LocalContext.current

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(androidx.compose.ui.graphics.Color(0xFF6C63FF)))
            .cornerRadius(16.dp)
            .clickable(actionRunCallback<OpenQuickAddAction>()),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_add),
            contentDescription = context.getString(R.string.cd_add_task_widget),
            modifier = GlanceModifier.size(40.dp)
        )
    }
}
