package de.hipp.app.taskcards.ui.app

import android.app.Activity
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import de.hipp.app.taskcards.R
import de.hipp.app.taskcards.di.RepositoryProvider
import de.hipp.app.taskcards.ui.theme.TaskCardsTheme
import de.hipp.app.taskcards.worker.DailyReminderScheduler
import de.hipp.app.taskcards.worker.NotificationChannels

@Composable
fun TaskCardsApp(initialDeepLinkUri: android.net.Uri? = null) {
    val context = LocalContext.current

    // Create notification channels on app start
    LaunchedEffect(Unit) {
        NotificationChannels.createNotificationChannels(context)
        Log.d("TaskCardsApp", "Notification channels created")
    }
    val preferencesRepo = RepositoryProvider.getPreferencesRepository(context)
    val highContrastMode by preferencesRepo.highContrastMode.collectAsState(initial = false)

    // Request notification permission for Android 13+ (POST_NOTIFICATIONS)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("TaskCardsApp", "Notification permission granted")
        } else {
            Log.w("TaskCardsApp", "Notification permission denied")
        }
    }

    // Request notification permission on first launch (Android 13+)
    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Schedule daily reminder if reminders are enabled
    LaunchedEffect(Unit) {
        val preferencesRepo = RepositoryProvider.getPreferencesRepository(context)
        val settings = preferencesRepo.settings.first()

        if (settings.remindersEnabled) {
            DailyReminderScheduler.scheduleDailyReminder(
                context,
                settings.reminderHour,
                settings.reminderMinute
            )
            Log.d("TaskCardsApp", "Daily reminder scheduled for ${settings.reminderHour}:${settings.reminderMinute}")
        }
    }

    // Authentication state
    val authService = remember { RepositoryProvider.getAuthService(context) }

    // Initialize authentication state synchronously before first composition
    val userId by remember {
        authService.observeAuthState()
    }.collectAsState(initial = null)

    val isLoading = remember { mutableStateOf(false) }
    val errorMessage = remember { mutableStateOf<String?>(null) }

    // Update RepositoryProvider with authentication state
    // Set immediately to avoid race condition
    RepositoryProvider.setAuthenticated(userId != null)

    // Google Sign-In launcher
    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    isLoading.value = true
                    errorMessage.value = null
                    authService.handleGoogleSignInResult(result.data)
                    isLoading.value = false
                    Log.d("TaskCardsApp", "Google Sign-In successful")
                } catch (e: Exception) {
                    isLoading.value = false
                    errorMessage.value = e.message ?: context.getString(R.string.auth_error)
                    Log.e("TaskCardsApp", "Google Sign-In failed", e)
                }
            }
        } else {
            isLoading.value = false
            errorMessage.value = context.getString(R.string.auth_error)
            Log.e("TaskCardsApp", "Google Sign-In canceled or failed")
        }
    }

    TaskCardsTheme(useHighContrast = highContrastMode) {
        // Always show main app - authentication is optional for local-only usage
        MainApp(
            isAuthenticated = userId != null,
            initialDeepLinkUri = initialDeepLinkUri,
            onSignInClick = {
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        isLoading.value = true
                        errorMessage.value = null
                        val intent = authService.getGoogleSignInIntent()
                        signInLauncher.launch(intent)
                    } catch (e: Exception) {
                        isLoading.value = false
                        errorMessage.value = e.message ?: context.getString(R.string.auth_error)
                        Log.e("TaskCardsApp", "Failed to get sign-in intent", e)
                    }
                }
            },
            onSignOutClick = {
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        authService.signOut()
                        Log.d("TaskCardsApp", "Signed out successfully")
                    } catch (e: Exception) {
                        Log.e("TaskCardsApp", "Error signing out", e)
                    }
                }
            },
            userEmail = remember(userId) {
                if (userId != null) {
                    runBlocking {
                        authService.getCurrentUserEmail()
                    }
                } else null
            }
        )
    }
}
