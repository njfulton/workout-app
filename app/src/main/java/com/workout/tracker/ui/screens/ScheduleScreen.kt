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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.workout.tracker.data.dao.TemplateWithExerciseCount
import com.workout.tracker.ui.viewmodel.ScheduleViewModel
import com.workout.tracker.ui.viewmodel.TemplateViewModel
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
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
            if (templates.isNotEmpty()) {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Schedule Workout")
                }
            }
        }
    ) { padding ->
        if (schedule.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    Text("No upcoming workouts", style = MaterialTheme.typography.titleMedium)
                    if (templates.isEmpty()) {
                        Text("Create a template first to schedule workouts", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Text("Tap + to schedule a workout", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(schedule) { item ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.templateName, style = MaterialTheme.typography.bodyLarge)
                                Text(dateFormat.format(Date(item.scheduledDate)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (item.isCompleted) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Completed", tint = MaterialTheme.colorScheme.primary)
                            } else {
                                IconButton(onClick = { scheduleViewModel.deleteScheduledWorkout(item) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        ScheduleWorkoutDialog(
            templates = templates,
            onDismiss = { showAddDialog = false },
            onSchedule = { templateId, daysFromNow ->
                scheduleViewModel.scheduleWorkout(templateId, LocalDate.now().plusDays(daysFromNow.toLong()))
                showAddDialog = false
            }
        )
    }
}

@Composable
fun ScheduleWorkoutDialog(
    templates: List<TemplateWithExerciseCount>,
    onDismiss: () -> Unit,
    onSchedule: (Long, Int) -> Unit
) {
    var selectedTemplate by remember { mutableStateOf<TemplateWithExerciseCount?>(null) }
    var daysFromNow by remember { mutableStateOf("0") }
    val dayLabels = listOf("Today" to 0, "Tomorrow" to 1, "In 2 days" to 2, "In 3 days" to 3, "In 4 days" to 4, "In 5 days" to 5, "In 6 days" to 6, "In 7 days" to 7)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schedule Workout") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Template:", style = MaterialTheme.typography.labelMedium)
                LazyColumn(modifier = Modifier.heightIn(max = 150.dp)) {
                    items(templates) { template ->
                        TextButton(onClick = { selectedTemplate = template }, modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = selectedTemplate == template, onClick = { selectedTemplate = template })
                                Text("${template.name} (${template.exerciseCount} ex.)")
                            }
                        }
                    }
                }
                Text("When:", style = MaterialTheme.typography.labelMedium)
                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                    items(dayLabels) { (label, days) ->
                        TextButton(onClick = { daysFromNow = days.toString() }, modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = daysFromNow == days.toString(), onClick = { daysFromNow = days.toString() })
                                Text(label)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { selectedTemplate?.let { onSchedule(it.id, daysFromNow.toInt()) } },
                enabled = selectedTemplate != null
            ) { Text("Schedule") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
