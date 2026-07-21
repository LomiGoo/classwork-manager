package com.lomigoo.classworkmanager.ui

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
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
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
import com.lomigoo.classworkmanager.R
import com.lomigoo.classworkmanager.data.Classwork
import dev.jeziellago.compose.markdowntext.MarkdownText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
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

    var currentFilter by remember { mutableStateOf("All") }
    var currentSortOrder by remember { mutableStateOf("Created Date") }
    var isAscending by remember { mutableStateOf(value = false) } // Default to Descending for Created Date
    var showSortMenu by remember { mutableStateOf(value = false) }

    var showAddDialog by remember { mutableStateOf(value = false) }
    var classworkToEdit by remember { mutableStateOf<Classwork?>(null) }
    var classworkToDelete by remember { mutableStateOf<Classwork?>(null) }
    var showAppInfo by remember { mutableStateOf(value = false) }
    var showUpdateOptions by remember { mutableStateOf(value = false) }
    var showNoUpdatesDialog by remember { mutableStateOf(value = false) }

    val uriHandler = LocalUriHandler.current
    val snackbarHostState = remember { SnackbarHostState() }

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
            else -> filtered.sortedBy { it.id } // Sort by ID (Creation order)
        }

        if (isAscending) sorted else sorted.reversed()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Classwork Manager") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                actions = {
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
                    IconButton(onClick = { showAppInfo = true }) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = "App Info")
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
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("All", "Pending", "Done").forEach { filter ->
                        FilterChip(
                            selected = currentFilter == filter,
                            onClick = { currentFilter = filter },
                            label = { Text(filter) },
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }

                Text(
                    text = "Sorted by: $currentSortOrder (${if (isAscending) "ASC" else "DESC"})",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                LazyColumn(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).weight(1f)) {
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
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            // Floating "What's New" / Update Notice (Bottom Left)
            SmallFloatingActionButton(
                onClick = {
                    if (updateInfo?.isUpdateAvailable == true) {
                        showUpdateOptions = true
                    } else if (updateInfo?.isWhatsNewAvailable == true) {
                        viewModel.showWhatsNew()
                    } else {
                        showNoUpdatesDialog = true
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            ) {
                BadgedBox(
                    badge = {
                        if (updateInfo?.isUpdateAvailable == true) {
                            Badge(containerColor = Color.Red) {
                                Text("!", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Announcement,
                        contentDescription = "Updates"
                    )
                }
            }
        }

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
                    Text(
                        text = info.releaseName ?: "What's New",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
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

        if (showAppInfo) {
            AlertDialog(
                onDismissRequest = { showAppInfo = false },
                title = { Text("App Info") },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
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

                        Text("GitHub:", style = MaterialTheme.typography.labelLarge)
                        Text(
                            text = "github.com/LomiGoo",
                            color = Color.Blue,
                            textDecoration = TextDecoration.Underline,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .padding(bottom = 8.dp)
                                .clickable { uriHandler.openUri("https://github.com/LomiGoo") },
                        )

                        Text("Source Code:", style = MaterialTheme.typography.labelLarge)
                        Text(
                            text = "github.com/LomiGoo/classwork-manager",
                            color = Color.Blue,
                            textDecoration = TextDecoration.Underline,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .padding(bottom = 8.dp)
                                .clickable {
                                    uriHandler.openUri("https://github.com/LomiGoo/classwork-manager")
                                },
                        )

                        Text("Release Application List:", style = MaterialTheme.typography.labelLarge)
                        Text(
                            text = "github.com/LomiGoo/classwork-manager/releases",
                            color = Color.Blue,
                            textDecoration = TextDecoration.Underline,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .padding(bottom = 8.dp)
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
                    TextButton(onClick = { showAppInfo = false }) { Text("Close") }
                }
            )
        }
    }
}

@Composable
fun ClassworkCard(
    classwork: Classwork,
    onToggleStatus: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onToggleStatus() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = classwork.isCompleted,
                    onCheckedChange = { onToggleStatus() },
                    modifier = Modifier.padding(end = 8.dp)
                )

                Column {
                    Text(
                        text = classwork.courseName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        textDecoration = if (classwork.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    )
                    Text(
                        text = classwork.actionDescription,
                        style = MaterialTheme.typography.bodyLarge,
                        textDecoration = if (classwork.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (classwork.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Target: ${classwork.dateTarget}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Created: ${classwork.dateCreated}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row {
                TextButton(onClick = onEdit) {
                    Text("EDIT", fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onDelete) {
                    Text("DELETE", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
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

    // Interaction source to detect clicks on the read-only field
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
                    onValueChange = { /* Handled by DatePicker */ },
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
