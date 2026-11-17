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

@Entity(tableName = "categories")
data class EventCategory(
    @androidx.room.PrimaryKey val name: String,
    val color: Long = 0xFF8B7CF6
)

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
        parentColumns = ["id"], childColumns = ["event_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("event_id")]
)
data class EventEntry(
    @androidx.room.PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "event_id") val eventId: Long,
    @ColumnInfo(name = "occurred_at") val occurredAt: Long = System.currentTimeMillis(),
    val note: String = "",
    @ColumnInfo(name = "duration_minutes") val durationMinutes: Int? = null,
    val active: Boolean = true
)

data class EventSummary(
    val id: Long,
    val name: String,
    val category: String,
    val color: Long,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "last_entry") val lastEntry: Long?,
    @ColumnInfo(name = "last_note") val lastNote: String?,
    @ColumnInfo(name = "entry_count") val entryCount: Int
)

@Dao
interface LogyDao {
    @Query("""
        SELECT e.*,
          (SELECT n.occurred_at FROM entries n WHERE n.event_id = e.id ORDER BY n.occurred_at DESC, n.id DESC LIMIT 1) AS last_entry,
          (SELECT n.note FROM entries n WHERE n.event_id = e.id ORDER BY n.occurred_at DESC, n.id DESC LIMIT 1) AS last_note,
          (SELECT COUNT(*) FROM entries n WHERE n.event_id = e.id) AS entry_count
        FROM events e ORDER BY COALESCE(last_entry, e.created_at) DESC
    """)
    fun observeEvents(): Flow<List<EventSummary>>

    @Query("SELECT * FROM categories ORDER BY LOWER(name)") fun observeCategories(): Flow<List<EventCategory>>
    @Query("SELECT * FROM events WHERE id = :id") fun observeEvent(id: Long): Flow<TrackedEvent?>
    @Query("SELECT * FROM entries WHERE event_id = :eventId ORDER BY occurred_at DESC, id DESC") fun observeEntries(eventId: Long): Flow<List<EventEntry>>
    @Query("SELECT * FROM entries ORDER BY occurred_at DESC, id DESC") fun observeAllEntries(): Flow<List<EventEntry>>
    @Query("SELECT * FROM events ORDER BY id") suspend fun allEvents(): List<TrackedEvent>
    @Query("SELECT * FROM entries ORDER BY id") suspend fun allEntries(): List<EventEntry>
    @Query("SELECT * FROM categories ORDER BY name") suspend fun allCategories(): List<EventCategory>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertEvent(event: TrackedEvent): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertEntry(entry: EventEntry): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertCategory(category: EventCategory)
    @Update suspend fun updateEvent(event: TrackedEvent)
    @Update suspend fun updateEntry(entry: EventEntry)
    @Delete suspend fun deleteEvent(event: TrackedEvent)
    @Delete suspend fun deleteEntry(entry: EventEntry)
    @Query("UPDATE events SET category = :newName WHERE category = :oldName") suspend fun updateEventsCategory(oldName: String, newName: String)
    @Query("DELETE FROM categories WHERE name = :name") suspend fun deleteCategoryByName(name: String)
    @Query("DELETE FROM entries") suspend fun clearEntries()
    @Query("DELETE FROM events") suspend fun clearEvents()
    @Query("DELETE FROM categories") suspend fun clearCategories()

    @Transaction
    suspend fun renameCategory(oldName: String, category: EventCategory) {
        insertCategory(category)
        updateEventsCategory(oldName, category.name)
        if (oldName != category.name) deleteCategoryByName(oldName)
    }

    @Transaction
    suspend fun removeCategory(name: String) {
        updateEventsCategory(name, "General")
        deleteCategoryByName(name)
    }

    @Transaction
    suspend fun replaceAll(categories: List<EventCategory>, events: List<TrackedEvent>, entries: List<EventEntry>) {
        clearEntries(); clearEvents(); clearCategories()
        val restored = categories.ifEmpty { listOf(EventCategory("General")) }
        restored.forEach { insertCategory(it) }
        if (restored.none { it.name == "General" }) insertCategory(EventCategory("General"))
        events.forEach { insertEvent(it) }
        entries.forEach { insertEntry(it) }
    }
}

@Database(entities = [TrackedEvent::class, EventEntry::class, EventCategory::class], version = 3, exportSchema = false)
abstract class LogyDatabase : RoomDatabase() { abstract fun dao(): LogyDao }
