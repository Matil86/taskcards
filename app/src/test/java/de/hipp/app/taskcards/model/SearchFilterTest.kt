package de.hipp.app.taskcards.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.util.Calendar

class SearchFilterTest : FunSpec({

    test("empty filter returns true for isEmpty()") {
        val filter = SearchFilter()
        filter.isEmpty() shouldBe true
    }

    test("filter with text query is not empty") {
        val filter = SearchFilter(textQuery = "test")
        filter.isEmpty() shouldBe false
    }

    test("filter with due date range is not empty") {
        val filter = SearchFilter(dueDateRange = DueDateRange.today())
        filter.isEmpty() shouldBe false
    }

    test("filter with non-default status is not empty") {
        val filter = SearchFilter(statusFilter = StatusFilter.ALL)
        filter.isEmpty() shouldBe false
    }

    test("getActiveFilterCount returns correct count") {
        val filter = SearchFilter(
            textQuery = "test",
            dueDateRange = DueDateRange.today(),
            statusFilter = StatusFilter.ALL
        )
        // 1 for text + 1 for date range + 1 for status
        filter.getActiveFilterCount() shouldBe 3
    }

    test("getActiveFilterCount ignores default status filter") {
        val filter = SearchFilter(
            textQuery = "test",
            statusFilter = StatusFilter.ACTIVE_ONLY // default
        )
        // 1 for text, status not counted as it's default
        filter.getActiveFilterCount() shouldBe 1
    }

    test("DueDateRange.today() creates range for current day") {
        val range = DueDateRange.today()
        range.displayName shouldBe "Today"
        range.start shouldNotBe null
        range.end shouldNotBe null

        // Verify start is beginning of today
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = range.start!!
        calendar.get(Calendar.HOUR_OF_DAY) shouldBe 0
        calendar.get(Calendar.MINUTE) shouldBe 0

        // Verify end is end of today
        calendar.timeInMillis = range.end!!
        calendar.get(Calendar.HOUR_OF_DAY) shouldBe 23
        calendar.get(Calendar.MINUTE) shouldBe 59
    }

    test("DueDateRange.overdue() has no start limit") {
        val range = DueDateRange.overdue()
        range.displayName shouldBe "Overdue"
        range.start shouldBe null
        range.end shouldNotBe null
    }

    test("DueDateRange.thisWeek() covers 7 days") {
        val range = DueDateRange.thisWeek()
        range.displayName shouldBe "This Week"
        range.start shouldNotBe null
        range.end shouldNotBe null

        val daysDiff = (range.end!! - range.start!!) / (1000 * 60 * 60 * 24)
        daysDiff shouldBe 7
    }

    test("DueDateRange.thisMonth() covers 30 days") {
        val range = DueDateRange.thisMonth()
        range.displayName shouldBe "Next 30 Days"
        range.start shouldNotBe null
        range.end shouldNotBe null

        val daysDiff = (range.end!! - range.start!!) / (1000 * 60 * 60 * 24)
        daysDiff shouldBe 30
    }

    test("StatusFilter enum has correct display names") {
        StatusFilter.ALL.displayName shouldBe "All Tasks"
        StatusFilter.ACTIVE_ONLY.displayName shouldBe "Active Only"
        StatusFilter.DONE_ONLY.displayName shouldBe "Completed Only"
        StatusFilter.REMOVED_ONLY.displayName shouldBe "Removed Only"
    }

    test("SavedSearch has default values") {
        val search = SavedSearch(
            name = "Test Search",
            filter = SearchFilter(textQuery = "test"),
            listId = "list-123"
        )
        search.id shouldNotBe ""
        search.createdAt shouldNotBe 0L
    }
})
