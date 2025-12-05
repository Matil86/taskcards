package de.hipp.app.taskcards.model

/**
 * Defines when a reminder notification should be shown for a task with a due date.
 */
enum class ReminderType {
    /** No reminder - task will not trigger a notification */
    NONE,

    /** Notify on the due date at the configured reminder time */
    ON_DUE_DATE,

    /** Notify 1 day before the due date at the configured reminder time */
    ONE_DAY_BEFORE,

    /** Notify 1 week before the due date at the configured reminder time */
    ONE_WEEK_BEFORE
}
