package com.workout.tracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.workout.tracker.ui.viewmodel.ActiveExercise
import com.workout.tracker.ui.viewmodel.ExerciseViewModel
import com.workout.tracker.ui.viewmodel.WorkoutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    navController: NavController,
    workoutViewModel: WorkoutViewModel,
    exerciseViewModel: ExerciseViewModel
) {
    val activeWorkout by workoutViewModel.activeWorkout.collectAsStateWithLifecycle()
    val restTimer by workoutViewModel.restTimerSeconds.collectAsStateWithLifecycle()
    val isTimerRunning by workoutViewModel.isTimerRunning.collectAsStateWithLifecycle()
    var showExercisePicker by remember { mutableStateOf(false) }
    var showFinishConfirm by remember { mutableStateOf(false) }

    // Elapsed workout timer
    val startTime = activeWorkout.workoutLog?.startTime ?: System.currentTimeMillis()
    var elapsedSeconds by remember { mutableStateOf(0L) }
    LaunchedEffect(startTime) {
        while (true) {
            elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000
            kotlinx.coroutines.delay(1000)
        }
    }
    val elapsedMinutes = elapsedSeconds / 60
    val elapsedSecs = elapsedSeconds % 60
    val elapsedStr = "${elapsedMinutes}:${elapsedSecs.toString().padStart(2, '0')}"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(activeWorkout.workoutLog?.name ?: "Workout")
                        Text(
                            elapsedStr,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { showFinishConfirm = true }) {
                        Text("Finish", color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showExercisePicker = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Exercise")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Rest timer
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = if (isTimerRunning) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer) else CardDefaults.cardColors()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Timer, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        if (isTimerRunning) {
                            Text(
                                "${restTimer / 60}:${(restTimer % 60).toString().padStart(2, '0')}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = { workoutViewModel.stopRestTimer() }) { Text("Stop") }
                        } else {
                            Text("Rest Timer", modifier = Modifier.weight(1f))
                            listOf(60, 90, 120).forEach { secs ->
                                TextButton(onClick = { workoutViewModel.startRestTimer(secs) }) {
                                    Text("${secs}s")
                                }
                            }
                        }
                    }
                }
            }

            // Exercises - group supersets together
            val exercises = activeWorkout.exercises
            val grouped = mutableListOf<List<ActiveExercise>>()
            var i = 0
            while (i < exercises.size) {
                val ex = exercises[i]
                if (ex.supersetGroup != null) {
                    val group = mutableListOf(ex)
                    while (i + 1 < exercises.size && exercises[i + 1].supersetGroup == ex.supersetGroup) {
                        i++
                        group.add(exercises[i])
                    }
                    grouped.add(group)
                } else {
                    grouped.add(listOf(ex))
                }
                i++
            }

            items(grouped.size) { groupIndex ->
                val group = grouped[groupIndex]
                if (group.size > 1) {
                    // Superset card
                    SupersetCard(
                        exercises = group,
                        onLogSet = { exerciseLogId, setNum, reps, weight, isWarmup ->
                            workoutViewModel.logSet(exerciseLogId, setNum, reps, weight, isWarmup)
                        },
                        onStartTimer = { workoutViewModel.startRestTimer(it) }
                    )
                } else {
                    ExerciseCard(
                        activeExercise = group[0],
                        onLogSet = { setNum, reps, weight, isWarmup ->
                            workoutViewModel.logSet(group[0].exerciseLogId, setNum, reps, weight, isWarmup)
                        },
                        onStartTimer = { workoutViewModel.startRestTimer(it) },
                        defaultRestSeconds = group[0].restSeconds
                    )
                }
            }

            if (activeWorkout.exercises.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        Text("Tap + to add exercises", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showExercisePicker) {
        ExercisePickerDialog(
            exerciseViewModel = exerciseViewModel,
            onDismiss = { showExercisePicker = false },
            onSelect = { exercise ->
                workoutViewModel.addExerciseToWorkout(exercise)
                showExercisePicker = false
            }
        )
    }

    if (showFinishConfirm) {
        AlertDialog(
            onDismissRequest = { showFinishConfirm = false },
            title = { Text("End Workout") },
            text = { Text("Save this workout to your history, or discard it?") },
            confirmButton = {
                TextButton(onClick = {
                    workoutViewModel.finishWorkout()
                    showFinishConfirm = false
                    navController.popBackStack(route = "home", inclusive = false)
                }) { Text("Save") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { showFinishConfirm = false }) { Text("Cancel") }
                    TextButton(onClick = {
                        workoutViewModel.discardWorkout()
                        showFinishConfirm = false
                        navController.popBackStack(route = "home", inclusive = false)
                    }) { Text("Discard", color = MaterialTheme.colorScheme.error) }
                }
            }
        )
    }
}

