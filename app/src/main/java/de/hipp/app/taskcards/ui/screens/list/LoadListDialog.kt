package de.hipp.app.taskcards.ui.screens.list

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import de.hipp.app.taskcards.R
import de.hipp.app.taskcards.ui.theme.GoldAction
import de.hipp.app.taskcards.ui.theme.focusIndicator

@Composable
fun LoadListDialog(
    loadText: TextFieldValue,
    onLoadTextChange: (TextFieldValue) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.list_load_dialog_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = loadText,
                    onValueChange = onLoadTextChange,
                    label = { Text(stringResource(R.string.list_id_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusIndicator(shape = RoundedCornerShape(12.dp))
                        .semantics { contentDescription = context.getString(R.string.list_id_input_description) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldAction,
                        focusedLabelColor = GoldAction
                    )
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.list_load_dialog_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(loadText.text.trim())
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.focusIndicator(shape = RoundedCornerShape(12.dp))
            ) { Text(stringResource(R.string.list_load_button)) }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.focusIndicator(shape = RoundedCornerShape(12.dp))
            ) { Text(stringResource(R.string.list_cancel_button)) }
        },
        shape = RoundedCornerShape(24.dp)
    )
}
