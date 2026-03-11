package de.hipp.app.taskcards.ui.app

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import de.hipp.app.taskcards.R
import de.hipp.app.taskcards.di.RepositoryProvider
import de.hipp.app.taskcards.ui.theme.TaskCardsTheme
import de.hipp.app.taskcards.worker.DailyReminderScheduler
import de.hipp.app.taskcards.worker.NotificationChannels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Root composable for the TaskCards application.
 * Handles notification channels, permission requests (POST_NOTIFICATIONS and
 * SCHEDULE_EXACT_ALARM), authentication state, daily reminders, and theming.
 *
 * @param initialDeepLinkUri Optional deep link URI to process on startup
 */
@Composable
fun TaskCardsApp(initialDeepLinkUri: Uri? = null) {
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

    // POST_NOTIFICATIONS is still required after the WorkManager→AlarmManager migration:
    // ReminderAlarmReceiver uses NotificationManagerCompat.notify() to show the
    // notification when the alarm fires — this requires POST_NOTIFICATIONS on Android 13+.
    // Request notification permission on first launch (Android 13+)
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Request SCHEDULE_EXACT_ALARM at startup (Android 12+).
    // Required for AlarmManager.setExactAndAllowWhileIdle() used by ReminderScheduler.
    // Requested after POST_NOTIFICATIONS so dialogs appear sequentially.
    val scheduleExactAlarmLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { /* permission state re-checked in SettingsScreen on resume */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                scheduleExactAlarmLauncher.launch(
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                )
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

    val userEmail by produceState<String?>(initialValue = null, key1 = userId) {
        value = if (userId != null) {
            try {
                authService.getCurrentUserEmail()
            } catch (e: Exception) {
                Log.e("TaskCardsApp", "Failed to get user email", e)
                null
            }
        } else null
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
                        authService.signInWithGoogle(context)
                        isLoading.value = false
                        Log.d("TaskCardsApp", "Google Sign-In successful")
                    } catch (e: Exception) {
                        isLoading.value = false
                        errorMessage.value = e.message ?: context.getString(R.string.auth_error)
                        Log.e("TaskCardsApp", "Google Sign-In failed", e)
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
            userEmail = userEmail
        )
    }
}
