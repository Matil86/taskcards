package de.hipp.app.taskcards.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.hipp.app.taskcards.R
import de.hipp.app.taskcards.model.Language
import de.hipp.app.taskcards.ui.theme.Dimensions

/**
 * Dialog for selecting app language with flag emojis and native names.
 */
@Composable
fun LanguagePickerDialog(
    currentLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val languages = Language.getAllLanguages()

    // Extract strings for semantics
    val selectedText = stringResource(R.string.cd_language_selected)
    val notSelectedText = stringResource(R.string.cd_language_not_selected)
    val closeDescription = stringResource(R.string.cd_close_language_picker)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.select_language),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column {
                languages.forEach { language ->
                    val selectionStatus = if (language.code == currentLanguage) selectedText else notSelectedText
                    val languageDescription = stringResource(
                        R.string.cd_language_option,
                        "${language.flag} ${language.nativeName}",
                        selectionStatus
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onLanguageSelected(language.code)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp)
                            .semantics {
                                contentDescription = languageDescription
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = language.flag,
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                        Text(
                            text = language.nativeName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (language.code == currentLanguage) FontWeight.Bold else FontWeight.Normal,
                            color = if (language.code == currentLanguage)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                        if (language.code == currentLanguage) {
                            Spacer(Modifier.weight(1f))
                            Text(
                                text = "✓",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .sizeIn(
                        minWidth = Dimensions.MinTouchTarget,
                        minHeight = Dimensions.MinTouchTarget
                    )
                    .semantics {
                        contentDescription = closeDescription
                    }
            ) {
                Text(stringResource(R.string.due_date_cancel))
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}
