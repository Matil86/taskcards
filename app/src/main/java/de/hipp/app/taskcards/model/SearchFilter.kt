package de.hipp.app.taskcards.model

import java.util.Calendar
import java.util.UUID

// Calendar constants for end of day/period calculations
private const val END_OF_DAY_HOUR = 23
private const val END_OF_DAY_MINUTE = 59
private const val END_OF_DAY_SECOND = 59
private const val END_OF_DAY_MILLISECOND = 999
private const val DAYS_IN_WEEK = 7
private const val DAYS_IN_MONTH = 30

/**
 * Represents a search and filter configuration for tasks.
 * All filter criteria are combined with AND logic.
 *
 * @param textQuery Case-insensitive text search query
 * @param dueDateRange Optional date range for due date filtering
 * @param statusFilter Filter by task status (active, done, removed, or all)
 */
data class SearchFilter(
    val textQuery: String = "",
    val dueDateRange: DueDateRange? = null,
    val statusFilter: StatusFilter = StatusFilter.ACTIVE_ONLY
) {
    /**
     * Returns true if this filter is empty (no criteria set).
     */
    fun isEmpty(): Boolean {
        return textQuery.isBlank() &&
                dueDateRange == null &&
                statusFilter == StatusFilter.ACTIVE_ONLY
    }

    /**
     * Returns the count of active filter criteria (excluding status filter if default).
     */
    fun getActiveFilterCount(): Int {
        var count = 0
        if (textQuery.isNotBlank()) count++
        if (dueDateRange != null) count++
        if (statusFilter != StatusFilter.ACTIVE_ONLY) count++
        return count
    }
}

/**
 * Filter tasks by completion and removal status.
 */
enum class StatusFilter(val displayName: String) {
    ALL("All Tasks"),
    ACTIVE_ONLY("Active Only"),
    DONE_ONLY("Completed Only"),
    REMOVED_ONLY("Removed Only")
}

/**
 * Represents a date range for filtering tasks by due date.
 * Both start and end are inclusive. Null values mean no limit in that direction.
 *
 * @param start Start timestamp in milliseconds (inclusive), null for no start limit
 * @param end End timestamp in milliseconds (inclusive), null for no end limit
 * @param displayName Human-readable name for this range
 */
data class DueDateRange(
    val start: Long?,
    val end: Long?,
    val displayName: String
) {
    companion object {
        /**
         * Returns a range covering today (midnight to end of day).
         */
        fun today(): DueDateRange {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfDay = calendar.timeInMillis

            calendar.set(Calendar.HOUR_OF_DAY, END_OF_DAY_HOUR)
            calendar.set(Calendar.MINUTE, END_OF_DAY_MINUTE)
            calendar.set(Calendar.SECOND, END_OF_DAY_SECOND)
            calendar.set(Calendar.MILLISECOND, END_OF_DAY_MILLISECOND)
            val endOfDay = calendar.timeInMillis

            return DueDateRange(
                start = startOfDay,
                end = endOfDay,
                displayName = "Today"
            )
        }

        /**
         * Returns a range covering the next 7 days from now.
         */
        fun thisWeek(): DueDateRange {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfToday = calendar.timeInMillis

            calendar.add(Calendar.DAY_OF_YEAR, DAYS_IN_WEEK)
            calendar.set(Calendar.HOUR_OF_DAY, END_OF_DAY_HOUR)
            calendar.set(Calendar.MINUTE, END_OF_DAY_MINUTE)
            calendar.set(Calendar.SECOND, END_OF_DAY_SECOND)
            calendar.set(Calendar.MILLISECOND, END_OF_DAY_MILLISECOND)
            val endOfWeek = calendar.timeInMillis

            return DueDateRange(
                start = startOfToday,
                end = endOfWeek,
                displayName = "This Week"
            )
        }

        /**
         * Returns a range covering all dates before today (overdue tasks).
         */
        fun overdue(): DueDateRange {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            calendar.set(Calendar.HOUR_OF_DAY, END_OF_DAY_HOUR)
            calendar.set(Calendar.MINUTE, END_OF_DAY_MINUTE)
            calendar.set(Calendar.SECOND, END_OF_DAY_SECOND)
            calendar.set(Calendar.MILLISECOND, END_OF_DAY_MILLISECOND)
            val endOfYesterday = calendar.timeInMillis

            return DueDateRange(
                start = null,
                end = endOfYesterday,
                displayName = "Overdue"
            )
        }

        /**
         * Returns a range covering the next 30 days from now.
         */
        fun thisMonth(): DueDateRange {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfToday = calendar.timeInMillis

            calendar.add(Calendar.DAY_OF_YEAR, DAYS_IN_MONTH)
            calendar.set(Calendar.HOUR_OF_DAY, END_OF_DAY_HOUR)
            calendar.set(Calendar.MINUTE, END_OF_DAY_MINUTE)
            calendar.set(Calendar.SECOND, END_OF_DAY_SECOND)
            calendar.set(Calendar.MILLISECOND, END_OF_DAY_MILLISECOND)
            val endOfMonth = calendar.timeInMillis

            return DueDateRange(
                start = startOfToday,
                end = endOfMonth,
                displayName = "Next 30 Days"
            )
        }
    }
}

/**
 * Represents a saved search/filter combination for quick access.
 *
 * @param id Unique identifier for this saved search
 * @param name User-provided name for this search
 * @param filter The search filter configuration
 * @param listId The list this saved search belongs to
 * @param createdAt Timestamp when this search was created
 */
data class SavedSearch(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val filter: SearchFilter,
    val listId: String,
    val createdAt: Long = System.currentTimeMillis()
)
