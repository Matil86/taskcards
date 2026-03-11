package de.hipp.app.taskcards.ui.app

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import de.hipp.app.taskcards.R
import de.hipp.app.taskcards.deeplink.DeepLinkResult
import de.hipp.app.taskcards.di.RepositoryProvider
import de.hipp.app.taskcards.ui.navigation.ModernNavigationBar

@Composable
fun MainApp(
    isAuthenticated: Boolean = false,
    userEmail: String? = null,
    initialDeepLinkUri: Uri? = null,
    onSignInClick: () -> Unit = {},
    onSignOutClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val authService = remember { RepositoryProvider.getAuthService(context) }
    val preferencesRepo = remember { RepositoryProvider.getPreferencesRepository(context) }
    val metadataRepo = remember { RepositoryProvider.getMetadataRepository(context) }
    var defaultListId by rememberDefaultListId(
        isAuthenticated = isAuthenticated,
        authService = authService,
        preferencesRepo = preferencesRepo,
        metadataRepo = metadataRepo,
        defaultListName = context.getString(R.string.list_default_name)
    )

    var showImportDialog by remember { mutableStateOf(false) }
    var deepLinkResult by remember { mutableStateOf<DeepLinkResult?>(null) }
    var qrScannedUri by remember { mutableStateOf<Uri?>(null) }

    DeepLinkProcessor(
        initialDeepLinkUri = initialDeepLinkUri,
        qrScannedUri = qrScannedUri,
        defaultListId = defaultListId,
        preferencesRepo = preferencesRepo,
        onNavigateToList = { listId ->
            defaultListId = listId
            navController.navigate("list")
            qrScannedUri = null
        },
        onShowImportDialog = { result ->
            deepLinkResult = result
            showImportDialog = true
            qrScannedUri = null
        }
    )

    DeepLinkImportHandler(
        showImportDialog = showImportDialog,
        deepLinkResult = deepLinkResult,
        defaultListId = defaultListId,
        navController = navController,
        onDismiss = {
            showImportDialog = false
            deepLinkResult = null
        }
    )

    Scaffold(
        bottomBar = {
            if (currentRoute?.startsWith("cards") == true || currentRoute?.startsWith("list") == true || currentRoute == "settings") {
                ModernNavigationBar(
                    currentRoute = currentRoute,
                    onNavigateToCards = { navController.navigate("cards") },
                    onNavigateToList = { navController.navigate("list") },
                    onNavigateToSettings = { navController.navigate("settings") }
                )
            }
        }
    ) { paddingValues ->
        MainNavHost(
            navController = navController,
            defaultListId = defaultListId,
            isAuthenticated = isAuthenticated,
            userEmail = userEmail,
            onSignInClick = onSignInClick,
            onSignOutClick = onSignOutClick,
            onLoadList = { scannedUrl ->
                try {
                    qrScannedUri = Uri.parse(scannedUrl)
                } catch (e: Exception) {
                    Log.e("MainApp", "Failed to parse QR code URL: $scannedUrl", e)
                    Toast.makeText(context, context.getString(R.string.qr_scan_invalid), Toast.LENGTH_SHORT).show()
                }
            },
            onDefaultListIdChanged = { listId ->
                defaultListId = listId
            },
            modifier = Modifier.padding(paddingValues)
        )
    }
}
