package de.hipp.app.taskcards.ui.screens.filter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.hipp.app.taskcards.R
import de.hipp.app.taskcards.model.DueDateRange
import de.hipp.app.taskcards.model.SavedSearch
import de.hipp.app.taskcards.model.SearchFilter
import de.hipp.app.taskcards.model.StatusFilter

/**
 * Bottom sheet for configuring search and filter criteria.
 *
 * Features:
 * - Apply saved searches for quick filtering
 * - Filter by status (all/active/done/removed)
 * - Filter by due date range (overdue, today, this week, this month)
 * - Save current filter configuration for reuse
 * - Clear all active filters
 *
 * The sheet provides a comprehensive filtering interface organized into sections:
 * 1. Saved Searches - Quick access to frequently used filters
 * 2. Status Filter - Filter by task completion status
 * 3. Due Date Presets - Filter by date-based criteria
 * 4. Action Buttons - Save, clear, and apply operations
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterBottomSheet(
    currentFilter: SearchFilter,
    savedSearches: List<SavedSearch>,
    onFilterChanged: (SearchFilter) -> Unit,
    onSaveSearch: (String) -> Unit,
    onApplySavedSearch: (SavedSearch) -> Unit,
    onDeleteSavedSearch: (String) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState()
) {
    var showSaveDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Text(
                text = stringResource(R.string.filter_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Saved Searches Section
            if (savedSearches.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.saved_searches),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(12.dp))

                savedSearches.forEach { saved ->
                    SavedSearchItem(
                        search = saved,
                        onApply = {
                            onApplySavedSearch(saved)
                            onDismiss()
                        },
                        onDelete = { onDeleteSavedSearch(saved.id) }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            }

            // Status Filter Section
            Text(
                text = stringResource(R.string.filter_status),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusFilter.entries.forEach { status ->
                    FilterChip(
                        selected = currentFilter.statusFilter == status,
                        onClick = {
                            onFilterChanged(currentFilter.copy(statusFilter = status))
                        },
                        label = { Text(status.displayName) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Due Date Presets Section
            Text(
                text = stringResource(R.string.filter_due_date),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val presets = listOf(
                    DueDateRange.overdue(),
                    DueDateRange.today(),
                    DueDateRange.thisWeek(),
                    DueDateRange.thisMonth()
                )

                presets.forEach { preset ->
                    FilterChip(
                        selected = currentFilter.dueDateRange?.displayName == preset.displayName,
                        onClick = {
                            val newRange = if (currentFilter.dueDateRange?.displayName == preset.displayName) {
                                null // Deselect if already selected
                            } else {
                                preset
                            }
                            onFilterChanged(currentFilter.copy(dueDateRange = newRange))
                        },
                        label = { Text(preset.displayName) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        onFilterChanged(SearchFilter())
                    }
                ) {
                    Text(stringResource(R.string.filter_clear_all))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!currentFilter.isEmpty()) {
                        Button(
                            onClick = { showSaveDialog = true }
                        ) {
                            Text(stringResource(R.string.filter_save_search))
                        }
                    }

                    Button(onClick = onDismiss) {
                        Text(stringResource(R.string.filter_apply))
                    }
                }
            }
        }
    }

    // Save Search Dialog
    if (showSaveDialog) {
        SaveSearchDialog(
            onDismiss = { showSaveDialog = false },
            onSave = { name ->
                onSaveSearch(name)
                showSaveDialog = false
            }
        )
    }
}
