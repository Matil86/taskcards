package de.hipp.app.taskcards.ui.viewmodel.list

import de.hipp.app.taskcards.model.DueDateRange
import de.hipp.app.taskcards.model.SavedSearch
import de.hipp.app.taskcards.model.SearchFilter
import de.hipp.app.taskcards.model.StatusFilter
import de.hipp.app.taskcards.model.TaskItem

/**
 * Extension functions for ListViewModel to handle search and filter operations.
 *
 * This file extracts filter logic from ListViewModel to improve maintainability
 * and follow Single Responsibility Principle.
 */

/**
 * Applies all filter criteria to the task list.
 * Filters are combined with AND logic.
 *
 * @param tasks The complete list of tasks to filter
 * @param filter The SearchFilter containing all filter criteria
 * @return Filtered list of tasks
 */
fun applyFilters(tasks: List<TaskItem>, filter: SearchFilter): List<TaskItem> {
    var filtered = tasks

    // Text search (case-insensitive)
    if (filter.textQuery.isNotBlank()) {
        filtered = filtered.filter { task ->
            task.text.contains(filter.textQuery, ignoreCase = true)
        }
    }

    // Due date range filter
    filter.dueDateRange?.let { range ->
        filtered = filtered.filter { task ->
            task.dueDate?.let { dueDate ->
                (range.start == null || dueDate >= range.start) &&
                        (range.end == null || dueDate <= range.end)
            } ?: false
        }
    }

    // Status filter
    filtered = when (filter.statusFilter) {
        StatusFilter.ALL -> filtered
        StatusFilter.ACTIVE_ONLY -> filtered.filter { !it.done && !it.removed }
        StatusFilter.DONE_ONLY -> filtered.filter { it.done }
        StatusFilter.REMOVED_ONLY -> filtered.filter { it.removed }
    }

    return filtered
}
