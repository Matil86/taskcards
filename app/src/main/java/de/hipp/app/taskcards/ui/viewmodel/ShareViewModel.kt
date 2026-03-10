package de.hipp.app.taskcards.ui.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.hipp.app.taskcards.model.ShareableList
import de.hipp.app.taskcards.model.ShareableTask
import de.hipp.app.taskcards.model.TaskItem
import de.hipp.app.taskcards.qr.QRCodeGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
/**
 * UI state for the share dialog.
 */
data class ShareUiState(
    val shareUrl: String? = null,
    val qrCodeBitmap: Bitmap? = null,
    val isGeneratingQR: Boolean = false,
    val isSavingQR: Boolean = false
)

/**
 * ViewModel for managing task and list sharing functionality.
 * Generates deep links and QR codes for sharing.
 */
class ShareViewModel(
    private val qrGenerator: QRCodeGenerator
) : ViewModel() {

    companion object {
        private const val TAG = "ShareViewModel"
    }

    private val _state = MutableStateFlow(ShareUiState())
    val state: StateFlow<ShareUiState> = _state.asStateFlow()

    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState.asStateFlow()

    /**
     * Generate a shareable deep link for a single task.
     * @param task The task to share
     */
    fun generateTaskShareUrl(task: TaskItem) {
        viewModelScope.launch {
            try {
                val shareableTask = ShareableTask(
                    text = task.text,
                    dueDate = task.dueDate,
                    reminderType = task.reminderType,
                    notes = null // TaskItem doesn't have notes yet
                )

                val url = shareableTask.toDeepLink()
                _state.value = _state.value.copy(shareUrl = url)
                _errorState.value = null
                Log.d(TAG, "Generated task share URL: $url")
            } catch (e: Exception) {
                Log.e(TAG, "Error generating share URL", e)
                _errorState.value = "Failed to generate share link: ${e.message}"
            }
        }
    }

    /**
     * Generate a shareable deep link for a complete list.
     * @param listName The name of the list
     * @param tasks List of tasks to share
     */
    fun generateListShareUrl(listName: String, tasks: List<TaskItem>) {
        viewModelScope.launch {
            try {
                val shareableTasks = tasks
                    .filter { !it.removed && !it.done } // Only share active tasks
                    .map { task ->
                        ShareableTask(
                            text = task.text,
                            dueDate = task.dueDate,
                            reminderType = task.reminderType,
                            notes = null
                        )
                    }

                if (shareableTasks.isEmpty()) {
                    _errorState.value = "No active tasks to share"
                    return@launch
                }

                val shareableList = ShareableList(listName, shareableTasks)
                val url = shareableList.toDeepLink()
                _state.value = _state.value.copy(shareUrl = url)
                _errorState.value = null
                Log.d(TAG, "Generated list share URL with ${shareableTasks.size} tasks")
            } catch (e: Exception) {
                Log.e(TAG, "Error generating share URL", e)
                _errorState.value = "Failed to generate share link: ${e.message}"
            }
        }
    }

    /**
     * Generate a QR code for the current share URL.
     */
    fun generateQRCode() {
        val url = _state.value.shareUrl
        if (url == null) {
            _errorState.value = "No URL to generate QR code from"
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isGeneratingQR = true)
            try {
                val bitmap = withContext(Dispatchers.Default) {
                    qrGenerator.generateQRCode(url)
                }

                if (bitmap != null) {
                    _state.value = _state.value.copy(
                        qrCodeBitmap = bitmap,
                        isGeneratingQR = false
                    )
                    _errorState.value = null
                    Log.d(TAG, "QR code generated successfully")
                } else {
                    _state.value = _state.value.copy(isGeneratingQR = false)
                    _errorState.value = "Failed to generate QR code"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error generating QR code", e)
                _errorState.value = "Failed to generate QR code: ${e.message}"
                _state.value = _state.value.copy(isGeneratingQR = false)
            }
        }
    }

    /**
     * Generate a shareable deep link for a list ID (for real-time collaboration).
     * This shares the Firestore list ID so all users can work on the same list in real-time.
     * @param listId The list ID to share (e.g., "default-list")
     */
    fun generateListIdShareUrl(listId: String) {
        viewModelScope.launch {
            try {
                val url = "taskcards://list/$listId"
                _state.value = _state.value.copy(shareUrl = url)
                _errorState.value = null
                Log.d(TAG, "Generated list ID share URL: $url")
            } catch (e: Exception) {
                Log.e(TAG, "Error generating list ID share URL", e)
                _errorState.value = "Failed to generate share link: ${e.message}"
            }
        }
    }

    /**
     * Clear the current share state.
     * Call this when dismissing the share dialog.
     */
    fun clearShareState() {
        _state.value = ShareUiState()
        _errorState.value = null
    }

    /**
     * Clear the current error message.
     */
    fun clearError() {
        _errorState.value = null
    }
}
