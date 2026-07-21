package com.lomigoo.classworkmanager.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Announcement
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lomigoo.classworkmanager.R
import com.lomigoo.classworkmanager.data.AppPreferences
import com.lomigoo.classworkmanager.data.Classwork
import dev.jeziellago.compose.markdowntext.MarkdownText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassworkApp(viewModel: ClassworkViewModel) {
    val rawClassworks by viewModel.classworks.collectAsState()
    val updateInfo by viewModel.updateInfo.collectAsState()
    val whatsNewInfo by viewModel.whatsNewInfo.collectAsState()
    val isCheckingUpdates by viewModel.isCheckingUpdates.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val dateFormat by viewModel.dateFormat.collectAsState()
    val selectedTheme by viewModel.selectedTheme.collectAsState()

    var currentFilter by remember { mutableStateOf("All") }
    var currentSortOrder by remember { mutableStateOf("Created Date") }
    var isAscending by remember { mutableStateOf(value = false) }
    var showSortMenu by remember { mutableStateOf(value = false) }

    var showAddDialog by remember { mutableStateOf(value = false) }
    var classworkToEdit by remember { mutableStateOf<Classwork?>(null) }
    var classworkToDelete by remember { mutableStateOf<Classwork?>(null) }
    var showSettings by remember { mutableStateOf(value = false) }
    var showUpdateOptions by remember { mutableStateOf(value = false) }
    var showNoUpdatesDialog by remember { mutableStateOf(value = false) }
    var dateDropdownExpanded by remember { mutableStateOf(false) }

    val uriHandler = LocalUriHandler.current
    val snackbarHostState = remember { SnackbarHostState() }

    fun formatDateForDisplay(dateString: String): String {
        return try {
            val date = LocalDate.parse(dateString, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            if (dateFormat == AppPreferences.FORMAT_READABLE) {
                date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))
            } else {
                dateString
            }
        } catch (e: Exception) {
            dateString
        }
    }

    LaunchedEffect(updateInfo) {
        updateInfo?.let {
            if (it.isUpdateAvailable) {
                val job = launch {
                    val result = snackbarHostState.showSnackbar(
                        message = "Update available: v${it.latestVersion}",
                        actionLabel = "Update",
                        duration = SnackbarDuration.Indefinite,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        showUpdateOptions = true
                    }
                }
                delay(6000)
                job.cancel()
            }
        }
    }

    val processedClassworks = remember(rawClassworks, currentFilter, currentSortOrder, isAscending) {
        val filtered = when (currentFilter) {
            "Pending" -> rawClassworks.filter { !it.isCompleted }
            "Done" -> rawClassworks.filter { it.isCompleted }
            else -> rawClassworks
        }

        val sorted = when (currentSortOrder) {
            "Course Name" -> filtered.sortedBy { it.courseName.lowercase() }
            "Target Date" -> filtered.sortedBy { it.dateTarget.lowercase() }
            else -> filtered.sortedBy { it.id }
        }

        if (isAscending) sorted else sorted.reversed()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Classwork Manager",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    // Notification Bell
                    IconButton(onClick = {
                        if (updateInfo?.isUpdateAvailable == true) {
                            showUpdateOptions = true
                        } else if (updateInfo?.isWhatsNewAvailable == true) {
                            viewModel.showWhatsNew()
                        } else {
                            showNoUpdatesDialog = true
                        }
                    }) {
                        BadgedBox(
                            badge = {
                                if (updateInfo?.isUpdateAvailable == true) {
                                    Badge(containerColor = MaterialTheme.colorScheme.error) {
                                        Text("!", color = MaterialTheme.colorScheme.onError, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Updates"
                            )
                        }
                    }

                    IconButton(onClick = { viewModel.toggleDarkMode() }) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme"
                        )
                    }

                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                        }
                        DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Sort by Date Created") },
                                onClick = { currentSortOrder = "Created Date"; showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Sort by Course Name") },
                                onClick = { currentSortOrder = "Course Name"; showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Sort by Target Date") },
                                onClick = { currentSortOrder = "Target Date"; showSortMenu = false }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(if (isAscending) "Order: Ascending" else "Order: Descending") },
                                onClick = { isAscending = !isAscending; showSortMenu = false },
                                trailingIcon = { Text(if (isAscending) "↑" else "↓") }
                            )
                        }
                    }

                    IconButton(onClick = { showSettings = true }) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Task")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("All", "Pending", "Done").forEach { filter ->
                    FilterChip(
                        selected = currentFilter == filter,
                        onClick = { currentFilter = filter },
                        label = { Text(filter) },
                        shape = RoundedCornerShape(4.dp)
                    )
                }
            }

            Text(
                text = "Sorted by: $currentSortOrder (${if (isAscending) "ASC" else "DESC"})",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
            )

            LazyColumn(modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp).weight(1f)) {
                if (processedClassworks.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (currentFilter == "All") "No classwork found. Tap + to create one!"
                                else "No tasks fit the criteria: $currentFilter",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                } else {
                    items(processedClassworks.size) { index ->
                        val item = processedClassworks[index]
                        ClassworkCard(
                            classwork = item,
                            onToggleStatus = {
                                viewModel.updateClasswork(item.copy(isCompleted = !item.isCompleted))
                            },
                            onEdit = { classworkToEdit = item },
                            onDelete = { classworkToDelete = item },
                            formatDate = { formatDateForDisplay(it) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        // Dialogs
        if (showNoUpdatesDialog) {
            AlertDialog(
                onDismissRequest = { showNoUpdatesDialog = false },
                title = { Text("App Status") },
                text = { Text("No Updates Available. You are using the latest version.") },
                confirmButton = {
                    TextButton(onClick = { showNoUpdatesDialog = false }) {
                        Text("OK")
                    }
                }
            )
        }

        if (showAddDialog) {
            ClassworkDialog(
                onDismiss = { showAddDialog = false },
                onSave = { course, action, target ->
                    viewModel.createClasswork(course, action, target)
                    showAddDialog = false
                }
            )
        }

        classworkToEdit?.let { task ->
            ClassworkDialog(
                initialCourse = task.courseName,
                initialAction = task.actionDescription,
                initialTarget = task.dateTarget,
                onDismiss = { classworkToEdit = null },
                onSave = { course, action, target ->
                    viewModel.updateClasswork(task.copy(courseName = course, actionDescription = action, dateTarget = target))
                    classworkToEdit = null
                }
            )
        }

        if (showUpdateOptions) {
            AlertDialog(
                onDismissRequest = { showUpdateOptions = false },
                title = { Text("Update Available") },
                text = { Text("How would you like to update Classwork Manager?") },
                confirmButton = {
                    Button(
                        onClick = {
                            updateInfo?.downloadUrl?.let { url ->
                                viewModel.startDownloadApk(url)
                            }
                            showUpdateOptions = false
                        },
                        enabled = updateInfo?.downloadUrl != null
                    ) {
                        Text("Direct Update")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        updateInfo?.releaseUrl?.let { url ->
                            uriHandler.openUri(url)
                        }
                        showUpdateOptions = false
                    }) {
                        Text("Manual (GitHub)")
                    }
                }
            )
        }

        classworkToDelete?.let { task ->
            AlertDialog(
                onDismissRequest = { classworkToDelete = null },
                title = { Text("Delete Classwork") },
                text = {
                    Text("Are you sure you want to delete '${task.courseName}: ${task.actionDescription}'? This action cannot be undone.")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteClasswork(task)
                            classworkToDelete = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { classworkToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        whatsNewInfo?.let { info ->
            AlertDialog(
                onDismissRequest = { viewModel.dismissWhatsNew() },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = info.releaseName ?: "What's New",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        MarkdownText(
                            markdown = info.releaseNotes ?: "Bug fixes and performance improvements.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            linkColor = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = { viewModel.dismissWhatsNew() }) {
                        Text("Got it!")
                    }
                }
            )
        }

        if (showSettings) {
            AlertDialog(
                onDismissRequest = { showSettings = false },
                title = { Text("Settings & App Info", style = MaterialTheme.typography.titleLarge) },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text("Preferences", 
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // App Theme Box
                        Text("App Theme", style = MaterialTheme.typography.labelLarge)
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.small,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                listOf(
                                    AppPreferences.THEME_NEUTRAL to Color(0xFF475569),
                                    AppPreferences.THEME_JRU to Color(0xFF0056D2),
                                    AppPreferences.THEME_EMERALD to Color(0xFF059669),
                                    AppPreferences.THEME_CRIMSON to Color(0xFFDC2626),
                                    AppPreferences.THEME_AMETHYST to Color(0xFF7C3AED)
                                ).forEach { (id, color) ->
                                    ThemeCircle(
                                        color = color,
                                        isSelected = selectedTheme == id,
                                        onClick = { viewModel.setTheme(id) }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Date Format Choice Box (Exposed Dropdown)
                        Text("Date Format", style = MaterialTheme.typography.labelLarge)
                        Spacer(modifier = Modifier.height(4.dp))
                        ExposedDropdownMenuBox(
                            expanded = dateDropdownExpanded,
                            onExpandedChange = { dateDropdownExpanded = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = if (dateFormat == AppPreferences.FORMAT_READABLE) "Readable (February 1, 2026)" else "Numbered (2026-02-01)",
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier.menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth(),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dateDropdownExpanded) },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                textStyle = MaterialTheme.typography.bodyMedium,
                                shape = MaterialTheme.shapes.small
                            )
                            ExposedDropdownMenu(
                                expanded = dateDropdownExpanded,
                                onDismissRequest = { dateDropdownExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Numbered (2026-02-01)", style = MaterialTheme.typography.bodyMedium) },
                                    onClick = {
                                        viewModel.setDateFormat(AppPreferences.FORMAT_NUMBERED)
                                        dateDropdownExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Readable (February 1, 2026)", style = MaterialTheme.typography.bodyMedium) },
                                    onClick = {
                                        viewModel.setDateFormat(AppPreferences.FORMAT_READABLE)
                                        dateDropdownExpanded = false
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(16.dp))

                        Text("App Information", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Version: ${viewModel.currentVersion} (Android Port)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Author: LomiGoo",
                            style = MaterialTheme.typography.bodySmall
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("GitHub:", style = MaterialTheme.typography.labelLarge)
                        }
                        Text(
                            text = "github.com/LomiGoo",
                            color = Color.Blue,
                            textDecoration = TextDecoration.Underline,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .padding(bottom = 12.dp)
                                .clickable { uriHandler.openUri("https://github.com/LomiGoo") },
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Source Code:", style = MaterialTheme.typography.labelLarge)
                        }
                        Text(
                            text = "github.com/LomiGoo/classwork-manager",
                            color = Color.Blue,
                            textDecoration = TextDecoration.Underline,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .padding(bottom = 12.dp)
                                .clickable {
                                    uriHandler.openUri("https://github.com/LomiGoo/classwork-manager")
                                },
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Announcement,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Release Application List:", style = MaterialTheme.typography.labelLarge)
                        }
                        Text(
                            text = "github.com/LomiGoo/classwork-manager/releases",
                            color = Color.Blue,
                            textDecoration = TextDecoration.Underline,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .padding(bottom = 12.dp)
                                .clickable {
                                    uriHandler.openUri("https://github.com/LomiGoo/classwork-manager/releases")
                                },
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { viewModel.checkForUpdates() },
                            enabled = !isCheckingUpdates,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isCheckingUpdates) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Checking...")
                            } else {
                                Text("Check for Updates")
                            }
                        }

                        updateInfo?.let { info ->
                            if (info.isUpdateAvailable && !isCheckingUpdates) {
                                Button(
                                    onClick = { showUpdateOptions = true },
                                    modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                                ) {
                                    Text("Update Available: v${info.latestVersion}")
                                }
                            } else if (!info.isUpdateAvailable && !isCheckingUpdates) {
                                Text(
                                    text = "You are on the latest version.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSettings = false }) { Text("Close") }
                }
            )
        }
    }
}

@Composable
fun ThemeCircle(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(42.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(21.dp),
        color = color,
        border = if (isSelected) BorderStroke(3.dp, MaterialTheme.colorScheme.outline) else null
    ) { }
}

@Composable
fun ClassworkCard(
    classwork: Classwork,
    onToggleStatus: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    formatDate: (String) -> String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clickable { onToggleStatus() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = classwork.isCompleted,
                        onCheckedChange = { onToggleStatus() },
                        modifier = Modifier.size(24.dp).padding(end = 4.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = classwork.courseName,
                        fontWeight = FontWeight.Bold, // Reduced from ExtraBold
                        style = MaterialTheme.typography.titleMedium,
                        textDecoration = if (classwork.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }

                Text(
                    text = formatDate(classwork.dateTarget),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (classwork.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium // Reduced from Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = classwork.actionDescription,
                style = MaterialTheme.typography.bodyMedium,
                textDecoration = if (classwork.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                color = if (classwork.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 32.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Added: ${formatDate(classwork.dateCreated)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 32.dp)
                )

                Row {
                    TextButton(
                        onClick = onEdit,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("EDIT", fontWeight = FontWeight.Normal, fontSize = 12.sp) // Reduced from Bold
                    }
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("DELETE", fontWeight = FontWeight.Normal, fontSize = 12.sp) // Reduced from Bold
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassworkDialog(
    initialCourse: String = "",
    initialAction: String = "",
    initialTarget: String = "",
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var course by remember { mutableStateOf(initialCourse) }
    var action by remember { mutableStateOf(initialAction) }
    var target by remember { mutableStateOf(initialTarget) }

    var showDatePicker by remember { mutableStateOf(false) }
    val minDate = remember {
        Calendar.getInstance().apply {
            set(2026, Calendar.JULY, 1, 0, 0, 0)
        }.timeInMillis
    }
    val datePickerState = rememberDatePickerState(
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis >= minDate
            }

            override fun isSelectableYear(year: Int): Boolean {
                return year >= 2026
            }
        }
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    LaunchedEffect(isPressed) {
        if (isPressed) {
            showDatePicker = true
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                            target = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                        }
                        showDatePicker = false
                    },
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialCourse.isEmpty()) "Create Classwork" else "Update Classwork") },
        text = {
            Column {
                OutlinedTextField(
                    value = course,
                    onValueChange = { course = it },
                    label = { Text("Course/Subject") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = action,
                    onValueChange = { action = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = target,
                    onValueChange = { },
                    label = { Text("Target Date") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    interactionSource = interactionSource
                )
            }
        },
        confirmButton = {
            Button(onClick = { if (course.isNotBlank() && action.isNotBlank()) onSave(course, action, target) }) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
