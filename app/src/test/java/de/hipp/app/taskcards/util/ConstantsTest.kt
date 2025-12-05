package de.hipp.app.taskcards.util

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.util.UUID

/**
 * Tests for Constants utility object.
 * Focuses on testing UUID generation and validation for authenticated list IDs.
 */
class ConstantsTest : FunSpec({

    test("DEFAULT_LIST_ID is 'default-list'") {
        Constants.DEFAULT_LIST_ID shouldBe "default-list"
    }

    test("generateNewListId() generates valid UUID format") {
        val listId = Constants.generateNewListId()

        // Should be a valid UUID format (36 chars with hyphens)
        listId.length shouldBe 36

        // Should be parseable as UUID without throwing
        UUID.fromString(listId)
    }

    test("generateNewListId() generates unique IDs each time") {
        val listId1 = Constants.generateNewListId()
        val listId2 = Constants.generateNewListId()
        val listId3 = Constants.generateNewListId()

        // Each call should generate a different UUID
        listId1 shouldNotBe listId2
        listId2 shouldNotBe listId3
        listId1 shouldNotBe listId3
    }

    test("isUuidListId() returns true for valid UUID") {
        val validUuid = "550e8400-e29b-41d4-a716-446655440000"
        Constants.isUuidListId(validUuid) shouldBe true
    }

    test("isUuidListId() returns true for generated UUID") {
        val generatedId = Constants.generateNewListId()
        Constants.isUuidListId(generatedId) shouldBe true
    }

    test("isUuidListId() returns false for default list ID") {
        Constants.isUuidListId(Constants.DEFAULT_LIST_ID) shouldBe false
    }

    test("isUuidListId() returns false for invalid UUID formats") {
        Constants.isUuidListId("not-a-uuid") shouldBe false
        Constants.isUuidListId("user-123-list") shouldBe false
        Constants.isUuidListId("") shouldBe false
        Constants.isUuidListId("550e8400") shouldBe false // Too short
        Constants.isUuidListId("550e8400-e29b-41d4-a716") shouldBe false // Incomplete
    }

    test("isUuidListId() handles edge cases gracefully") {
        Constants.isUuidListId("   ") shouldBe false
        Constants.isUuidListId("550e8400-e29b-41d4-a716-446655440000-extra") shouldBe false
        Constants.isUuidListId("XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX") shouldBe false
    }

    test("generateNewListId() generates UUID v4 format") {
        val listId = Constants.generateNewListId()
        val uuid = UUID.fromString(listId)

        // UUID v4 has version 4 and variant 2
        uuid.version() shouldBe 4
        uuid.variant() shouldBe 2
    }
})
