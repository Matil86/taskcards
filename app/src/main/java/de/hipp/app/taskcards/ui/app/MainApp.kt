package de.hipp.app.taskcards.ui.app

import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.EntryPointAccessors
import de.hipp.app.taskcards.R
import de.hipp.app.taskcards.deeplink.DeepLinkResult
import de.hipp.app.taskcards.di.RepositoryProvider
import de.hipp.app.taskcards.ui.MainActivity
import de.hipp.app.taskcards.ui.navigation.ModernNavigationBar
import de.hipp.app.taskcards.ui.screens.cards.CardsScreen
import de.hipp.app.taskcards.ui.screens.list.ListScreen
import de.hipp.app.taskcards.ui.screens.settings.SettingsScreen
import de.hipp.app.taskcards.util.Constants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(
    isAuthenticated: Boolean = false,
    userEmail: String? = null,
    initialDeepLinkUri: android.net.Uri? = null,
    onSignInClick: () -> Unit = {},
    onSignOutClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Get auth service to retrieve user ID
    val authService = remember { RepositoryProvider.getAuthService(context) }
    val preferencesRepo = remember { RepositoryProvider.getPreferencesRepository(context) }

    // Authentication-based list ID selection:
    // - Authenticated users get UUID-based list IDs for Firestore cloud sync
    //   Each authenticated user gets a unique UUID that persists across sessions
    // - Unauthenticated users use DEFAULT_LIST_ID ("default-list")
    //   for local Room storage only (no Firestore access)
    var defaultListId by remember { mutableStateOf(Constants.DEFAULT_LIST_ID) }

    // Generate or retrieve UUID-based list ID for authenticated users
    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated) {
            val userId = authService.getCurrentUserId()
            if (userId != null) {
                // Check if user already has a stored list ID
                val storedListId = preferencesRepo.getLastUsedListId()

                if (storedListId != null && Constants.isUuidListId(storedListId)) {
                    // User has an existing UUID-based list ID
                    defaultListId = storedListId
                    Log.d("MainApp", "Using existing UUID list ID: $storedListId")
                } else {
                    // Generate new UUID for this authenticated user
                    val newListId = Constants.generateNewListId()
                    preferencesRepo.setLastUsedListId(newListId)
                    defaultListId = newListId
                    Log.d("MainApp", "Generated new UUID list ID: $newListId")
                }
            }
        } else {
            // Unauthenticated - use default list
            defaultListId = Constants.DEFAULT_LIST_ID
            Log.d("MainApp", "Using default list ID (unauthenticated)")
        }
    }

    // Deep link handling state
    var showImportDialog by remember { mutableStateOf(false) }
    var deepLinkResult by remember { mutableStateOf<DeepLinkResult?>(null) }
    var qrScannedUri by remember { mutableStateOf<android.net.Uri?>(null) }

    // Handle QR code scanned deep links
    LaunchedEffect(qrScannedUri) {
        if (qrScannedUri != null) {
            Log.d("MainApp", "Processing QR scanned deep link: $qrScannedUri")

            val entryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext,
                MainActivity.DeepLinkHandlerEntryPoint::class.java
            )
            val deepLinkHandler = entryPoint.deepLinkHandler()

            val result = deepLinkHandler.handleDeepLink(
                qrScannedUri!!,
                defaultListId
            )

            when (result) {
                is DeepLinkResult.Task, is DeepLinkResult.List -> {
                    deepLinkResult = result
                    showImportDialog = true
                }
                is DeepLinkResult.NavigateToList -> {
                    // Navigate directly to the shared list (real-time collaboration)
                    Log.d("MainApp", "Navigating to shared list from QR: ${result.listId}")

                    // Update defaultListId to the scanned list ID for real-time collaboration
                    defaultListId = result.listId
                    preferencesRepo.setLastUsedListId(result.listId)

                    navController.navigate("list")

                    // Show success notification with list ID
                    val successMessage = context.getString(R.string.list_loaded_success, result.listId)
                    Toast.makeText(context, successMessage, Toast.LENGTH_LONG).show()
                }
                is DeepLinkResult.Invalid -> {
                    val errorMessage = context.getString(R.string.list_load_error, result.reason)
                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                }
            }

            // Clear the QR scanned URI after processing
            qrScannedUri = null
        }
    }

    // Handle initial deep link
    LaunchedEffect(initialDeepLinkUri) {
        if (initialDeepLinkUri != null) {
            Log.d("MainApp", "Processing deep link: $initialDeepLinkUri")

            val entryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext,
                MainActivity.DeepLinkHandlerEntryPoint::class.java
            )
            val deepLinkHandler = entryPoint.deepLinkHandler()

            val result = deepLinkHandler.handleDeepLink(
                initialDeepLinkUri,
                defaultListId
            )

            when (result) {
                is DeepLinkResult.Task, is DeepLinkResult.List -> {
                    deepLinkResult = result
                    showImportDialog = true
                }
                is DeepLinkResult.NavigateToList -> {
                    // Navigate directly to the shared list (real-time collaboration)
                    Log.d("MainApp", "Navigating to shared list: ${result.listId}")

                    // Update defaultListId to the scanned list ID for real-time collaboration
                    defaultListId = result.listId
                    preferencesRepo.setLastUsedListId(result.listId)

                    navController.navigate("list")

                    // Show success notification with list ID
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

    // Import confirmation dialog
    if (showImportDialog && deepLinkResult != null) {
        DeepLinkImportDialog(
            result = deepLinkResult!!,
            onConfirm = {
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val entryPoint = EntryPointAccessors.fromApplication(
                            context.applicationContext,
                            MainActivity.DeepLinkHandlerEntryPoint::class.java
                        )
                        val deepLinkHandler = entryPoint.deepLinkHandler()

                        when (val result = deepLinkResult) {
                            is DeepLinkResult.Task -> {
                                deepLinkHandler.importTask(result.task, result.targetListId)
                                Toast.makeText(context, "Task imported successfully!", Toast.LENGTH_SHORT).show()
                            }
                            is DeepLinkResult.List -> {
                                deepLinkHandler.importList(result.list)
                                Toast.makeText(context, "List imported successfully!", Toast.LENGTH_SHORT).show()
                                navController.navigate("list/$defaultListId")
                            }
                            else -> {}
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
                        Log.e("MainApp", "Failed to import from deep link", e)
                    } finally {
                        showImportDialog = false
                        deepLinkResult = null
                    }
                }
            },
            onDismiss = {
                showImportDialog = false
                deepLinkResult = null
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        bottomBar = {
            // Show bottom navigation for primary screens: cards, list, and settings
            if (currentRoute?.startsWith("cards") == true || currentRoute?.startsWith("list") == true || currentRoute == "settings") {
                ModernNavigationBar(
                    currentRoute = currentRoute,
                    onNavigateToCards = {
                        navController.navigate("cards")
                    },
                    onNavigateToList = {
                        navController.navigate("list")
                    },
                    onNavigateToSettings = { navController.navigate("settings") }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            NavHost(
                navController = navController,
                startDestination = "cards"
            ) {
                composable("cards") {
                    CardsScreen(listId = defaultListId)
                }
                composable("list") {
                    ListScreen(
                        listId = defaultListId,
                        onLoadList = { scannedUrl ->
                            // Process QR code scanned URL through deep link handler
                            try {
                                qrScannedUri = android.net.Uri.parse(scannedUrl)
                            } catch (e: Exception) {
                                Log.e("MainApp", "Failed to parse QR code URL: $scannedUrl", e)
                                val errorMessage = context.getString(R.string.qr_scan_invalid)
                                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                            }
                        },
                        onNavigateToListSelector = null // Disabled in single-list mode
                    )
                }
                composable("settings") {
                    SettingsScreen(
                        isAuthenticated = isAuthenticated,
                        userEmail = userEmail,
                        onSignInClick = onSignInClick,
                        onSignOutClick = onSignOutClick
                    )
                }
            }
        }
    }
}
