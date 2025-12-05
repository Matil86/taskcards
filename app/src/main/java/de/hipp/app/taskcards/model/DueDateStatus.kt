package de.hipp.app.taskcards.model

import java.util.Calendar

/**
 * Determines a task's due date status relative to the current time.
 * Used for visual indicators and sorting.
 */
enum class DueDateStatus {
    /** Past the due date */
    OVERDUE,

    /** Due today */
    TODAY,

    /** Due within the next 7 days */
    THIS_WEEK,

    /** Due more than 7 days from now */
    LATER,

    /** No due date set */
    NO_DUE_DATE
}

/**
 * Calculates the status of a due date relative to the current time.
 * @param dueDate timestamp in milliseconds, or null if no due date
 * @return the appropriate [DueDateStatus]
 */
fun calculateDueDateStatus(dueDate: Long?): DueDateStatus {
    if (dueDate == null) return DueDateStatus.NO_DUE_DATE

    val calendar = Calendar.getInstance()
    val now = calendar.timeInMillis

    // Get start of today (00:00:00)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    val startOfToday = calendar.timeInMillis

    // Get end of today (23:59:59)
    calendar.set(Calendar.HOUR_OF_DAY, 23)
    calendar.set(Calendar.MINUTE, 59)
    calendar.set(Calendar.SECOND, 59)
    calendar.set(Calendar.MILLISECOND, 999)
    val endOfToday = calendar.timeInMillis

    // Get end of this week (7 days from now)
    calendar.timeInMillis = startOfToday
    calendar.add(Calendar.DAY_OF_YEAR, 7)
    calendar.set(Calendar.HOUR_OF_DAY, 23)
    calendar.set(Calendar.MINUTE, 59)
    calendar.set(Calendar.SECOND, 59)
    calendar.set(Calendar.MILLISECOND, 999)
    val endOfWeek = calendar.timeInMillis

    return when {
        dueDate < startOfToday -> DueDateStatus.OVERDUE
        dueDate <= endOfToday -> DueDateStatus.TODAY
        dueDate <= endOfWeek -> DueDateStatus.THIS_WEEK
        else -> DueDateStatus.LATER
    }
}

/**
 * Gets the sort priority for a due date status.
 * Lower values mean higher priority (appear first in sorted lists).
 */
fun DueDateStatus.sortPriority(): Int = when (this) {
    DueDateStatus.OVERDUE -> 0
    DueDateStatus.TODAY -> 1
    DueDateStatus.THIS_WEEK -> 2
    DueDateStatus.LATER -> 3
    DueDateStatus.NO_DUE_DATE -> 4
}
