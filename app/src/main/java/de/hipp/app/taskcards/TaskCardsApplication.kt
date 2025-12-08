package de.hipp.app.taskcards

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import dagger.hilt.android.HiltAndroidApp
import de.hipp.app.taskcards.widget.WidgetUpdateWorker
import de.hipp.app.taskcards.worker.NotificationChannels

/**
 * Application class for TaskCards.
 *
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection.
 * Initializes Firebase services (Crashlytics, Performance, Analytics).
 * Schedules periodic widget updates and creates notification channels.
 *
 * MONITORING STACK:
 * - Firebase Crashlytics: Crash reporting integrated with Firebase ecosystem
 * - Firebase Performance: App performance metrics and traces
 *
 * Monitoring is configured to only run in RELEASE builds
 * to avoid polluting production data with development testing.
 */
@HiltAndroidApp
class TaskCardsApplication : Application() {

    private var appStartTrace: Trace? = null

    override fun onCreate() {
        super.onCreate()

        // Start app initialization trace
        appStartTrace = FirebasePerformance.getInstance().newTrace("app_initialization")
        appStartTrace?.start()

        try {
            // Initialize Firebase (must be first)
            initializeFirebase()

            // Initialize app features
            initializeNotificationChannels()
            scheduleBackgroundWork()

            // Mark successful initialization
            appStartTrace?.putAttribute("status", "success")
            Log.d(TAG, "Application initialized successfully")
        } catch (e: Exception) {
            // Report initialization failure
            appStartTrace?.putAttribute("status", "failure")
            appStartTrace?.putAttribute("error", e.message ?: "Unknown error")
            Log.e(TAG, "Application initialization failed", e)
            FirebaseCrashlytics.getInstance().recordException(e)
            throw e
        } finally {
            // Stop app initialization trace
            appStartTrace?.stop()
        }
    }

    /**
     * Initialize Firebase services.
     * Configures Crashlytics and Performance Monitoring for production use.
     */
    private fun initializeFirebase() {
        // Initialize Firebase SDK
        FirebaseApp.initializeApp(this)

        // Configure Firebase Crashlytics
        FirebaseCrashlytics.getInstance().apply {
            // Only enable in release builds to avoid polluting production data
            setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)

            if (!BuildConfig.DEBUG) {
                // Set custom keys for debugging
                setCustomKey("app_version", BuildConfig.VERSION_NAME)
                setCustomKey("build_type", BuildConfig.BUILD_TYPE)

                Log.d(TAG, "Firebase Crashlytics enabled for release build")
            }
        }

        // Configure Firebase Performance Monitoring
        FirebasePerformance.getInstance().apply {
            // Performance monitoring is automatically disabled in debug builds
            // and enabled in release builds by the Firebase SDK

            // Set data collection explicitly based on build type
            isPerformanceCollectionEnabled = !BuildConfig.DEBUG

            if (!BuildConfig.DEBUG) {
                Log.d(TAG, "Firebase Performance Monitoring enabled for release build")
            }
        }
    }

    /**
     * Initialize notification channels for task reminders.
     * Required for Android 8.0+ (API 26+).
     */
    private fun initializeNotificationChannels() {
        try {
            NotificationChannels.createNotificationChannels(this)
            Log.d(TAG, "Notification channels created")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create notification channels", e)
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }

    /**
     * Schedule periodic background work.
     * Schedules widget updates to keep home screen widgets in sync.
     */
    private fun scheduleBackgroundWork() {
        try {
            WidgetUpdateWorker.scheduleWidgetUpdates(this)
            Log.d(TAG, "Background work scheduled")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule background work", e)
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }

    companion object {
        private const val TAG = "TaskCardsApplication"
    }
}

