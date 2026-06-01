package com.workout.tracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.workout.tracker.data.entity.Exercise
import com.workout.tracker.data.entity.ExerciseCategory
import com.workout.tracker.ui.viewmodel.ExerciseViewModel
import com.workout.tracker.ui.viewmodel.WorkoutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickLogScreen(
    navController: NavController,
    workoutViewModel: WorkoutViewModel,
    exerciseViewModel: ExerciseViewModel
) {
    val exercises by exerciseViewModel.exercises.collectAsStateWithLifecycle()
    var selectedExercise by remember { mutableStateOf<Exercise?>(null) }
    var repsText by remember { mutableStateOf("") }
    var weightText by remember { mutableStateOf("") }
    var showSuccess by remember { mutableStateOf(false) }

    // Filter to bodyweight-friendly exercises for quick access
    val quickExercises = remember(exercises) {
        exercises.filter { it.category == ExerciseCategory.BODYWEIGHT || it.name.contains("Push", ignoreCase = true) || it.name.contains("Pull", ignoreCase = true) || it.name.contains("Plank", ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quick Log") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(4.dp))
            Text("Log a quick set — push-ups, pull-ups, Peloton ride, or anything else.", style = MaterialTheme.typography.bodyMedium)

            if (showSuccess) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Logged!", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            // Quick select common exercises
            Text("Quick Select", style = MaterialTheme.typography.titleSmall)
            LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                items(quickExercises.take(8)) { exercise ->
                    TextButton(
                        onClick = { selectedExercise = exercise },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = selectedExercise == exercise, onClick = { selectedExercise = exercise })
                            Text(exercise.name)
                        }
                    }
                }
            }

            // Or pick from full list
            var showPicker by remember { mutableStateOf(false) }
            OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
                Text(selectedExercise?.name ?: "Choose from all exercises...")
            }

            // Reps and weight
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
                    label = { Text("Weight (optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Button(
                onClick = {
                    val exercise = selectedExercise ?: return@Button
                    val reps = repsText.toIntOrNull() ?: return@Button
                    val weight = weightText.toDoubleOrNull()
                    workoutViewModel.quickLog(exercise, reps, weight)
                    repsText = ""
                    weightText = ""
                    showSuccess = true
                },
                enabled = selectedExercise != null && repsText.toIntOrNull() != null,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(Icons.Default.Bolt, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Log It")
            }

            if (showPicker) {
                ExercisePickerDialog(
                    exerciseViewModel = exerciseViewModel,
                    onDismiss = { showPicker = false },
                    onSelect = { selectedExercise = it; showPicker = false }
                )
            }
        }
    }
}
