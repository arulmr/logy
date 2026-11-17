package app.logy.data

import java.util.Locale

enum class EventSort(val label: String) {
    OLDEST_LOGGED("Oldest logged"),
    NEWEST_LOGGED("Newest logged"),
    NAME("Name")
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
