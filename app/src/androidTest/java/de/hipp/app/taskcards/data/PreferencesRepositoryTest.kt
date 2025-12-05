package de.hipp.app.taskcards.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import de.hipp.app.taskcards.data.preferences.PreferencesRepository
import de.hipp.app.taskcards.data.preferences.PreferencesRepositoryImpl
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.File

/**
 * Instrumented tests for PreferencesRepository.
 * Tests DataStore persistence on device/emulator.
 */
class PreferencesRepositoryTest : StringSpec({
    lateinit var context: Context
    lateinit var repo: PreferencesRepository

    beforeSpec {
        context = ApplicationProvider.getApplicationContext()
    }

    beforeTest {
        // Clean up DataStore files before each test
        val dataStoreFile = File(context.filesDir, "datastore/settings.preferences_pb")
        dataStoreFile.delete()
        repo = PreferencesRepositoryImpl(context)
    }

    "default highContrastMode is false" {
        runTest {
            val value = repo.highContrastMode.first()
            value shouldBe false
        }
    }

    "setHighContrastMode to true updates the value" {
        runTest {
            repo.setHighContrastMode(true)
            val value = repo.highContrastMode.first()
            value shouldBe true
        }
    }

    "setHighContrastMode to false updates the value" {
        runTest {
            repo.setHighContrastMode(true)
            repo.setHighContrastMode(false)
            val value = repo.highContrastMode.first()
            value shouldBe false
        }
    }

    "multiple updates are persisted correctly" {
        runTest {
            repo.setHighContrastMode(true)
            repo.highContrastMode.first() shouldBe true

            repo.setHighContrastMode(false)
            repo.highContrastMode.first() shouldBe false

            repo.setHighContrastMode(true)
            repo.highContrastMode.first() shouldBe true
        }
    }

    "preferences persist across repository instances" {
        runTest {
            repo.setHighContrastMode(true)
            repo.highContrastMode.first() shouldBe true

            // Create a new repository instance with the same context
            val newRepo = PreferencesRepositoryImpl(context)
            newRepo.highContrastMode.first() shouldBe true
        }
    }
})
