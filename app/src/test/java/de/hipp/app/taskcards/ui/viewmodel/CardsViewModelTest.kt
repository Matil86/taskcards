package de.hipp.app.taskcards.ui.viewmodel

import de.hipp.app.taskcards.data.InMemoryTaskListRepository
import de.hipp.app.taskcards.util.Constants
import de.hipp.app.taskcards.util.MockStringProvider
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/**
 * Tests for CardsViewModel.
 *
 * Dispatcher strategy:
 * - Dispatchers.Main is set to UnconfinedTestDispatcher so that viewModelScope
 *   (which uses Dispatchers.Main.immediate) propagates state eagerly.
 * - The repo and vm use StandardTestDispatcher for explicit timing control.
 * - After backgroundScope.launch, runCurrent() is called to force the
 *   subscription to be established before any operations — this makes
 *   WhileSubscribed propagation deterministic in CI.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CardsViewModelTest : FunSpec({
    val testDispatcher = StandardTestDispatcher()
    val mainDispatcher = UnconfinedTestDispatcher(testDispatcher.scheduler)
    lateinit var repo: InMemoryTaskListRepository
    lateinit var strings: MockStringProvider
    lateinit var vm: CardsViewModel

    beforeTest {
        Dispatchers.setMain(mainDispatcher)
        repo = InMemoryTaskListRepository(testDispatcher)
        strings = MockStringProvider()
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

    context("skipCard") {
        test("skipCard should keep task active and move it to end of order") {
            runTest(testDispatcher) {
                backgroundScope.launch { vm.state.collect {} }
                testDispatcher.scheduler.runCurrent()  // establish subscription before adding tasks

                val taskA = repo.addTask(Constants.DEFAULT_LIST_ID, "Task A")
                val taskB = repo.addTask(Constants.DEFAULT_LIST_ID, "Task B")
                val taskC = repo.addTask(Constants.DEFAULT_LIST_ID, "Task C")
                testDispatcher.scheduler.advanceUntilIdle()

                vm.skipCard(taskA.id)
                testDispatcher.scheduler.advanceUntilIdle()  // advance past 300ms debounce

                val state = vm.state.value
                val all = state.visibleCards

                val skippedTask = all.firstOrNull { it.id == taskA.id }
                skippedTask shouldNotBe null
                skippedTask!!.done.shouldBeFalse()
                skippedTask.removed.shouldBeFalse()

                val orderedIds = all.map { it.id }
                val indexA = orderedIds.indexOf(taskA.id)
                val indexB = orderedIds.indexOf(taskB.id)
                val indexC = orderedIds.indexOf(taskC.id)
                (indexA > indexB) shouldBe true
                (indexA > indexC) shouldBe true
            }
        }

        test("skipCard should not set error on success") {
            runTest(testDispatcher) {
                backgroundScope.launch { vm.state.collect {} }
                testDispatcher.scheduler.runCurrent()

                repo.addTask(Constants.DEFAULT_LIST_ID, "Single task")
                testDispatcher.scheduler.advanceUntilIdle()

                vm.skipCard(vm.state.value.visibleCards.first().id)
                testDispatcher.scheduler.advanceUntilIdle()

                vm.errorState.value.shouldBeNull()
            }
        }
    }

    context("swipeComplete") {
        test("swipeComplete with complete=true should mark task done") {
            runTest(testDispatcher) {
                backgroundScope.launch { vm.state.collect {} }
                testDispatcher.scheduler.runCurrent()

                repo.addTask(Constants.DEFAULT_LIST_ID, "Task to complete")
                testDispatcher.scheduler.advanceUntilIdle()

                val task = vm.state.value.visibleCards.first()

                vm.swipeComplete(task.id, true)
                testDispatcher.scheduler.advanceUntilIdle()

                val stillVisible = vm.state.value.visibleCards.any { it.id == task.id }
                stillVisible shouldBe false

                val doneTask = repo.observeTasks(Constants.DEFAULT_LIST_ID).first()
                    .firstOrNull { it.id == task.id }
                doneTask shouldNotBe null
                doneTask!!.done shouldBe true
            }
        }

        test("swipeComplete with complete=true should not set error on success") {
            runTest(testDispatcher) {
                backgroundScope.launch { vm.state.collect {} }
                testDispatcher.scheduler.runCurrent()

                val task = repo.addTask(Constants.DEFAULT_LIST_ID, "Task")
                testDispatcher.scheduler.advanceUntilIdle()

                vm.swipeComplete(task.id, true)
                testDispatcher.scheduler.advanceUntilIdle()

                vm.errorState.value.shouldBeNull()
            }
        }
    }

    context("Stale Repo Regression") {
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
        // Contract: CardsViewModel MUST be initialised with a repo that contains tasks
        // for the given listId. The fix in CardsScreen is:
        //   remember(listId) { RepositoryProvider.getRepository() }
        // -----------------------------------------------------------------------

        test("CardsViewModel shows active tasks when repo contains tasks for the given listId") {
            runTest(testDispatcher) {
                val firestoreListId = "firestore-list-abc"
                val correctRepo = InMemoryTaskListRepository(testDispatcher)
                correctRepo.addTask(firestoreListId, "Haushalt")
                correctRepo.addTask(firestoreListId, "Einkaufen")
                testDispatcher.scheduler.advanceUntilIdle()

                val vmCorrect = CardsViewModel(
                    listId = firestoreListId,
                    repo = correctRepo,
                    strings = strings,
                    dispatcher = testDispatcher
                )
                backgroundScope.launch { vmCorrect.state.collect {} }
                testDispatcher.scheduler.advanceUntilIdle()

                vmCorrect.state.value.totalActive shouldBe 2
                vmCorrect.state.value.visibleCards.map { it.text }.toSet() shouldBe setOf("Haushalt", "Einkaufen")
            }
        }

        test("CardsViewModel shows zero tasks when repo does not contain tasks for the given listId (stale-repo scenario)") {
            runTest(testDispatcher) {
                val firestoreListId = "firestore-list-abc"

                val staleRoomRepo = InMemoryTaskListRepository(testDispatcher)
                staleRoomRepo.addTask(Constants.DEFAULT_LIST_ID, "Local task in Room")
                testDispatcher.scheduler.advanceUntilIdle()

                val vmStale = CardsViewModel(
                    listId = firestoreListId,
                    repo = staleRoomRepo,
                    strings = strings,
                    dispatcher = testDispatcher
                )
                backgroundScope.launch { vmStale.state.collect {} }
                testDispatcher.scheduler.advanceUntilIdle()

                vmStale.state.value.totalActive shouldBe 0
            }
        }
    }
})
