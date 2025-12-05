package de.hipp.app.taskcards.data.preferences

import de.hipp.app.taskcards.model.DueDateRange
import de.hipp.app.taskcards.model.SavedSearch
import de.hipp.app.taskcards.model.SearchFilter
import de.hipp.app.taskcards.model.StatusFilter

/**
 * Utility object for serializing and deserializing SavedSearch objects to/from strings.
 * Used for DataStore persistence of saved searches.
 */
object SavedSearchSerializer {
    private const val FIELD_SEPARATOR = "|"
    private const val LIST_SEPARATOR = "|||"

    /**
     * Serializes a list of SavedSearch objects into a string for DataStore storage.
     * Format: id|name|listId|createdAt|textQuery|dueDateRangeData|statusFilter|||id|name|...
     */
    fun serialize(searches: List<SavedSearch>): String {
        return searches.joinToString(LIST_SEPARATOR) { search ->
            val filter = search.filter
            val dueDateRangeStr = filter.dueDateRange?.let { range ->
                "${range.start ?: "null"},${range.end ?: "null"},${range.displayName}"
            } ?: "null"

            listOf(
                search.id,
                search.name.replace(FIELD_SEPARATOR, "_"), // Escape separator
                search.listId,
                search.createdAt.toString(),
                filter.textQuery.replace(FIELD_SEPARATOR, "_"), // Escape separator
                dueDateRangeStr,
                filter.statusFilter.name
            ).joinToString(FIELD_SEPARATOR)
        }
    }

    /**
     * Deserializes a string from DataStore into a list of SavedSearch objects.
     * Returns empty list if string is blank or parsing fails.
     */
    fun deserialize(serialized: String): List<SavedSearch> {
        if (serialized.isBlank()) return emptyList()

        return try {
            serialized.split(LIST_SEPARATOR).mapNotNull { searchStr ->
                if (searchStr.isBlank()) return@mapNotNull null

                val parts = searchStr.split(FIELD_SEPARATOR)
                if (parts.size != 7) return@mapNotNull null

                val id = parts[0]
                val name = parts[1]
                val listId = parts[2]
                val createdAt = parts[3].toLongOrNull() ?: System.currentTimeMillis()
                val textQuery = parts[4]

                val dueDateRange = if (parts[5] != "null") {
                    val rangeParts = parts[5].split(",")
                    if (rangeParts.size == 3) {
                        DueDateRange(
                            start = if (rangeParts[0] == "null") null else rangeParts[0].toLongOrNull(),
                            end = if (rangeParts[1] == "null") null else rangeParts[1].toLongOrNull(),
                            displayName = rangeParts[2]
                        )
                    } else null
                } else null

                val statusFilter = try {
                    StatusFilter.valueOf(parts[6])
                } catch (e: Exception) {
                    StatusFilter.ACTIVE_ONLY
                }

                SavedSearch(
                    id = id,
                    name = name,
                    listId = listId,
                    createdAt = createdAt,
                    filter = SearchFilter(
                        textQuery = textQuery,
                        dueDateRange = dueDateRange,
                        statusFilter = statusFilter
                    )
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
