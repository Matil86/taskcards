package de.hipp.app.taskcards.deeplink

import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import de.hipp.app.taskcards.data.TaskListMetadataRepository
import de.hipp.app.taskcards.data.TaskListRepository
import de.hipp.app.taskcards.model.ShareableList
import de.hipp.app.taskcards.model.ShareableTask
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

/**
 * Handles deep link processing and task/list import operations.
 * Validates deep links, imports tasks, and creates new lists.
 */
class DeepLinkHandler(
    private val taskRepo: TaskListRepository,
    private val listMetadataRepo: TaskListMetadataRepository
) {
    companion object {
        private const val TAG = "DeepLinkHandler"
    }

    /**
     * Process a deep link URI and determine the appropriate action.
     * @param uri The deep link URI to process
     * @param currentListId The currently active list ID (used for task imports)
     * @return A DeepLinkResult indicating what action should be taken
     */
    suspend fun handleDeepLink(uri: Uri, currentListId: String): DeepLinkResult {
        return try {
            when (uri.host) {
                "task" -> handleTaskDeepLink(uri, currentListId)
                "list" -> handleListDeepLink(uri)
                else -> {
                    Log.w(TAG, "Unknown deep link host: ${uri.host}")
                    DeepLinkResult.Invalid("Unknown link type: ${uri.host}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling deep link", e)
            DeepLinkResult.Invalid("Error processing link: ${e.message}")
        }
    }

    /**
     * Handle a task deep link (taskcards://task?...).
     */
    private suspend fun handleTaskDeepLink(uri: Uri, targetListId: String): DeepLinkResult {
        val shareableTask = ShareableTask.fromDeepLink(uri)
            ?: return DeepLinkResult.Invalid("Invalid task link format")

        return DeepLinkResult.Task(shareableTask, targetListId)
    }

    /**
     * Handle a list deep link.
     * Supports two formats:
     * - taskcards://list/{listId} - Navigate to existing list (real-time collaboration)
     * - taskcards://list?data=... - Import list snapshot (legacy)
     */
    private suspend fun handleListDeepLink(uri: Uri): DeepLinkResult {
        // Check if this is a list ID format (path-based): taskcards://list/{listId}
        val pathSegments = uri.pathSegments
        if (pathSegments.isNotEmpty()) {
            val listId = pathSegments[0]
            Log.d(TAG, "Navigating to list ID: $listId")

            // Add current user as contributor (idempotent arrayUnion)
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                try {
                    val db = FirebaseFirestore.getInstance()
                    db.collection("lists").document(listId)
                        .update("contributors", FieldValue.arrayUnion(currentUser.uid))
                        .await()
                    Log.d(TAG, "Added ${currentUser.uid} as contributor to list $listId")
                } catch (e: Exception) {
                    Log.d(TAG, "Could not add as contributor: ${e.message}")
                }
            }

            return DeepLinkResult.NavigateToList(listId)
        }

        // Otherwise, try to parse as shareable list data (query-based legacy format)
        val shareableList = ShareableList.fromDeepLink(uri)
            ?: return DeepLinkResult.Invalid("Invalid list link format")

        if (shareableList.tasks.isEmpty()) {
            return DeepLinkResult.Invalid("Cannot import empty list")
        }

        return DeepLinkResult.List(shareableList)
    }

    /**
     * Import a task into the specified list.
     * @param shareableTask The task to import
     * @param listId The list to import into
     * @return The ID of the newly created task
     */
    suspend fun importTask(shareableTask: ShareableTask, listId: String): String {
        try {
            // Create the basic task
            val task = taskRepo.addTask(listId, shareableTask.text)

            // Set due date and reminder if present
            if (shareableTask.dueDate != null) {
                taskRepo.updateTaskDueDate(
                    listId = listId,
                    taskId = task.id,
                    dueDate = shareableTask.dueDate,
                    reminderType = shareableTask.reminderType
                )
            }

            Log.d(TAG, "Successfully imported task: ${task.id}")
            return task.id
        } catch (e: Exception) {
            Log.e(TAG, "Error importing task", e)
            throw e
        }
    }

    /**
     * Import a complete list with all its tasks.
     * Creates a new list and imports all tasks with their metadata.
     * @param shareableList The list to import
     * @return The ID of the newly created list
     */
    suspend fun importList(shareableList: ShareableList): String {
        try {
            // Create the new list
            val listId = listMetadataRepo.createList(shareableList.name)
            Log.d(TAG, "Created new list: $listId with name '${shareableList.name}'")

            // Import all tasks
            var successCount = 0
            var failureCount = 0

            shareableList.tasks.forEach { task ->
                try {
                    importTask(task, listId)
                    successCount++
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to import task: ${task.text}", e)
                    failureCount++
                }
            }

            Log.d(TAG, "List import complete: $successCount succeeded, $failureCount failed")
            return listId
        } catch (e: Exception) {
            Log.e(TAG, "Error importing list", e)
            throw e
        }
    }
}

/**
 * Result of processing a deep link.
 */
sealed class DeepLinkResult {
    /**
     * Deep link contains a single task to import.
     * @param task The task data
     * @param targetListId The list to import into
     */
    data class Task(val task: ShareableTask, val targetListId: String) : DeepLinkResult()

    /**
     * Deep link contains a complete list to import.
     * @param list The list data with all tasks
     */
    data class List(val list: ShareableList) : DeepLinkResult()

    /**
     * Deep link contains a list ID for navigation (real-time collaboration).
     * @param listId The list ID to navigate to
     */
    data class NavigateToList(val listId: String) : DeepLinkResult()

    /**
     * Deep link is invalid or malformed.
     * @param reason Human-readable reason for the error
     */
    data class Invalid(val reason: String) : DeepLinkResult()
}
