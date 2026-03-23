package com.workout.tracker.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.workout.tracker.data.dao.TemplateWithExerciseCount
import com.workout.tracker.ui.navigation.Screen
import com.workout.tracker.ui.viewmodel.TemplateViewModel
import java.time.*
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineBuilderScreen(
    navController: NavController,
    templateViewModel: TemplateViewModel
) {
    val templates by templateViewModel.templates.collectAsStateWithLifecycle()
    val importResult by templateViewModel.importResult.collectAsStateWithLifecycle()

    // Step tracking: 0 = select templates, 1 = configure schedule
    var step by remember { mutableStateOf(0) }
    var selectedTemplates by remember { mutableStateOf<List<TemplateWithExerciseCount>>(emptyList()) }
    var routineName by remember { mutableStateOf("") }
    var weekCount by remember { mutableStateOf(10) }
    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var clearFutureFirst by remember { mutableStateOf(true) }
    var showDatePicker by remember { mutableStateOf(false) }
    var dayAssignments by remember { mutableStateOf<Map<Int, List<DayOfWeek>>>(emptyMap()) }

    // Auto-generate routine name from selected templates
    LaunchedEffect(selectedTemplates) {
        if (routineName.isEmpty() || routineName == generateRoutineName(selectedTemplates.dropLast(1))) {
            routineName = generateRoutineName(selectedTemplates)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (step == 0) "Build Routine" else "Configure Schedule") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (step == 1) step = 0 else navController.popBackStack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (step == 0) {
            // Step 1: Select templates
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Selection summary
                if (selectedTemplates.isNotEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        tonalElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${selectedTemplates.size} workout${if (selectedTemplates.size != 1) "s" else ""} selected",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    selectedTemplates.joinToString(", ") { it.name },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 2
                                )
                            }
                            Button(
                                onClick = {
                                    dayAssignments = templateViewModel.getDefaultDayAssignmentsPublic(selectedTemplates.size)
                                    step = 1
                                }
                            ) {
                                Text("Next")
                            }
                        }
                    }
                }

                if (templates.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.FitnessCenter,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(16.dp))
                            Text("No templates yet", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Create or import workout templates first",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { navController.navigate(Screen.ImportRoutine.route) }) {
                                Text("Import Routine")
                            }
                        }
                    }
                } else {
                    Text(
                        "Select workouts to include in your routine:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(templates) { template ->
                            val isSelected = selectedTemplates.any { it.id == template.id }
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    selectedTemplates = if (isSelected) {
                                        selectedTemplates.filter { it.id != template.id }
                                    } else {
                                        selectedTemplates + template
                                    }
                                },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = {
                                            selectedTemplates = if (isSelected) {
                                                selectedTemplates.filter { t -> t.id != template.id }
                                            } else {
                                                selectedTemplates + template
                                            }
                                        }
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            template.name,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            "${template.exerciseCount} exercises",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (isSelected) {
                                        val order = selectedTemplates.indexOfFirst { it.id == template.id } + 1
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    "$order",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        } else {
            // Step 2: Configure schedule
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Routine name
                item {
                    OutlinedTextField(
                        value = routineName,
                        onValueChange = { routineName = it },
                        label = { Text("Routine Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Clear future schedule
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = clearFutureFirst, onCheckedChange = { clearFutureFirst = it })
                        Spacer(Modifier.width(4.dp))
                        Text("Clear future schedule first", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                // Start date
                item {
                    Text("Start Date", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        val formatter = java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d, yyyy")
                        Text(startDate.format(formatter))
                    }
                }

                // Week count
                item {
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
                }

                // Day assignments
                item {
                    Text("Day Assignments", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Choose which days each workout runs:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                items(selectedTemplates.size) { index ->
                    val template = selectedTemplates[index]
                    Column {
                        Text(
                            template.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(4.dp))
                        DayOfWeekPicker(
                            selectedDays = dayAssignments[index] ?: emptyList(),
                            onDaysChanged = { days ->
                                dayAssignments = dayAssignments.toMutableMap().apply {
                                    put(index, days)
                                }
                            }
                        )
                    }
                }

                // Build button
                item {
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            templateViewModel.buildRoutineFromTemplates(
                                routineName = routineName.ifBlank { "My Routine" },
                                templateIds = selectedTemplates.map { it.id },
                                templateNames = selectedTemplates.map { it.name },
                                dayAssignments = dayAssignments,
                                weeks = weekCount,
                                startDate = startDate,
                                clearFutureFirst = clearFutureFirst
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = routineName.isNotBlank() && dayAssignments.values.any { it.isNotEmpty() }
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Build & Schedule Routine")
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }

    // Date picker
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

    // Show result and navigate
    importResult?.let { result ->
        AlertDialog(
            onDismissRequest = { templateViewModel.clearImportResult() },
            title = { Text("Routine Built") },
            text = { Text(result) },
            confirmButton = {
                TextButton(onClick = {
                    templateViewModel.clearImportResult()
                    navController.navigate(Screen.Schedule.route) {
                        popUpTo(Screen.Home.route)
                    }
                }) { Text("View Schedule") }
            },
            dismissButton = {
                TextButton(onClick = {
                    templateViewModel.clearImportResult()
                    navController.popBackStack()
                }) { Text("Done") }
            }
        )
    }
}

private fun generateRoutineName(templates: List<TemplateWithExerciseCount>): String {
    return when {
        templates.isEmpty() -> ""
        templates.size == 1 -> templates[0].name
        templates.size <= 3 -> templates.joinToString("/") { it.name.split(" ").first() }
        else -> "${templates.size}-Day Program"
    }
}
