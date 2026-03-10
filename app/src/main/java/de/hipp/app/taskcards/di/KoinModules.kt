package de.hipp.app.taskcards.di

/**
 * Koin dependency injection modules for TaskCards.
 *
 * Repository bindings in [appModule] delegate to [RepositoryProvider], which is the single
 * source of truth for repository lifecycle. [RepositoryProvider] handles auth-aware switching:
 * Room is used for offline/unauthenticated access, and Firestore is used once the user is
 * authenticated. By delegating here, Koin-injected workers and ViewModels always receive the
 * correct repository implementation regardless of authentication state.
 */

import com.google.firebase.firestore.FirebaseFirestore
import de.hipp.app.taskcards.analytics.Analytics
import de.hipp.app.taskcards.data.AppDatabase
import de.hipp.app.taskcards.data.FirestoreTaskListMetadataRepository
import de.hipp.app.taskcards.data.FirestoreTaskListRepository
import de.hipp.app.taskcards.data.InMemoryTaskListMetadataRepository
import de.hipp.app.taskcards.data.RoomTaskListRepository
import de.hipp.app.taskcards.data.TaskListMetadataRepository
import de.hipp.app.taskcards.data.TaskListRepository
import de.hipp.app.taskcards.data.preferences.PreferencesRepository
import de.hipp.app.taskcards.deeplink.DeepLinkHandler
import de.hipp.app.taskcards.qr.QRCodeGenerator
import de.hipp.app.taskcards.ui.viewmodel.CardsViewModel
import de.hipp.app.taskcards.ui.viewmodel.ListViewModel
import de.hipp.app.taskcards.ui.viewmodel.SettingsViewModel
import de.hipp.app.taskcards.ui.viewmodel.ShareViewModel
import de.hipp.app.taskcards.ui.viewmodel.StartupViewModel
import de.hipp.app.taskcards.worker.DailyReminderWorker
import de.hipp.app.taskcards.worker.ReminderScheduler
import de.hipp.app.taskcards.worker.ReminderWorker
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.dsl.module

val appModule = module {
    single<PreferencesRepository> { RepositoryProvider.getPreferencesRepository(androidContext()) }
    single<StringProvider> { RepositoryProvider.getStringProvider(androidContext()) }
    single<Analytics> { RepositoryProvider.getAnalytics() }
    single<CoroutineDispatcher> { Dispatchers.Main }
    single { ReminderScheduler(androidContext()) }
    factory<TaskListRepository> {
        if (RepositoryProvider.isAuthenticated()) {
            FirestoreTaskListRepository(
                firestore = FirebaseFirestore.getInstance(),
                reminderScheduler = get(),
                preferencesRepo = get()
            )
        } else {
            val db = AppDatabase.getInstance(androidContext())
            RoomTaskListRepository(
                dao = db.taskDao(),
                reminderScheduler = get(),
                preferencesRepo = get()
            )
        }
    }
    factory<TaskListMetadataRepository> {
        if (RepositoryProvider.isAuthenticated()) {
            FirestoreTaskListMetadataRepository(firestore = FirebaseFirestore.getInstance())
        } else {
            InMemoryTaskListMetadataRepository()
        }
    }
    single { DeepLinkHandler(get(), get()) }
    single { QRCodeGenerator() }
}

val viewModelModule = module {
    viewModel { parameters ->
        ListViewModel(
            savedStateHandle = parameters.get(),
            repo = get(),
            prefsRepo = get(),
            strings = get(),
            analytics = get(),
            dispatcher = get()
        )
    }
    viewModel { parameters ->
        CardsViewModel(
            savedStateHandle = parameters.get(),
            repo = get(),
            strings = get(),
            analytics = get(),
            dispatcher = get()
        )
    }
    viewModel { ShareViewModel(get()) }
    viewModel {
        SettingsViewModel(
            context = androidContext(),
            preferencesRepo = get(),
            strings = get(),
            dispatcher = get()
        )
    }
    viewModel { StartupViewModel(get(), get()) }
}

val workerModule = module {
    workerOf(::ReminderWorker)
    workerOf(::DailyReminderWorker)
}
