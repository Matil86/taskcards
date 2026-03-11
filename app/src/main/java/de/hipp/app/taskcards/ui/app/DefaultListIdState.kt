package de.hipp.app.taskcards.ui.app

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import de.hipp.app.taskcards.auth.AuthService
import de.hipp.app.taskcards.data.TaskListMetadataRepository
import de.hipp.app.taskcards.data.preferences.PreferencesRepository
import de.hipp.app.taskcards.util.Constants
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first

@Composable
internal fun rememberDefaultListId(
    isAuthenticated: Boolean,
    authService: AuthService,
    preferencesRepo: PreferencesRepository,
    metadataRepo: TaskListMetadataRepository,
    defaultListName: String = "My Tasks"
): MutableState<String> {
    val state = remember { mutableStateOf(Constants.DEFAULT_LIST_ID) }

    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated) {
            authService.getCurrentUserId() ?: return@LaunchedEffect

            val storedId = preferencesRepo.getLastUsedListId()

            // Wait for the first non-empty list emission from the snapshot listener.
            // Empty emissions are skipped — they are either the immediate local-cache result
            // (empty on a fresh install) or a legitimately empty list (handled by timeout below).
            // Returning users get an instant response from their local Firestore cache.
            // Fresh-install users wait for the server response (~2–5s).
            // If 10 seconds pass with no non-empty result, fall back to a direct server fetch
            // as the authoritative "no lists exist" confirmation.
            val resolvedId: String? = try {
                val lists = withTimeout(10_000) {
                    metadataRepo.observeTaskLists()
                        .filter { it.isNotEmpty() }
                        .first()
                }
                selectDefaultList(lists, storedId)
            } catch (e: TimeoutCancellationException) {
                // 10 s with no non-empty result — confirm with server
                try {
                    metadataRepo.getDefaultListId()
                } catch (ex: Exception) {
                    Log.w("DefaultListIdState", "Could not fetch lists: ${ex.message}")
                    null
                }
            } catch (e: Exception) {
                Log.w("DefaultListIdState", "Could not fetch lists: ${e.message}")
                try { metadataRepo.getDefaultListId() } catch (ex: Exception) { null }
            }

            if (resolvedId != null) {
                // Lists exist — use the resolved ID and persist the preference
                state.value = resolvedId
                preferencesRepo.setLastUsedListId(resolvedId)
            } else {
                // No lists exist — create one
                val newListId = Constants.generateNewListId()
                preferencesRepo.setLastUsedListId(newListId)
                state.value = newListId
                try {
                    metadataRepo.ensureListExists(newListId, defaultListName)
                } catch (e: Exception) {
                    Log.w("DefaultListIdState", "Could not create list: ${e.message}")
                }
            }
        } else {
            state.value = Constants.DEFAULT_LIST_ID
        }
    }

    return state
}
