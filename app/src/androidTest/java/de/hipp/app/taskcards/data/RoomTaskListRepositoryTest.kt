package de.hipp.app.taskcards.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

/**
 * Comprehensive instrumented tests for RoomTaskListRepository.
 * Uses in-memory Room database on device/emulator.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class RoomTaskListRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var database: AppDatabase
    private lateinit var repository: RoomTaskListRepository

    companion object {
        lateinit var context: Context

        @JvmStatic
        @BeforeClass
        fun setupClass() {
            context = ApplicationProvider.getApplicationContext()
        }
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        // Tests don't need reminder scheduling, so pass null for scheduler and preferences
        repository = RoomTaskListRepository(database.taskDao(), null, null, testDispatcher)
    }

    @After
    fun teardown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun addingTaskInsertsItAtTheTopLowestOrder() = runTest {
        val listId = "list1"
        val first = repository.addTask(listId, "Task 1")
        val second = repository.addTask(listId, "Task 2")

        val tasks = repository.observeTasks(listId).first()
        tasks shouldHaveSize 2
        tasks[0].text shouldBe "Task 2"
        tasks[1].text shouldBe "Task 1"
        tasks[0].order shouldBe (tasks[1].order - 1)
    }

    @Test
    fun addingTaskTrimsWhitespace() = runTest {
        val listId = "list1"
        val task = repository.addTask(listId, "  Task with spaces  ")
        task.text shouldBe "Task with spaces"
    }

    @Test
    fun addingEmptyTaskThrowsException() = runTest {
        shouldThrow<IllegalArgumentException> {
            repository.addTask("list1", "   ")
        }
    }

    @Test
    fun addingTaskExceedingMaxLengthThrowsException() = runTest {
        val longText = "a".repeat(TaskListRepository.MAX_TASK_TEXT_LENGTH + 1)
        shouldThrow<IllegalArgumentException> {
            repository.addTask("list1", longText)
        }
    }

    @Test
    fun removingTaskSetsRemovedFlagToTrue() = runTest {
        val listId = "list1"
        val task = repository.addTask(listId, "Task 1")
        repository.removeTask(listId, task.id)

        val tasks = repository.observeTasks(listId).first()
        tasks shouldHaveSize 1
        tasks[0].removed shouldBe true
    }

    @Test
    fun restoringTaskSetsRemovedFlagToFalse() = runTest {
        val listId = "list1"
        val task = repository.addTask(listId, "Task 1")
        repository.removeTask(listId, task.id)
        repository.restoreTask(listId, task.id)

        val tasks = repository.observeTasks(listId).first()
        tasks shouldHaveSize 1
        tasks[0].removed shouldBe false
    }

    @Test
    fun markingTaskAsDoneUpdatesDoneFlag() = runTest {
        val listId = "list1"
        val task = repository.addTask(listId, "Task 1")
        repository.markDone(listId, task.id, true)

        val tasks = repository.observeTasks(listId).first()
        tasks[0].done shouldBe true

        repository.markDone(listId, task.id, false)
        val updatedTasks = repository.observeTasks(listId).first()
        updatedTasks[0].done shouldBe false
    }

    @Test
    fun movingTaskToHigherPositionReordersTasks() = runTest {
        val listId = "list1"
        val t1 = repository.addTask(listId, "Task 1")
        val t2 = repository.addTask(listId, "Task 2")
        val t3 = repository.addTask(listId, "Task 3")

        // Initial order: [t3, t2, t1]
        repository.moveTask(listId, t3.id, 2)

        val tasks = repository.observeTasks(listId).first()
        tasks.map { it.text } shouldContainExactly listOf("Task 2", "Task 1", "Task 3")
    }

    @Test
    fun movingTaskToLowerPositionReordersTasks() = runTest {
        val listId = "list1"
        val t1 = repository.addTask(listId, "Task 1")
        val t2 = repository.addTask(listId, "Task 2")
        val t3 = repository.addTask(listId, "Task 3")

        // Initial order: [t3, t2, t1]
        repository.moveTask(listId, t1.id, 0)

        val tasks = repository.observeTasks(listId).first()
        tasks.map { it.text } shouldContainExactly listOf("Task 1", "Task 3", "Task 2")
    }

    @Test
    fun movingTaskToInvalidIndexBoundsToValidRange() = runTest {
        val listId = "list1"
        val t1 = repository.addTask(listId, "Task 1")
        val t2 = repository.addTask(listId, "Task 2")

        repository.moveTask(listId, t2.id, 999)

        val tasks = repository.observeTasks(listId).first()
        tasks.map { it.text } shouldContainExactly listOf("Task 1", "Task 2")
    }

    @Test
    fun movingNonExistentTaskDoesNothing() = runTest {
        val listId = "list1"
        repository.addTask(listId, "Task 1")
        repository.moveTask(listId, "non-existent-id", 0)

        val tasks = repository.observeTasks(listId).first()
        tasks shouldHaveSize 1
    }

    @Test
    fun tasksAreSortedByRemovedStatusFirstThenOrder() = runTest {
        val listId = "list1"
        val t1 = repository.addTask(listId, "Task 1")
        val t2 = repository.addTask(listId, "Task 2")
        val t3 = repository.addTask(listId, "Task 3")

        repository.removeTask(listId, t2.id)

        val tasks = repository.observeTasks(listId).first()
        tasks.map { it.text } shouldContainExactly listOf("Task 3", "Task 1", "Task 2")
        tasks.map { it.removed } shouldContainExactly listOf(false, false, true)
    }

    @Test
    fun clearingListRemovesAllTasks() = runTest {
        val listId = "list1"
        repository.addTask(listId, "Task 1")
        repository.addTask(listId, "Task 2")

        repository.clearList(listId)

        val tasks = repository.observeTasks(listId).first()
        tasks shouldHaveSize 0
    }

    @Test
    fun getActiveListCountReturnsCorrectCount() = runTest {
        repository.addTask("list1", "Task 1")
        repository.addTask("list2", "Task 2")
        repository.addTask("list1", "Task 3")

        repository.getActiveListCount() shouldBe 2
    }

    @Test
    fun multipleListsAreIsolatedFromEachOther() = runTest {
        repository.addTask("list1", "Task 1")
        repository.addTask("list2", "Task 2")

        val list1Tasks = repository.observeTasks("list1").first()
        val list2Tasks = repository.observeTasks("list2").first()

        list1Tasks shouldHaveSize 1
        list2Tasks shouldHaveSize 1
        list1Tasks[0].text shouldBe "Task 1"
        list2Tasks[0].text shouldBe "Task 2"
    }

    @Test
    fun complexSequenceOfOperationsMaintainsDataIntegrity() = runTest {
        val listId = "list1"
        val t1 = repository.addTask(listId, "Task 1")
        val t2 = repository.addTask(listId, "Task 2")
        val t3 = repository.addTask(listId, "Task 3")

        repository.markDone(listId, t2.id, true)
        repository.removeTask(listId, t3.id)
        repository.moveTask(listId, t1.id, 0)

        val tasks = repository.observeTasks(listId).first()
        tasks shouldHaveSize 3
        tasks[0].text shouldBe "Task 1"
        tasks[0].done shouldBe false
        tasks[0].removed shouldBe false
        tasks[1].text shouldBe "Task 2"
        tasks[1].done shouldBe true
        tasks[1].removed shouldBe false
        tasks[2].text shouldBe "Task 3"
        tasks[2].removed shouldBe true
    }

    @Test
    fun orderCompactionAfterMultipleOperations() = runTest {
        val listId = "list1"
        repository.addTask(listId, "Task 1")
        repository.addTask(listId, "Task 2")
        val t3 = repository.addTask(listId, "Task 3")

        // Move task around multiple times
        repository.moveTask(listId, t3.id, 2)
        repository.moveTask(listId, t3.id, 0)

        val tasks = repository.observeTasks(listId).first()
        // Verify orders are compact (0, 1, 2)
        tasks.filter { !it.removed }.map { it.order } shouldContainExactly listOf(0, 1, 2)
    }
}
