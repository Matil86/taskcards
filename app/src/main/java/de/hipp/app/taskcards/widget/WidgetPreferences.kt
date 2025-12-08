package de.hipp.app.taskcards.widget

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Data class representing widget configuration.
 */
data class WidgetPreferences(
    val widgetId: Int,
    val listId: String,
    val widgetType: WidgetType
)

/**
 * Types of widgets available in the app.
 */
enum class WidgetType {
    /** Shows all active tasks from a list */
    TASK_LIST,

    /** Single button to quickly add tasks */
    QUICK_ADD,

    /** Shows only tasks due today */
    DUE_TODAY
}

/**
 * Repository for managing widget preferences using SharedPreferences.
 * Stores widget configuration (list ID and widget type) per widget instance.
 */
class WidgetPreferencesRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)

    /**
     * Save widget configuration for a specific widget ID.
     */
    fun saveWidgetConfig(widgetId: Int, listId: String, type: WidgetType) {
        prefs.edit {
            putString("${widgetId}_listId", listId)
            putString("${widgetId}_type", type.name)
        }
    }

    /**
     * Get widget configuration for a specific widget ID.
     * Returns null if no configuration exists.
     */
    fun getWidgetConfig(widgetId: Int): WidgetPreferences? {
        val listId = prefs.getString("${widgetId}_listId", null) ?: return null
        val typeName = prefs.getString("${widgetId}_type", null) ?: return null
        val type = try {
            WidgetType.valueOf(typeName)
        } catch (e: IllegalArgumentException) {
            null
        } ?: return null

        return WidgetPreferences(widgetId, listId, type)
    }

    /**
     * Delete widget configuration when widget is removed.
     */
    fun deleteWidgetConfig(widgetId: Int) {
        prefs.edit {
            remove("${widgetId}_listId")
            remove("${widgetId}_type")
        }
    }

    /**
     * Get the default list ID for Quick Add widget.
     * Returns null if no default is set.
     */
    fun getDefaultListId(): String? {
        return prefs.getString("default_list_id", null)
    }

    /**
     * Set the default list ID for Quick Add widget.
     */
    fun setDefaultListId(listId: String) {
        prefs.edit {
            putString("default_list_id", listId)
        }
    }
}
