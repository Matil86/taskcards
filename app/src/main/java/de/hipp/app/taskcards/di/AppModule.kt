package de.hipp.app.taskcards.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import de.hipp.app.taskcards.analytics.Analytics
import de.hipp.app.taskcards.analytics.FirebaseAnalyticsImpl
import de.hipp.app.taskcards.data.AppDatabase
import de.hipp.app.taskcards.data.InMemoryTaskListMetadataRepository
import de.hipp.app.taskcards.data.preferences.PreferencesRepository
import de.hipp.app.taskcards.data.preferences.PreferencesRepositoryImpl
import de.hipp.app.taskcards.data.RoomTaskListRepository
import de.hipp.app.taskcards.data.TaskListMetadataRepository
import de.hipp.app.taskcards.data.TaskListRepository
import de.hipp.app.taskcards.worker.ReminderScheduler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

/**
 * Hilt module for providing application-level dependencies.
 *
 * This module provides:
 * - TaskListRepository (singleton) - Room-backed persistent storage
 * - PreferencesRepository (singleton)
 * - StringProvider (singleton)
 * - Analytics (singleton) - Firebase Analytics
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Provides the singleton instance of TaskListRepository.
     * Using RoomTaskListRepository for persistent storage.
     */
    @Provides
    @Singleton
    fun provideTaskListRepository(
        @ApplicationContext context: Context,
        reminderScheduler: ReminderScheduler,
        preferencesRepository: PreferencesRepository
    ): TaskListRepository {
        val database = AppDatabase.getInstance(context)
        return RoomTaskListRepository(database.taskDao(), reminderScheduler, preferencesRepository)
    }

    /**
     * Provides the singleton instance of PreferencesRepository.
     * Uses DataStore for persistent storage of user preferences.
     */
    @Provides
    @Singleton
    fun providePreferencesRepository(
        @ApplicationContext context: Context
    ): PreferencesRepository {
        return PreferencesRepositoryImpl(context)
    }

    /**
     * Provides the singleton instance of StringProvider.
     * Allows ViewModels to access string resources without direct Context dependency.
     */
    @Provides
    @Singleton
    fun provideStringProvider(
        @ApplicationContext context: Context
    ): StringProvider {
        return AndroidStringProvider(context)
    }

    /**
     * Provides the singleton instance of Analytics.
     * Using Firebase Analytics for event tracking.
     */
    @Provides
    @Singleton
    fun provideAnalytics(): Analytics {
        return FirebaseAnalyticsImpl()
    }

    /**
     * Provides the CoroutineDispatcher for ViewModels.
     * Uses Dispatchers.Main for UI-related coroutines.
     */
    @Provides
    @Singleton
    fun provideCoroutineDispatcher(): CoroutineDispatcher {
        return Dispatchers.Main
    }

    /**
     * Provides the singleton instance of TaskListMetadataRepository.
     * Using InMemoryTaskListMetadataRepository for now.
     */
    @Provides
    @Singleton
    fun provideTaskListMetadataRepository(): TaskListMetadataRepository {
        return InMemoryTaskListMetadataRepository()
    }

    /**
     * Provides the singleton instance of ReminderScheduler.
     * Used for scheduling task reminder notifications with WorkManager.
     */
    @Provides
    @Singleton
    fun provideReminderScheduler(
        @ApplicationContext context: Context
    ): ReminderScheduler {
        return ReminderScheduler(context)
    }
}
