package com.workout.tracker.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.workout.tracker.data.entity.TemplateExercise
import com.workout.tracker.ui.navigation.Screen
import com.workout.tracker.ui.viewmodel.TemplateExerciseDetail
import com.workout.tracker.ui.viewmodel.TemplateViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineOverviewScreen(
    navController: NavController,
    templateViewModel: TemplateViewModel
) {
    val overview by templateViewModel.routineOverview.collectAsStateWithLifecycle()
    var expandedWeek by remember { mutableStateOf<Int?>(null) }

    // Cache of loaded template exercises: templateId -> list of details
    val templateExercisesCache = remember { mutableStateMapOf<Long, List<TemplateExerciseDetail>>() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        templateViewModel.loadRoutineOverview()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Routine") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        val data = overview
        if (data == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No active routine.\nImport a routine from the Templates screen.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    data.routine.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Week ${data.currentWeek} of ${data.totalWeeks}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Progress bar
            item {
                LinearProgressIndicator(
                    progress = data.currentWeek.toFloat() / data.totalWeeks.toFloat(),
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                )
            }

            // Weeks
            items(data.weeks.size) { idx ->
                val week = data.weeks[idx]
                val isExpanded = expandedWeek == week.weekNumber
                RoutineWeekCard(
                    week = week,
                    isExpanded = isExpanded,
                    templateExercisesCache = templateExercisesCache,
                    onToggleExpand = {
                        if (isExpanded) {
                            expandedWeek = null
                        } else {
                            expandedWeek = week.weekNumber
                            // Load exercises for all unique templateIds in this week
                            val templateIds = week.items.mapNotNull { it.templateId }.distinct()
                            for (tid in templateIds) {
                                if (tid !in templateExercisesCache) {
                                    coroutineScope.launch {
                                        val details = templateViewModel.getTemplateExerciseDetails(tid)
                                        templateExercisesCache[tid] = details
                                    }
                                }
                            }
                        }
                    },
                    onEditTemplate = { templateId ->
                        navController.navigate(Screen.EditTemplate.createRoute(templateId))
                    },
                    onUpdateExercise = { updatedExercise ->
                        templateViewModel.updateTemplateExercise(updatedExercise)
                        // Refresh the cache for this template
                        coroutineScope.launch {
                            val details = templateViewModel.getTemplateExerciseDetails(updatedExercise.templateId)
                            templateExercisesCache[updatedExercise.templateId] = details
                        }
                    }
                )
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun RoutineWeekCard(
    week: TemplateViewModel.RoutineWeek,
    isExpanded: Boolean,
    templateExercisesCache: Map<Long, List<TemplateExerciseDetail>>,
    onToggleExpand: () -> Unit,
    onEditTemplate: (Long) -> Unit,
    onUpdateExercise: (TemplateExercise) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }
    val weekStartStr = dateFormat.format(
        Date.from(week.startDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
    )
    val weekEndStr = dateFormat.format(
        Date.from(week.startDate.plusDays(6).atStartOfDay(ZoneId.systemDefault()).toInstant())
    )

    val bgColor = when {
        week.isCurrent -> MaterialTheme.colorScheme.primaryContainer
        week.isPast -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.surface
    }

    val borderColor = if (isExpanded) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleExpand() },
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(1.dp, borderColor),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Week ${week.weekNumber}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "$weekStartStr – $weekEndStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                if (week.isCurrent) {
                    Text(
                        "CURRENT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(4.dp))
                }
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (week.isPhaseStart && week.weekNumber > 1) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "New phase — workload changes",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Phase label from scheduled items
            val phaseLabel = week.items.firstOrNull { !it.label.isNullOrBlank() && it.label.lowercase() != "rest" && it.label.lowercase() != "rest day" }?.label
            if (phaseLabel != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    phaseLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            if (week.items.isEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "No workouts scheduled",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            } else {
                // Collapsed: show summary of scheduled items
                if (!isExpanded) {
                    Spacer(Modifier.height(6.dp))
                    week.items.forEach { item ->
                        val isRestDay = item.label?.lowercase()?.contains("rest") == true
                        val icon = when {
                            isRestDay -> Icons.Default.Hotel
                            item.isCompleted -> Icons.Default.CheckCircle
                            else -> Icons.Default.FitnessCenter
                        }
                        val tint = when {
                            item.isCompleted -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = tint)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                item.templateName ?: item.label ?: "Workout",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                item.dayOfWeek(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Expanded: show template details with exercise lists
            AnimatedVisibility(
                visible = isExpanded && week.items.isNotEmpty(),
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    @Suppress("DEPRECATION") Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(8.dp))

                    // Group items by templateId to show each template once
                    val templateGroups = week.items
                        .filter { it.templateId != null }
                        .groupBy { it.templateId!! }

                    templateGroups.forEach { (templateId, items) ->
                        val templateName = items.firstOrNull()?.templateName ?: "Workout"
                        val exercises = templateExercisesCache[templateId]
                        val days = items.map { it.dayOfWeek() }.distinct().joinToString(", ")

                        TemplateDetailCard(
                            templateId = templateId,
                            templateName = templateName,
                            scheduledDays = days,
                            exercises = exercises,
                            onEditTemplate = onEditTemplate,
                            onUpdateExercise = onUpdateExercise
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    // Show rest days / non-template items
                    val restItems = week.items.filter { it.templateId == null }
                    restItems.forEach { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Icon(
                                Icons.Default.Hotel,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                item.label ?: "Rest Day",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                item.dayOfWeek(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TemplateDetailCard(
    templateId: Long,
    templateName: String,
    scheduledDays: String,
    exercises: List<TemplateExerciseDetail>?,
    onEditTemplate: (Long) -> Unit,
    onUpdateExercise: (TemplateExercise) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Template header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.FitnessCenter,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        templateName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        scheduledDays,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(
                    onClick = { onEditTemplate(templateId) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Edit", style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(Modifier.height(8.dp))

            // Exercise list
            if (exercises == null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Loading exercises...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (exercises.isEmpty()) {
                Text(
                    "No exercises in this template",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                exercises.forEachIndexed { index, detail ->
                    ExerciseRow(
                        detail = detail,
                        onUpdateExercise = onUpdateExercise
                    )
                    if (index < exercises.lastIndex) {
                        @Suppress("DEPRECATION") Divider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseRow(
    detail: TemplateExerciseDetail,
    onUpdateExercise: (TemplateExercise) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editSets by remember { mutableStateOf(detail.templateExercise.targetSets.toString()) }
    var editReps by remember { mutableStateOf(detail.templateExercise.targetReps.toString()) }
    var editRest by remember { mutableStateOf(detail.templateExercise.restSeconds.toString()) }

    // Reset edit fields when the underlying data changes
    LaunchedEffect(detail.templateExercise) {
        editSets = detail.templateExercise.targetSets.toString()
        editReps = detail.templateExercise.targetReps.toString()
        editRest = detail.templateExercise.restSeconds.toString()
    }

    val te = detail.templateExercise
    val exerciseName = detail.exercise.name
    val setsReps = "${te.targetSets}×${te.targetReps}"
    val restStr = formatRest(te.restSeconds)
    val supersetLabel = if (te.supersetGroup != null) {
        val letter = ('A' + (te.supersetGroup - 1).coerceAtLeast(0))
        "$letter "
    } else ""

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (!isEditing) {
                    isEditing = true
                }
            }
            .padding(vertical = 4.dp)
    ) {
        // Collapsed row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (supersetLabel.isNotEmpty()) {
                Text(
                    supersetLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            Text(
                exerciseName,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
            Text(
                "$setsReps  $restStr",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Inline edit fields
        AnimatedVisibility(
            visible = isEditing,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = editSets,
                        onValueChange = { editSets = it.filter { c -> c.isDigit() } },
                        label = { Text("Sets") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = editReps,
                        onValueChange = { editReps = it.filter { c -> c.isDigit() } },
                        label = { Text("Reps") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = editRest,
                        onValueChange = { editRest = it.filter { c -> c.isDigit() } },
                        label = { Text("Rest (s)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        // Reset and close
                        editSets = te.targetSets.toString()
                        editReps = te.targetReps.toString()
                        editRest = te.restSeconds.toString()
                        isEditing = false
                    }) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val newSets = editSets.toIntOrNull() ?: te.targetSets
                            val newReps = editReps.toIntOrNull() ?: te.targetReps
                            val newRest = editRest.toIntOrNull() ?: te.restSeconds
                            onUpdateExercise(
                                te.copy(
                                    targetSets = newSets,
                                    targetReps = newReps,
                                    restSeconds = newRest
                                )
                            )
                            isEditing = false
                        },
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Save", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

private fun formatRest(seconds: Int): String {
    return when {
        seconds >= 60 && seconds % 60 == 0 -> "${seconds / 60}m rest"
        seconds >= 60 -> "${seconds / 60}m ${seconds % 60}s rest"
        else -> "${seconds}s rest"
    }
}

private fun com.workout.tracker.data.dao.ScheduledWorkoutWithTemplate.dayOfWeek(): String {
    return java.time.Instant.ofEpochMilli(this.scheduledDate)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalDate()
        .dayOfWeek
        .getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault())
}
