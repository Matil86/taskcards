package de.hipp.app.taskcards.ui.app

import de.hipp.app.taskcards.model.TaskList
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class DefaultListSelectionTest : StringSpec({

    "selectDefaultList with 0 lists returns null (new list needed)" {
        val result = selectDefaultList(emptyList(), null)
        result shouldBe null
    }

    "selectDefaultList with 1 list returns that list's id" {
        val list = TaskList(id = "uuid-1", name = "My Tasks", createdAt = 0L, lastModifiedAt = 0L)
        val result = selectDefaultList(listOf(list), null)
        result shouldBe "uuid-1"
    }

    "selectDefaultList with 2 lists and valid stored id returns stored id" {
        val list1 = TaskList(id = "uuid-1", name = "List 1", createdAt = 0L, lastModifiedAt = 0L)
        val list2 = TaskList(id = "uuid-2", name = "List 2", createdAt = 0L, lastModifiedAt = 0L)
        val result = selectDefaultList(listOf(list1, list2), "uuid-2")
        result shouldBe "uuid-2"
    }

    "selectDefaultList with 2 lists and invalid stored id returns most recently modified" {
        val older = TaskList(id = "uuid-1", name = "Old", createdAt = 0L, lastModifiedAt = 1000L)
        val newer = TaskList(id = "uuid-2", name = "New", createdAt = 0L, lastModifiedAt = 2000L)
        val result = selectDefaultList(listOf(older, newer), "uuid-invalid")
        result shouldBe "uuid-2"
    }
})
