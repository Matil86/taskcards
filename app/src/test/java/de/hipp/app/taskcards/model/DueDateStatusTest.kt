package de.hipp.app.taskcards.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import java.util.Calendar

/**
 * Tests for DueDateStatus calculation logic.
 */
class DueDateStatusTest : FunSpec({

    test("null due date returns NO_DUE_DATE") {
        calculateDueDateStatus(null) shouldBe DueDateStatus.NO_DUE_DATE
    }

    test("past due date returns OVERDUE") {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1) // Yesterday
        val yesterday = calendar.timeInMillis

        calculateDueDateStatus(yesterday) shouldBe DueDateStatus.OVERDUE
    }

    test("due date today returns TODAY") {
        val calendar = Calendar.getInstance()
        // Set to noon today
        calendar.set(Calendar.HOUR_OF_DAY, 12)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val today = calendar.timeInMillis

        calculateDueDateStatus(today) shouldBe DueDateStatus.TODAY
    }

    test("due date within 7 days returns THIS_WEEK") {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 3) // 3 days from now
        val inThreeDays = calendar.timeInMillis

        calculateDueDateStatus(inThreeDays) shouldBe DueDateStatus.THIS_WEEK
    }

    test("due date exactly 7 days from now returns THIS_WEEK") {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 7)
        val inSevenDays = calendar.timeInMillis

        calculateDueDateStatus(inSevenDays) shouldBe DueDateStatus.THIS_WEEK
    }

    test("due date more than 7 days from now returns LATER") {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 14) // 2 weeks from now
        val inTwoWeeks = calendar.timeInMillis

        calculateDueDateStatus(inTwoWeeks) shouldBe DueDateStatus.LATER
    }

    test("sortPriority returns correct order") {
        DueDateStatus.OVERDUE.sortPriority() shouldBe 0
        DueDateStatus.TODAY.sortPriority() shouldBe 1
        DueDateStatus.THIS_WEEK.sortPriority() shouldBe 2
        DueDateStatus.LATER.sortPriority() shouldBe 3
        DueDateStatus.NO_DUE_DATE.sortPriority() shouldBe 4
    }

    test("overdue has highest priority") {
        val overduePriority = DueDateStatus.OVERDUE.sortPriority()
        val todayPriority = DueDateStatus.TODAY.sortPriority()
        val weekPriority = DueDateStatus.THIS_WEEK.sortPriority()
        val laterPriority = DueDateStatus.LATER.sortPriority()
        val noDueDatePriority = DueDateStatus.NO_DUE_DATE.sortPriority()

        overduePriority shouldBeLessThan todayPriority
        todayPriority shouldBeLessThan weekPriority
        weekPriority shouldBeLessThan laterPriority
        laterPriority shouldBeLessThan noDueDatePriority
    }
})
