package de.hipp.app.taskcards.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.hipp.app.taskcards.R
import de.hipp.app.taskcards.data.preferences.PreferencesRepository
import de.hipp.app.taskcards.di.StringProvider
import de.hipp.app.taskcards.util.Logger
import de.hipp.app.taskcards.worker.DailyReminderScheduler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
class SettingsViewModel(
    private val context: Context,
    private val preferencesRepo: PreferencesRepository,
    private val strings: StringProvider,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main
) : ViewModel() {

    data class UiState(
        val highContrastMode: Boolean = false,
        val remindersEnabled: Boolean = true,
        val reminderHour: Int = 9,
        val reminderMinute: Int = 0,
        val notificationSound: Boolean = true,
        val notificationVibration: Boolean = true,
        val language: String = "system",
        val error: String? = null
    )

    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState.asStateFlow()

    val state: StateFlow<UiState> = preferencesRepo.settings
        .map { settings ->
            UiState(
                highContrastMode = settings.highContrastMode,
                remindersEnabled = settings.remindersEnabled,
                reminderHour = settings.reminderHour,
                reminderMinute = settings.reminderMinute,
                notificationSound = settings.notificationSound,
                notificationVibration = settings.notificationVibration,
                language = settings.language
            )
        }
        .stateIn(
            scope = viewModelScope,
            // WhileSubscribed(5000): Keeps flow active for 5s after last subscriber unsubscribes
            // This prevents rapid resubscription overhead during configuration changes
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UiState()
        )

    /**
     * Enables or disables high-contrast colour mode.
     *
     * @param enabled `true` to turn on high contrast, `false` to turn it off.
     */
    fun setHighContrastMode(enabled: Boolean) {
        viewModelScope.launch(dispatcher) {
            try {
                preferencesRepo.setHighContrastMode(enabled)
                _errorState.value = null
            } catch (e: Exception) {
                _errorState.value = strings.getString(R.string.error_failed_to_save_setting, e.message ?: "")
                try {
                    Logger.e(TAG, { "Error setting high contrast mode" }, e)
                } catch (_: Exception) {
                    // Ignore Log exceptions in unit tests
                }
            }
        }
    }

    /**
     * Enables or disables daily reminder notifications globally.
     * Schedules or cancels the daily reminder worker accordingly.
     *
     * @param enabled `true` to enable reminders, `false` to cancel them.
     */
    fun setRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch(dispatcher) {
            try {
                preferencesRepo.setRemindersEnabled(enabled)

                // Schedule or cancel daily reminder based on enabled state
                if (enabled) {
                    val currentState = state.value
                    DailyReminderScheduler.scheduleDailyReminder(
                        context,
                        currentState.reminderHour,
                        currentState.reminderMinute
                    )
                } else {
                    DailyReminderScheduler.cancelDailyReminder(context)
                }

                _errorState.value = null
            } catch (e: Exception) {
                _errorState.value = strings.getString(R.string.error_failed_to_save_setting, e.message ?: "")
                try {
                    Logger.e(TAG, { "Error setting reminders enabled" }, e)
                } catch (_: Exception) {
                    // Ignore Log exceptions in unit tests
                }
            }
        }
    }

    /**
     * Updates the time at which the daily reminder fires and reschedules the worker.
     *
     * @param hour Hour of the day (0–23).
     * @param minute Minute of the hour (0–59).
     */
    fun setReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch(dispatcher) {
            try {
                preferencesRepo.setReminderTime(hour, minute)

                // Reschedule daily reminder with new time (if reminders are enabled)
                val currentState = state.value
                if (currentState.remindersEnabled) {
                    DailyReminderScheduler.scheduleDailyReminder(context, hour, minute)
                    Logger.d(TAG, { "Daily reminder rescheduled for $hour:$minute" })
                }

                _errorState.value = null
            } catch (e: Exception) {
                _errorState.value = strings.getString(R.string.error_failed_to_save_setting, e.message ?: "")
                try {
                    Logger.e(TAG, { "Error setting reminder time" }, e)
                } catch (_: Exception) {
                    // Ignore Log exceptions in unit tests
                }
            }
        }
    }

    /**
     * Enables or disables the notification sound for reminders.
     *
     * @param enabled `true` to play sound, `false` for silent notifications.
     */
    fun setNotificationSound(enabled: Boolean) {
        viewModelScope.launch(dispatcher) {
            try {
                preferencesRepo.setNotificationSound(enabled)
                _errorState.value = null
            } catch (e: Exception) {
                _errorState.value = strings.getString(R.string.error_failed_to_save_setting, e.message ?: "")
                try {
                    Logger.e(TAG, { "Error setting notification sound" }, e)
                } catch (_: Exception) {
                    // Ignore Log exceptions in unit tests
                }
            }
        }
    }

    /**
     * Enables or disables vibration for reminder notifications.
     *
     * @param enabled `true` to vibrate, `false` for no vibration.
     */
    fun setNotificationVibration(enabled: Boolean) {
        viewModelScope.launch(dispatcher) {
            try {
                preferencesRepo.setNotificationVibration(enabled)
                _errorState.value = null
            } catch (e: Exception) {
                _errorState.value = strings.getString(R.string.error_failed_to_save_setting, e.message ?: "")
                try {
                    Logger.e(TAG, { "Error setting notification vibration" }, e)
                } catch (_: Exception) {
                    // Ignore Log exceptions in unit tests
                }
            }
        }
    }

    /**
     * Persists the chosen UI language without triggering activity recreation.
     *
     * @param languageCode BCP-47 language tag (e.g. "en", "de") or "system" for the device default.
     */
    fun setLanguage(languageCode: String) {
        viewModelScope.launch(dispatcher) {
            try {
                preferencesRepo.setLanguage(languageCode)
                _errorState.value = null
            } catch (e: Exception) {
                _errorState.value = strings.getString(R.string.error_failed_to_save_setting, e.message ?: "")
                try {
                    Logger.e(TAG, { "Error setting language" }, e)
                } catch (_: Exception) {
                    // Ignore Log exceptions in unit tests
                }
            }
        }
    }

    /**
     * Sets language and calls onComplete callback after preference is saved.
     * Uses viewModelScope to avoid LeftCompositionCancellationException.
     * The callback runs in the ViewModel's scope which survives activity recreation.
     */
    fun setLanguageAndRecreate(languageCode: String, onComplete: () -> Unit) {
        viewModelScope.launch(dispatcher) {
            try {
                preferencesRepo.setLanguage(languageCode)
                _errorState.value = null
                // Call the callback after successful save
                onComplete()
            } catch (e: Exception) {
                _errorState.value = strings.getString(R.string.error_failed_to_save_setting, e.message ?: "")
                try {
                    Logger.e(TAG, { "Error setting language" }, e)
                } catch (_: Exception) {
                    // Ignore Log exceptions in unit tests
                }
            }
        }
    }

    /** Clears the current error message. */
    fun clearError() {
        _errorState.value = null
    }

    companion object {
        private const val TAG = "SettingsViewModel"
    }
}
