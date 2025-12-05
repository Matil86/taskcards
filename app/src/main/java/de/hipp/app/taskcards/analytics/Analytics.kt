package de.hipp.app.taskcards.analytics

/**
 * Analytics interface for tracking events.
 * Abstracts analytics implementation for testability.
 */
interface Analytics {
    /**
     * Log an event with optional parameters.
     */
    fun logEvent(eventName: String, params: Map<String, Any> = emptyMap())

    /**
     * Set a user property.
     */
    fun setUserProperty(name: String, value: String)

    /**
     * Set the current screen name.
     */
    fun setCurrentScreen(screenName: String)
}

/**
 * Analytics event names.
 */
object AnalyticsEvents {
    const val TASK_CREATED = "task_created"
    const val TASK_COMPLETED = "task_completed"
    const val TASK_DELETED = "task_deleted"
    const val TASK_UPDATED = "task_updated"
    const val TASK_REORDERED = "task_reordered"
    const val TASK_RESTORED = "task_restored"
    const val SCREEN_VIEW = "screen_view"
    const val HIGH_CONTRAST_ENABLED = "high_contrast_enabled"
    const val SWIPE_GESTURE_USED = "swipe_gesture_used"
    const val SEARCH_USED = "search_used"
    const val FILTER_APPLIED = "filter_applied"
    const val FILTER_CLEARED = "filter_cleared"
    const val SAVED_SEARCH_CREATED = "saved_search_created"
    const val SAVED_SEARCH_APPLIED = "saved_search_applied"
    const val SAVED_SEARCH_DELETED = "saved_search_deleted"
}

/**
 * Analytics parameter names.
 */
object AnalyticsParams {
    const val SCREEN_NAME = "screen_name"
    const val LIST_ID = "list_id"
    const val TASK_COUNT = "task_count"
    const val FROM_INDEX = "from_index"
    const val TO_INDEX = "to_index"
    const val GESTURE_TYPE = "gesture_type"
}

/**
 * Screen names for analytics.
 */
object ScreenNames {
    const val CARDS = "cards"
    const val LIST = "list"
    const val SETTINGS = "settings"
}
