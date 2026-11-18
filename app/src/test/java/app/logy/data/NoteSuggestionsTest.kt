package app.logy.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteSuggestionsTest {
    @Test
    fun returnsDistinctMatchesWithLatestFirst() {
        val entries = listOf(
            EventEntry(id = 1, eventId = 7, occurredAt = 100, note = "Morning walk"),
            EventEntry(id = 2, eventId = 7, occurredAt = 300, note = "Evening walk"),
            EventEntry(id = 3, eventId = 7, occurredAt = 200, note = "MORNING WALK"),
            EventEntry(id = 4, eventId = 7, occurredAt = 400, note = "Read a book")
        )

        assertEquals(listOf("Evening walk", "MORNING WALK"), matchingNoteSuggestions(entries, "walk"))
    }

    @Test
    fun hidesSuggestionsForBlankOrExactInput() {
        val entries = listOf(EventEntry(id = 1, eventId = 7, occurredAt = 100, note = "Morning walk"))
        assertTrue(matchingNoteSuggestions(entries, "").isEmpty())
        assertTrue(matchingNoteSuggestions(entries, "morning walk").isEmpty())
    }

    @Test
    fun limitsSuggestionsToFiveMostRecentMatches() {
        val entries = (1L..7L).map { id ->
            EventEntry(id = id, eventId = id, occurredAt = id * 100, note = "Shared note $id")
        }

        assertEquals(
            listOf("Shared note 7", "Shared note 6", "Shared note 5", "Shared note 4", "Shared note 3"),
            matchingNoteSuggestions(entries, "shared")
        )
    }
}
