package de.hipp.app.taskcards.data.preferences

/**
 * Data class representing user settings.
 * @param remindersEnabled whether task reminders are globally enabled
 * @param reminderHour hour of day (0-23) when reminders should be shown
 * @param reminderMinute minute of hour (0-59) when reminders should be shown
 * @param notificationSound whether notification sound is enabled
 * @param notificationVibration whether notification vibration is enabled
 * @param highContrastMode whether high contrast UI mode is enabled
 * @param language ISO language code ("en", "de", "ja") or "system" for device default
 */
data class Settings(
    val remindersEnabled: Boolean = true,
    val reminderHour: Int = 9,
    val reminderMinute: Int = 0,
    val notificationSound: Boolean = true,
    val notificationVibration: Boolean = true,
    val highContrastMode: Boolean = false,
    val language: String = "system"
)
