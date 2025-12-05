package de.hipp.app.taskcards.util

import android.util.Log
import de.hipp.app.taskcards.BuildConfig

/**
 * BuildConfig-aware logger that only logs in DEBUG builds.
 * Prevents log spam in production and improves performance.
 *
 * Usage:
 * ```
 * Logger.d(TAG) { "Debug message" }
 * Logger.e(TAG) { "Error message" }
 * Logger.w(TAG) { "Warning message" }
 * ```
 *
 * The lambda-based API ensures that expensive string operations
 * (like concatenation or formatting) are only executed in DEBUG builds.
 */
object Logger {
    /**
     * Log debug message. Only logs in DEBUG builds.
     * @param tag The log tag
     * @param message Lambda that produces the log message
     */
    inline fun d(tag: String, message: () -> String) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, message())
        }
    }

    /**
     * Log error message. Only logs in DEBUG builds.
     * @param tag The log tag
     * @param message Lambda that produces the log message
     * @param throwable Optional throwable to log
     */
    inline fun e(tag: String, message: () -> String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            Log.e(tag, message(), throwable)
        }
    }

    /**
     * Log warning message. Only logs in DEBUG builds.
     * @param tag The log tag
     * @param message Lambda that produces the log message
     */
    inline fun w(tag: String, message: () -> String) {
        if (BuildConfig.DEBUG) {
            Log.w(tag, message())
        }
    }
}
