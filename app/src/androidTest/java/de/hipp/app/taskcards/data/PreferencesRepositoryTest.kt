package de.hipp.app.taskcards.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import de.hipp.app.taskcards.data.preferences.PreferencesRepository
import de.hipp.app.taskcards.data.preferences.PreferencesRepositoryImpl
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import java.io.File

/**
 * Instrumented tests for PreferencesRepository.
 * Tests DataStore persistence on device/emulator.
 */
class PreferencesRepositoryTest {

    companion object {
        lateinit var context: Context

        @JvmStatic
        @BeforeClass
        fun setupClass() {
            context = ApplicationProvider.getApplicationContext()
        }
    }

    private lateinit var repo: PreferencesRepository

    @Before
    fun setup() {
        // Clean up DataStore files before each test
        val dataStoreFile = File(context.filesDir, "datastore/settings.preferences_pb")
        dataStoreFile.delete()
        repo = PreferencesRepositoryImpl(context)
    }

    @Test
    fun defaultHighContrastModeIsFalse() = runTest {
        val value = repo.highContrastMode.first()
        value shouldBe false
    }

    @Test
    fun setHighContrastModeToTrueUpdatesTheValue() = runTest {
        repo.setHighContrastMode(true)
        val value = repo.highContrastMode.first()
        value shouldBe true
    }

    @Test
    fun setHighContrastModeToFalseUpdatesTheValue() = runTest {
        repo.setHighContrastMode(true)
        repo.setHighContrastMode(false)
        val value = repo.highContrastMode.first()
        value shouldBe false
    }

    @Test
    fun multipleUpdatesArePersistedCorrectly() = runTest {
        repo.setHighContrastMode(true)
        repo.highContrastMode.first() shouldBe true

        repo.setHighContrastMode(false)
        repo.highContrastMode.first() shouldBe false

        repo.setHighContrastMode(true)
        repo.highContrastMode.first() shouldBe true
    }

    @Test
    fun preferencesPersistAcrossRepositoryInstances() = runTest {
        repo.setHighContrastMode(true)
        repo.highContrastMode.first() shouldBe true

        // Create a new repository instance with the same context
        val newRepo = PreferencesRepositoryImpl(context)
        newRepo.highContrastMode.first() shouldBe true
    }
}
