package app.logy

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
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
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import app.logy.data.EventEntry
import app.logy.data.EventSummary
import app.logy.data.TrackedEvent
import app.logy.ui.LogyTheme
import app.logy.ui.formatDateTime
import app.logy.ui.formatElapsed
import kotlinx.coroutines.launch
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

private enum class Page { HOME, SETTINGS, DETAIL }

@Composable
private fun LogyApp(viewModel: LogyViewModel) {
    var page by rememberSaveable { mutableStateOf(Page.HOME) }
    var selectedId by rememberSaveable { mutableLongStateOf(0L) }
    var pendingImport by remember { mutableStateOf<Uri?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {
                    it.write(viewModel.exportCsv())
                } ?: error("Could not open the selected file.")
            }.onSuccess { snackbar.showSnackbar("Backup exported") }
                .onFailure { snackbar.showSnackbar(it.message ?: "Export failed") }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> pendingImport = uri }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { outerPadding ->
        Box(Modifier.padding(outerPadding)) {
            AnimatedContent(targetState = page, label = "page") { target ->
                when (target) {
                    Page.HOME -> HomeScreen(
                        viewModel = viewModel,
                        openSettings = { page = Page.SETTINGS },
                        openEvent = { selectedId = it; page = Page.DETAIL }
                    )
                    Page.SETTINGS -> SettingsScreen(
                        theme = viewModel.theme.collectAsStateWithLifecycle().value,
                        onTheme = viewModel::setTheme,
                        goBack = { page = Page.HOME },
                        export = { exportLauncher.launch("logy-backup.csv") },
                        import = { importLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain")) }
                    )
                    Page.DETAIL -> DetailScreen(
                        id = selectedId,
                        viewModel = viewModel,
                        goBack = { page = Page.HOME }
                    )
                }
            }
        }
    }

    pendingImport?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            icon = { Icon(Icons.Outlined.Upload, null) },
            title = { Text("Restore this backup?") },
            text = { Text("This replaces every event and entry currently stored on this device. This action cannot be undone.") },
            dismissButton = { TextButton(onClick = { pendingImport = null }) { Text("Cancel") } },
            confirmButton = {
                Button(onClick = {
                    pendingImport = null
                    scope.launch {
                        runCatching {
                            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use {
                                viewModel.importCsv(it)
                            } ?: error("Could not open the selected file.")
                        }.onSuccess { snackbar.showSnackbar("Backup restored") }
                            .onFailure { snackbar.showSnackbar(it.message ?: "Import failed") }
                    }
                }) { Text("Replace data") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    viewModel: LogyViewModel,
    openSettings: () -> Unit,
    openEvent: (Long) -> Unit
) {
    val events by viewModel.events.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var adding by remember { mutableStateOf(false) }
    val filtered = remember(events, query) {
        events.filter { it.name.contains(query, true) || it.category.contains(query, true) }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Logy", fontWeight = FontWeight.Bold) },
                actions = { IconButton(onClick = openSettings) { Icon(Icons.Outlined.Settings, "Settings") } }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { adding = true },
                icon = { Icon(Icons.Outlined.Add, null) },
                text = { Text("New event") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Your timeline", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Remember when, without remembering to remember.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(20.dp))
                OutlinedTextField(
                    value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    trailingIcon = if (query.isNotEmpty()) {{ IconButton(onClick = { query = "" }) { Icon(Icons.Outlined.Close, "Clear") } }} else null,
                    placeholder = { Text("Search events or categories") }, singleLine = true,
                    shape = RoundedCornerShape(20.dp)
                )
                Spacer(Modifier.height(8.dp))
            }
            if (filtered.isEmpty()) {
                item { EmptyState(hasQuery = query.isNotEmpty(), onAdd = { adding = true }) }
            } else {
                items(filtered, key = { it.id }) { item -> EventCard(item, openEvent) }
            }
        }
    }

    if (adding) AddEventDialog(
        dismiss = { adding = false },
        add = { name, category, color -> viewModel.addEvent(name, category, color); adding = false }
    )
}

@Composable
private fun EventCard(event: EventSummary, openEvent: (Long) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { openEvent(event.id) },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(52.dp).clip(CircleShape).background(Color(event.color)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Outlined.History, null, tint = Color.White) }
            Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                Text(event.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(event.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(formatElapsed(event.lastEntry), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                Text("${event.entryCount} ${if (event.entryCount == 1) "entry" else "entries"}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun EmptyState(hasQuery: Boolean, onAdd: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
            Icon(if (hasQuery) Icons.Outlined.Search else Icons.Outlined.Schedule, null, Modifier.padding(22.dp).size(32.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text(if (hasQuery) "Nothing found" else "Start your first timeline", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(if (hasQuery) "Try a different search" else "Track anything that matters to you", color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (!hasQuery) { Spacer(Modifier.height(18.dp)); OutlinedButton(onClick = onAdd) { Text("Create event") } }
    }
}

private val swatches = listOf(0xFF8B7CF6, 0xFFEC6F8C, 0xFF00A98F, 0xFFFF9F43, 0xFF3D8BFF, 0xFF9B6B43)

@Composable
private fun AddEventDialog(dismiss: () -> Unit, add: (String, String, Long) -> Unit) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var color by remember { mutableLongStateOf(swatches.first()) }
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text("What do you want to track?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Event name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(category, { category = it }, label = { Text("Category (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Outlined.Category, null) })
                Text("Accent color", style = MaterialTheme.typography.labelLarge)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    swatches.forEach { item ->
                        Box(
                            Modifier.size(if (color == item) 42.dp else 36.dp).clip(CircleShape).background(Color(item)).clickable { color = item }
                        )
                    }
                }
            }
        },
        dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } },
        confirmButton = { Button(onClick = { add(name, category, color) }, enabled = name.isNotBlank()) { Text("Create") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailScreen(id: Long, viewModel: LogyViewModel, goBack: () -> Unit) {
    val event by remember(id) { viewModel.event(id) }.collectAsStateWithLifecycle(initialValue = null)
    val entries by remember(id) { viewModel.entries(id) }.collectAsStateWithLifecycle(initialValue = emptyList())
    var adding by remember { mutableStateOf(false) }
    var deleteEvent by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(event?.name ?: "Event", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = goBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } },
                actions = { IconButton(onClick = { deleteEvent = true }) { Icon(Icons.Outlined.Delete, "Delete event") } }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { adding = true }, icon = { Icon(Icons.Outlined.Add, null) }, text = { Text("Add entry") })
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            event?.let { current ->
                item {
                    Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Color(current.color))) {
                        Column(Modifier.fillMaxWidth().padding(24.dp)) {
                            Text("LAST LOGGED", color = Color.White.copy(alpha = .78f), style = MaterialTheme.typography.labelMedium)
                            Text(formatElapsed(entries.firstOrNull()?.occurredAt), color = Color.White, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(12.dp))
                            Text("${entries.size} total entries  •  ${current.category}", color = Color.White.copy(alpha = .88f))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("History", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
            if (entries.isEmpty()) item { EmptyHistory { adding = true } }
            items(entries, key = { it.id }) { entry -> EntryCard(entry) { viewModel.deleteEntry(entry) } }
        }
    }

    if (adding) AddEntryDialog(dismiss = { adding = false }) { time, note ->
        viewModel.addEntry(id, time, note); adding = false
    }
    if (deleteEvent) AlertDialog(
        onDismissRequest = { deleteEvent = false },
        title = { Text("Delete this event?") },
        text = { Text("The event and its complete history will be permanently removed.") },
        dismissButton = { TextButton(onClick = { deleteEvent = false }) { Text("Cancel") } },
        confirmButton = { Button(onClick = { event?.let(viewModel::deleteEvent); deleteEvent = false; goBack() }) { Text("Delete") } }
    )
}

@Composable
private fun EmptyHistory(add: () -> Unit) {
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("No entries yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Add the first moment to this timeline", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp)); OutlinedButton(onClick = add) { Text("Add now") }
        }
    }
}

