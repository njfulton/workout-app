package com.workout.tracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import com.workout.tracker.ui.viewmodel.ExerciseViewModel
import com.workout.tracker.ui.viewmodel.TemplateExerciseConfig
import com.workout.tracker.ui.viewmodel.TemplateViewModel

data class TemplateExerciseEntry(
    val exercise: Exercise,
    val config: TemplateExerciseConfig = TemplateExerciseConfig()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTemplateScreen(
    navController: NavController,
    templateViewModel: TemplateViewModel,
    exerciseViewModel: ExerciseViewModel
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val selectedExercises = remember { mutableStateListOf<TemplateExerciseEntry>() }
    var showExercisePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Template") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            templateViewModel.createTemplate(
                                name = name,
                                description = description.ifBlank { null },
                                exercises = selectedExercises.map { it.exercise.id to it.config }
                            )
                            navController.popBackStack()
                        },
                        enabled = name.isNotBlank() && selectedExercises.isNotEmpty()
                    ) { Text("Save") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Template Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            item {
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description (optional)") }, modifier = Modifier.fillMaxWidth())
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Exercises", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    TextButton(onClick = { showExercisePicker = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add")
                    }
                }
            }
            itemsIndexed(selectedExercises) { index, entry ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${index + 1}. ${entry.exercise.name}", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            IconButton(onClick = { selectedExercises.removeAt(index) }) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(20.dp))
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = entry.config.sets.toString(),
                                onValueChange = { v -> v.toIntOrNull()?.let { selectedExercises[index] = entry.copy(config = entry.config.copy(sets = it)) } },
                                label = { Text("Sets") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f), singleLine = true
                            )
                            OutlinedTextField(
                                value = entry.config.reps.toString(),
                                onValueChange = { v -> v.toIntOrNull()?.let { selectedExercises[index] = entry.copy(config = entry.config.copy(reps = it)) } },
                                label = { Text("Reps") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f), singleLine = true
                            )
                            OutlinedTextField(
                                value = entry.config.restSeconds.toString(),
                                onValueChange = { v -> v.toIntOrNull()?.let { selectedExercises[index] = entry.copy(config = entry.config.copy(restSeconds = it)) } },
                                label = { Text("Rest(s)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f), singleLine = true
                            )
                        }
                    }
                }
            }
            if (selectedExercises.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("Tap + Add to add exercises", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    if (showExercisePicker) {
        ExercisePickerDialog(
            exerciseViewModel = exerciseViewModel,
            onDismiss = { showExercisePicker = false },
            onSelect = { exercise ->
                selectedExercises.add(TemplateExerciseEntry(exercise))
                showExercisePicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisePickerDialog(
    exerciseViewModel: ExerciseViewModel,
    onDismiss: () -> Unit,
    onSelect: (Exercise) -> Unit
) {
    val exercises by exerciseViewModel.exercises.collectAsStateWithLifecycle()
    val searchQuery by exerciseViewModel.searchQuery.collectAsStateWithLifecycle()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Exercise") },
        text = {
            Column {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { exerciseViewModel.setSearchQuery(it) },
                    placeholder = { Text("Search...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(exercises) { exercise ->
                        TextButton(onClick = { onSelect(exercise) }, modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(exercise.name)
                                Text(exercise.muscleGroup.name.lowercase().replace("_", " "), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
