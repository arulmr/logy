package app.logy.data

fun matchingNoteSuggestions(entries: List<EventEntry>, query: String, limit: Int = 6): List<String> {
    val value = query.trim()
    if (value.isEmpty()) return emptyList()
    return entries.asSequence()
        .filter { it.note.isNotBlank() && it.note.contains(value, ignoreCase = true) }
        .sortedWith(compareByDescending<EventEntry> { it.occurredAt }.thenByDescending { it.id })
        .map { it.note.trim() }
        .distinctBy { it.lowercase() }
        .filterNot { it.equals(value, ignoreCase = true) }
        .take(limit)
        .toList()
}
