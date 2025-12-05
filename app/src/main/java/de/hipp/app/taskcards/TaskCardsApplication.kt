package de.hipp.app.taskcards

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp
import de.hipp.app.taskcards.widget.WidgetUpdateWorker
import de.hipp.app.taskcards.worker.NotificationChannels

/**
 * Application class for TaskCards.
 *
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection.
 * Initializes Firebase services including Crashlytics and Analytics.
 * Schedules periodic widget updates.
 */
@HiltAndroidApp
class TaskCardsApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Initialize Firebase Crashlytics
        FirebaseCrashlytics.getInstance().apply {
            // Enable/disable crash collection (useful for debug builds)
            setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
        }

        // Initialize notification channels for task reminders
        NotificationChannels.createNotificationChannels(this)

        // Schedule periodic widget updates
        WidgetUpdateWorker.scheduleWidgetUpdates(this)
    }
}
