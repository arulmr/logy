package app.logy

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.logy.data.EventCategory
import app.logy.data.EventEntry
import app.logy.data.EventSummary
import app.logy.data.TrackedEvent
import app.logy.ui.LogyTheme
import app.logy.ui.formatDate
import app.logy.ui.formatDateTime
import app.logy.ui.formatDuration
import app.logy.ui.formatElapsed
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar

class MainActivity : ComponentActivity() {
    private val viewModel: LogyViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val theme by viewModel.theme.collectAsStateWithLifecycle()
            LogyTheme(theme) { LogyApp(viewModel) }
        }
    }
}

private enum class Page { HOME, CATEGORIES, SETTINGS, DETAIL }

@Composable
private fun LogyApp(viewModel: LogyViewModel) {
    var page by rememberSaveable { mutableStateOf(Page.HOME) }
    var selectedId by rememberSaveable { mutableLongStateOf(0L) }
    var pendingImport by remember { mutableStateOf<Uri?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    BackHandler(enabled = page != Page.HOME) { page = Page.HOME }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) scope.launch {
            runCatching { context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(viewModel.exportCsv()) } ?: error("Could not open the selected file.") }
                .onSuccess { snackbar.showSnackbar("Backup exported") }
                .onFailure { snackbar.showSnackbar(it.message ?: "Export failed") }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { pendingImport = it }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { outerPadding ->
        Box(Modifier.padding(outerPadding)) {
            AnimatedContent(targetState = page, label = "page") { target ->
                when (target) {
                    Page.HOME -> HomeScreen(
                        viewModel, openCategories = { page = Page.CATEGORIES },
                        openSettings = { page = Page.SETTINGS },
                        openEvent = { selectedId = it; page = Page.DETAIL }
                    )
                    Page.CATEGORIES -> CategoriesScreen(viewModel) { page = Page.HOME }
                    Page.SETTINGS -> SettingsScreen(
                        theme = viewModel.theme.collectAsStateWithLifecycle().value,
                        onTheme = viewModel::setTheme, goBack = { page = Page.HOME },
                        export = { exportLauncher.launch("logy-v2-backup.csv") },
                        import = { importLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain")) }
                    )
                    Page.DETAIL -> DetailScreen(selectedId, viewModel) { page = Page.HOME }
                }
            }
        }
    }

    pendingImport?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImport = null }, icon = { Icon(Icons.Outlined.Upload, null) },
            title = { Text("Restore this backup?") },
            text = { Text("This replaces every category, event, and log stored on this device. This action cannot be undone.") },
            dismissButton = { TextButton(onClick = { pendingImport = null }) { Text("Cancel") } },
            confirmButton = { Button(onClick = {
                pendingImport = null
                scope.launch {
                    runCatching { context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { viewModel.importCsv(it) } ?: error("Could not open the selected file.") }
                        .onSuccess { snackbar.showSnackbar("Backup restored") }
                        .onFailure { snackbar.showSnackbar(it.message ?: "Import failed") }
                }
            }) { Text("Replace data") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(viewModel: LogyViewModel, openCategories: () -> Unit, openSettings: () -> Unit, openEvent: (Long) -> Unit) {
    val events by viewModel.events.collectAsStateWithLifecycle()
    val allEntries by viewModel.allEntries.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var fromDate by rememberSaveable { mutableStateOf<Long?>(null) }
    var toDate by rememberSaveable { mutableStateOf<Long?>(null) }
    var choosingDates by remember { mutableStateOf(false) }
    var addingEvent by remember { mutableStateOf(false) }
    var loggingEventId by remember { mutableStateOf<Long?>(null) }
    val hasDateRange = fromDate != null && toDate != null
    val rangedLogs = remember(allEntries, fromDate, toDate) {
        if (!hasDateRange) allEntries else allEntries.filter { it.occurredAt in fromDate!!..toDate!! }
    }
    val periodStats = remember(rangedLogs) { rangedLogs.groupBy { it.eventId } }
    val matchingLogs = remember(rangedLogs, query) {
        if (query.isBlank()) emptyMap() else rangedLogs.filter { it.note.contains(query.trim(), true) }.groupBy { it.eventId }
    }
    val filtered = remember(events, query, matchingLogs, hasDateRange, periodStats) {
        events.filter { event ->
            val matchesText = query.isBlank() || event.name.contains(query.trim(), true) ||
                event.category.contains(query.trim(), true) || matchingLogs.containsKey(event.id)
            val matchesDate = !hasDateRange || periodStats.containsKey(event.id)
            matchesText && matchesDate
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Logy", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = openCategories) { Icon(Icons.Outlined.Category, "Categories") }
                    IconButton(onClick = openSettings) { Icon(Icons.Outlined.Settings, "Settings") }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { addingEvent = true }, icon = { Icon(Icons.Outlined.Add, null) }, text = { Text("Event") })
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 92.dp)
        ) {
            item {
                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    trailingIcon = { Row {
                        if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Outlined.Close, "Clear search") }
                        IconButton(onClick = { choosingDates = true }) { Icon(Icons.Outlined.CalendarMonth, "Select date range", tint = if (hasDateRange) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
                    } },
                    placeholder = { Text("Search events and log notes") }, singleLine = true,
                    shape = RoundedCornerShape(28.dp)
                )
                if (hasDateRange) {
                    Surface(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(Modifier.fillMaxWidth().padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${formatDate(fromDate!!)} – ${formatDate(toDate!!)}", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                            IconButton(onClick = { fromDate = null; toDate = null }) { Icon(Icons.Outlined.Close, "Clear date range") }
                        }
                    }
                }
            }
            if (filtered.isEmpty()) item { EmptyState(query.isNotBlank() || hasDateRange) { addingEvent = true } }
            items(filtered, key = { it.id }) { event ->
                val logs = periodStats[event.id].orEmpty()
                EventRow(
                    event = event, matches = matchingLogs[event.id].orEmpty(), searching = query.isNotBlank(),
                    displayedLogCount = if (hasDateRange) logs.size else event.entryCount,
                    displayedDuration = if (hasDateRange) logs.sumOf { it.durationMinutes ?: 0 } else null,
                    periodSelected = hasDateRange,
                    open = { openEvent(event.id) }, addLog = { loggingEventId = event.id }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .6f))
            }
        }
    }

    if (addingEvent) EventDialog(
        categories = categories, initial = null, dismiss = { addingEvent = false },
        save = { name, category, color -> viewModel.addEvent(name, category, color); addingEvent = false }
    )
    loggingEventId?.let { id ->
        EntryDialog(initial = null, dismiss = { loggingEventId = null }) { time, note, duration, active ->
            viewModel.addEntry(id, time, note, duration, active); loggingEventId = null
        }
    }
    if (choosingDates) DateRangeDialog(
        initialFrom = fromDate, initialTo = toDate, dismiss = { choosingDates = false },
        apply = { from, to -> fromDate = from; toDate = to; choosingDates = false }
    )
}

