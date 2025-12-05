package de.hipp.app.taskcards.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import de.hipp.app.taskcards.data.TaskListMetadataRepository
import de.hipp.app.taskcards.model.TaskList
import de.hipp.app.taskcards.ui.theme.TaskCardsTheme
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Activity for configuring a widget.
 * Allows the user to select which task list to display and the widget type.
 */
@AndroidEntryPoint
class WidgetConfigActivity : ComponentActivity() {

    @Inject
    lateinit var taskListMetadataRepo: TaskListMetadataRepository

    private val widgetId by lazy {
        intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set result to CANCELLED initially
        setResult(RESULT_CANCELED)

        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            TaskCardsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WidgetConfigScreen(
                        taskListMetadataRepo = taskListMetadataRepo,
                        onConfigSelected = { listId, widgetType ->
                            saveWidgetConfig(listId, widgetType)
                            finishWithSuccess()
                        },
                        onCancel = {
                            finish()
                        }
                    )
                }
            }
        }
    }

    private fun saveWidgetConfig(listId: String, widgetType: WidgetType) {
        val prefs = WidgetPreferencesRepository(this)
        prefs.saveWidgetConfig(widgetId, listId, widgetType)
    }

    private fun finishWithSuccess() {
        val resultValue = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        }
        setResult(RESULT_OK, resultValue)

        // Trigger widget update
        lifecycleScope.launch {
            try {
                val glanceId = GlanceAppWidgetManager(this@WidgetConfigActivity)
                    .getGlanceIdBy(widgetId)

                when (val prefs = WidgetPreferencesRepository(this@WidgetConfigActivity)
                    .getWidgetConfig(widgetId)?.widgetType) {
                    WidgetType.TASK_LIST -> TaskListWidget().update(this@WidgetConfigActivity, glanceId)
                    WidgetType.DUE_TODAY -> DueTodayWidget().update(this@WidgetConfigActivity, glanceId)
                    WidgetType.QUICK_ADD -> QuickAddWidget().update(this@WidgetConfigActivity, glanceId)
                    else -> {}
                }
            } catch (e: Exception) {
                // Log error but still finish activity
                android.util.Log.e("WidgetConfigActivity", "Error updating widget", e)
            }
        }

        finish()
    }
}

@Composable
fun WidgetConfigScreen(
    taskListMetadataRepo: TaskListMetadataRepository,
    onConfigSelected: (String, WidgetType) -> Unit,
    onCancel: () -> Unit
) {
    var selectedListId by remember { mutableStateOf<String?>(null) }
    var selectedWidgetType by remember { mutableStateOf(WidgetType.TASK_LIST) }

    // Observe available lists
    val lists by taskListMetadataRepo.observeTaskLists().collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Text(
            text = "Configure Widget",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Widget type selector
        Text(
            text = "Widget Type",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        WidgetType.values().forEach { type ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedWidgetType = type }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedWidgetType == type,
                    onClick = { selectedWidgetType = type }
                )
                Text(
                    text = when (type) {
                        WidgetType.TASK_LIST -> "Task List - Shows active tasks"
                        WidgetType.QUICK_ADD -> "Quick Add - Button to add tasks"
                        WidgetType.DUE_TODAY -> "Due Today - Shows today's tasks"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // List selector (only show for TASK_LIST and DUE_TODAY)
        if (selectedWidgetType == WidgetType.TASK_LIST || selectedWidgetType == WidgetType.DUE_TODAY) {
            Text(
                text = "Select Task List",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (lists.isEmpty()) {
                Text(
                    text = "No task lists available. Create a list in the app first.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    items(lists) { list ->
                        TaskListItem(
                            list = list,
                            selected = selectedListId == list.id,
                            onSelect = { selectedListId = list.id }
                        )
                    }
                }
            }
        } else {
            // For QUICK_ADD, auto-select first list or use default
            if (lists.isNotEmpty() && selectedListId == null) {
                selectedListId = lists.first().id
            }
            Spacer(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }

            Spacer(modifier = Modifier.padding(horizontal = 8.dp))

            Button(
                onClick = {
                    selectedListId?.let { listId ->
                        onConfigSelected(listId, selectedWidgetType)
                    }
                },
                enabled = selectedListId != null,
                modifier = Modifier.weight(1f)
            ) {
                Text("Save")
            }
        }
    }
}

@Composable
fun TaskListItem(
    list: TaskList,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect
        )
        Column(
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Text(
                text = list.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}