@Composable
fun ExerciseCard(
    activeExercise: ActiveExercise,
    onLogSet: (Int, Int, Double?, Boolean) -> Unit,
    onStartTimer: (Int) -> Unit,
    defaultRestSeconds: Int = 90
) {
    var repsText by remember { mutableStateOf("") }
    var weightText by remember { mutableStateOf("") }
    var isWarmup by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    val nextSetNumber = activeExercise.sets.size + 1

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(activeExercise.exercise.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text("${activeExercise.sets.size} sets", style = MaterialTheme.typography.bodySmall)
            }

            // Overload suggestion
            activeExercise.overloadSuggestion?.let { suggestion ->
                if (suggestion.suggestedWeight > suggestion.currentWeight) {
                    Spacer(Modifier.height(4.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(suggestion.reason, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // History toggle
            if (activeExercise.history.isNotEmpty()) {
                TextButton(onClick = { showHistory = !showHistory }) {
                    Icon(
                        if (showHistory) Icons.Default.ExpandLess else Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(if (showHistory) "Hide History" else "Show History", style = MaterialTheme.typography.bodySmall)
                }

                if (showHistory) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("Recent Performance", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(4.dp))
                            // Group by workout date
                            val byWorkout = activeExercise.history.groupBy { it.startTime }
                            val dateFormat = java.text.SimpleDateFormat("MMM d", java.util.Locale.US)
                            byWorkout.entries.take(5).forEach { (startTime, sets) ->
                                val dateStr = dateFormat.format(java.util.Date(startTime))
                                val setsStr = sets.joinToString(", ") { set ->
                                    val w = set.weightLbs?.let { "${it.toInt()}lb" } ?: "BW"
                                    "${w}x${set.reps}"
                                }
                                Text(
                                    "$dateStr: $setsStr",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Previous sets (current workout)
            activeExercise.sets.forEach { set ->
                Text(
                    "Set ${set.setNumber}: ${set.reps} reps${if (set.weightLbs != null) " @ ${set.weightLbs}lbs" else ""}${if (set.isWarmup) " (warmup)" else ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(8.dp))
            Divider()
            Spacer(Modifier.height(8.dp))

            // New set input
            Text("Set $nextSetNumber", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = repsText,
                    onValueChange = { repsText = it },
                    label = { Text("Reps") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    label = { Text("Weight") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isWarmup, onCheckedChange = { isWarmup = it })
                Text("Warmup", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = {
                        val reps = repsText.toIntOrNull() ?: return@Button
                        val weight = weightText.toDoubleOrNull()
                        onLogSet(nextSetNumber, reps, weight, isWarmup)
                        repsText = ""
                        weightText = ""
                        isWarmup = false
                        onStartTimer(defaultRestSeconds)
                    },
                    enabled = repsText.toIntOrNull() != null
                ) { Text("Log Set") }
            }
        }
    }
}

@Composable
fun SupersetCard(
    exercises: List<ActiveExercise>,
    onLogSet: (Long, Int, Int, Double?, Boolean) -> Unit,
    onStartTimer: (Int) -> Unit
) {
    var activeTab by remember { mutableStateOf(0) }
    val restSeconds = exercises.firstOrNull()?.restSeconds ?: 90

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column {
            // Superset header
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("SUPERSET", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Text(
                    exercises.joinToString(" + ") { it.exercise.name },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            // Tabs for each exercise
            TabRow(selectedTabIndex = activeTab) {
                exercises.forEachIndexed { index, ex ->
                    Tab(
                        selected = activeTab == index,
                        onClick = { activeTab = index },
                        text = { Text(ex.exercise.name, maxLines = 1) }
                    )
                }
            }

            // Content for selected exercise
            val currentExercise = exercises.getOrNull(activeTab) ?: return@Card
            Column(modifier = Modifier.padding(16.dp)) {
                // Overload suggestion
                currentExercise.overloadSuggestion?.let { suggestion ->
                    if (suggestion.suggestedWeight > suggestion.currentWeight) {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(suggestion.reason, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }

                // Previous sets
                currentExercise.sets.forEach { set ->
                    Text(
                        "Set ${set.setNumber}: ${set.reps} reps${if (set.weightLbs != null) " @ ${set.weightLbs}lbs" else ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(8.dp))
                Divider()
                Spacer(Modifier.height(8.dp))

                // Set input
                val nextSetNumber = currentExercise.sets.size + 1
                var repsText by remember(activeTab) { mutableStateOf("") }
                var weightText by remember(activeTab) { mutableStateOf("") }

                Text("Set $nextSetNumber", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = repsText,
                        onValueChange = { repsText = it },
                        label = { Text("Reps") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f), singleLine = true
                    )
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { weightText = it },
                        label = { Text("Weight") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f), singleLine = true
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = {
                            val reps = repsText.toIntOrNull() ?: return@Button
                            val weight = weightText.toDoubleOrNull()
                            onLogSet(currentExercise.exerciseLogId, nextSetNumber, reps, weight, false)
                            repsText = ""
                            weightText = ""
                            // Auto-advance to next exercise in superset, or start rest timer if last
                            if (activeTab < exercises.size - 1) {
                                activeTab++
                            } else {
                                activeTab = 0
                                onStartTimer(restSeconds)
                            }
                        },
                        enabled = repsText.toIntOrNull() != null
                    ) { Text("Log & Next") }
                }
            }
        }
    }
}