@Composable
private fun EventRow(
    event: EventSummary,
    matches: List<EventEntry>,
    searching: Boolean,
    displayedLogCount: Int,
    displayedDuration: Int?,
    periodSelected: Boolean,
    open: () -> Unit,
    addLog: () -> Unit
) {
    Column(Modifier.fillMaxWidth().clickable(onClick = open).padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(event.name, Modifier.weight(1f), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (event.lastEntry != null) Text(
                formatDateTime(event.lastEntry),
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = addLog).padding(6.dp),
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary
            )
            IconButton(onClick = addLog, modifier = Modifier.size(40.dp)) { Icon(Icons.Outlined.Add, "Add log", tint = MaterialTheme.colorScheme.primary) }
        }
        if (!event.lastNote.isNullOrBlank()) Text(event.lastNote, Modifier.padding(end = 8.dp, top = 2.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (searching && matches.isNotEmpty()) {
            Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                matches.take(3).forEach { log ->
                    Text("• ${log.note}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(formatDateTime(log.occurredAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (matches.size > 3) Text("+${matches.size - 3} more matching logs", style = MaterialTheme.typography.labelSmall)
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            LabelPill(event.category)
            Text(
                if (periodSelected) "$displayedLogCount logs • ${formatDuration(displayedDuration ?: 0)} total" else "$displayedLogCount ${if (displayedLogCount == 1) "log" else "logs"}",
                Modifier.padding(start = 10.dp), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            Surface(modifier = Modifier.clickable(onClick = addLog), shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Text(formatElapsed(event.lastEntry), Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun DateRangeDialog(initialFrom: Long?, initialTo: Long?, dismiss: () -> Unit, apply: (Long, Long) -> Unit) {
    var from by remember(initialFrom) { mutableStateOf(initialFrom) }
    var to by remember(initialTo) { mutableStateOf(initialTo) }
    val context = LocalContext.current
    fun pick(current: Long?, endOfDay: Boolean, selected: (Long) -> Unit) {
        val calendar = Calendar.getInstance().apply { timeInMillis = current ?: System.currentTimeMillis() }
        DatePickerDialog(context, { _, year, month, day ->
            calendar.set(year, month, day, if (endOfDay) 23 else 0, if (endOfDay) 59 else 0, if (endOfDay) 59 else 0)
            calendar.set(Calendar.MILLISECOND, if (endOfDay) 999 else 0)
            selected(calendar.timeInMillis)
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }
    val valid = from != null && to != null && from!! <= to!!
    AlertDialog(
        onDismissRequest = dismiss, icon = { Icon(Icons.Outlined.CalendarMonth, null) }, title = { Text("Filter by date") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { pick(from, false) { from = it } }, modifier = Modifier.fillMaxWidth()) { Text(from?.let { "From: ${formatDate(it)}" } ?: "Select from date") }
            OutlinedButton(onClick = { pick(to, true) { to = it } }, modifier = Modifier.fillMaxWidth()) { Text(to?.let { "To: ${formatDate(it)}" } ?: "Select to date") }
            if (from != null && to != null && from!! > to!!) Text("The from date must be before the to date.", color = MaterialTheme.colorScheme.error)
        } },
        dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } },
        confirmButton = { Button(onClick = { apply(from!!, to!!) }, enabled = valid) { Text("Apply") } }
    )
}

@Composable
private fun LabelPill(text: String) {
    Surface(shape = RoundedCornerShape(5.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Text(text, Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EmptyState(hasQuery: Boolean, onAdd: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 64.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(if (hasQuery) Icons.Outlined.Search else Icons.Outlined.Schedule, null, Modifier.size(42.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        Text(if (hasQuery) "Nothing found" else "Create your first event", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(if (hasQuery) "Try another event, category, or note" else "Keep the moments that matter within reach", color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (!hasQuery) { Spacer(Modifier.height(16.dp)); OutlinedButton(onClick = onAdd) { Text("Create event") } }
    }
}

private val swatches = listOf(0xFF8B7CF6, 0xFFEC6F8C, 0xFF00A98F, 0xFFFF9F43, 0xFF3D8BFF, 0xFF9B6B43)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventDialog(categories: List<EventCategory>, initial: TrackedEvent?, dismiss: () -> Unit, save: (String, String, Long) -> Unit) {
    val choices = categories.ifEmpty { listOf(EventCategory("General")) }
    var name by remember(initial) { mutableStateOf(initial?.name.orEmpty()) }
    var category by remember(initial, choices) { mutableStateOf(initial?.category?.takeIf { value -> choices.any { it.name == value } } ?: choices.first().name) }
    var color by remember(initial) { mutableLongStateOf(initial?.color ?: swatches.first()) }
    var expanded by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(if (initial == null) "Create event" else "Edit event") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("Event name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                OutlinedTextField(
                    value = category, onValueChange = {}, readOnly = true, label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    choices.forEach { item -> DropdownMenuItem(text = { Text(item.name) }, onClick = { category = item.name; expanded = false }) }
                }
            }
            Text("Accent color", style = MaterialTheme.typography.labelLarge)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                swatches.forEach { item -> Box(Modifier.size(if (color == item) 42.dp else 34.dp).clip(CircleShape).background(Color(item)).clickable { color = item }) }
            }
        } },
        dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } },
        confirmButton = { Button(onClick = { save(name, category, color) }, enabled = name.isNotBlank()) { Text(if (initial == null) "Create" else "Save") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailScreen(id: Long, viewModel: LogyViewModel, goBack: () -> Unit) {
    val event by remember(id) { viewModel.event(id) }.collectAsStateWithLifecycle(initialValue = null)
    val entries by remember(id) { viewModel.entries(id) }.collectAsStateWithLifecycle(initialValue = emptyList())
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    var query by rememberSaveable(id) { mutableStateOf("") }
    var adding by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<EventEntry?>(null) }
    var deletingEntry by remember { mutableStateOf<EventEntry?>(null) }
    var deletingEvent by remember { mutableStateOf(false) }
    val filtered = remember(entries, query) { if (query.isBlank()) entries else entries.filter { it.note.contains(query.trim(), true) } }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(
            title = { Text(event?.name ?: "Event", maxLines = 1, overflow = TextOverflow.Ellipsis) },
            navigationIcon = { IconButton(onClick = goBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } },
            actions = {
                IconButton(onClick = { editingEvent = true }) { Icon(Icons.Outlined.Edit, "Edit event") }
                IconButton(onClick = { deletingEvent = true }) { Icon(Icons.Outlined.Delete, "Delete event") }
            }
        ) },
        floatingActionButton = { ExtendedFloatingActionButton(onClick = { adding = true }, icon = { Icon(Icons.Outlined.Add, null) }, text = { Text("Log") }) }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            event?.let { current -> item {
                Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color(current.color))) {
                    Column(Modifier.fillMaxWidth().padding(22.dp)) {
                        Text("LAST LOGGED", color = Color.White.copy(alpha = .76f), style = MaterialTheme.typography.labelMedium)
                        Text(formatElapsed(entries.firstOrNull()?.occurredAt), color = Color.White, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                        Text("${entries.size} logs  •  ${current.category}", color = Color.White.copy(alpha = .9f))
                    }
                }
                OutlinedTextField(
                    value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    trailingIcon = if (query.isNotEmpty()) {{ IconButton(onClick = { query = "" }) { Icon(Icons.Outlined.Close, "Clear") } }} else null,
                    placeholder = { Text("Search notes in this event") }, singleLine = true, shape = RoundedCornerShape(20.dp)
                )
            } }
            if (filtered.isEmpty()) item { EmptyHistory(hasQuery = query.isNotBlank()) { adding = true } }
            items(filtered, key = { it.id }) { entry -> EntryCard(entry, edit = { editingEntry = entry }, delete = { deletingEntry = entry }) }
        }
    }

    if (adding) EntryDialog(null, { adding = false }) { time, note, duration, active -> viewModel.addEntry(id, time, note, duration, active); adding = false }
    editingEntry?.let { entry -> EntryDialog(entry, { editingEntry = null }) { time, note, duration, active -> viewModel.updateEntry(entry, time, note, duration, active); editingEntry = null } }
    deletingEntry?.let { entry -> AlertDialog(
        onDismissRequest = { deletingEntry = null }, title = { Text("Delete this log?") },
        text = { Text("This log will be permanently removed. This action cannot be undone.") },
        dismissButton = { TextButton(onClick = { deletingEntry = null }) { Text("Cancel") } },
        confirmButton = { Button(onClick = { viewModel.deleteEntry(entry); deletingEntry = null }) { Text("Delete") } }
    ) }
    if (editingEvent) event?.let { current -> EventDialog(categories, current, { editingEvent = false }) { name, category, color -> viewModel.updateEvent(current, name, category, color); editingEvent = false } }
    if (deletingEvent) AlertDialog(
        onDismissRequest = { deletingEvent = false }, title = { Text("Delete this event?") },
        text = { Text("The event and its complete log history will be permanently removed.") },
        dismissButton = { TextButton(onClick = { deletingEvent = false }) { Text("Cancel") } },
        confirmButton = { Button(onClick = { event?.let(viewModel::deleteEvent); deletingEvent = false; goBack() }) { Text("Delete") } }
    )
}

@Composable
private fun EmptyHistory(hasQuery: Boolean, add: () -> Unit) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.fillMaxWidth().padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (hasQuery) "No matching notes" else "No logs yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(if (hasQuery) "Try another search" else "Add the first moment to this timeline", color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!hasQuery) { Spacer(Modifier.height(10.dp)); OutlinedButton(onClick = add) { Text("Add now") } }
        }
    }
}

@Composable
private fun EntryCard(entry: EventEntry, edit: () -> Unit, delete: () -> Unit) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(formatDateTime(entry.occurredAt), fontWeight = FontWeight.SemiBold)
                if (entry.note.isNotBlank()) Text(entry.note, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 3.dp))
                if (entry.durationMinutes != null || !entry.active) {
                    Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        entry.durationMinutes?.let { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.Timer, null, Modifier.size(15.dp)); Text(formatDuration(it), Modifier.padding(start = 5.dp), style = MaterialTheme.typography.labelMedium) } }
                        if (!entry.active) LabelPill("Inactive")
                    }
                }
            }
            IconButton(onClick = edit) { Icon(Icons.Outlined.Edit, "Edit log") }
            IconButton(onClick = delete) { Icon(Icons.Outlined.Delete, "Delete log", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun EntryDialog(initial: EventEntry?, dismiss: () -> Unit, save: (Long, String, Int?, Boolean) -> Unit) {
    var note by remember(initial) { mutableStateOf(initial?.note.orEmpty()) }
    var timestamp by remember(initial) { mutableLongStateOf(initial?.occurredAt ?: System.currentTimeMillis()) }
    var hours by remember(initial) { mutableStateOf(initial?.durationMinutes?.div(60)?.toString().orEmpty()) }
    var minutes by remember(initial) { mutableStateOf(initial?.durationMinutes?.rem(60)?.toString().orEmpty()) }
    var active by remember(initial) { mutableStateOf(initial?.active ?: true) }
    val context = LocalContext.current
    val hoursValue = hours.toIntOrNull() ?: 0
    val minutesValue = minutes.toIntOrNull() ?: 0
    val durationValid = hoursValue >= 0 && minutesValue in 0..59 && (hours.isBlank() || hours.toIntOrNull() != null) && (minutes.isBlank() || minutes.toIntOrNull() != null)
    fun chooseTime() {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        DatePickerDialog(context, { _, year, month, day ->
            TimePickerDialog(context, { _, hour, minute -> calendar.set(year, month, day, hour, minute, 0); timestamp = calendar.timeInMillis }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }
    AlertDialog(
        onDismissRequest = dismiss, icon = { Icon(Icons.Outlined.Schedule, null) },
        title = { Text(if (initial == null) "Add log" else "Edit log") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
            OutlinedButton(onClick = ::chooseTime, modifier = Modifier.fillMaxWidth()) { Text(formatDateTime(timestamp)) }
            OutlinedTextField(note, { note = it }, label = { Text("Note (optional)") }, minLines = 3, modifier = Modifier.fillMaxWidth())
            Text("Duration (optional)", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(hours, { hours = it.filter(Char::isDigit) }, label = { Text("Hours") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                OutlinedTextField(minutes, { minutes = it.filter(Char::isDigit) }, label = { Text("Minutes") }, singleLine = true, isError = minutesValue !in 0..59, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
            }
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { active = !active }.padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = active, onCheckedChange = { active = it })
                Column { Text("Active", fontWeight = FontWeight.SemiBold); Text("New logs are active by default", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        } },
        dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } },
        confirmButton = { Button(onClick = { val total = hoursValue * 60 + minutesValue; save(timestamp, note, total.takeIf { it > 0 }, active) }, enabled = durationValid) { Text("Save") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoriesScreen(viewModel: LogyViewModel, goBack: () -> Unit) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<EventCategory?>(null) }
    var deleting by remember { mutableStateOf<EventCategory?>(null) }
    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Categories") }, navigationIcon = { IconButton(onClick = goBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } }) },
        floatingActionButton = { ExtendedFloatingActionButton(onClick = { adding = true }, icon = { Icon(Icons.Outlined.Add, null) }, text = { Text("Category") }) }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Text("Organize events with reusable categories.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(categories, key = { it.name }) { category ->
                Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(38.dp).clip(CircleShape).background(Color(category.color)))
                        Text(category.name, Modifier.weight(1f).padding(start = 14.dp), fontWeight = FontWeight.SemiBold)
                        IconButton(onClick = { editing = category }) { Icon(Icons.Outlined.Edit, "Edit category") }
                        if (category.name != "General") IconButton(onClick = { deleting = category }) { Icon(Icons.Outlined.Delete, "Delete category") }
                    }
                }
            }
        }
    }
    if (adding) CategoryDialog(null, { adding = false }) { name, color -> viewModel.addCategory(name, color); adding = false }
    editing?.let { category -> CategoryDialog(category, { editing = null }) { name, color -> viewModel.updateCategory(category.name, name, color); editing = null } }
    deleting?.let { category -> AlertDialog(
        onDismissRequest = { deleting = null }, title = { Text("Delete ${category.name}?") },
        text = { Text("Events in this category will move to General. No events or logs will be deleted.") },
        dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
        confirmButton = { Button(onClick = { viewModel.deleteCategory(category); deleting = null }) { Text("Delete") } }
    ) }
}

@Composable
private fun CategoryDialog(initial: EventCategory?, dismiss: () -> Unit, save: (String, Long) -> Unit) {
    var name by remember(initial) { mutableStateOf(initial?.name.orEmpty()) }
    var color by remember(initial) { mutableLongStateOf(initial?.color ?: swatches.first()) }
    AlertDialog(
        onDismissRequest = dismiss, title = { Text(if (initial == null) "Create category" else "Edit category") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("Category name") }, enabled = initial?.name != "General", singleLine = true, modifier = Modifier.fillMaxWidth())
            Text("Color", style = MaterialTheme.typography.labelLarge)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { swatches.forEach { item -> Box(Modifier.size(if (color == item) 42.dp else 34.dp).clip(CircleShape).background(Color(item)).clickable { color = item }) } }
        } },
        dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } },
        confirmButton = { Button(onClick = { save(name, color) }, enabled = name.isNotBlank()) { Text("Save") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(theme: ThemeMode, onTheme: (ThemeMode) -> Unit, goBack: () -> Unit, export: () -> Unit, import: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }
    var downloading by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var downloadedApk by remember { mutableStateOf<File?>(null) }
    var updateMessage by remember { mutableStateOf<String?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        downloadedApk?.let { apk ->
            if (context.packageManager.canRequestPackageInstalls()) UpdateManager.install(context, apk)
            else updateMessage = "Allow installs from Logy, then tap Check for updates again."
        }
    }
    fun installOrRequest(apk: File) {
        if (context.packageManager.canRequestPackageInstalls()) UpdateManager.install(context, apk)
        else permissionLauncher.launch(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")))
    }

    Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text("Settings") }, navigationIcon = { IconButton(onClick = goBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            item {
                Text("Appearance", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { ThemeMode.entries.forEach { mode ->
                    if (mode == theme) Button(onClick = { onTheme(mode) }, modifier = Modifier.weight(1f)) { Text(mode.name.lowercase().replaceFirstChar(Char::uppercase)) }
                    else OutlinedButton(onClick = { onTheme(mode) }, modifier = Modifier.weight(1f)) { Text(mode.name.lowercase().replaceFirstChar(Char::uppercase)) }
                } }
            }
            item { HorizontalDivider() }
            item {
                Text("Updates", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Check GitHub Releases and install updates without copying APK files.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                SettingsAction(
                    Icons.Outlined.SystemUpdate,
                    if (checking) "Checking GitHub…" else "Check for updates",
                    "Network access is used only when you request an update check"
                ) {
                    if (!checking) scope.launch {
                        checking = true; updateMessage = null
                        runCatching { UpdateManager.check(context) }
                            .onSuccess { available -> if (available == null) updateMessage = "Logy is up to date." else updateInfo = available }
                            .onFailure { updateMessage = it.message ?: "Update check failed." }
                        checking = false
                    }
                }
                updateMessage?.let { Text(it, Modifier.padding(top = 10.dp), color = MaterialTheme.colorScheme.primary) }
            }
            item { HorizontalDivider() }
            item {
                Text("Your data", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Your events and logs stay on this device and are never uploaded.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                SettingsAction(Icons.Outlined.Download, "Export CSV backup", "Save categories, events, logs, and durations", export)
                Spacer(Modifier.height(10.dp))
                SettingsAction(Icons.Outlined.Upload, "Import CSV backup", "Supports Logy v1, v2, and v2.2 backups", import)
            }
            item { HorizontalDivider() }
            item { Text("Logy 2.2", fontWeight = FontWeight.SemiBold); Text("Private, local-first, and made for your moments.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }

    updateInfo?.let { update -> AlertDialog(
        onDismissRequest = { if (!downloading) updateInfo = null },
        icon = { Icon(Icons.Outlined.SystemUpdate, null) },
        title = { Text("Logy ${update.version} is available") },
        text = { Text(if (downloading) "Downloading the signed APK from GitHub…" else "Download the release and open Android's installer?") },
        dismissButton = { TextButton(onClick = { updateInfo = null }, enabled = !downloading) { Text("Later") } },
        confirmButton = { Button(onClick = {
            if (!downloading) scope.launch {
                downloading = true
                runCatching { UpdateManager.download(context, update) }
                    .onSuccess { apk -> downloadedApk = apk; updateInfo = null; installOrRequest(apk) }
                    .onFailure { updateMessage = it.message ?: "Download failed."; updateInfo = null }
                downloading = false
            }
        }, enabled = !downloading) { Text(if (downloading) "Downloading…" else "Download & install") } }
    ) }
}

@Composable
private fun SettingsAction(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, action: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = action), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            FilledIconButton(onClick = action) { Icon(icon, null) }
            Column(Modifier.padding(start = 14.dp)) { Text(title, fontWeight = FontWeight.SemiBold); Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}