@Composable
private fun EntryCard(entry: EventEntry, delete: () -> Unit) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondary))
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(formatDateTime(entry.occurredAt), fontWeight = FontWeight.SemiBold)
                if (entry.note.isNotBlank()) Text(entry.note, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = delete) { Icon(Icons.Outlined.Delete, "Delete entry", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun AddEntryDialog(dismiss: () -> Unit, add: (Long, String) -> Unit) {
    var note by remember { mutableStateOf("") }
    var timestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val context = LocalContext.current
    fun chooseTime() {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        DatePickerDialog(context, { _, year, month, day ->
            TimePickerDialog(context, { _, hour, minute ->
                calendar.set(year, month, day, hour, minute, 0)
                timestamp = calendar.timeInMillis
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }
    AlertDialog(
        onDismissRequest = dismiss,
        icon = { Icon(Icons.Outlined.Schedule, null) },
        title = { Text("Add an entry") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedButton(onClick = ::chooseTime, modifier = Modifier.fillMaxWidth()) { Text(formatDateTime(timestamp)) }
                OutlinedTextField(note, { note = it }, label = { Text("Note (optional)") }, minLines = 3, modifier = Modifier.fillMaxWidth())
            }
        },
        dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } },
        confirmButton = { Button(onClick = { add(timestamp, note) }) { Text("Save entry") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    theme: ThemeMode,
    onTheme: (ThemeMode) -> Unit,
    goBack: () -> Unit,
    export: () -> Unit,
    import: () -> Unit
) {
    Scaffold(topBar = {
        CenterAlignedTopAppBar(
            title = { Text("Settings") },
            navigationIcon = { IconButton(onClick = goBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } }
        )
    }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Text("Appearance", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        if (mode == theme) Button(onClick = { onTheme(mode) }, modifier = Modifier.weight(1f)) { Text(mode.name.lowercase().replaceFirstChar(Char::uppercase)) }
                        else OutlinedButton(onClick = { onTheme(mode) }, modifier = Modifier.weight(1f)) { Text(mode.name.lowercase().replaceFirstChar(Char::uppercase)) }
                    }
                }
            }
            item { HorizontalDivider() }
            item {
                Text("Your data", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Everything stays on this device. Logy never requests internet access.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                SettingsAction(Icons.Outlined.Download, "Export CSV backup", "Save all events and entries to a file", export)
                Spacer(Modifier.height(10.dp))
                SettingsAction(Icons.Outlined.Upload, "Import CSV backup", "Replace local data from a Logy file", import)
            }
            item { HorizontalDivider() }
            item {
                Text("Logy 1.0", fontWeight = FontWeight.SemiBold)
                Text("Private, offline, and made for your moments.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SettingsAction(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, action: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = action),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            FilledIconButton(onClick = action) { Icon(icon, null) }
            Column(Modifier.padding(start = 14.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
