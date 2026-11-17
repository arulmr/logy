package app.logy.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "events")
data class TrackedEvent(
    @androidx.room.PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String = "General",
    val color: Long = 0xFF8B7CF6,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "entries",
    foreignKeys = [ForeignKey(
        entity = TrackedEvent::class,
        parentColumns = ["id"],
        childColumns = ["event_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("event_id")]
)
data class EventEntry(
    @androidx.room.PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "event_id") val eventId: Long,
    @ColumnInfo(name = "occurred_at") val occurredAt: Long = System.currentTimeMillis(),
    val note: String = ""
)

data class EventSummary(
    val id: Long,
    val name: String,
    val category: String,
    val color: Long,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "last_entry") val lastEntry: Long?,
    @ColumnInfo(name = "entry_count") val entryCount: Int
)

@Dao
interface LogyDao {
    @Query("""
        SELECT e.*, MAX(n.occurred_at) AS last_entry, COUNT(n.id) AS entry_count
        FROM events e LEFT JOIN entries n ON e.id = n.event_id
        GROUP BY e.id ORDER BY COALESCE(MAX(n.occurred_at), e.created_at) DESC
    """)
    fun observeEvents(): Flow<List<EventSummary>>

    @Query("SELECT * FROM events WHERE id = :id")
    fun observeEvent(id: Long): Flow<TrackedEvent?>

    @Query("SELECT * FROM entries WHERE event_id = :eventId ORDER BY occurred_at DESC")
    fun observeEntries(eventId: Long): Flow<List<EventEntry>>

    @Query("SELECT * FROM events ORDER BY id")
    suspend fun allEvents(): List<TrackedEvent>

    @Query("SELECT * FROM entries ORDER BY id")
    suspend fun allEntries(): List<EventEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: TrackedEvent): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: EventEntry): Long

    @Update suspend fun updateEvent(event: TrackedEvent)
    @Update suspend fun updateEntry(entry: EventEntry)
    @Delete suspend fun deleteEvent(event: TrackedEvent)
    @Delete suspend fun deleteEntry(entry: EventEntry)

    @Query("DELETE FROM entries") suspend fun clearEntries()
    @Query("DELETE FROM events") suspend fun clearEvents()

    @Transaction
    suspend fun replaceAll(events: List<TrackedEvent>, entries: List<EventEntry>) {
        clearEntries()
        clearEvents()
        events.forEach { insertEvent(it) }
        entries.forEach { insertEntry(it) }
    }
}

@Database(entities = [TrackedEvent::class, EventEntry::class], version = 1, exportSchema = false)
abstract class LogyDatabase : RoomDatabase() {
    abstract fun dao(): LogyDao
}
