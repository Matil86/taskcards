package de.hipp.app.taskcards.ui.screens.settings.cards

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.hipp.app.taskcards.R
import de.hipp.app.taskcards.ui.screens.settings.formatTime24Hour
import de.hipp.app.taskcards.ui.theme.Dimensions
import de.hipp.app.taskcards.ui.theme.focusIndicator

@Composable
fun NotificationSettingsCard(
    remindersEnabled: Boolean,
    reminderHour: Int,
    reminderMinute: Int,
    notificationSound: Boolean,
    notificationVibration: Boolean,
    onRemindersToggle: (Boolean) -> Unit,
    onReminderTimeClick: () -> Unit,
    onNotificationSoundToggle: (Boolean) -> Unit,
    onNotificationVibrationToggle: (Boolean) -> Unit,
    canScheduleExactAlarms: Boolean
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_notifications),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Permission warning banner (shown when exact alarm permission is not granted)
            if (!canScheduleExactAlarms) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_exact_alarm_required),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Button(
                        onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                            )
                        }
                    ) {
                        Text(text = stringResource(R.string.settings_grant_alarm_permission))
                    }
                }
            }

            // Enable Reminders
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_reminders_enabled),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.settings_reminders_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = remindersEnabled,
                    onCheckedChange = onRemindersToggle,
                    enabled = canScheduleExactAlarms,
                    modifier = Modifier
                        .sizeIn(
                            minWidth = Dimensions.MinTouchTarget,
                            minHeight = Dimensions.MinTouchTarget
                        )
                        .focusIndicator(shape = RoundedCornerShape(16.dp))
                )
            }

            // Reminder Time (only show if reminders enabled and permission granted)
            if (remindersEnabled && canScheduleExactAlarms) {
                // Extract strings for semantics
                val clickLabel = stringResource(R.string.settings_reminder_time)
                val reminderTimeDescription = stringResource(
                    R.string.cd_reminder_time_setting,
                    formatTime24Hour(reminderHour, reminderMinute)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            onClickLabel = clickLabel
                        ) { onReminderTimeClick() }
                        .semantics {
                            contentDescription = reminderTimeDescription
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_reminder_time),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = formatTime24Hour(reminderHour, reminderMinute),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Notification Sound
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_notification_sound),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.settings_notification_sound_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = notificationSound,
                        onCheckedChange = onNotificationSoundToggle,
                        modifier = Modifier
                            .sizeIn(
                                minWidth = Dimensions.MinTouchTarget,
                                minHeight = Dimensions.MinTouchTarget
                            )
                            .focusIndicator(shape = RoundedCornerShape(16.dp))
                    )
                }

                // Notification Vibration
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_notification_vibration),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.settings_notification_vibration_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = notificationVibration,
                        onCheckedChange = onNotificationVibrationToggle,
                        modifier = Modifier
                            .sizeIn(
                                minWidth = Dimensions.MinTouchTarget,
                                minHeight = Dimensions.MinTouchTarget
                            )
                            .focusIndicator(shape = RoundedCornerShape(16.dp))
                    )
                }
            }
        }
    }
}
