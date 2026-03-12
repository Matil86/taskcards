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

import de.hipp.app.taskcards.analytics.Analytics
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
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    single<PreferencesRepository> { RepositoryProvider.getPreferencesRepository(androidContext()) }
    single<StringProvider> { RepositoryProvider.getStringProvider(androidContext()) }
    single<Analytics> { RepositoryProvider.getAnalytics() }
    single<CoroutineDispatcher> { Dispatchers.Main }
    single { ReminderScheduler(androidContext()) }
    // factory<> (not single<>) is intentional: RepositoryProvider already caches repos
    // internally with synchronized locking. factory ensures each injection gets the
    // current auth-aware implementation — critical for real-time Firestore sync.
    // single<> would cache RoomRepository at startup and never switch to Firestore after login.
    factory<TaskListRepository> { RepositoryProvider.getRepository(androidContext()) }
    factory<TaskListMetadataRepository> { RepositoryProvider.getMetadataRepository(androidContext()) }
    single { DeepLinkHandler(get(), get()) }
    single { QRCodeGenerator() }
}

val viewModelModule = module {
    // viewModel{} (from org.koin.core.module.dsl) required for these two: both have a secondary
    // constructor (listId: String) that causes ambiguity with viewModelOf(::ClassName)
    viewModel {
        ListViewModel(
            savedStateHandle = get(),
            repo = get(),
            prefsRepo = get(),
            strings = get(),
            analytics = get(),
            dispatcher = get()
        )
    }
    viewModel {
        CardsViewModel(
            savedStateHandle = get(),
            repo = get(),
            strings = get(),
            analytics = get(),
            dispatcher = get()
        )
    }
    viewModelOf(::ShareViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::StartupViewModel)
}

val workerModule = module {
    workerOf(::ReminderWorker)
    workerOf(::DailyReminderWorker)
}
