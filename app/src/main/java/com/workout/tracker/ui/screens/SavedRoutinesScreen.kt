package com.workout.tracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.workout.tracker.data.entity.RoutineUsageHistory
import com.workout.tracker.data.entity.SavedRoutine
import com.workout.tracker.ui.viewmodel.TemplateViewModel
import com.workout.tracker.ui.viewmodel.ScheduleViewModel
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedRoutinesScreen(
    navController: NavController,
    templateViewModel: TemplateViewModel,
    scheduleViewModel: ScheduleViewModel
) {
    val savedRoutines by templateViewModel.savedRoutines.collectAsStateWithLifecycle()
    var expandedRoutineId by remember { mutableStateOf<Long?>(null) }
    var editingNotesRoutine by remember { mutableStateOf<SavedRoutine?>(null) }
    var reimportRoutine by remember { mutableStateOf<SavedRoutine?>(null) }
    var deleteConfirmRoutine by remember { mutableStateOf<SavedRoutine?>(null) }
    val importResult by templateViewModel.importResult.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Saved Routines") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (savedRoutines.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("No saved routines", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Routines are saved automatically when you import them",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(savedRoutines, key = { it.id }) { routine ->
                    SavedRoutineCard(
                        routine = routine,
                        isExpanded = expandedRoutineId == routine.id,
                        templateViewModel = templateViewModel,
                        onToggleExpand = {
                            expandedRoutineId = if (expandedRoutineId == routine.id) null else routine.id
                        },
                        onEditNotes = { editingNotesRoutine = routine },
                        onReimport = { reimportRoutine = routine },
                        onDelete = { deleteConfirmRoutine = routine }
                    )
                }
            }
        }
    }

    // Edit notes dialog
    editingNotesRoutine?.let { routine ->
        EditNotesDialog(
            currentNotes = routine.notes ?: "",
            onDismiss = { editingNotesRoutine = null },
            onSave = { notes ->
                templateViewModel.updateSavedRoutineNotes(routine.id, notes)
                editingNotesRoutine = null
            }
        )
    }

    // Reimport dialog
    reimportRoutine?.let { routine ->
        ReimportDialog(
            routine = routine,
            templateViewModel = templateViewModel,
            scheduleViewModel = scheduleViewModel,
            onDismiss = { reimportRoutine = null },
            navController = navController
        )
    }

    // Delete confirmation
    deleteConfirmRoutine?.let { routine ->
        AlertDialog(
            onDismissRequest = { deleteConfirmRoutine = null },
            title = { Text("Delete Routine") },
            text = { Text("Delete \"${routine.name}\"? This only removes the saved routine, not any workout history.") },
            confirmButton = {
                TextButton(onClick = {
                    templateViewModel.deleteSavedRoutine(routine)
                    deleteConfirmRoutine = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmRoutine = null }) { Text("Cancel") }
            }
        )
    }

    // Show import result
    if (importResult != null && reimportRoutine != null) {
        LaunchedEffect(importResult) {
            reimportRoutine = null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedRoutineCard(
    routine: SavedRoutine,
    isExpanded: Boolean,
    templateViewModel: TemplateViewModel,
    onToggleExpand: () -> Unit,
    onEditNotes: () -> Unit,
    onReimport: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val routineNames = templateViewModel.parseRoutineNamesJson(routine.routineNamesJson)
    val dayAssignments = templateViewModel.parseDayAssignmentsJson(routine.dayAssignmentsJson)
    val usageHistory by templateViewModel.getUsageHistory(routine.id).collectAsStateWithLifecycle(emptyList())

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onToggleExpand
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        routine.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${routineNames.size} routines \u2022 ${routine.weekCount} weeks",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand"
                )
            }

            if (isExpanded) {
                Spacer(Modifier.height(12.dp))
                Divider()
                Spacer(Modifier.height(12.dp))

                // Routine names with day assignments
                Text(
                    "Routines",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                routineNames.forEachIndexed { index, name ->
                    val days = dayAssignments[index] ?: emptyList()
                    val dayText = days.joinToString(", ") {
                        it.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                    }
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            name,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        if (dayText.isNotEmpty()) {
                            Text(
                                dayText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Notes
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Notes",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onEditNotes, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit notes", modifier = Modifier.size(16.dp))
                    }
                }
                Text(
                    routine.notes ?: "No notes yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (routine.notes != null) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Usage history
                if (usageHistory.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Previous Runs",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    usageHistory.forEach { usage ->
                        val start = dateFormat.format(Date(usage.startDate))
                        val end = usage.endDate?.let { dateFormat.format(Date(it)) } ?: "ongoing"
                        Row(
                            modifier = Modifier.padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "$start \u2013 $end",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // Action buttons
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onReimport,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Re-import")
                    }
                    OutlinedButton(
                        onClick = onDelete
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun EditNotesDialog(
    currentNotes: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var notes by remember { mutableStateOf(currentNotes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Routine Notes") },
        text = {
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                maxLines = 10,
                placeholder = { Text("How did this routine go? What would you change?") }
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(notes) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReimportDialog(
    routine: SavedRoutine,
    templateViewModel: TemplateViewModel,
    scheduleViewModel: ScheduleViewModel,
    onDismiss: () -> Unit,
    navController: NavController
) {
    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var weekCount by remember { mutableStateOf(routine.weekCount) }
    var dayAssignments by remember {
        mutableStateOf(templateViewModel.parseDayAssignmentsJson(routine.dayAssignmentsJson))
    }
    val routineNames = remember { templateViewModel.parseRoutineNamesJson(routine.routineNamesJson) }
    var showDatePicker by remember { mutableStateOf(false) }
    var clearFirst by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Re-import: ${routine.name}") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Clear schedule option
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = clearFirst, onCheckedChange = { clearFirst = it })
                    Spacer(Modifier.width(4.dp))
                    Text("Clear future schedule first", style = MaterialTheme.typography.bodyMedium)
                }

                // Start date
                Text("Start Date", style = MaterialTheme.typography.labelMedium)
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    val formatter = java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d, yyyy")
                    Text(startDate.format(formatter))
                }

                // Week count
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Weeks:", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = { if (weekCount > 1) weekCount-- },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
                    }
                    Text("$weekCount", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    IconButton(
                        onClick = { weekCount++ },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp))
                    }
                }

                // Day assignments
                Text("Day Assignments", style = MaterialTheme.typography.labelMedium)
                routineNames.forEachIndexed { index, name ->
                    Column {
                        Text(name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(4.dp))
                        DayOfWeekPicker(
                            selectedDays = dayAssignments[index] ?: emptyList(),
                            onDaysChanged = { days ->
                                dayAssignments = dayAssignments.toMutableMap().apply {
                                    put(index, days)
                                }
                            }
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (clearFirst) {
                        scheduleViewModel.clearFutureSchedule()
                    }
                    templateViewModel.reimportSavedRoutine(routine, startDate, dayAssignments, weekCount)
                    onDismiss()
                    navController.navigate("schedule") { popUpTo("home") }
                }
            ) { Text("Import") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        startDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
