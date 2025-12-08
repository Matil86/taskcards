package de.hipp.app.taskcards.widget

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dagger.hilt.android.AndroidEntryPoint
import de.hipp.app.taskcards.data.TaskListMetadataRepository
import de.hipp.app.taskcards.data.TaskListRepository
import de.hipp.app.taskcards.ui.theme.TaskCardsTheme
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Activity that shows a dialog to quickly add a task.
 * Launched from the Quick Add widget.
 */
@AndroidEntryPoint
class QuickAddActivity : ComponentActivity() {

    @Inject
    lateinit var taskListRepo: TaskListRepository

    @Inject
    lateinit var taskListMetadataRepo: TaskListMetadataRepository

    @Suppress("LongMethod")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TaskCardsTheme {
                var taskText by remember { mutableStateOf("") }
                var isLoading by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()

                Dialog(onDismissRequest = { finish() }) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                        ) {
                            Text(
                                text = "Quick Add Task",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = taskText,
                                onValueChange = { taskText = it },
                                label = { Text("Task") },
                                placeholder = { Text("What needs to be done?") },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isLoading,
                                singleLine = false,
                                maxLines = 3
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedButton(
                                    onClick = { finish() },
                                    modifier = Modifier.weight(1f),
                                    enabled = !isLoading
                                ) {
                                    Text("Cancel")
                                }

                                Spacer(modifier = Modifier.padding(horizontal = 8.dp))

                                Button(
                                    onClick = {
                                        scope.launch {
                                            isLoading = true
                                            try {
                                                val trimmedText = taskText.trim()
                                                if (trimmedText.isNotEmpty()) {
                                                    // Get default list or first available list
                                                    val prefs = WidgetPreferencesRepository(this@QuickAddActivity)
                                                    val listId = prefs.getDefaultListId()
                                                        ?: taskListMetadataRepo.getDefaultListId()

                                                    if (listId != null) {
                                                        taskListRepo.addTask(listId, trimmedText)
                                                        Toast.makeText(
                                                            this@QuickAddActivity,
                                                            "Task added",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                        finish()
                                                    } else {
                                                        Toast.makeText(
                                                            this@QuickAddActivity,
                                                            "No task list available",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                Toast.makeText(
                                                    this@QuickAddActivity,
                                                    "Error adding task: ${e.message}",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } finally {
                                                isLoading = false
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    enabled = !isLoading && taskText.trim().isNotEmpty()
                                ) {
                                    Text("Add")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
