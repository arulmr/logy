package app.logy.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.StringReader

class CsvBackupTest {
    @Test
    fun v3RoundTripPreservesCategoriesDurationsActiveStateAndComplexNotes() {
        val categories = listOf(EventCategory("Family", 0xFFEC6F8C))
        val events = listOf(TrackedEvent(7, "Call, Mum", "Family", 0xFF8B7CF6, 1_700_000_000_000))
        val entries = listOf(EventEntry(11, 7, 1_700_000_100_000, "Said \"hello\"\nEverything is good.", 95, false))

        val restored = CsvBackup.import(StringReader(CsvBackup.export(categories, events, entries)))

        assertEquals(categories, restored.categories)
        assertEquals(events, restored.events)
        assertEquals(entries, restored.entries)
    }

    @Test
    fun importsV2BackupAsActive() {
        val csv = """record_type,event_id,event_name,category,category_color,event_color,event_created_at,entry_id,occurred_at,note,duration_minutes
category,,,Family,4293681036,,,,,,
event,7,Call Mum,Family,,4287339766,1700000000000,,,,
entry,7,,,,,,11,1700000100000,Hello,20
"""
        val restored = CsvBackup.import(StringReader(csv))
        assertTrue(restored.entries.single().active)
    }

    @Test
    fun importsV1BackupWithEmptyDurations() {
        val csv = """record_type,event_id,event_name,category,color,event_created_at,entry_id,occurred_at,note
event,7,Call Mum,Family,4287339766,1700000000000,,,
entry,7,,,,,11,1700000100000,Hello
"""
        val restored = CsvBackup.import(StringReader(csv))
        assertEquals("Family", restored.categories.single().name)
        assertEquals(null, restored.entries.single().durationMinutes)
        assertTrue(restored.entries.single().active)
    }

    @Test
    fun rejectsFilesThatAreNotLogyBackups() {
        assertTrue(runCatching { CsvBackup.import(StringReader("name,date\nTest,Today")) }.isFailure)
    }
}
