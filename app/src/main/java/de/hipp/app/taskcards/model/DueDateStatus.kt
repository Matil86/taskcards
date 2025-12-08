package de.hipp.app.taskcards.model

import java.util.Calendar

// Calendar constants for due date calculations
private const val END_OF_DAY_HOUR = 23
private const val END_OF_DAY_MINUTE = 59
private const val END_OF_DAY_SECOND = 59
private const val END_OF_DAY_MILLISECOND = 999
private const val DAYS_IN_WEEK = 7

// Sort priority constants
private const val PRIORITY_OVERDUE = 0
private const val PRIORITY_TODAY = 1
private const val PRIORITY_THIS_WEEK = 2
private const val PRIORITY_LATER = 3
private const val PRIORITY_NO_DUE_DATE = 4

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

    // Get start of today (00:00:00)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    val startOfToday = calendar.timeInMillis

    // Get end of today (23:59:59)
    calendar.set(Calendar.HOUR_OF_DAY, END_OF_DAY_HOUR)
    calendar.set(Calendar.MINUTE, END_OF_DAY_MINUTE)
    calendar.set(Calendar.SECOND, END_OF_DAY_SECOND)
    calendar.set(Calendar.MILLISECOND, END_OF_DAY_MILLISECOND)
    val endOfToday = calendar.timeInMillis

    // Get end of this week (7 days from now)
    calendar.timeInMillis = startOfToday
    calendar.add(Calendar.DAY_OF_YEAR, DAYS_IN_WEEK)
    calendar.set(Calendar.HOUR_OF_DAY, END_OF_DAY_HOUR)
    calendar.set(Calendar.MINUTE, END_OF_DAY_MINUTE)
    calendar.set(Calendar.SECOND, END_OF_DAY_SECOND)
    calendar.set(Calendar.MILLISECOND, END_OF_DAY_MILLISECOND)
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
    DueDateStatus.OVERDUE -> PRIORITY_OVERDUE
    DueDateStatus.TODAY -> PRIORITY_TODAY
    DueDateStatus.THIS_WEEK -> PRIORITY_THIS_WEEK
    DueDateStatus.LATER -> PRIORITY_LATER
    DueDateStatus.NO_DUE_DATE -> PRIORITY_NO_DUE_DATE
}
