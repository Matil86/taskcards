package de.hipp.app.taskcards.ui.app

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import de.hipp.app.taskcards.deeplink.DeepLinkHandler
import de.hipp.app.taskcards.deeplink.DeepLinkResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.compose.getKoin

/**
 * Stateful handler that shows [DeepLinkImportDialog] and runs the import on confirm.
 */
@Composable
internal fun DeepLinkImportHandler(
    showImportDialog: Boolean,
    deepLinkResult: DeepLinkResult?,
    defaultListId: String,
    navController: NavHostController,
    onDismiss: () -> Unit
) {
    if (!showImportDialog || deepLinkResult == null) return
    val context = LocalContext.current
    val koin = getKoin()
    DeepLinkImportDialog(
        result = deepLinkResult,
        onConfirm = {
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val handler = koin.get<DeepLinkHandler>()
                    when (deepLinkResult) {
                        is DeepLinkResult.Task -> {
                            handler.importTask(deepLinkResult.task, deepLinkResult.targetListId)
                            Toast.makeText(context, "Task imported successfully!", Toast.LENGTH_SHORT).show()
                        }
                        is DeepLinkResult.List -> {
                            handler.importList(deepLinkResult.list)
                            Toast.makeText(context, "List imported successfully!", Toast.LENGTH_SHORT).show()
                            navController.navigate("list/$defaultListId")
                        }
                        else -> {}
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("MainApp", "Failed to import from deep link", e)
                } finally {
                    onDismiss()
                }
            }
        },
        onDismiss = onDismiss
    )
}

/**
 * Dialog for confirming deep link imports (tasks or lists).
 * Shows a preview and requires user confirmation before importing.
 */
@Composable
fun DeepLinkImportDialog(
    result: DeepLinkResult,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = when (result) {
                    is DeepLinkResult.Task -> "Import Task?"
                    is DeepLinkResult.List -> "Import List?"
                    else -> "Import"
                }
            )
        },
        text = {
            Column {
                when (result) {
                    is DeepLinkResult.Task -> {
                        Text(
                            text = "Do you want to import this task?",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = result.task.text,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (result.task.dueDate != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Due: ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(result.task.dueDate))}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    is DeepLinkResult.List -> {
                        Text(
                            text = "Do you want to import this list with ${result.list.tasks.size} tasks?",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = result.list.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Tasks:",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                                result.list.tasks.take(5).forEach { task ->
                                    Text(
                                        text = "• ${task.text}",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                                    )
                                }
                                if (result.list.tasks.size > 5) {
                                    Text(
                                        text = "... and ${result.list.tasks.size - 5} more",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(start = 8.dp, top = 2.dp),
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                    )
                                }
                            }
                        }
                    }
                    else -> {}
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
