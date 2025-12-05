package de.hipp.app.taskcards.ui.screens.list

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import de.hipp.app.taskcards.R
import de.hipp.app.taskcards.ui.theme.BrandPurple
import de.hipp.app.taskcards.ui.theme.Dimensions
import de.hipp.app.taskcards.ui.theme.focusIndicator

@Composable
fun AddItemRow(
    onAdd: (String) -> Unit,
    onLoadList: () -> Unit = {}
) {
    var text by remember { mutableStateOf(TextFieldValue("")) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp)
        ) {
            // Load list button on the left
            FilledTonalIconButton(
                onClick = onLoadList,
                modifier = Modifier
                    .sizeIn(
                        minWidth = Dimensions.MinTouchTarget,
                        minHeight = Dimensions.MinTouchTarget
                    )
                    .focusIndicator(shape = RoundedCornerShape(12.dp)),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = BrandPurple.copy(alpha = 0.2f),
                    contentColor = BrandPurple
                )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.List,
                    contentDescription = stringResource(R.string.list_load_icon_description)
                )
            }
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .weight(1f)
                    .focusIndicator(shape = RoundedCornerShape(12.dp))
                    .testTag("AddTaskInput"),
                label = { Text(stringResource(R.string.list_add_task_label)) },
                placeholder = { Text(stringResource(R.string.list_add_placeholder)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandPurple,
                    focusedLabelColor = BrandPurple,
                    cursorColor = BrandPurple
                )
            )
            Spacer(Modifier.width(8.dp))
            FloatingActionButton(
                onClick = {
                    if (text.text.isNotBlank()) {
                        onAdd(text.text)
                        text = TextFieldValue("")
                    }
                },
                containerColor = if (text.text.isNotBlank()) BrandPurple else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (text.text.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(Dimensions.FabSize)
                    .focusIndicator(shape = RoundedCornerShape(16.dp))
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.list_add_icon_description))
            }
        }
    }
}
