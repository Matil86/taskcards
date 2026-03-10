package de.hipp.app.taskcards.analytics

import android.os.Bundle
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics

/**
 * Firebase implementation of Analytics interface.
 */
class FirebaseAnalyticsImpl : Analytics {
    private val firebaseAnalytics: FirebaseAnalytics = Firebase.analytics

    override fun logEvent(eventName: String, params: Map<String, Any>) {
        val bundle = Bundle().apply {
            params.forEach { (key, value) ->
                when (value) {
                    is String -> putString(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Double -> putDouble(key, value)
                    is Boolean -> putBoolean(key, value)
                    else -> putString(key, value.toString())
                }
            }
        }
        firebaseAnalytics.logEvent(eventName, bundle)
    }

    override fun setUserProperty(name: String, value: String) {
        firebaseAnalytics.setUserProperty(name, value)
    }

    override fun setCurrentScreen(screenName: String) {
        logEvent(AnalyticsEvents.SCREEN_VIEW, mapOf(AnalyticsParams.SCREEN_NAME to screenName))
    }
}

/**
 * No-op implementation for testing.
 */
class NoOpAnalytics : Analytics {
    override fun logEvent(eventName: String, params: Map<String, Any>) {
        // No-op
    }

    override fun setUserProperty(name: String, value: String) {
        // No-op
    }

    override fun setCurrentScreen(screenName: String) {
        // No-op
    }
}
