package app.logy.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.StringReader

class CsvBackupTest {
    @Test
    fun roundTripPreservesEventsAndComplexNotes() {
        val events = listOf(
            TrackedEvent(7, "Call, Mum", "Family", 0xFF8B7CF6, 1_700_000_000_000)
        )
        val entries = listOf(
            EventEntry(11, 7, 1_700_000_100_000, "Said \"hello\"\nEverything is good.")
        )

        val (restoredEvents, restoredEntries) = CsvBackup.import(
            StringReader(CsvBackup.export(events, entries))
        )

        assertEquals(events, restoredEvents)
        assertEquals(entries, restoredEntries)
    }

    @Test
    fun rejectsFilesThatAreNotLogyBackups() {
        val result = runCatching { CsvBackup.import(StringReader("name,date\nTest,Today")) }
        assertTrue(result.isFailure)
    }
}
