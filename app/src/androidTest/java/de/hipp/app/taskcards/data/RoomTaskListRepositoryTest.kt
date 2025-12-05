package de.hipp.app.taskcards.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/**
 * Comprehensive instrumented tests for RoomTaskListRepository.
 * Uses in-memory Room database on device/emulator.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class RoomTaskListRepositoryTest : StringSpec({
    val testDispatcher = StandardTestDispatcher()
    lateinit var context: Context
    lateinit var database: AppDatabase
    lateinit var repository: RoomTaskListRepository

    beforeSpec {
        context = ApplicationProvider.getApplicationContext()
    }

    beforeTest {
        Dispatchers.setMain(testDispatcher)
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        // Tests don't need reminder scheduling, so pass null for scheduler and preferences
        repository = RoomTaskListRepository(database.taskDao(), null, null, testDispatcher)
    }

    afterTest {
        database.close()
        Dispatchers.resetMain()
    }

    "adding task inserts it at the top (lowest order)" {
        runTest {
            val listId = "list1"
            val first = repository.addTask(listId, "Task 1")
            val second = repository.addTask(listId, "Task 2")

            val tasks = repository.observeTasks(listId).first()
            tasks shouldHaveSize 2
            tasks[0].text shouldBe "Task 2"
            tasks[1].text shouldBe "Task 1"
            tasks[0].order shouldBe (tasks[1].order - 1)
        }
    }

    "adding task trims whitespace" {
        runTest {
            val listId = "list1"
            val task = repository.addTask(listId, "  Task with spaces  ")
            task.text shouldBe "Task with spaces"
        }
    }

    "adding empty task throws exception" {
        runTest {
            shouldThrow<IllegalArgumentException> {
                repository.addTask("list1", "   ")
            }
        }
    }

    "adding task exceeding max length throws exception" {
        runTest {
            val longText = "a".repeat(TaskListRepository.MAX_TASK_TEXT_LENGTH + 1)
            shouldThrow<IllegalArgumentException> {
                repository.addTask("list1", longText)
            }
        }
    }

    "removing task sets removed flag to true" {
        runTest {
            val listId = "list1"
            val task = repository.addTask(listId, "Task 1")
            repository.removeTask(listId, task.id)

            val tasks = repository.observeTasks(listId).first()
            tasks shouldHaveSize 1
            tasks[0].removed shouldBe true
        }
    }

    "restoring task sets removed flag to false" {
        runTest {
            val listId = "list1"
            val task = repository.addTask(listId, "Task 1")
            repository.removeTask(listId, task.id)
            repository.restoreTask(listId, task.id)

            val tasks = repository.observeTasks(listId).first()
            tasks shouldHaveSize 1
            tasks[0].removed shouldBe false
        }
    }

    "marking task as done updates done flag" {
        runTest {
            val listId = "list1"
            val task = repository.addTask(listId, "Task 1")
            repository.markDone(listId, task.id, true)

            val tasks = repository.observeTasks(listId).first()
            tasks[0].done shouldBe true

            repository.markDone(listId, task.id, false)
            val updatedTasks = repository.observeTasks(listId).first()
            updatedTasks[0].done shouldBe false
        }
    }

    "moving task to higher position reorders tasks" {
        runTest {
            val listId = "list1"
            val t1 = repository.addTask(listId, "Task 1")
            val t2 = repository.addTask(listId, "Task 2")
            val t3 = repository.addTask(listId, "Task 3")

            // Initial order: [t3, t2, t1]
            repository.moveTask(listId, t3.id, 2)

            val tasks = repository.observeTasks(listId).first()
            tasks.map { it.text } shouldContainExactly listOf("Task 2", "Task 1", "Task 3")
        }
    }

    "moving task to lower position reorders tasks" {
        runTest {
            val listId = "list1"
            val t1 = repository.addTask(listId, "Task 1")
            val t2 = repository.addTask(listId, "Task 2")
            val t3 = repository.addTask(listId, "Task 3")

            // Initial order: [t3, t2, t1]
            repository.moveTask(listId, t1.id, 0)

            val tasks = repository.observeTasks(listId).first()
            tasks.map { it.text } shouldContainExactly listOf("Task 1", "Task 3", "Task 2")
        }
    }

    "moving task to invalid index bounds to valid range" {
        runTest {
            val listId = "list1"
            val t1 = repository.addTask(listId, "Task 1")
            val t2 = repository.addTask(listId, "Task 2")

            repository.moveTask(listId, t2.id, 999)

            val tasks = repository.observeTasks(listId).first()
            tasks.map { it.text } shouldContainExactly listOf("Task 1", "Task 2")
        }
    }

    "moving non-existent task does nothing" {
        runTest {
            val listId = "list1"
            repository.addTask(listId, "Task 1")
            repository.moveTask(listId, "non-existent-id", 0)

            val tasks = repository.observeTasks(listId).first()
            tasks shouldHaveSize 1
        }
    }

    "tasks are sorted by removed status first, then order" {
        runTest {
            val listId = "list1"
            val t1 = repository.addTask(listId, "Task 1")
            val t2 = repository.addTask(listId, "Task 2")
            val t3 = repository.addTask(listId, "Task 3")

            repository.removeTask(listId, t2.id)

            val tasks = repository.observeTasks(listId).first()
            tasks.map { it.text } shouldContainExactly listOf("Task 3", "Task 1", "Task 2")
            tasks.map { it.removed } shouldContainExactly listOf(false, false, true)
        }
    }

    "clearing list removes all tasks" {
        runTest {
            val listId = "list1"
            repository.addTask(listId, "Task 1")
            repository.addTask(listId, "Task 2")

            repository.clearList(listId)

            val tasks = repository.observeTasks(listId).first()
            tasks shouldHaveSize 0
        }
    }

    "getActiveListCount returns correct count" {
        runTest {
            repository.addTask("list1", "Task 1")
            repository.addTask("list2", "Task 2")
            repository.addTask("list1", "Task 3")

            repository.getActiveListCount() shouldBe 2
        }
    }

    "multiple lists are isolated from each other" {
        runTest {
            repository.addTask("list1", "Task 1")
            repository.addTask("list2", "Task 2")

            val list1Tasks = repository.observeTasks("list1").first()
            val list2Tasks = repository.observeTasks("list2").first()

            list1Tasks shouldHaveSize 1
            list2Tasks shouldHaveSize 1
            list1Tasks[0].text shouldBe "Task 1"
            list2Tasks[0].text shouldBe "Task 2"
        }
    }

    "complex sequence of operations maintains data integrity" {
        runTest {
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
    }

    "order compaction after multiple operations" {
        runTest {
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
})
