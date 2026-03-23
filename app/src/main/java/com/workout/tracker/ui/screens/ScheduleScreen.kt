package com.workout.tracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.workout.tracker.data.dao.ScheduledWorkoutWithTemplate
import com.workout.tracker.data.dao.TemplateWithExerciseCount
import com.workout.tracker.data.entity.WorkoutType
import com.workout.tracker.ui.navigation.Screen
import com.workout.tracker.ui.viewmodel.ScheduleViewModel
import com.workout.tracker.ui.viewmodel.TemplateViewModel
import com.workout.tracker.ui.viewmodel.WorkoutViewModel
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    navController: NavController,
    scheduleViewModel: ScheduleViewModel,
    templateViewModel: TemplateViewModel,
    workoutViewModel: WorkoutViewModel
) {
    val schedule by scheduleViewModel.upcomingSchedule.collectAsStateWithLifecycle()
    val templates by templateViewModel.templates.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("EEE, MMM d", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Schedule") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Schedule")
            }
        }
    ) { padding ->
        if (schedule.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    Text("No upcoming schedule", style = MaterialTheme.typography.titleMedium)
                    Text("Tap + to add workouts, rest days, or cardio", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(schedule, key = { it.id }) { item ->
                    ScheduleItemCard(item, dateFormat, scheduleViewModel, workoutViewModel, navController)
                }
            }
        }
    }

    if (showAddDialog) {
        ScheduleWorkoutDialog(
            templates = templates,
            onDismiss = { showAddDialog = false },
            onScheduleTemplate = { templateId, date ->
                scheduleViewModel.scheduleWorkout(templateId, date)
                showAddDialog = false
            },
            onScheduleLabel = { label, date ->
                scheduleViewModel.scheduleNonTemplate(label, date)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun ScheduleItemCard(
    item: ScheduledWorkoutWithTemplate,
    dateFormat: SimpleDateFormat,
    scheduleViewModel: ScheduleViewModel,
    workoutViewModel: WorkoutViewModel,
    navController: NavController
) {
    val displayName = item.templateName ?: item.label ?: "Unknown"
    val isRestDay = item.label?.lowercase()?.contains("rest") == true
    val isCardio = item.label?.lowercase()?.contains("cardio") == true
    val isDone = item.isCompleted || item.isSkipped
    var showMoveDialog by remember { mutableStateOf(false) }

    val icon = when {
        isRestDay -> Icons.Default.Hotel
        isCardio -> Icons.Default.DirectionsRun
        item.templateId != null -> Icons.Default.FitnessCenter
        else -> Icons.Default.Event
    }

    val containerColor = when {
        item.isCompleted -> MaterialTheme.colorScheme.secondaryContainer
        item.isSkipped -> MaterialTheme.colorScheme.surfaceVariant
        isRestDay -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        textDecoration = if (item.isSkipped) TextDecoration.LineThrough else TextDecoration.None
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            dateFormat.format(Date(item.scheduledDate)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (item.isCompleted) {
                            Spacer(Modifier.width(8.dp))
                            Text("Done", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        } else if (item.isSkipped) {
                            Spacer(Modifier.width(8.dp))
                            Text("Skipped", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                if (item.isCompleted) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Completed", tint = MaterialTheme.colorScheme.primary)
                } else if (item.isSkipped) {
                    Icon(Icons.Default.Cancel, contentDescription = "Skipped", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp))
                }
            }

            // Action buttons for non-completed items
            if (!isDone) {
                Spacer(Modifier.height(8.dp))

                // Start workout button (only for template-based items)
                if (item.templateId != null) {
                    Button(
                        onClick = {
                            workoutViewModel.startWorkout(
                                name = item.templateName ?: "Workout",
                                type = WorkoutType.STRENGTH,
                                templateId = item.templateId
                            )
                            scheduleViewModel.markCompleted(item)
                            navController.navigate(Screen.ActiveWorkout.route) {
                                popUpTo(Screen.Home.route)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Start Workout")
                    }
                    Spacer(Modifier.height(4.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Complete button
                    FilledTonalButton(
                        onClick = { scheduleViewModel.markCompleted(item) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Done", style = MaterialTheme.typography.labelMedium)
                    }

                    // Skip button
                    OutlinedButton(
                        onClick = { scheduleViewModel.markSkipped(item) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Skip", style = MaterialTheme.typography.labelMedium)
                    }

                    // Move button
                    OutlinedButton(
                        onClick = { showMoveDialog = true },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.EditCalendar, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Move", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }

    if (showMoveDialog) {
        MoveDateDialog(
            currentDateMillis = item.scheduledDate,
            onDismiss = { showMoveDialog = false },
            onMove = { newDate ->
                scheduleViewModel.reschedule(item, newDate)
                showMoveDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoveDateDialog(
    currentDateMillis: Long,
    onDismiss: () -> Unit,
    onMove: (LocalDate) -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = currentDateMillis
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                    val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                    onMove(date)
                }
            }) { Text("Move") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ScheduleWorkoutDialog(
    templates: List<TemplateWithExerciseCount>,
    onDismiss: () -> Unit,
    onScheduleTemplate: (Long, LocalDate) -> Unit,
    onScheduleLabel: (String, LocalDate) -> Unit
) {
    var selectedTemplate by remember { mutableStateOf<TemplateWithExerciseCount?>(null) }
    var selectedLabel by remember { mutableStateOf<String?>(null) }
    var customLabel by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    val quickOptions = listOf("Rest Day", "Cardio", "Mobility")
    val hasSelection = selectedTemplate != null || selectedLabel != null || customLabel.isNotBlank()
    val dateFormat = remember { SimpleDateFormat("EEE, MMM d", Locale.getDefault()) }
    val displayDate = dateFormat.format(
        Date.from(selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schedule") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Quick options - wrapped
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    quickOptions.forEach { option ->
                        FilterChip(
                            selected = selectedLabel == option,
                            onClick = {
                                selectedLabel = if (selectedLabel == option) null else option
                                selectedTemplate = null
                                customLabel = ""
                            },
                            label = { Text(option) }
                        )
                    }
                }

                OutlinedTextField(
                    value = customLabel,
                    onValueChange = {
                        customLabel = it
                        if (it.isNotBlank()) { selectedLabel = null; selectedTemplate = null }
                    },
                    label = { Text("Or type custom...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (templates.isNotEmpty()) {
                    Text("Template:", style = MaterialTheme.typography.labelMedium)
                    LazyColumn(modifier = Modifier.heightIn(max = 120.dp)) {
                        items(templates) { template ->
                            TextButton(
                                onClick = {
                                    selectedTemplate = if (selectedTemplate == template) null else template
                                    selectedLabel = null; customLabel = ""
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = selectedTemplate == template, onClick = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("${template.name} (${template.exerciseCount} ex.)")
                                }
                            }
                        }
                    }
                }

                Divider()

                // Date selector
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(displayDate)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when {
                        selectedTemplate != null -> onScheduleTemplate(selectedTemplate!!.id, selectedDate)
                        selectedLabel != null -> onScheduleLabel(selectedLabel!!, selectedDate)
                        customLabel.isNotBlank() -> onScheduleLabel(customLabel, selectedDate)
                    }
                },
                enabled = hasSelection
            ) { Text("Schedule") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        selectedDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
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
