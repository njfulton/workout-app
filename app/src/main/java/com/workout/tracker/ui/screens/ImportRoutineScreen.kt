package com.workout.tracker.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import com.workout.tracker.ui.viewmodel.TemplateViewModel
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportRoutineScreen(
    navController: NavController,
    templateViewModel: TemplateViewModel
) {
    var routineText by remember { mutableStateOf("") }
    var parseResult by remember { mutableStateOf<TemplateViewModel.ParseResult?>(null) }
    var parseError by remember { mutableStateOf<String?>(null) }
    var isImporting by remember { mutableStateOf(false) }
    val importResult by templateViewModel.importResult.collectAsStateWithLifecycle()

    // Schedule config state
    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var weekCount by remember { mutableStateOf(10) }
    var dayAssignments by remember { mutableStateOf<Map<Int, List<DayOfWeek>>>(emptyMap()) }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(importResult) {
        if (importResult != null) isImporting = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import Routine") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (parseResult != null && importResult == null) {
                            // Go back to text entry
                            parseResult = null
                            parseError = null
                        } else {
                            navController.popBackStack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (importResult != null) {
                // Phase 3: Show result
                val isError = importResult!!.startsWith("Import failed")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isError) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (isError) Icons.Default.Error else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (isError) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (isError) "Error" else "Import Complete",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(importResult!!, style = MaterialTheme.typography.bodyMedium)
                        if (!isError) {
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = {
                                    templateViewModel.clearImportResult()
                                    navController.navigate("schedule") { popUpTo("home") }
                                }) { Text("View Schedule") }
                                OutlinedButton(onClick = {
                                    templateViewModel.clearImportResult()
                                    navController.navigate("templates") { popUpTo("home") }
                                }) { Text("View Templates") }
                            }
                        }
                    }
                }
            } else if (parseResult != null) {
                // Phase 2: Configure schedule
                val result = parseResult!!

                Text(
                    "Configure Schedule",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Found ${result.routines.size} routine(s). Choose which days each routine runs and when to start.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Start date
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
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
                }

                // Week count
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Program Length", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = { if (weekCount > 1) weekCount-- },
                                enabled = weekCount > 1
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease")
                            }
                            Text(
                                "$weekCount weeks",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            IconButton(onClick = { weekCount++ }) {
                                Icon(Icons.Default.Add, contentDescription = "Increase")
                            }
                        }
                    }
                }

                // Per-routine day assignments
                Text(
                    "Day Assignments",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                result.routines.forEachIndexed { index, routine ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                routine.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "${routine.exerciseCount} exercises",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
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
                }

                // Generate button
                Button(
                    onClick = {
                        isImporting = true
                        templateViewModel.clearImportResult()
                        templateViewModel.importWithSchedule(startDate, dayAssignments, weekCount)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isImporting && dayAssignments.values.any { it.isNotEmpty() }
                ) {
                    if (isImporting) {
                        Text("Generating\u2026")
                    } else {
                        Icon(Icons.Default.Event, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Import & Build Schedule")
                    }
                }
            } else {
                // Phase 1: Text entry
                Text(
                    "Paste your routine below",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Supports multiple days, sets x reps, rest times, supersets (A1/A2), and progression phases. Creates templates and builds your schedule automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = routineText,
                    onValueChange = { routineText = it },
                    label = { Text("Routine text") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp),
                    maxLines = 50
                )

                if (parseError != null) {
                    Text(
                        parseError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Button(
                    onClick = {
                        val result = templateViewModel.parseRoutineText(routineText)
                        if (result != null) {
                            parseResult = result
                            parseError = null
                            weekCount = result.detectedWeeks
                            dayAssignments = result.defaultDayAssignments
                        } else {
                            parseError = "Could not parse any routines from the text. Make sure each routine starts with \"Routine:\" and includes exercises."
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = routineText.isNotBlank()
                ) {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Parse Routines")
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

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

@Composable
fun DayOfWeekPicker(
    selectedDays: List<DayOfWeek>,
    onDaysChanged: (List<DayOfWeek>) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        DayOfWeek.values().forEach { day ->
            val isSelected = day in selectedDays
            val label = day.getDisplayName(TextStyle.NARROW, Locale.getDefault())
            Surface(
                modifier = Modifier.size(36.dp),
                shape = CircleShape,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
                border = if (isSelected) null
                else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                onClick = {
                    val newDays = if (isSelected) selectedDays - day else selectedDays + day
                    onDaysChanged(newDays.sortedBy { it.value })
                }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
