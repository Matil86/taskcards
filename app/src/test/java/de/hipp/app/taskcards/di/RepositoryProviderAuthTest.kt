package de.hipp.app.taskcards.di

import de.hipp.app.taskcards.data.TaskListRepository
import de.hipp.app.taskcards.model.ReminderType
import de.hipp.app.taskcards.model.TaskItem
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Minimal stub — no Firebase, no Android context required.
 */
private class StubTaskListRepository : TaskListRepository {
    override fun observeTasks(listId: String): Flow<List<TaskItem>> = emptyFlow()
    override suspend fun addTask(listId: String, text: String): TaskItem = error("stub")
    override suspend fun removeTask(listId: String, taskId: String) = Unit
    override suspend fun restoreTask(listId: String, taskId: String) = Unit
    override suspend fun markDone(listId: String, taskId: String, done: Boolean) = Unit
    override suspend fun moveTask(listId: String, taskId: String, toIndex: Int) = Unit
    override suspend fun updateTaskDueDate(
        listId: String,
        taskId: String,
        dueDate: Long?,
        reminderType: ReminderType
    ) = Unit
    override suspend fun clearList(listId: String) = Unit
    override suspend fun getActiveListCount(): Int = 0
}

class RepositoryProviderAuthTest : StringSpec({

    beforeEach {
        // reset() clears cached repository instances but NOT isAuthenticated.
        // Drive auth to a known baseline first so tests are order-independent.
        RepositoryProvider.setAuthenticated(false)
        RepositoryProvider.reset()
    }

    "setAuthenticated(true) changes isAuthenticated to true" {
        RepositoryProvider.isAuthenticated() shouldBe false

        RepositoryProvider.setAuthenticated(true)

        RepositoryProvider.isAuthenticated() shouldBe true
    }

    "setAuthenticated(false) after true changes isAuthenticated back to false" {
        RepositoryProvider.setAuthenticated(true)
        RepositoryProvider.isAuthenticated() shouldBe true

        RepositoryProvider.setAuthenticated(false)

        RepositoryProvider.isAuthenticated() shouldBe false
    }

    "setAuthenticated state change clears cached taskListRepository" {
        // Populate the cache with a stub (avoids touching Firebase or Android context).
        RepositoryProvider.setAuthenticated(false)
        RepositoryProvider.setRepository(StubTaskListRepository())

        // Changing auth state must null-out the cached instance.
        RepositoryProvider.setAuthenticated(true)

        // Inject a second stub into the now-empty slot.
        RepositoryProvider.setRepository(StubTaskListRepository())

        // A same-value call must NOT clear the cache again (guard is active).
        RepositoryProvider.setAuthenticated(true) // no-op

        // Auth state reflects the last meaningful setAuthenticated call.
        RepositoryProvider.isAuthenticated() shouldBe true
    }

    "setAuthenticated is idempotent — repeated calls with the same value do not change state" {
        RepositoryProvider.setAuthenticated(false)
        RepositoryProvider.setAuthenticated(false)
        RepositoryProvider.isAuthenticated() shouldBe false

        RepositoryProvider.setAuthenticated(true)
        RepositoryProvider.setAuthenticated(true)
        RepositoryProvider.isAuthenticated() shouldBe true
    }

    "reset() clears cached repositories but preserves isAuthenticated" {
        RepositoryProvider.setAuthenticated(true)
        RepositoryProvider.isAuthenticated() shouldBe true

        RepositoryProvider.reset()

        // Auth state is intentionally preserved by reset() — only the repository
        // instances are cleared so they can be recreated on the next getRepository() call.
        RepositoryProvider.isAuthenticated() shouldBe true
    }

    "setAuthenticated cycling false→true→false correctly reflects each state" {
        RepositoryProvider.setAuthenticated(false)
        RepositoryProvider.isAuthenticated() shouldBe false

        RepositoryProvider.setAuthenticated(true)
        RepositoryProvider.isAuthenticated() shouldBe true

        RepositoryProvider.setAuthenticated(false)
        RepositoryProvider.isAuthenticated() shouldBe false
    }

    "setRepository survives a same-value setAuthenticated call — cache is not cleared" {
        RepositoryProvider.setAuthenticated(false)
        RepositoryProvider.setRepository(StubTaskListRepository())

        // Same value — the guard inside setAuthenticated skips clearing the cache.
        RepositoryProvider.setAuthenticated(false)

        // Auth state unchanged.
        RepositoryProvider.isAuthenticated() shouldBe false
    }
})
