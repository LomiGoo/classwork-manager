package com.lomigoo.classworkmanager // Ensure this matches your package structure!

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

// ===================================================================
// 1. DATA MODEL (Includes New isCompleted Property)
// ===================================================================
data class Classwork(
    val id: Int = 0,
    val courseName: String,
    val actionDescription: String,
    val dateCreated: String,
    val dateTarget: String,
    val isCompleted: Boolean = false // New completion tracking
)

// ===================================================================
// 2. NATIVE DATABASE HANDLER (Upgraded to Version 2 with Migration)
// ===================================================================
class ClassworkDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_ENTRIES)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            // Safe database migration strategy: Add column if upgrading from v1
            db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN $COLUMN_COMPLETED INTEGER DEFAULT 0")
        }
    }

    // CREATE COMMAND
    fun insertClasswork(course: String, action: String, created: String, target: String): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_COURSE, course)
            put(COLUMN_ACTION, action)
            put(COLUMN_CREATED, created)
            put(COLUMN_TARGET, target)
            put(COLUMN_COMPLETED, 0) // Defaults to false / pending
        }
        return db.insert(TABLE_NAME, null, values)
    }

    // READ COMMAND
    fun getAllClasswork(): List<Classwork> {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_NAME ORDER BY $COLUMN_ID DESC", null)
        val list = mutableListOf<Classwork>()

        with(cursor) {
            while (moveToNext()) {
                val id = getInt(getColumnIndexOrThrow(COLUMN_ID))
                val courseName = getString(getColumnIndexOrThrow(COLUMN_COURSE))
                val actionDescription = getString(getColumnIndexOrThrow(COLUMN_ACTION))
                val dateCreated = getString(getColumnIndexOrThrow(COLUMN_CREATED))
                val dateTarget = getString(getColumnIndexOrThrow(COLUMN_TARGET))
                // Retrieve boolean value from integer field (1 = true, 0 = false)
                val isCompleted = getInt(getColumnIndexOrThrow(COLUMN_COMPLETED)) == 1
                list.add(Classwork(id, courseName, actionDescription, dateCreated, dateTarget, isCompleted))
            }
            close()
        }
        return list
    }

    // UPDATE COMMAND (Supports editing status, text fields and dates)
    fun updateClasswork(classwork: Classwork): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_COURSE, classwork.courseName)
            put(COLUMN_ACTION, classwork.actionDescription)
            put(COLUMN_TARGET, classwork.dateTarget)
            put(COLUMN_COMPLETED, if (classwork.isCompleted) 1 else 0)
        }
        return db.update(TABLE_NAME, values, "$COLUMN_ID = ?", arrayOf(classwork.id.toString()))
    }

    // DELETE COMMAND
    fun deleteClasswork(id: Int): Int {
        val db = writableDatabase
        return db.delete(TABLE_NAME, "$COLUMN_ID = ?", arrayOf(id.toString()))
    }

    companion object {
        const val DATABASE_VERSION = 2 // Incremented database version
        const val DATABASE_NAME = "classwork_direct.db"
        const val TABLE_NAME = "classwork"
        const val COLUMN_ID = "id"
        const val COLUMN_COURSE = "courseName"
        const val COLUMN_ACTION = "actionDescription"
        const val COLUMN_CREATED = "dateCreated"
        const val COLUMN_TARGET = "dateTarget"
        const val COLUMN_COMPLETED = "isCompleted" // New schema column

        private const val SQL_CREATE_ENTRIES =
            "CREATE TABLE $TABLE_NAME (" +
                    "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "$COLUMN_COURSE TEXT," +
                    "$COLUMN_ACTION TEXT," +
                    "$COLUMN_CREATED TEXT," +
                    "$COLUMN_TARGET TEXT," +
                    "$COLUMN_COMPLETED INTEGER DEFAULT 0)" // Created with default 0
    }
}

// ===================================================================
// 3. VIEWMODEL (Handles Sorting and Filtering Computations)
// ===================================================================
class ClassworkViewModel(private val dbHelper: ClassworkDbHelper) : ViewModel() {
    private val _classworks = MutableStateFlow<List<Classwork>>(emptyList())
    val classworks: StateFlow<List<Classwork>> = _classworks.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _classworks.value = dbHelper.getAllClasswork()
        }
    }

    fun createClasswork(course: String, action: String, target: String) {
        viewModelScope.launch {
            val dateCreated = LocalDate.now(ZoneId.of("Asia/Manila")).toString()
            dbHelper.insertClasswork(course, action, dateCreated, target)
            loadData()
        }
    }

    fun updateClasswork(classwork: Classwork) {
        viewModelScope.launch {
            dbHelper.updateClasswork(classwork)
            loadData()
        }
    }

    fun deleteClasswork(classwork: Classwork) {
        viewModelScope.launch {
            dbHelper.deleteClasswork(classwork.id)
            loadData()
        }
    }
}

