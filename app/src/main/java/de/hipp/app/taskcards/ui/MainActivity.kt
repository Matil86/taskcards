package de.hipp.app.taskcards.ui

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.components.SingletonComponent
import de.hipp.app.taskcards.data.preferences.PreferencesRepositoryImpl
import de.hipp.app.taskcards.deeplink.DeepLinkHandler
import de.hipp.app.taskcards.di.RepositoryProvider
import de.hipp.app.taskcards.ui.app.TaskCardsApp
import de.hipp.app.taskcards.util.LocaleHelper

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        // Apply locale from preferences before attaching base context
        val prefsRepo = RepositoryProvider.getPreferencesRepository(newBase)
        val languageCode = (prefsRepo as? PreferencesRepositoryImpl)?.getLanguageSync() ?: "system"
        val localeContext = LocaleHelper.setLocale(newBase, languageCode)
        super.attachBaseContext(localeContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle deep link from initial intent
        val deepLinkUri = intent?.data

        setContent {
            TaskCardsApp(initialDeepLinkUri = deepLinkUri)
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        // Handle deep link when app is already running
        intent.data?.let { uri ->
            android.util.Log.d("MainActivity", "New deep link received: $uri")
            // The deep link will be handled by the existing TaskCardsApp instance
            // We need to communicate this to the app
            setIntent(intent)
        }
    }

    // Entry point for Hilt to provide DeepLinkHandler
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface DeepLinkHandlerEntryPoint {
        fun deepLinkHandler(): DeepLinkHandler
    }
}
