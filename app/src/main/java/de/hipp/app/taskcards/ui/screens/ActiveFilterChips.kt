package de.hipp.app.taskcards.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.hipp.app.taskcards.R
import de.hipp.app.taskcards.model.SearchFilter
import de.hipp.app.taskcards.model.StatusFilter
import de.hipp.app.taskcards.ui.theme.Gold400
import de.hipp.app.taskcards.ui.theme.Gold600

/**
 * Displays active filter criteria as dismissible chips below the search bar.
 * Shows:
 * - Active due date range
 * - Status filter (if not default)
 * - Clear all button
 *
 * Uses spring-based animations for smooth chip appearance/dismissal.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActiveFilterChips(
    filter: SearchFilter,
    onClearDueDateRange: () -> Unit,
    onClearStatusFilter: () -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    AnimatedVisibility(
        visible = !filter.isEmpty(),
        enter = expandVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) + fadeIn(),
        exit = shrinkVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) + fadeOut()
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FlowRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Due date chip
                filter.dueDateRange?.let { range ->
                    InputChip(
                        selected = true,
                        onClick = onClearDueDateRange,
                        label = { Text(range.displayName) },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(
                                    R.string.filter_chip_remove,
                                    range.displayName
                                ),
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        colors = InputChipDefaults.inputChipColors(
                            selectedContainerColor = Gold400.copy(alpha = 0.18f),
                            selectedLabelColor = onSurface,
                            selectedTrailingIconColor = Gold600,
                            selectedLeadingIconColor = Gold600
                        )
                    )
                }

                // Status chip (only if not default)
                if (filter.statusFilter != StatusFilter.ACTIVE_ONLY) {
                    InputChip(
                        selected = true,
                        onClick = onClearStatusFilter,
                        label = { Text(filter.statusFilter.displayName) },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(
                                    R.string.filter_chip_remove,
                                    filter.statusFilter.displayName
                                ),
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        colors = InputChipDefaults.inputChipColors(
                            selectedContainerColor = Gold400.copy(alpha = 0.18f),
                            selectedLabelColor = onSurface,
                            selectedTrailingIconColor = Gold600,
                            selectedLeadingIconColor = Gold600
                        )
                    )
                }
            }

            // Clear all button (only show if filters are active)
            if (hasActiveFilters(filter)) {
                TextButton(onClick = onClearAll) {
                    Text(stringResource(R.string.filter_clear_all))
                }
            }
        }
    }
}

/**
 * Checks if there are any removable filters active.
 * (Excludes status filter if it's the default ACTIVE_ONLY)
 */
private fun hasActiveFilters(filter: SearchFilter): Boolean {
    return filter.dueDateRange != null ||
            filter.statusFilter != StatusFilter.ACTIVE_ONLY
}
