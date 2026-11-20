package app.logy.data

import java.util.Locale

enum class EventSort(val label: String) {
    OLDEST_LOGGED("Oldest logged"),
    NEWEST_LOGGED("Newest logged"),
    NAME("Name")
}

fun includeEventForDateRange(event: EventSummary, hasDateRange: Boolean, hasLogsInRange: Boolean): Boolean =
    !hasDateRange || event.entryCount == 0 || hasLogsInRange

fun applySearchResultTimestamps(
    events: List<EventSummary>,
    matchingLogs: Map<Long, List<EventEntry>>
): List<EventSummary> = events.map { event ->
    val newestMatch = matchingLogs[event.id].orEmpty().maxWithOrNull(
        compareBy<EventEntry> { it.occurredAt }.thenBy { it.id }
    )
    event.copy(lastEntry = newestMatch?.occurredAt, lastNote = newestMatch?.note)
}

fun applyEventListOptions(
    events: List<EventSummary>,
    category: String?,
    sort: EventSort
): List<EventSummary> {
    val filtered = if (category == null) events else events.filter { it.category == category }
    val nameComparator = compareBy<EventSummary> { it.name.lowercase(Locale.ROOT) }.thenBy { it.id }
    return when (sort) {
        EventSort.OLDEST_LOGGED -> filtered.sortedWith(
            compareBy<EventSummary> { it.lastEntry ?: Long.MIN_VALUE }
                .then(nameComparator)
        )
        EventSort.NEWEST_LOGGED -> filtered.sortedWith(
            compareByDescending<EventSummary> { it.lastEntry ?: Long.MIN_VALUE }
                .then(nameComparator)
        )
        EventSort.NAME -> filtered.sortedWith(nameComparator)
    }
}
