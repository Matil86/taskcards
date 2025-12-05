package de.hipp.app.taskcards.testutil

import de.hipp.app.taskcards.di.StringProvider

/**
 * Mock StringProvider for tests.
 * Returns simple test messages to verify error handling without requiring Android Context.
 */
class TestStringProvider : StringProvider {
    override fun getString(resId: Int): String = "Test error message"
    override fun getString(resId: Int, vararg formatArgs: Any): String =
        "Test error message: ${formatArgs.joinToString()}"
}
