package de.hipp.app.taskcards.ui.screens.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.hipp.app.taskcards.R
import de.hipp.app.taskcards.ui.theme.Dimensions
import de.hipp.app.taskcards.ui.theme.Felt700
import de.hipp.app.taskcards.ui.theme.GoldAction
import de.hipp.app.taskcards.ui.theme.Gold400
import de.hipp.app.taskcards.ui.theme.Gold600
import de.hipp.app.taskcards.ui.theme.focusIndicator

@Composable
fun ListHeader(
    listId: String,
    listName: String = "",
    taskCount: Int,
    doneCount: Int,
    hasActiveFilters: Boolean,
    onOpenFilter: () -> Unit,
    onNavigateToListSelector: (() -> Unit)? = null,
    onShareList: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = listName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = LocalContext.current.getString(R.string.list_header_completed, doneCount, taskCount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.semantics {
                            liveRegion = LiveRegionMode.Polite
                        }
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Switch list button — only shown when list selector navigation is available
                if (onNavigateToListSelector != null) {
                    FilledTonalIconButton(
                        onClick = onNavigateToListSelector,
                        modifier = Modifier
                            .sizeIn(
                                minWidth = Dimensions.MinTouchTarget,
                                minHeight = Dimensions.MinTouchTarget
                            )
                            .focusIndicator(shape = RoundedCornerShape(12.dp)),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = GoldAction.copy(alpha = 0.15f),
                            contentColor = GoldAction
                        )
                    ) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = stringResource(R.string.switch_list))
                    }
                }

                // Share button
                FilledTonalIconButton(
                    onClick = onShareList,
                    modifier = Modifier
                        .sizeIn(
                            minWidth = Dimensions.MinTouchTarget,
                            minHeight = Dimensions.MinTouchTarget
                        )
                        .focusIndicator(shape = RoundedCornerShape(12.dp)),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = Felt700,
                        contentColor = GoldAction
                    )
                ) {
                    Icon(Icons.Default.Share, contentDescription = stringResource(R.string.share_list_description))
                }

                // Filter button with badge if filters active
                Box {
                    FilledTonalIconButton(
                        onClick = onOpenFilter,
                        modifier = Modifier
                            .sizeIn(
                                minWidth = Dimensions.MinTouchTarget,
                                minHeight = Dimensions.MinTouchTarget
                            )
                            .focusIndicator(shape = RoundedCornerShape(12.dp)),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = if (hasActiveFilters) Gold400.copy(alpha = 0.2f) else Felt700,
                            contentColor = if (hasActiveFilters) Gold600 else GoldAction
                        )
                    ) {
                        Icon(Icons.Default.FilterList, contentDescription = stringResource(R.string.filter_title))
                    }
                    if (hasActiveFilters) {
                        Surface(
                            modifier = Modifier
                                .size(12.dp)
                                .align(Alignment.TopEnd)
                                .offset(x = (-4).dp, y = 4.dp),
                            shape = CircleShape,
                            color = Gold600
                        ) {}
                    }
                }
            }
        }
    }
}
