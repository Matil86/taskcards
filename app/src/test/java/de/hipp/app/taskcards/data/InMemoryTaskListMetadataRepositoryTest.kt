package de.hipp.app.taskcards.data

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher

@OptIn(ExperimentalCoroutinesApi::class)
class InMemoryTaskListMetadataRepositoryTest : FunSpec({

    test("initializes with default list 'My Tasks'") {
        val repo = InMemoryTaskListMetadataRepository(UnconfinedTestDispatcher())
        val lists = repo.observeTaskLists().first()

        lists shouldHaveSize 1
        lists.first().name shouldBe "My Tasks"
    }

    test("creates new list with unique ID") {
        val repo = InMemoryTaskListMetadataRepository(UnconfinedTestDispatcher())
        val listId = repo.createList("Shopping List")

        listId.shouldNotBeNull()
        val lists = repo.observeTaskLists().first()
        lists shouldHaveSize 2
        lists.any { it.id == listId && it.name == "Shopping List" } shouldBe true
    }

    test("renames existing list") {
        val repo = InMemoryTaskListMetadataRepository(UnconfinedTestDispatcher())
        val listId = repo.createList("Old Name")
        repo.renameList(listId, "New Name")

        val lists = repo.observeTaskLists().first()
        val renamedList = lists.first { it.id == listId }
        renamedList.name shouldBe "New Name"
    }

    test("deletes existing list") {
        val repo = InMemoryTaskListMetadataRepository(UnconfinedTestDispatcher())
        val listId = repo.createList("To Delete")
        repo.deleteList(listId)

        val lists = repo.observeTaskLists().first()
        lists.none { it.id == listId } shouldBe true
    }

    test("getDefaultListId returns first list by creation time") {
        val repo = InMemoryTaskListMetadataRepository(UnconfinedTestDispatcher())
        val defaultId = repo.getDefaultListId()

        defaultId.shouldNotBeNull()
        val lists = repo.observeTaskLists().first()
        lists.first().id shouldBe defaultId
    }

    test("returns null for default list ID when no lists exist") {
        val repo = InMemoryTaskListMetadataRepository(UnconfinedTestDispatcher())

        // Delete the default list
        val defaultId = repo.getDefaultListId()
        if (defaultId != null) {
            repo.deleteList(defaultId)
        }

        repo.getDefaultListId().shouldBeNull()
    }

    test("throws error when creating list with empty name") {
        val repo = InMemoryTaskListMetadataRepository(UnconfinedTestDispatcher())

        shouldThrow<IllegalArgumentException> {
            repo.createList("")
        }
    }

    test("throws error when creating list with whitespace-only name") {
        val repo = InMemoryTaskListMetadataRepository(UnconfinedTestDispatcher())

        shouldThrow<IllegalArgumentException> {
            repo.createList("   ")
        }
    }

    test("throws error when creating list with name exceeding 100 characters") {
        val repo = InMemoryTaskListMetadataRepository(UnconfinedTestDispatcher())
        val longName = "a".repeat(101)

        shouldThrow<IllegalArgumentException> {
            repo.createList(longName)
        }
    }

    test("throws error when renaming to empty name") {
        val repo = InMemoryTaskListMetadataRepository(UnconfinedTestDispatcher())
        val listId = repo.createList("Valid Name")

        shouldThrow<IllegalArgumentException> {
            repo.renameList(listId, "")
        }
    }

    test("throws error when renaming non-existent list") {
        val repo = InMemoryTaskListMetadataRepository(UnconfinedTestDispatcher())

        shouldThrow<IllegalArgumentException> {
            repo.renameList("non-existent-id", "New Name")
        }
    }

    test("throws error when deleting non-existent list") {
        val repo = InMemoryTaskListMetadataRepository(UnconfinedTestDispatcher())

        shouldThrow<IllegalArgumentException> {
            repo.deleteList("non-existent-id")
        }
    }

    test("lists are sorted by creation time") {
        val repo = InMemoryTaskListMetadataRepository(UnconfinedTestDispatcher())
        val id1 = repo.createList("First")
        Thread.sleep(10) // Ensure different timestamps
        val id2 = repo.createList("Second")
        Thread.sleep(10)
        val id3 = repo.createList("Third")

        val lists = repo.observeTaskLists().first()
        lists.map { it.id } shouldContainExactly listOf(
            lists.first { it.name == "My Tasks" }.id,
            id1,
            id2,
            id3
        )
    }

    test("updates lastModifiedAt when renaming") {
        val repo = InMemoryTaskListMetadataRepository(UnconfinedTestDispatcher())
        val listId = repo.createList("Original")

        val originalList = repo.observeTaskLists().first().first { it.id == listId }
        val originalModified = originalList.lastModifiedAt

        Thread.sleep(50) // Ensure timestamp difference (increased from 10ms to 50ms for reliability)
        repo.renameList(listId, "Modified")

        val updatedList = repo.observeTaskLists().first().first { it.id == listId }
        // Verify that lastModifiedAt has been updated (should be greater than original)
        updatedList.lastModifiedAt shouldBe updatedList.lastModifiedAt // Always passes - verify structure
        (updatedList.lastModifiedAt > originalModified) shouldBe true // Strict check for actual update
    }

    test("trims whitespace from list names") {
        val repo = InMemoryTaskListMetadataRepository(UnconfinedTestDispatcher())
        val listId = repo.createList("  Trimmed Name  ")

        val lists = repo.observeTaskLists().first()
        val createdList = lists.first { it.id == listId }
        createdList.name shouldBe "Trimmed Name"
    }

    test("multiple lists can have same name") {
        val repo = InMemoryTaskListMetadataRepository(UnconfinedTestDispatcher())
        val id1 = repo.createList("Same Name")
        val id2 = repo.createList("Same Name")

        val lists = repo.observeTaskLists().first()
        lists.count { it.name == "Same Name" } shouldBe 2
        id1 shouldBe id1 // Different IDs
        id2 shouldBe id2
        (id1 != id2) shouldBe true
    }

    test("thread-safe concurrent operations") {
        runBlocking {
            val repo = InMemoryTaskListMetadataRepository(UnconfinedTestDispatcher())
            val scope = CoroutineScope(UnconfinedTestDispatcher())
            val jobs = mutableListOf<kotlinx.coroutines.Job>()

            // Launch 10 concurrent create operations
            repeat(10) { i ->
                jobs.add(
                    scope.launch {
                        repo.createList("List $i")
                    }
                )
            }

            jobs.forEach { it.join() }

            val lists = repo.observeTaskLists().first()

            // Should have default list + 10 created lists
            lists shouldHaveSize 11
        }
    }
})
