package de.hipp.app.taskcards.ui.viewmodel.list

import de.hipp.app.taskcards.model.TaskItem

/**
 * Utility functions for generating text representations of task lists.
 *
 * This file extracts text generation logic from ListViewModel to improve
 * maintainability and follow Single Responsibility Principle.
 */

/**
 * Generates a text representation of the task list for clipboard copy or sharing.
 * Includes all active (non-removed) tasks with checkboxes for done/active status.
 *
 * @param tasks The list of tasks to format
 * @return Formatted text string with checkboxes for done/active tasks
 */
fun generateListText(tasks: List<TaskItem>): String {
    val activeTasks = tasks.filter { !it.removed }
    return buildString {
        appendLine("My Task List")
        appendLine("=".repeat(40))
        activeTasks.forEach { task ->
            val status = if (task.done) "[✓]" else "[ ]"
            appendLine("$status ${task.text}")
        }
        if (activeTasks.isEmpty()) {
            appendLine("(No tasks)")
        }
    }
}
