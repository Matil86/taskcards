package de.hipp.app.taskcards.ui.app

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import de.hipp.app.taskcards.auth.AuthService
import de.hipp.app.taskcards.data.preferences.PreferencesRepository
import de.hipp.app.taskcards.util.Constants

@Composable
internal fun rememberDefaultListId(
    isAuthenticated: Boolean,
    authService: AuthService,
    preferencesRepo: PreferencesRepository
): MutableState<String> {
    val state = remember { mutableStateOf(Constants.DEFAULT_LIST_ID) }

    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated) {
            val userId = authService.getCurrentUserId()
            if (userId != null) {
                val storedListId = preferencesRepo.getLastUsedListId()
                if (storedListId != null && Constants.isUuidListId(storedListId)) {
                    state.value = storedListId
                    Log.d("MainApp", "Using existing UUID list ID: $storedListId")
                } else {
                    val newListId = Constants.generateNewListId()
                    preferencesRepo.setLastUsedListId(newListId)
                    state.value = newListId
                    Log.d("MainApp", "Generated new UUID list ID: $newListId")
                }
            }
        } else {
            state.value = Constants.DEFAULT_LIST_ID
            Log.d("MainApp", "Using default list ID (unauthenticated)")
        }
    }

    return state
}
