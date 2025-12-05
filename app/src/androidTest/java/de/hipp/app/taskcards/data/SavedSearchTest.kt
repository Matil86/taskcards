package de.hipp.app.taskcards.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import de.hipp.app.taskcards.model.DueDateRange
import de.hipp.app.taskcards.model.SavedSearch
import de.hipp.app.taskcards.model.SearchFilter
import de.hipp.app.taskcards.model.StatusFilter
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.File

/**
 * Instrumented tests for SavedSearch functionality in PreferencesRepository.
 * Tests CRUD operations on saved searches with DataStore persistence.
 */
class SavedSearchTest : StringSpec({
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

    "initially no saved searches exist" {
        runTest {
            val searches = repo.savedSearches.first()
            searches.shouldBeEmpty()
        }
    }

    "saveSavedSearch adds a new search" {
        runTest {
            val search = SavedSearch(
                id = "search-1",
                name = "Work Tasks",
                listId = "list-1",
                filter = SearchFilter(
                    textQuery = "meeting",
                    statusFilter = StatusFilter.ACTIVE_ONLY,
                    dueDateRange = null
                )
            )

            repo.saveSavedSearch(search)

            val searches = repo.savedSearches.first()
            searches shouldHaveSize 1
            searches[0].name shouldBe "Work Tasks"
            searches[0].filter.textQuery shouldBe "meeting"
        }
    }

    "getSavedSearchesForList filters by list ID" {
        runTest {
            val search1 = SavedSearch(
                id = "search-1",
                name = "List 1 Search",
                listId = "list-1",
                filter = SearchFilter()
            )
            val search2 = SavedSearch(
                id = "search-2",
                name = "List 2 Search",
                listId = "list-2",
                filter = SearchFilter()
            )

            repo.saveSavedSearch(search1)
            repo.saveSavedSearch(search2)

            val list1Searches = repo.getSavedSearchesForList("list-1").first()
            val list2Searches = repo.getSavedSearchesForList("list-2").first()

            list1Searches shouldHaveSize 1
            list1Searches[0].name shouldBe "List 1 Search"

            list2Searches shouldHaveSize 1
            list2Searches[0].name shouldBe "List 2 Search"
        }
    }

    "deleteSavedSearch removes the search" {
        runTest {
            val search = SavedSearch(
                id = "search-to-delete",
                name = "Temporary Search",
                listId = "list-1",
                filter = SearchFilter(textQuery = "test")
            )

            repo.saveSavedSearch(search)
            val searchesBefore = repo.savedSearches.first()
            searchesBefore shouldHaveSize 1

            repo.deleteSavedSearch("search-to-delete")

            val searchesAfter = repo.savedSearches.first()
            searchesAfter.shouldBeEmpty()
        }
    }

    "deleteSavedSearch removes only the specified search" {
        runTest {
            val search1 = SavedSearch(
                id = "search-1",
                name = "Keep This",
                listId = "list-1",
                filter = SearchFilter()
            )
            val search2 = SavedSearch(
                id = "search-2",
                name = "Delete This",
                listId = "list-1",
                filter = SearchFilter()
            )

            repo.saveSavedSearch(search1)
            repo.saveSavedSearch(search2)

            repo.deleteSavedSearch("search-2")

            val searches = repo.savedSearches.first()
            searches shouldHaveSize 1
            searches[0].id shouldBe "search-1"
            searches[0].name shouldBe "Keep This"
        }
    }

    "updateSavedSearch modifies existing search" {
        runTest {
            val originalSearch = SavedSearch(
                id = "search-1",
                name = "Original Name",
                listId = "list-1",
                filter = SearchFilter(textQuery = "original")
            )

            repo.saveSavedSearch(originalSearch)

            val updatedSearch = originalSearch.copy(
                name = "Updated Name",
                filter = SearchFilter(textQuery = "updated")
            )

            repo.updateSavedSearch(updatedSearch)

            val searches = repo.savedSearches.first()
            searches shouldHaveSize 1
            searches[0].name shouldBe "Updated Name"
            searches[0].filter.textQuery shouldBe "updated"
        }
    }

    "updateSavedSearch throws error for non-existent search" {
        runTest {
            val search = SavedSearch(
                id = "non-existent",
                name = "Does Not Exist",
                listId = "list-1",
                filter = SearchFilter()
            )

            shouldThrow<IllegalArgumentException> {
                repo.updateSavedSearch(search)
            }
        }
    }

    "saveSavedSearch enforces max 10 searches per list" {
        runTest {
            // Add 10 searches
            repeat(10) { index ->
                repo.saveSavedSearch(
                    SavedSearch(
                        id = "search-$index",
                        name = "Search $index",
                        listId = "list-1",
                        filter = SearchFilter()
                    )
                )
            }

            // Attempt to add 11th search should fail
            shouldThrow<IllegalArgumentException> {
                repo.saveSavedSearch(
                    SavedSearch(
                        id = "search-11",
                        name = "Search 11",
                        listId = "list-1",
                        filter = SearchFilter()
                    )
                )
            }
        }
    }

    "max search limit is per-list, not global" {
        runTest {
            // Add 10 searches to list-1
            repeat(10) { index ->
                repo.saveSavedSearch(
                    SavedSearch(
                        id = "list1-search-$index",
                        name = "List 1 Search $index",
                        listId = "list-1",
                        filter = SearchFilter()
                    )
                )
            }

            // Should still be able to add to list-2
            repo.saveSavedSearch(
                SavedSearch(
                    id = "list2-search-1",
                    name = "List 2 Search 1",
                    listId = "list-2",
                    filter = SearchFilter()
                )
            )

            val list1Searches = repo.getSavedSearchesForList("list-1").first()
            val list2Searches = repo.getSavedSearchesForList("list-2").first()

            list1Searches shouldHaveSize 10
            list2Searches shouldHaveSize 1
        }
    }

    "saved searches persist with complex filters" {
        runTest {
            val search = SavedSearch(
                id = "complex-search",
                name = "Complex Filter",
                listId = "list-1",
                filter = SearchFilter(
                    textQuery = "urgent meeting",
                    statusFilter = StatusFilter.ACTIVE_ONLY,
                    dueDateRange = DueDateRange(
                        start = 1000L,
                        end = 2000L,
                        displayName = "This Week"
                    )
                )
            )

            repo.saveSavedSearch(search)

            val searches = repo.savedSearches.first()
            searches shouldHaveSize 1

            val retrieved = searches[0]
            retrieved.name shouldBe "Complex Filter"
            retrieved.filter.textQuery shouldBe "urgent meeting"
            retrieved.filter.statusFilter shouldBe StatusFilter.ACTIVE_ONLY
            retrieved.filter.dueDateRange shouldNotBe null
            retrieved.filter.dueDateRange?.start shouldBe 1000L
            retrieved.filter.dueDateRange?.end shouldBe 2000L
            retrieved.filter.dueDateRange?.displayName shouldBe "This Week"
        }
    }

    "deleteSavedSearch is idempotent (no error if not found)" {
        runTest {
            // Deleting non-existent search should not throw
            repo.deleteSavedSearch("non-existent-id")

            val searches = repo.savedSearches.first()
            searches.shouldBeEmpty()
        }
    }

    "saved searches persist across repository instances" {
        runTest {
            val search = SavedSearch(
                id = "persistent-search",
                name = "Persistent",
                listId = "list-1",
                filter = SearchFilter(textQuery = "persist")
            )

            repo.saveSavedSearch(search)

            // Create new repository instance
            val newRepo = PreferencesRepositoryImpl(context)
            val searches = newRepo.savedSearches.first()

            searches shouldHaveSize 1
            searches[0].name shouldBe "Persistent"
        }
    }
})
