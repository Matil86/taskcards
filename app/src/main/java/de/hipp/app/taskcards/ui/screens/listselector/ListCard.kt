package de.hipp.app.taskcards.ui.screens.listselector

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.hipp.app.taskcards.R
import de.hipp.app.taskcards.model.TaskList
import de.hipp.app.taskcards.ui.theme.AccentGreen
import de.hipp.app.taskcards.ui.theme.BrandPurple
import de.hipp.app.taskcards.ui.theme.Dimensions
import de.hipp.app.taskcards.ui.theme.focusIndicator

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ListCard(
    list: TaskList,
    taskCount: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val cardDescription = context.getString(
        R.string.list_selector_list_card_description,
        list.name,
        taskCount
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(Dimensions.CornerRadiusLarge),
                ambientColor = BrandPurple.copy(alpha = 0.2f),
                spotColor = BrandPurple.copy(alpha = 0.2f)
            )
            .clip(RoundedCornerShape(Dimensions.CornerRadiusLarge))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .focusIndicator(shape = RoundedCornerShape(Dimensions.CornerRadiusLarge))
            .semantics { contentDescription = cardDescription },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(Dimensions.CornerRadiusLarge)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = list.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stringResource(R.string.list_selector_task_count, taskCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    onClick = onLongClick,
                    modifier = Modifier
                        .size(Dimensions.MinTouchTarget)
                        .clip(RoundedCornerShape(Dimensions.CornerRadiusSmall))
                        .focusIndicator(shape = RoundedCornerShape(Dimensions.CornerRadiusSmall)),
                    color = AccentGreen.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(Dimensions.CornerRadiusSmall)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.list_selector_rename_button),
                            tint = AccentGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Surface(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(Dimensions.MinTouchTarget)
                        .clip(RoundedCornerShape(Dimensions.CornerRadiusSmall))
                        .focusIndicator(shape = RoundedCornerShape(Dimensions.CornerRadiusSmall)),
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(Dimensions.CornerRadiusSmall)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.list_selector_delete_button),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