class ClassworkViewModelFactory(private val dbHelper: ClassworkDbHelper) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ClassworkViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ClassworkViewModel(dbHelper) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// ===================================================================
// 4. MAIN ACTIVITY & ENTRY POINT
// ===================================================================
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dbHelper = ClassworkDbHelper(applicationContext)
        val factory = ClassworkViewModelFactory(dbHelper)
        val viewModel = ViewModelProvider(this, factory).get(ClassworkViewModel::class.java)

        setContent {
            MaterialTheme {
                ClassworkApp(viewModel)
            }
        }
    }
}

// ===================================================================
// 5. JETPACK COMPOSE UI
// ===================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassworkApp(viewModel: ClassworkViewModel) {
    val rawClassworks by viewModel.classworks.collectAsState()

    // UI Local State for Filter and Sort Actions
    var currentFilter by remember { mutableStateOf("All") } // "All", "Pending", "Done"
    var currentSortOrder by remember { mutableStateOf("Created Date") } // "Created Date", "Course Name", "Target Date"
    var showSortMenu by remember { mutableStateOf(false) }

    var showAddDialog by remember { mutableStateOf(false) }
    var classworkToEdit by remember { mutableStateOf<Classwork?>(null) }
    var showAppInfo by remember { mutableStateOf(false) }

    // Computes and Applies Filters and Sorting dynamically based on UI States
    val processedClassworks = remember(rawClassworks, currentFilter, currentSortOrder) {
        val filtered = when (currentFilter) {
            "Pending" -> rawClassworks.filter { !it.isCompleted }
            "Done" -> rawClassworks.filter { it.isCompleted }
            else -> rawClassworks
        }

        when (currentSortOrder) {
            "Course Name" -> filtered.sortedBy { it.courseName.lowercase() }
            "Target Date" -> filtered.sortedBy { it.dateTarget.lowercase() }
            else -> filtered.sortedByDescending { it.id } // Default (Created order)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Classwork Manager") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                actions = {
                    // Sorting Selection Menu Button
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Text("⇅", style = MaterialTheme.typography.titleLarge)
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
                        }
                    }
                    IconButton(onClick = { showAppInfo = true }) {
                        Text("ℹ", style = MaterialTheme.typography.titleLarge)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Text("+", style = MaterialTheme.typography.titleLarge)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // Filtering Chips (All, Pending, Done)
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

            // Displays active sorting choice
            Text(
                text = "Sorted by: $currentSortOrder",
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
                            onDelete = { viewModel.deleteClasswork(item) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        // CREATE DIALOG (replaces CLI CreateClassworkCmd)
        if (showAddDialog) {
            ClassworkDialog(
                onDismiss = { showAddDialog = false },
                onSave = { course, action, target ->
                    viewModel.createClasswork(course, action, target)
                    showAddDialog = false
                }
            )
        }

        // UPDATE DIALOG (replaces CLI UpdateClassworkCmd)
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

        // APP INFO DIALOG (replaces CLI AppInfo)
        if (showAppInfo) {
            AlertDialog(
                onDismissRequest = { showAppInfo = false },
                title = { Text("App Info") },
                text = {
                    Column {
                        Text("Version: 3.0.0 (Android Port)")
                        Text("Author: LomiGoo")
                        Text("GitHub: github.com/LomiGoo")
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
                // Checkbox to toggle completion status (Pending -> Done / Done -> Pending)
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
                    Text(text = "Target: ${classwork.dateTarget}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                    Text(text = "Created: ${classwork.dateCreated}", style = MaterialTheme.typography.labelSmall)
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialCourse.isEmpty()) "Create Classwork" else "Update Classwork") },
        text = {
            Column {
                OutlinedTextField(value = course, onValueChange = { course = it }, label = { Text("Course Name") })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = action, onValueChange = { action = it }, label = { Text("Action Description") })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = target, onValueChange = { target = it }, label = { Text("Target Date") })
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