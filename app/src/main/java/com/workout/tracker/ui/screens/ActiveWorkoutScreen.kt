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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(activeWorkout.workoutLog?.name ?: "Workout") },
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

            // Exercises
            items(activeWorkout.exercises) { activeExercise ->
                ExerciseCard(
                    activeExercise = activeExercise,
                    onLogSet = { setNum, reps, weight, isWarmup ->
                        workoutViewModel.logSet(activeExercise.exerciseLogId, setNum, reps, weight, isWarmup)
                    },
                    onStartTimer = { workoutViewModel.startRestTimer(it) }
                )
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
            title = { Text("Finish Workout?") },
            text = { Text("This will save your workout to history.") },
            confirmButton = {
                TextButton(onClick = {
                    workoutViewModel.finishWorkout()
                    showFinishConfirm = false
                    navController.popBackStack(route = "home", inclusive = false)
                }) { Text("Finish") }
            },
            dismissButton = { TextButton(onClick = { showFinishConfirm = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun ExerciseCard(
    activeExercise: ActiveExercise,
    onLogSet: (Int, Int, Double?, Boolean) -> Unit,
    onStartTimer: (Int) -> Unit
) {
    var repsText by remember { mutableStateOf("") }
    var weightText by remember { mutableStateOf("") }
    var isWarmup by remember { mutableStateOf(false) }
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

            Spacer(Modifier.height(8.dp))

            // Previous sets
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
                        onStartTimer(90)
                    },
                    enabled = repsText.toIntOrNull() != null
                ) { Text("Log Set") }
            }
        }
    }
}
