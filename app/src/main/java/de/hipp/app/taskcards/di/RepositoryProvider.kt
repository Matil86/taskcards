package de.hipp.app.taskcards.di

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import de.hipp.app.taskcards.analytics.Analytics
import de.hipp.app.taskcards.analytics.FirebaseAnalyticsImpl
import de.hipp.app.taskcards.auth.AuthService
import de.hipp.app.taskcards.auth.FirebaseAuthService
import de.hipp.app.taskcards.data.AppDatabase
import de.hipp.app.taskcards.data.FirestoreTaskListMetadataRepository
import de.hipp.app.taskcards.data.FirestoreTaskListRepository
import de.hipp.app.taskcards.data.InMemoryTaskListMetadataRepository
import de.hipp.app.taskcards.data.InMemoryTaskListRepository
import de.hipp.app.taskcards.data.RoomTaskListRepository
import de.hipp.app.taskcards.data.preferences.PreferencesRepository
import de.hipp.app.taskcards.data.preferences.PreferencesRepositoryImpl
import de.hipp.app.taskcards.data.TaskListMetadataRepository
import de.hipp.app.taskcards.data.TaskListRepository

object RepositoryProvider {
    private const val TAG = "RepositoryProvider"

    private var taskListRepository: TaskListRepository? = null
    private var taskListMetadataRepository: TaskListMetadataRepository? = null
    private var preferencesRepository: PreferencesRepository? = null
    private var stringProvider: StringProvider? = null
    private var analytics: Analytics? = null
    private var authService: AuthService? = null
    private var isAuthenticated: Boolean = false

    /**
     * Get the AuthService instance.
     * Uses Firebase Authentication for user management.
     */
    fun getAuthService(context: Context): AuthService {
        return authService ?: synchronized(this) {
            authService ?: FirebaseAuthService(context.applicationContext).also {
                authService = it
            }
        }
    }

    /**
     * Set authentication state.
     * Switches between local (Room) and cloud (Firestore) repositories.
     */
    fun setAuthenticated(authenticated: Boolean) {
        if (isAuthenticated != authenticated) {
            isAuthenticated = authenticated
            // Clear repositories to force recreation with new authentication state
            taskListRepository = null
            taskListMetadataRepository = null
            Log.d(TAG, "Authentication state changed to: $authenticated. Repositories will be recreated.")
        }
    }

    /**
     * Check if user is authenticated.
     */
    fun isAuthenticated(): Boolean = isAuthenticated

    /**
     * Get the TaskListRepository instance.
     * When authenticated: Uses Firestore for collaborative cloud storage.
     * When not authenticated: Uses Room for persistent local storage.
     * For tests, call setRepository() to inject a custom implementation.
     */
    fun getRepository(context: Context): TaskListRepository {
        return taskListRepository ?: synchronized(this) {
            taskListRepository ?: run {
                if (isAuthenticated) {
                    Log.d(TAG, "Using Firestore repository for collaborative lists")
                    FirestoreTaskListRepository(
                        firestore = FirebaseFirestore.getInstance()
                    )
                } else {
                    Log.d(TAG, "Using Room repository for persistent local storage (not authenticated)")
                    val database = AppDatabase.getInstance(context.applicationContext)
                    RoomTaskListRepository(
                        dao = database.taskDao()
                    )
                }
            }.also {
                taskListRepository = it
            }
        }
    }

    /**
     * Set a custom repository implementation (mainly for testing).
     */
    fun setRepository(repository: TaskListRepository) {
        taskListRepository = repository
    }

    /**
     * Reset repositories (useful for testing).
     */
    fun reset() = synchronized(this) {
        taskListRepository = null
        taskListMetadataRepository = null
    }

    /**
     * Get the TaskListMetadataRepository instance.
     * When authenticated: Uses Firestore for cloud-synchronized list metadata.
     * When not authenticated: Uses in-memory storage.
     * For tests, call setMetadataRepository() to inject a custom implementation.
     */
    fun getMetadataRepository(context: Context): TaskListMetadataRepository {
        return taskListMetadataRepository ?: synchronized(this) {
            taskListMetadataRepository ?: run {
                if (isAuthenticated) {
                    Log.d(TAG, "Using Firestore metadata repository for collaborative lists")
                    FirestoreTaskListMetadataRepository(
                        firestore = FirebaseFirestore.getInstance()
                    )
                } else {
                    Log.d(TAG, "Using in-memory metadata repository for local storage (not authenticated)")
                    InMemoryTaskListMetadataRepository()
                }
            }.also {
                taskListMetadataRepository = it
            }
        }
    }

    /**
     * Set a custom metadata repository implementation (mainly for testing).
     */
    fun setMetadataRepository(repository: TaskListMetadataRepository) {
        taskListMetadataRepository = repository
    }

    fun getPreferencesRepository(context: Context): PreferencesRepository {
        return preferencesRepository ?: PreferencesRepositoryImpl(context.applicationContext).also {
            preferencesRepository = it
        }
    }

    fun getStringProvider(context: Context): StringProvider {
        return stringProvider ?: AndroidStringProvider(context.applicationContext).also {
            stringProvider = it
        }
    }

    /**
     * Get the Analytics instance.
     * Uses Firebase Analytics by default.
     * For tests, call setAnalytics() to inject a no-op implementation.
     */
    fun getAnalytics(): Analytics {
        return analytics ?: synchronized(this) {
            analytics ?: FirebaseAnalyticsImpl().also {
                analytics = it
            }
        }
    }

    /**
     * Set a custom analytics implementation (mainly for testing).
     */
    fun setAnalytics(analyticsImpl: Analytics) {
        analytics = analyticsImpl
    }
}
