# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep line numbers for debugging stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep data classes for reflection and serialization
-keep class de.hipp.android.taskcards.model.** { *; }
-keep class de.hipp.app.taskcards.model.** { *; }

# Keep ViewModels
-keep class * extends androidx.lifecycle.ViewModel { *; }

# Jetpack Compose
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.foundation.** { *; }
-keep class androidx.compose.material3.** { *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# Kotlin Serialization (if used in future)
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Keep Kotlin metadata for reflection
-keep class kotlin.Metadata { *; }

# Keep R8 from removing Flow operators
-keep class kotlinx.coroutines.flow.** { *; }

# StateFlow and SharedFlow
-keep class kotlinx.coroutines.flow.StateFlow { *; }
-keep class kotlinx.coroutines.flow.SharedFlow { *; }

# Navigation Compose
-keep class androidx.navigation.** { *; }
-keepnames class androidx.navigation.fragment.NavHostFragment

# Lifecycle
-keep class androidx.lifecycle.** { *; }

# DataStore
-keep class androidx.datastore.*.** { *; }
-keep class de.hipp.android.taskcards.data.PreferencesRepository { *; }
-keepclassmembers class * extends androidx.datastore.preferences.core.Preferences {
    *;
}

# Firebase Crashlytics
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception
-keep class com.google.firebase.crashlytics.** { *; }
-dontwarn com.google.firebase.crashlytics.**

# Firebase Performance Monitoring
-keep class com.google.firebase.perf.** { *; }
-keep class com.google.android.gms.common.** { *; }
-dontwarn com.google.firebase.perf.**
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable

# Firebase Analytics
-keep class com.google.firebase.analytics.** { *; }
-dontwarn com.google.firebase.analytics.**

# Firebase Firestore
-keep class com.google.firebase.firestore.** { *; }
-keep class com.google.firestore.v1.** { *; }
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <methods>;
}
-dontwarn com.google.firebase.firestore.**
-dontwarn io.grpc.**

# Firebase Auth
-keep class com.google.firebase.auth.** { *; }
-keepclassmembers class com.google.firebase.auth.** { *; }
-dontwarn com.google.firebase.auth.**

# Google Credential Manager (replaces deprecated Google Sign-In)
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }
-dontwarn androidx.credentials.**

# Keep auth service implementations
-keep class de.hipp.android.taskcards.auth.** { *; }
-keep class de.hipp.app.taskcards.auth.** { *; }

# Room Database
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**
-keep class de.hipp.android.taskcards.data.TaskEntity { *; }
-keep class de.hipp.android.taskcards.data.TaskDao { *; }
-keep class de.hipp.android.taskcards.data.AppDatabase { *; }

# WorkManager for reminders
-keep class androidx.work.** { *; }
-keep class de.hipp.app.taskcards.worker.** { *; }

# Due Date & Reminder Types (Enums)
-keep enum de.hipp.app.taskcards.model.ReminderType { *; }
-keep enum de.hipp.app.taskcards.model.DueDateStatus { *; }

# Search and Filter Model Classes
-keep class de.hipp.app.taskcards.model.SearchFilter { *; }
-keep enum de.hipp.app.taskcards.model.StatusFilter { *; }
-keep class de.hipp.app.taskcards.model.DueDateRange { *; }
-keep class de.hipp.app.taskcards.model.SavedSearch { *; }
-keep class de.hipp.app.taskcards.model.Category { *; }
-keep enum de.hipp.app.taskcards.model.CategoryColor { *; }

# Glance Widgets
-keep class androidx.glance.** { *; }
-keep class de.hipp.app.taskcards.widget.** { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidget { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }
-keepclassmembers class de.hipp.app.taskcards.widget.** {
    *;
}

# Deep Link Sharing
-keep class de.hipp.app.taskcards.model.ShareableTask { *; }
-keep class de.hipp.app.taskcards.model.ShareableList { *; }
-keep class de.hipp.app.taskcards.deeplink.** { *; }
-keepclassmembers class de.hipp.app.taskcards.model.ShareableTask {
    *** toDeepLink();
    *** fromDeepLink(...);
}
-keepclassmembers class de.hipp.app.taskcards.model.ShareableList {
    *** toDeepLink();
    *** fromDeepLink(...);
}

# ZXing QR Code Library
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# Kotlin Serialization (used for ShareableList)
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keep,includedescriptorclasses class de.hipp.app.taskcards.model.**$$serializer { *; }
-keepclassmembers class de.hipp.app.taskcards.model.** {
    *** Companion;
}
-keepclasseswithmembers class de.hipp.app.taskcards.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Remove logging in release
# Note: Keep error and warning logs for Firebase Crashlytics
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
