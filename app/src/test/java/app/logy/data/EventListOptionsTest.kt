package app.logy.data

import org.junit.Assert.assertEquals
import org.junit.Test

class EventListOptionsTest {
    private val events = listOf(
        summary(1, "Zoo", "Personal", 300),
        summary(2, "alpha", "Work", 100),
        summary(3, "Beta", "Work", null),
        summary(4, "Delta", "Personal", 200)
    )

    @Test
    fun oldestLoggedIsDefaultOrderAndNeverLoggedComesFirst() {
        assertEquals(
            listOf(3L, 2L, 4L, 1L),
            applyEventListOptions(events, null, EventSort.OLDEST_LOGGED).map { it.id }
        )
    }

    @Test
    fun newestLoggedPlacesNeverLoggedLast() {
        assertEquals(
            listOf(1L, 4L, 2L, 3L),
            applyEventListOptions(events, null, EventSort.NEWEST_LOGGED).map { it.id }
        )
    }

    @Test
    fun filtersByCategoryAndSortsNamesIgnoringCase() {
        assertEquals(
            listOf(2L, 3L),
            applyEventListOptions(events, "Work", EventSort.NAME).map { it.id }
        )
    }

    @Test
    fun dateRangeKeepsEventsThatHaveNoLogs() {
        val neverLogged = summary(5, "Never", "Personal", null)
        assertEquals(true, includeEventForDateRange(neverLogged, hasDateRange = true, hasLogsInRange = false))
        assertEquals(false, includeEventForDateRange(events.first().copy(entryCount = 1), hasDateRange = true, hasLogsInRange = false))
    }

    @Test
    fun searchSortingUsesNewestMatchingLogInsteadOfUnrelatedEventLog() {
        val adjusted = applySearchResultTimestamps(
            events,
            mapOf(
                1L to listOf(EventEntry(id = 10, eventId = 1, occurredAt = 50, note = "match")),
                2L to listOf(EventEntry(id = 11, eventId = 2, occurredAt = 250, note = "match"))
            )
        )

        assertEquals(listOf(2L, 1L, 3L, 4L), applyEventListOptions(adjusted, null, EventSort.NEWEST_LOGGED).map { it.id })
    }

    private fun summary(id: Long, name: String, category: String, lastEntry: Long?) = EventSummary(
        id = id,
        name = name,
        category = category,
        color = 0,
        createdAt = 0,
        lastEntry = lastEntry,
        lastNote = null,
        entryCount = 0
    )
}
