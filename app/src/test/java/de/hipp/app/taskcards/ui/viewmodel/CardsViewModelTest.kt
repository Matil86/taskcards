package de.hipp.app.taskcards.ui.viewmodel

import de.hipp.app.taskcards.data.InMemoryTaskListRepository
import de.hipp.app.taskcards.di.StringProvider
import de.hipp.app.taskcards.util.Constants
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/**
 * Tests for CardsViewModel.
 *
 * Uses UnconfinedTestDispatcher for everything (repo, vm dispatcher, setMain, runTest).
 * With UnconfinedTestDispatcher:
 *   - backgroundScope.launch runs immediately (no scheduler advance needed)
 *   - repo operations run immediately (no withContext scheduling delay)
 *   - stateIn/WhileSubscribed propagates immediately once a subscriber joins
 * Delays (e.g. skipCard 300ms debounce) still use virtual time via
 * testDispatcher.scheduler.advanceUntilIdle().
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CardsViewModelTest : StringSpec({
    val testDispatcher = UnconfinedTestDispatcher()
    lateinit var repo: InMemoryTaskListRepository
    lateinit var strings: MockCardsStringProvider
    lateinit var vm: CardsViewModel

    beforeTest {
        Dispatchers.setMain(testDispatcher)
        repo = InMemoryTaskListRepository(testDispatcher)
        strings = MockCardsStringProvider()
        vm = CardsViewModel(
            listId = Constants.DEFAULT_LIST_ID,
            repo = repo,
            strings = strings,
            dispatcher = testDispatcher
        )
    }

    afterTest {
        Dispatchers.resetMain()
    }

    // -----------------------------------------------------------------------
    // BRUCE — RED: skipCard
    // -----------------------------------------------------------------------

    "skipCard should keep task active and move it to end of order" {
        runTest(testDispatcher) {
            backgroundScope.launch { vm.state.collect {} }  // activate WhileSubscribed

            // Add three tasks so we can verify the skip-to-end behaviour
            val taskA = repo.addTask(Constants.DEFAULT_LIST_ID, "Task A")
            val taskB = repo.addTask(Constants.DEFAULT_LIST_ID, "Task B")
            val taskC = repo.addTask(Constants.DEFAULT_LIST_ID, "Task C")

            // Skip Task A (currently the front of the visible deck)
            vm.skipCard(taskA.id)
            testDispatcher.scheduler.advanceUntilIdle()  // advance past 300ms debounce

            val state = vm.state.value
            val all = state.visibleCards

            // Task A must still be in the deck (not done, not removed)
            val skippedTask = all.firstOrNull { it.id == taskA.id }
            skippedTask shouldNotBe null
            skippedTask!!.done.shouldBeFalse()
            skippedTask.removed.shouldBeFalse()

            // Task A should now appear after Task B and Task C
            val orderedIds = all.map { it.id }
            val indexA = orderedIds.indexOf(taskA.id)
            val indexB = orderedIds.indexOf(taskB.id)
            val indexC = orderedIds.indexOf(taskC.id)
            (indexA > indexB) shouldBe true
            (indexA > indexC) shouldBe true
        }
    }

    "skipCard should not set error on success" {
        runTest(testDispatcher) {
            backgroundScope.launch { vm.state.collect {} }

            repo.addTask(Constants.DEFAULT_LIST_ID, "Single task")

            vm.skipCard(repo.observeTasks(Constants.DEFAULT_LIST_ID).first().first().id)
            testDispatcher.scheduler.advanceUntilIdle()

            vm.errorState.value.shouldBeNull()
        }
    }

    // -----------------------------------------------------------------------
    // BRUCE — GREEN verification: swipeComplete with complete=true marks done
    // -----------------------------------------------------------------------

    "swipeComplete with complete=true should mark task done" {
        runTest(testDispatcher) {
            backgroundScope.launch { vm.state.collect {} }

            repo.addTask(Constants.DEFAULT_LIST_ID, "Task to complete")

            val task = vm.state.value.visibleCards.first()

            vm.swipeComplete(task.id, true)

            // Task is no longer in visibleCards (done tasks are filtered out)
            val stillVisible = vm.state.value.visibleCards.any { it.id == task.id }
            stillVisible shouldBe false

            // Verify in repo that the task is actually marked done
            val doneTask = repo.observeTasks(Constants.DEFAULT_LIST_ID).first()
                .firstOrNull { it.id == task.id }
            doneTask shouldNotBe null
            doneTask!!.done shouldBe true
        }
    }

    "swipeComplete with complete=true should not set error on success" {
        runTest(testDispatcher) {
            backgroundScope.launch { vm.state.collect {} }

            val task = repo.addTask(Constants.DEFAULT_LIST_ID, "Task")

            vm.swipeComplete(task.id, true)

            vm.errorState.value.shouldBeNull()
        }
    }

    // -----------------------------------------------------------------------
    // BRUCE — Regression: stale-repo bug (CardsScreen remember without key)
    //
    // Bug: CardsScreen used `remember { RepositoryProvider.getRepository() }`
    // without a key. After auth resolves, defaultListId changes from "default-list"
    // to the Firestore list ID. A new CardsViewModel is created for the new listId,
    // but it still receives the old Room repo (captured before auth). Room has no
    // tasks for the Firestore list ID → totalActive = 0 → completion celebration
    // shown even though tasks exist.
    //
    // Contract verified here: CardsViewModel MUST be initialised with a repo that
    // actually contains tasks for the given listId. When it is, tasks are visible.
    // When a different repo is used (the stale-repo scenario), tasks are invisible.
    // The fix in CardsScreen is: `remember(listId) { RepositoryProvider.getRepository() }`
    // so the repo is re-fetched whenever listId changes.
    // -----------------------------------------------------------------------

    "CardsViewModel shows active tasks when repo contains tasks for the given listId" {
        runTest(testDispatcher) {
            val firestoreListId = "firestore-list-abc"
            val correctRepo = InMemoryTaskListRepository(testDispatcher)
            correctRepo.addTask(firestoreListId, "Haushalt")
            correctRepo.addTask(firestoreListId, "Einkaufen")

            val vmCorrect = CardsViewModel(
                listId = firestoreListId,
                repo = correctRepo,
                strings = strings,
                dispatcher = testDispatcher
            )
            backgroundScope.launch { vmCorrect.state.collect {} }

            // Tasks must be visible — this is the acceptance criterion after the fix
            vmCorrect.state.value.totalActive shouldBe 2
            vmCorrect.state.value.visibleCards.map { it.text }.toSet() shouldBe setOf("Haushalt", "Einkaufen")
        }
    }

    "CardsViewModel shows zero tasks when repo does not contain tasks for the given listId (stale-repo scenario)" {
        runTest(testDispatcher) {
            val firestoreListId = "firestore-list-abc"

            // Stale repo (Room): has no knowledge of firestoreListId
            val staleRoomRepo = InMemoryTaskListRepository(testDispatcher)
            staleRoomRepo.addTask(Constants.DEFAULT_LIST_ID, "Local task in Room")

            val vmStale = CardsViewModel(
                listId = firestoreListId,
                repo = staleRoomRepo, // ← wrong repo — simulates the bug
                strings = strings,
                dispatcher = testDispatcher
            )
            backgroundScope.launch { vmStale.state.collect {} }

            // Stale repo returns nothing for firestoreListId → celebration screen shown incorrectly
            vmStale.state.value.totalActive shouldBe 0
        }
    }
})

private class MockCardsStringProvider : StringProvider {
    override fun getString(resId: Int): String = "Error message"
    override fun getString(resId: Int, vararg formatArgs: Any): String = "Error message"
}
