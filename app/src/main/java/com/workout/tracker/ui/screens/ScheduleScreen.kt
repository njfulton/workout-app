package com.workout.tracker.ui.screens

import androidx.compose.foundation.layout.*
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
import com.workout.tracker.ui.viewmodel.ScheduleViewModel
import com.workout.tracker.ui.viewmodel.TemplateViewModel
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    navController: NavController,
    scheduleViewModel: ScheduleViewModel,
    templateViewModel: TemplateViewModel
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
                    ScheduleItemCard(item, dateFormat, scheduleViewModel)
                }
            }
        }
    }

    if (showAddDialog) {
        ScheduleWorkoutDialog(
            templates = templates,
            onDismiss = { showAddDialog = false },
            onScheduleTemplate = { templateId, daysFromNow ->
                scheduleViewModel.scheduleWorkout(templateId, LocalDate.now().plusDays(daysFromNow.toLong()))
                showAddDialog = false
            },
            onScheduleLabel = { label, daysFromNow ->
                scheduleViewModel.scheduleNonTemplate(label, LocalDate.now().plusDays(daysFromNow.toLong()))
                showAddDialog = false
            }
        )
    }
}

@Composable
fun ScheduleItemCard(
    item: ScheduledWorkoutWithTemplate,
    dateFormat: SimpleDateFormat,
    scheduleViewModel: ScheduleViewModel
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
            currentDate = dateFormat.format(Date(item.scheduledDate)),
            onDismiss = { showMoveDialog = false },
            onMove = { daysFromNow ->
                scheduleViewModel.reschedule(item, LocalDate.now().plusDays(daysFromNow.toLong()))
                showMoveDialog = false
            }
        )
    }
}

@Composable
fun MoveDateDialog(
    currentDate: String,
    onDismiss: () -> Unit,
    onMove: (Int) -> Unit
) {
    var selectedDays by remember { mutableStateOf(0) }
    val dateFormat = remember { SimpleDateFormat("EEE, MMM d", Locale.getDefault()) }
    val dayOptions = (0..14).map { days ->
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, days) }
        val label = when (days) {
            0 -> "Today - ${dateFormat.format(cal.time)}"
            1 -> "Tomorrow - ${dateFormat.format(cal.time)}"
            else -> dateFormat.format(cal.time)
        }
        label to days
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move Workout") },
        text = {
            Column {
                Text("Currently: $currentDate", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Text("Move to:", style = MaterialTheme.typography.labelMedium)
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(dayOptions) { (label, days) ->
                        TextButton(onClick = { selectedDays = days }, modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = selectedDays == days, onClick = null)
                                Spacer(Modifier.width(4.dp))
                                Text(label)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onMove(selectedDays) }) { Text("Move") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ScheduleWorkoutDialog(
    templates: List<TemplateWithExerciseCount>,
    onDismiss: () -> Unit,
    onScheduleTemplate: (Long, Int) -> Unit,
    onScheduleLabel: (String, Int) -> Unit
) {
    var selectedTemplate by remember { mutableStateOf<TemplateWithExerciseCount?>(null) }
    var selectedLabel by remember { mutableStateOf<String?>(null) }
    var customLabel by remember { mutableStateOf("") }
    var daysFromNow by remember { mutableStateOf(0) }

    val quickOptions = listOf("Rest Day", "Cardio", "Stretching / Mobility")
    val dayLabels = listOf(
        "Today" to 0, "Tomorrow" to 1, "In 2 days" to 2, "In 3 days" to 3,
        "In 4 days" to 4, "In 5 days" to 5, "In 6 days" to 6, "In 7 days" to 7
    )

    val hasSelection = selectedTemplate != null || selectedLabel != null || customLabel.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schedule") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Quick options:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    Text("Templates:", style = MaterialTheme.typography.labelMedium)
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

                Text("When:", style = MaterialTheme.typography.labelMedium)
                LazyColumn(modifier = Modifier.heightIn(max = 150.dp)) {
                    items(dayLabels) { (label, days) ->
                        TextButton(onClick = { daysFromNow = days }, modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = daysFromNow == days, onClick = null)
                                Spacer(Modifier.width(4.dp))
                                Text(label)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when {
                        selectedTemplate != null -> onScheduleTemplate(selectedTemplate!!.id, daysFromNow)
                        selectedLabel != null -> onScheduleLabel(selectedLabel!!, daysFromNow)
                        customLabel.isNotBlank() -> onScheduleLabel(customLabel, daysFromNow)
                    }
                },
                enabled = hasSelection
            ) { Text("Schedule") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
