package de.hipp.app.taskcards.data

import de.hipp.app.taskcards.model.ReminderType
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import java.util.Calendar

class InMemoryTaskListRepositoryTest : FunSpec({

    test("new tasks are ordered by priority (last added first)") {
        val repo = InMemoryTaskListRepository()
        val a = repo.addTask("list1", "A")
        val b = repo.addTask("list1", "B")
        val c = repo.addTask("list1", "C")

        repo.observeTasks("list1").first().map { it.id } shouldContainExactly listOf(c.id, b.id, a.id)
    }

    test("removed tasks are sorted after active tasks") {
        val repo = InMemoryTaskListRepository()
        val a = repo.addTask("list2", "A")
        val b = repo.addTask("list2", "B")
        repo.removeTask("list2", a.id)

        val tasks = repo.observeTasks("list2").first()
        tasks.map { it.id } shouldContainExactly listOf(b.id, a.id)
        tasks.last().removed shouldBe true
    }

    test("restored tasks return to active set") {
        val repo = InMemoryTaskListRepository()
        val a = repo.addTask("list3", "A")
        repo.removeTask("list3", a.id)
        repo.restoreTask("list3", a.id)

        repo.observeTasks("list3").first().first().removed shouldBe false
    }

    test("moving tasks updates order compactly") {
        val repo = InMemoryTaskListRepository()
        val a = repo.addTask("list4", "A")
        val b = repo.addTask("list4", "B")
        val c = repo.addTask("list4", "C")
        repo.moveTask("list4", b.id, toIndex = 0)

        val active = repo.observeTasks("list4").first().filter { !it.removed }
        active.map { it.id } shouldContainExactly listOf(b.id, c.id, a.id)
        active.map { it.order } shouldContainExactly listOf(0, 1, 2)
    }

    test("marking task as done updates done flag") {
        val repo = InMemoryTaskListRepository()
        val task = repo.addTask("list5", "Task")
        repo.markDone("list5", task.id, true)

        val tasks = repo.observeTasks("list5").first()
        tasks.first().done shouldBe true
    }

    test("marking task as undone updates done flag") {
        val repo = InMemoryTaskListRepository()
        val task = repo.addTask("list6", "Task")
        repo.markDone("list6", task.id, true)
        repo.markDone("list6", task.id, false)

        val tasks = repo.observeTasks("list6").first()
        tasks.first().done shouldBe false
    }

    test("marking removed task as done preserves removed status") {
        val repo = InMemoryTaskListRepository()
        val task = repo.addTask("list7", "Task")
        repo.removeTask("list7", task.id)
        repo.markDone("list7", task.id, true)

        val tasks = repo.observeTasks("list7").first()
        tasks.first().removed shouldBe true
        tasks.first().done shouldBe true
    }

    test("multiple lists are isolated from each other") {
        val repo = InMemoryTaskListRepository()
        val task1 = repo.addTask("listA", "Task A")
        val task2 = repo.addTask("listB", "Task B")

        val tasksA = repo.observeTasks("listA").first()
        val tasksB = repo.observeTasks("listB").first()

        tasksA shouldHaveSize 1
        tasksB shouldHaveSize 1
        tasksA.first().id shouldBe task1.id
        tasksB.first().id shouldBe task2.id
    }

    test("observing empty list returns empty collection") {
        val repo = InMemoryTaskListRepository()

        repo.observeTasks("emptyList").first().shouldBeEmpty()
    }

    test("moving task to same position is handled correctly") {
        val repo = InMemoryTaskListRepository()
        val a = repo.addTask("list8", "A")
        val b = repo.addTask("list8", "B")
        val c = repo.addTask("list8", "C")

        repo.moveTask("list8", c.id, toIndex = 0)

        val active = repo.observeTasks("list8").first().filter { !it.removed }
        active.map { it.id } shouldContainExactly listOf(c.id, b.id, a.id)
    }

    test("moving task to out-of-bounds index is coerced") {
        val repo = InMemoryTaskListRepository()
        val a = repo.addTask("list9", "A")
        val b = repo.addTask("list9", "B")
        val c = repo.addTask("list9", "C")

        repo.moveTask("list9", c.id, toIndex = 999)

        val active = repo.observeTasks("list9").first().filter { !it.removed }
        active.map { it.id } shouldContainExactly listOf(b.id, a.id, c.id)
        active.last().id shouldBe c.id
    }

    test("moving task to negative index is coerced to zero") {
        val repo = InMemoryTaskListRepository()
        val a = repo.addTask("list10", "A")
        val b = repo.addTask("list10", "B")

        repo.moveTask("list10", a.id, toIndex = -5)

        val active = repo.observeTasks("list10").first().filter { !it.removed }
        active.first().id shouldBe a.id
    }

    test("moving removed task does nothing") {
        val repo = InMemoryTaskListRepository()
        val a = repo.addTask("list11", "A")
        val b = repo.addTask("list11", "B")
        val c = repo.addTask("list11", "C")
        repo.removeTask("list11", c.id)

        repo.moveTask("list11", c.id, toIndex = 0)

        val active = repo.observeTasks("list11").first().filter { !it.removed }
        active.map { it.id } shouldContainExactly listOf(b.id, a.id)
    }

    test("moving non-existent task does nothing") {
        val repo = InMemoryTaskListRepository()
        val a = repo.addTask("list12", "A")
        val b = repo.addTask("list12", "B")

        repo.moveTask("list12", "non-existent-id", toIndex = 0)

        val active = repo.observeTasks("list12").first().filter { !it.removed }
        active.map { it.id } shouldContainExactly listOf(b.id, a.id)
    }

    test("operations on non-existent task are ignored") {
        val repo = InMemoryTaskListRepository()
        repo.addTask("list13", "A")

        repo.removeTask("list13", "non-existent")
        repo.restoreTask("list13", "non-existent")
        repo.markDone("list13", "non-existent", true)

        val tasks = repo.observeTasks("list13").first()
        tasks shouldHaveSize 1
    }

    test("multiple operations on same task work correctly") {
        val repo = InMemoryTaskListRepository()
        val task = repo.addTask("list14", "Task")

        repo.markDone("list14", task.id, true)
        repo.removeTask("list14", task.id)
        repo.restoreTask("list14", task.id)
        repo.markDone("list14", task.id, false)

        val result = repo.observeTasks("list14").first().first()
        result.removed shouldBe false
        result.done shouldBe false
    }

    test("removing and restoring multiple tasks preserves order") {
        val repo = InMemoryTaskListRepository()
        val a = repo.addTask("list15", "A")
        val b = repo.addTask("list15", "B")
        val c = repo.addTask("list15", "C")

        repo.removeTask("list15", b.id)
        repo.removeTask("list15", c.id)
        repo.restoreTask("list15", b.id)

        val tasks = repo.observeTasks("list15").first()
        val active = tasks.filter { !it.removed }
        active.map { it.id } shouldContainExactly listOf(b.id, a.id)
        tasks.last().id shouldBe c.id
        tasks.last().removed shouldBe true
    }

    test("task order values are negative and decrease with each addition") {
        val repo = InMemoryTaskListRepository()
        val a = repo.addTask("list16", "A")
        val b = repo.addTask("list16", "B")
        val c = repo.addTask("list16", "C")

        val tasks = repo.observeTasks("list16").first()
        tasks[0].order shouldBe (c.order)
        tasks[1].order shouldBe (b.order)
        tasks[2].order shouldBe (a.order)
        c.order shouldBe (b.order - 1)
        b.order shouldBe (a.order - 1)
    }

    test("moving task from end to beginning updates all orders") {
        val repo = InMemoryTaskListRepository()
        val a = repo.addTask("list17", "A")
        val b = repo.addTask("list17", "B")
        val c = repo.addTask("list17", "C")
        val d = repo.addTask("list17", "D")

        repo.moveTask("list17", a.id, toIndex = 0)

        val active = repo.observeTasks("list17").first().filter { !it.removed }
        active.map { it.id } shouldContainExactly listOf(a.id, d.id, c.id, b.id)
        active.map { it.order } shouldContainExactly listOf(0, 1, 2, 3)
    }

    // Memory Management Tests

    test("clearList removes list from memory") {
        val repo = InMemoryTaskListRepository()
        repo.addTask("list18", "Task 1")
        repo.addTask("list18", "Task 2")

        val beforeClear = repo.observeTasks("list18").first()
        beforeClear shouldHaveSize 2

        repo.clearList("list18")

        val afterClear = repo.observeTasks("list18").first()
        afterClear shouldHaveSize 0
    }

    test("clearList does not affect other lists") {
        val repo = InMemoryTaskListRepository()
        repo.addTask("list19", "List A Task")
        repo.addTask("list20", "List B Task")

        repo.clearList("list19")

        val listA = repo.observeTasks("list19").first()
        val listB = repo.observeTasks("list20").first()

        listA shouldHaveSize 0
        listB shouldHaveSize 1
        listB[0].text shouldBe "List B Task"
    }

    test("getActiveListCount returns zero initially") {
        val repo = InMemoryTaskListRepository()
        repo.getActiveListCount() shouldBe 0
    }

    test("getActiveListCount increments with each new list") {
        val repo = InMemoryTaskListRepository()
        repo.addTask("list21", "Task")
        repo.getActiveListCount() shouldBe 1

        repo.addTask("list22", "Task")
        repo.getActiveListCount() shouldBe 2

        repo.addTask("list23", "Task")
        repo.getActiveListCount() shouldBe 3
    }

    test("getActiveListCount does not increment when adding to existing list") {
        val repo = InMemoryTaskListRepository()
        repo.addTask("list24", "Task 1")
        repo.addTask("list24", "Task 2")
        repo.addTask("list24", "Task 3")

        repo.getActiveListCount() shouldBe 1
    }

    test("getActiveListCount decrements after clearList") {
        val repo = InMemoryTaskListRepository()
        repo.addTask("list25", "Task A")
        repo.addTask("list26", "Task B")
        repo.addTask("list27", "Task C")
        repo.getActiveListCount() shouldBe 3

        repo.clearList("list25")
        repo.getActiveListCount() shouldBe 2

        repo.clearList("list26")
        repo.getActiveListCount() shouldBe 1

        repo.clearList("list27")
        repo.getActiveListCount() shouldBe 0
    }

    test("clearList on non-existent list does not error") {
        val repo = InMemoryTaskListRepository()
        // Should not throw
        repo.clearList("non-existent-list")
        repo.getActiveListCount() shouldBe 0
    }

    test("memory management with multiple operations") {
        val repo = InMemoryTaskListRepository()

        // Create 3 lists
        repo.addTask("list28", "Task A1")
        repo.addTask("list28", "Task A2")
        repo.addTask("list29", "Task B1")
        repo.addTask("list30", "Task C1")
        repo.getActiveListCount() shouldBe 3

        // Clear one
        repo.clearList("list29")
        repo.getActiveListCount() shouldBe 2

        // Verify remaining lists still work
        val list28 = repo.observeTasks("list28").first()
        val list30 = repo.observeTasks("list30").first()
        list28 shouldHaveSize 2
        list30 shouldHaveSize 1

        // Add to cleared list creates it again
        repo.addTask("list29", "Task B-new")
        repo.getActiveListCount() shouldBe 3
    }

    // Input Validation Tests

    test("addTask throws exception for empty text") {
        val repo = InMemoryTaskListRepository()
        shouldThrow<IllegalArgumentException> {
            repo.addTask("list31", "")
        }
    }

    test("addTask throws exception for whitespace-only text") {
        val repo = InMemoryTaskListRepository()
        shouldThrow<IllegalArgumentException> {
            repo.addTask("list32", "   ")
        }
    }

    test("addTask throws exception for text exceeding max length") {
        val repo = InMemoryTaskListRepository()
        val tooLongText = "a".repeat(TaskListRepository.MAX_TASK_TEXT_LENGTH + 1)
        shouldThrow<IllegalArgumentException> {
            repo.addTask("list33", tooLongText)
        }
    }

    test("addTask accepts text at exactly max length") {
        val repo = InMemoryTaskListRepository()
        val maxLengthText = "a".repeat(TaskListRepository.MAX_TASK_TEXT_LENGTH)
        val task = repo.addTask("list34", maxLengthText)
        task.text shouldBe maxLengthText
    }

    test("addTask trims whitespace from input") {
        val repo = InMemoryTaskListRepository()
        val task = repo.addTask("list35", "  Task with spaces  ")
        task.text shouldBe "Task with spaces"
    }

    test("addTask accepts valid text within length limits") {
        val repo = InMemoryTaskListRepository()
        val validText = "This is a valid task"
        val task = repo.addTask("list36", validText)
        task.text shouldBe validText
    }

    // Due Date Tests

    test("updateTaskDueDate sets due date and reminder type") {
        val repo = InMemoryTaskListRepository()
        val task = repo.addTask("list37", "Task with due date")
        val tomorrow = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis

        repo.updateTaskDueDate("list37", task.id, tomorrow, ReminderType.ON_DUE_DATE)

        val updated = repo.observeTasks("list37").first().first()
        updated.dueDate shouldBe tomorrow
        updated.reminderType shouldBe ReminderType.ON_DUE_DATE
    }

    test("updateTaskDueDate can clear due date") {
        val repo = InMemoryTaskListRepository()
        val task = repo.addTask("list38", "Task")
        val tomorrow = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis

        repo.updateTaskDueDate("list38", task.id, tomorrow, ReminderType.ONE_DAY_BEFORE)
        repo.updateTaskDueDate("list38", task.id, null, ReminderType.NONE)

        val updated = repo.observeTasks("list38").first().first()
        updated.dueDate shouldBe null
        updated.reminderType shouldBe ReminderType.NONE
    }

    test("updateTaskDueDate clearing date also clears reminder") {
        val repo = InMemoryTaskListRepository()
        val task = repo.addTask("list39", "Task")
        val tomorrow = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis

        repo.updateTaskDueDate("list39", task.id, tomorrow, ReminderType.ONE_WEEK_BEFORE)
        repo.updateTaskDueDate("list39", task.id, null, ReminderType.ON_DUE_DATE) // Try to set reminder without date

        val updated = repo.observeTasks("list39").first().first()
        updated.dueDate shouldBe null
        updated.reminderType shouldBe ReminderType.NONE // Should be NONE, not ON_DUE_DATE
    }

    test("updateTaskDueDate rejects past dates") {
        val repo = InMemoryTaskListRepository()
        val task = repo.addTask("list40", "Task")
        val yesterday = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }.timeInMillis

        shouldThrow<IllegalArgumentException> {
            repo.updateTaskDueDate("list40", task.id, yesterday, ReminderType.ON_DUE_DATE)
        }
    }

    test("updateTaskDueDate allows today as due date") {
        val repo = InMemoryTaskListRepository()
        val task = repo.addTask("list41", "Task")
        val todayNoon = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        repo.updateTaskDueDate("list41", task.id, todayNoon, ReminderType.ON_DUE_DATE)

        val updated = repo.observeTasks("list41").first().first()
        updated.dueDate shouldBe todayNoon
    }

    test("new tasks have null due date by default") {
        val repo = InMemoryTaskListRepository()
        val task = repo.addTask("list42", "Task")

        task.dueDate shouldBe null
        task.reminderType shouldBe ReminderType.NONE
    }
})
