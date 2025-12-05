package de.hipp.app.taskcards.ui.screens.filter

import de.hipp.app.taskcards.model.SearchFilter
import de.hipp.app.taskcards.model.StatusFilter

/**
 * Utility for building human-readable filter summaries.
 *
 * Converts a SearchFilter into a concise text representation showing:
 * - Text query (in quotes)
 * - Due date range
 * - Status filter (if not default ACTIVE_ONLY)
 *
 * Parts are joined with bullet separators.
 *
 * Example outputs:
 * - "urgent" • Overdue • All tasks
 * - Today
 * - Done tasks
 */
fun buildFilterSummary(filter: SearchFilter): String {
    val parts = mutableListOf<String>()

    if (filter.textQuery.isNotBlank()) {
        parts.add("\"${filter.textQuery}\"")
    }

    if (filter.dueDateRange != null) {
        parts.add(filter.dueDateRange.displayName)
    }

    if (filter.statusFilter != StatusFilter.ACTIVE_ONLY) {
        parts.add(filter.statusFilter.displayName)
    }

    return parts.joinToString(" • ")
}
