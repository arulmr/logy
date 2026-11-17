package app.logy.data

import java.io.Reader

data class BackupData(
    val categories: List<EventCategory>,
    val events: List<TrackedEvent>,
    val entries: List<EventEntry>
)

object CsvBackup {
    private val v1Header = listOf(
        "record_type", "event_id", "event_name", "category", "color",
        "event_created_at", "entry_id", "occurred_at", "note"
    )
    private val header = listOf(
        "record_type", "event_id", "event_name", "category", "category_color",
        "event_color", "event_created_at", "entry_id", "occurred_at", "note", "duration_minutes"
    )

    fun export(categories: List<EventCategory>, events: List<TrackedEvent>, entries: List<EventEntry>): String = buildString {
        appendLine(header.joinToString(","))
        categories.forEach { category ->
            appendLine(row("category", "", "", category.name, category.color, "", "", "", "", "", ""))
        }
        events.forEach { event ->
            appendLine(row("event", event.id, event.name, event.category, "", event.color, event.createdAt, "", "", "", ""))
        }
        entries.forEach { entry ->
            appendLine(row("entry", entry.eventId, "", "", "", "", "", entry.id, entry.occurredAt, entry.note, entry.durationMinutes ?: ""))
        }
    }

    fun import(reader: Reader): BackupData {
        val rows = parse(reader.readText())
        require(rows.isNotEmpty()) { "This CSV file is empty." }
        return when (rows.first()) {
            header -> importV2(rows)
            v1Header -> importV1(rows)
            else -> error("This is not a supported Logy CSV backup.")
        }
    }

    private fun importV2(rows: List<List<String>>): BackupData {
        val categories = mutableListOf<EventCategory>()
        val events = mutableListOf<TrackedEvent>()
        val entries = mutableListOf<EventEntry>()
        rows.drop(1).filter { it.any(String::isNotBlank) }.forEachIndexed { index, values ->
            require(values.size == header.size) { "Invalid CSV row ${index + 2}." }
            when (values[0]) {
                "category" -> categories += EventCategory(values[3], values[4].toLong())
                "event" -> events += TrackedEvent(values[1].toLong(), values[2], values[3], values[5].toLong(), values[6].toLong())
                "entry" -> entries += EventEntry(values[7].toLong(), values[1].toLong(), values[8].toLong(), values[9], values[10].toIntOrNull())
                else -> error("Unknown record type on row ${index + 2}.")
            }
        }
        validate(events, entries)
        val known = categories.map { it.name }.toMutableSet()
        events.forEach { if (known.add(it.category)) categories += EventCategory(it.category, it.color) }
        return BackupData(categories, events, entries)
    }

    private fun importV1(rows: List<List<String>>): BackupData {
        val events = mutableListOf<TrackedEvent>()
        val entries = mutableListOf<EventEntry>()
        rows.drop(1).filter { it.any(String::isNotBlank) }.forEachIndexed { index, values ->
            require(values.size == v1Header.size) { "Invalid CSV row ${index + 2}." }
            when (values[0]) {
                "event" -> events += TrackedEvent(values[1].toLong(), values[2], values[3], values[4].toLong(), values[5].toLong())
                "entry" -> entries += EventEntry(values[6].toLong(), values[1].toLong(), values[7].toLong(), values[8])
                else -> error("Unknown record type on row ${index + 2}.")
            }
        }
        validate(events, entries)
        val categories = events.distinctBy { it.category }.map { EventCategory(it.category, it.color) }
        return BackupData(categories, events, entries)
    }

    private fun validate(events: List<TrackedEvent>, entries: List<EventEntry>) {
        val ids = events.map { it.id }.toSet()
        require(entries.all { it.eventId in ids }) { "Backup contains a log without its event." }
    }

    private fun row(vararg values: Any): String = values.joinToString(",") { escape(it.toString()) }
    private fun escape(value: String): String = if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' })
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
                quoted && c == '"' && i + 1 < text.length && text[i + 1] == '"' -> { field.append('"'); i++ }
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
