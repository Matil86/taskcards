package de.hipp.app.taskcards.ui.app

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import de.hipp.app.taskcards.R
import de.hipp.app.taskcards.data.preferences.PreferencesRepository
import de.hipp.app.taskcards.deeplink.DeepLinkHandler
import de.hipp.app.taskcards.deeplink.DeepLinkResult
import org.koin.compose.getKoin

/**
 * Composable that handles deep link and QR-code import processing.
 *
 * Extracted from MainApp to keep navigation setup lean and give deep link
 * orchestration a single, clearly named home.
 *
 * @param initialDeepLinkUri  URI received when the app was launched via a deep link intent.
 * @param qrScannedUri        URI produced by scanning a QR code inside the app.
 * @param defaultListId       The list ID used as the import target when the deep link does not
 *                            specify one explicitly.
 * @param preferencesRepo     Repository used to persist a newly-adopted list ID.
 * @param onNavigateToList    Called when the deep link result is a direct navigation to a list.
 * @param onShowImportDialog  Called when the deep link result requires a confirmation dialog.
 */
@Composable
fun DeepLinkProcessor(
    initialDeepLinkUri: Uri?,
    qrScannedUri: Uri?,
    defaultListId: String,
    preferencesRepo: PreferencesRepository,
    onNavigateToList: (listId: String) -> Unit,
    onShowImportDialog: (DeepLinkResult) -> Unit
) {
    val context = LocalContext.current
    val koin = getKoin()

    // Handle QR code scanned deep links
    LaunchedEffect(qrScannedUri) {
        if (qrScannedUri != null) {
            Log.d("DeepLinkProcessor", "Processing QR scanned deep link")

            val deepLinkHandler = koin.get<DeepLinkHandler>()

            val result = deepLinkHandler.handleDeepLink(qrScannedUri, defaultListId)

            when (result) {
                is DeepLinkResult.Task, is DeepLinkResult.List -> {
                    onShowImportDialog(result)
                }
                is DeepLinkResult.NavigateToList -> {
                    Log.d("DeepLinkProcessor", "Navigating to shared list from QR")
                    preferencesRepo.setLastUsedListId(result.listId)
                    onNavigateToList(result.listId)
                    val successMessage = context.getString(R.string.list_loaded_success, result.listId)
                    Toast.makeText(context, successMessage, Toast.LENGTH_LONG).show()
                }
                is DeepLinkResult.Invalid -> {
                    val errorMessage = context.getString(R.string.list_load_error, result.reason)
                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Handle initial deep link (app launched via deep link intent)
    LaunchedEffect(initialDeepLinkUri) {
        if (initialDeepLinkUri != null) {
            Log.d("DeepLinkProcessor", "Processing deep link")

            val deepLinkHandler = koin.get<DeepLinkHandler>()

            val result = deepLinkHandler.handleDeepLink(initialDeepLinkUri, defaultListId)

            when (result) {
                is DeepLinkResult.Task, is DeepLinkResult.List -> {
                    onShowImportDialog(result)
                }
                is DeepLinkResult.NavigateToList -> {
                    Log.d("DeepLinkProcessor", "Navigating to shared list")
                    preferencesRepo.setLastUsedListId(result.listId)
                    onNavigateToList(result.listId)
                    val successMessage = context.getString(R.string.list_loaded_success, result.listId)
                    Toast.makeText(context, successMessage, Toast.LENGTH_LONG).show()
                }
                is DeepLinkResult.Invalid -> {
                    val errorMessage = context.getString(R.string.list_load_error, result.reason)
                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
