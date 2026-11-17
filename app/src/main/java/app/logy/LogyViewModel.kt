package app.logy

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.logy.data.CsvBackup
import app.logy.data.EventEntry
import app.logy.data.EventSummary
import app.logy.data.TrackedEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.Reader

enum class ThemeMode { SYSTEM, LIGHT, DARK }

class LogyViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = (application as LogyApplication).database.dao()
    private val prefs = application.getSharedPreferences("appearance", 0)

    val events: StateFlow<List<EventSummary>> = dao.observeEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _theme = MutableStateFlow(
        runCatching { ThemeMode.valueOf(prefs.getString("theme", ThemeMode.SYSTEM.name)!!) }
            .getOrDefault(ThemeMode.SYSTEM)
    )
    val theme: StateFlow<ThemeMode> = _theme

    fun event(id: Long): Flow<TrackedEvent?> = dao.observeEvent(id)
    fun entries(id: Long): Flow<List<EventEntry>> = dao.observeEntries(id)

    fun setTheme(mode: ThemeMode) {
        _theme.value = mode
        prefs.edit().putString("theme", mode.name).apply()
    }

    fun addEvent(name: String, category: String, color: Long) = viewModelScope.launch {
        dao.insertEvent(TrackedEvent(name = name.trim(), category = category.trim().ifBlank { "General" }, color = color))
    }

    fun addEntry(eventId: Long, occurredAt: Long, note: String) = viewModelScope.launch {
        dao.insertEntry(EventEntry(eventId = eventId, occurredAt = occurredAt, note = note.trim()))
    }

    fun deleteEvent(event: TrackedEvent) = viewModelScope.launch { dao.deleteEvent(event) }
    fun deleteEntry(entry: EventEntry) = viewModelScope.launch { dao.deleteEntry(entry) }

    suspend fun exportCsv(): String = CsvBackup.export(dao.allEvents(), dao.allEntries())

    suspend fun importCsv(reader: Reader) {
        val (events, entries) = CsvBackup.import(reader)
        dao.replaceAll(events, entries)
    }
}
