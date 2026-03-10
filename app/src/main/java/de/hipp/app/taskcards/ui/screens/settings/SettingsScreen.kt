package de.hipp.app.taskcards.ui.screens.settings

import android.app.AlarmManager
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import de.hipp.app.taskcards.BuildConfig
import de.hipp.app.taskcards.R
import de.hipp.app.taskcards.di.RepositoryProvider
import de.hipp.app.taskcards.ui.screens.settings.cards.AppVersionCard
import de.hipp.app.taskcards.ui.screens.settings.cards.AuthenticationCard
import de.hipp.app.taskcards.ui.screens.settings.cards.HighContrastModeCard
import de.hipp.app.taskcards.ui.screens.settings.cards.LanguageSettingCard
import de.hipp.app.taskcards.ui.screens.settings.cards.NotificationSettingsCard
import de.hipp.app.taskcards.ui.viewmodel.SettingsViewModel
import de.hipp.app.taskcards.ui.viewmodel.factoryOf

@Composable
fun SettingsScreen(
    isAuthenticated: Boolean = false,
    userEmail: String? = null,
    onSignInClick: () -> Unit = {},
    onSignOutClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val prefsRepo = RepositoryProvider.getPreferencesRepository(context)
    val strings = remember { RepositoryProvider.getStringProvider(context) }
    val vm: SettingsViewModel = viewModel(factory = factoryOf { SettingsViewModel(context, prefsRepo, strings) })
    val state by vm.state.collectAsState()
    val errorState by vm.errorState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val lifecycleOwner = LocalLifecycleOwner.current
    var canScheduleExactAlarms by remember {
        mutableStateOf(
            (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
        )
    }

    // Re-check when app comes back to foreground (user may have granted permission in Settings)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                canScheduleExactAlarms = (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Time picker dialog state
    var showTimePickerDialog by remember { mutableStateOf(false) }

    // Language picker dialog state
    var showLanguagePickerDialog by remember { mutableStateOf(false) }

    // Show error snackbar when error occurs
    LaunchedEffect(errorState) {
        errorState?.let { errorMessage ->
            snackbarHostState.showSnackbar(
                message = errorMessage,
                actionLabel = context.getString(R.string.action_dismiss),
                duration = androidx.compose.material3.SnackbarDuration.Short
            )
            vm.clearError()
        }
    }

    val gradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surface
        )
    )

    // Wrap content in Box to properly position SnackbarHost
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // Header
        androidx.compose.material3.Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        // High Contrast Mode Setting
        HighContrastModeCard(
            enabled = state.highContrastMode,
            onToggle = vm::setHighContrastMode
        )

        // Language Setting Card
        LanguageSettingCard(
            currentLanguage = state.language,
            onLanguageClick = { showLanguagePickerDialog = true }
        )

        // Notification Settings Card
        NotificationSettingsCard(
            remindersEnabled = state.remindersEnabled,
            reminderHour = state.reminderHour,
            reminderMinute = state.reminderMinute,
            notificationSound = state.notificationSound,
            notificationVibration = state.notificationVibration,
            onRemindersToggle = vm::setRemindersEnabled,
            onReminderTimeClick = { showTimePickerDialog = true },
            onNotificationSoundToggle = vm::setNotificationSound,
            onNotificationVibrationToggle = vm::setNotificationVibration,
            canScheduleExactAlarms = canScheduleExactAlarms
        )

        // Authentication Card
        AuthenticationCard(
            isAuthenticated = isAuthenticated,
            userEmail = userEmail,
            onSignInClick = onSignInClick,
            onSignOutClick = onSignOutClick
        )

        Spacer(modifier = Modifier.weight(1f))

        // App Version
        AppVersionCard(
            appName = stringResource(R.string.app_name),
            versionName = context.getString(R.string.settings_version, BuildConfig.VERSION_NAME)
        )
        }

        // SnackbarHost for error messages positioned at bottom
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                actionColor = MaterialTheme.colorScheme.error,
                shape = RoundedCornerShape(12.dp)
            )
        }
    }

    // Show time picker dialog when requested
    if (showTimePickerDialog) {
        TimePickerDialog(
            initialHour = state.reminderHour,
            initialMinute = state.reminderMinute,
            onDismiss = { showTimePickerDialog = false },
            onConfirm = { hour, minute ->
                vm.setReminderTime(hour, minute)
                showTimePickerDialog = false
            }
        )
    }

    // Show language picker dialog when requested
    if (showLanguagePickerDialog) {
        LanguagePickerDialog(
            currentLanguage = state.language,
            onLanguageSelected = { languageCode ->
                // Use ViewModel's scope to avoid LeftCompositionCancellationException
                // The callback runs after language is saved, then recreates activity
                vm.setLanguageAndRecreate(languageCode) {
                    activity?.recreate()
                }
                showLanguagePickerDialog = false
            },
            onDismiss = { showLanguagePickerDialog = false }
        )
    }
}
