package de.hipp.app.taskcards.ui.screens.settings

import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.hipp.app.taskcards.R
import de.hipp.app.taskcards.ui.theme.Dimensions

/**
 * Dialog for selecting a time using Material3 TimePicker.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true // Use 24-hour format
    )

    // Extract strings for semantics
    val selectTimeDescription = stringResource(R.string.cd_select_reminder_time)
    val confirmDescription = stringResource(R.string.cd_confirm_time_selection)
    val cancelDescription = stringResource(R.string.cd_cancel_time_selection)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.settings_reminder_time),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            TimePicker(
                state = timePickerState,
                modifier = Modifier.semantics {
                    contentDescription = selectTimeDescription
                }
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(timePickerState.hour, timePickerState.minute)
                },
                modifier = Modifier
                    .sizeIn(
                        minWidth = Dimensions.MinTouchTarget,
                        minHeight = Dimensions.MinTouchTarget
                    )
                    .semantics {
                        contentDescription = confirmDescription
                    }
            ) {
                Text(stringResource(R.string.due_date_save))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .sizeIn(
                        minWidth = Dimensions.MinTouchTarget,
                        minHeight = Dimensions.MinTouchTarget
                    )
                    .semantics {
                        contentDescription = cancelDescription
                    }
            ) {
                Text(stringResource(R.string.due_date_cancel))
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

/**
 * Formats time in 24-hour format.
 */
fun formatTime24Hour(hour: Int, minute: Int): String {
    return String.format("%02d:%02d", hour, minute)
}
