package app.logy.data

import java.io.Reader

object CsvBackup {
    private val header = listOf(
        "record_type", "event_id", "event_name", "category", "color",
        "event_created_at", "entry_id", "occurred_at", "note"
    )

    fun export(events: List<TrackedEvent>, entries: List<EventEntry>): String = buildString {
        appendLine(header.joinToString(","))
        events.forEach { event ->
            appendLine(listOf(
                "event", event.id, event.name, event.category, event.color,
                event.createdAt, "", "", ""
            ).joinToString(",") { escape(it.toString()) })
        }
        entries.forEach { entry ->
            appendLine(listOf(
                "entry", entry.eventId, "", "", "", "", entry.id,
                entry.occurredAt, entry.note
            ).joinToString(",") { escape(it.toString()) })
        }
    }

    fun import(reader: Reader): Pair<List<TrackedEvent>, List<EventEntry>> {
        val rows = parse(reader.readText())
        require(rows.isNotEmpty() && rows.first() == header) { "This is not a Logy CSV backup." }
        val events = mutableListOf<TrackedEvent>()
        val entries = mutableListOf<EventEntry>()
        rows.drop(1).filter { it.any(String::isNotBlank) }.forEachIndexed { index, row ->
            require(row.size == header.size) { "Invalid CSV row ${index + 2}." }
            when (row[0]) {
                "event" -> events += TrackedEvent(
                    id = row[1].toLong(), name = row[2], category = row[3],
                    color = row[4].toLong(), createdAt = row[5].toLong()
                )
                "entry" -> entries += EventEntry(
                    id = row[6].toLong(), eventId = row[1].toLong(),
                    occurredAt = row[7].toLong(), note = row[8]
                )
                else -> error("Unknown record type on row ${index + 2}.")
            }
        }
        val ids = events.map { it.id }.toSet()
        require(entries.all { it.eventId in ids }) { "Backup contains an entry without its event." }
        return events to entries
    }

    private fun escape(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' })
            "\"${value.replace("\"", "\"\"")}\"" else value

    private fun parse(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var quoted = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                quoted && c == '"' && i + 1 < text.length && text[i + 1] == '"' -> {
                    field.append('"'); i++
                }
                c == '"' -> quoted = !quoted
                !quoted && c == ',' -> { row += field.toString(); field.clear() }
                !quoted && (c == '\n' || c == '\r') -> {
                    if (c == '\r' && i + 1 < text.length && text[i + 1] == '\n') i++
                    row += field.toString(); field.clear(); rows += row; row = mutableListOf()
                }
                else -> field.append(c)
            }
            i++
        }
        require(!quoted) { "Unclosed quoted field in CSV." }
        if (field.isNotEmpty() || row.isNotEmpty()) { row += field.toString(); rows += row }
        return rows
    }
}
