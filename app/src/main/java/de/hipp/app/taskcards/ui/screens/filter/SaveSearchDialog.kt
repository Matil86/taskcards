package de.hipp.app.taskcards.ui.screens.filter

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.hipp.app.taskcards.R

/**
 * Dialog for saving a search with a custom name.
 *
 * Presents:
 * - Text field for search name input
 * - Save button (enabled when name is not blank)
 * - Cancel button to dismiss
 */
@Composable
fun SaveSearchDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var searchName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.save_search_dialog_title)) },
        text = {
            Column {
                Text(stringResource(R.string.save_search_dialog_message))
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = searchName,
                    onValueChange = { searchName = it },
                    label = { Text(stringResource(R.string.save_search_name_label)) },
                    placeholder = { Text(stringResource(R.string.save_search_name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(searchName) },
                enabled = searchName.isNotBlank()
            ) {
                Text(stringResource(R.string.save_search_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.save_search_cancel))
            }
        }
    )
}
