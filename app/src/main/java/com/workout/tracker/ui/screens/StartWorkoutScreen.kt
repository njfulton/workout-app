package com.workout.tracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.workout.tracker.data.entity.WorkoutType
import com.workout.tracker.ui.navigation.Screen
import com.workout.tracker.ui.viewmodel.TemplateViewModel
import com.workout.tracker.ui.viewmodel.WorkoutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartWorkoutScreen(
    navController: NavController,
    templateViewModel: TemplateViewModel,
    workoutViewModel: WorkoutViewModel
) {
    val templates by templateViewModel.templates.collectAsStateWithLifecycle()
    var showEmptyDialog by remember { mutableStateOf(false) }
    var workoutName by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(WorkoutType.STRENGTH) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Start Workout") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            // Empty workout option
            item {
                Card(
                    onClick = { showEmptyDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Empty Workout", style = MaterialTheme.typography.titleMedium)
                            Text("Start from scratch and add exercises as you go", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            if (templates.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text("From Template", style = MaterialTheme.typography.titleMedium)
                }
                items(templates) { template ->
                    Card(
                        onClick = {
                            workoutViewModel.startWorkout(
                                name = template.name,
                                type = WorkoutType.STRENGTH,
                                templateId = template.id
                            )
                            navController.navigate(Screen.ActiveWorkout.route) {
                                popUpTo(Screen.Home.route)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(template.name, style = MaterialTheme.typography.bodyLarge)
                                Text("${template.exerciseCount} exercises", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    if (showEmptyDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyDialog = false },
            title = { Text("New Workout") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = workoutName,
                        onValueChange = { workoutName = it },
                        label = { Text("Workout Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Type:", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        WorkoutType.entries.forEach { type ->
                            FilterChip(
                                selected = selectedType == type,
                                onClick = { selectedType = type },
                                label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        workoutViewModel.startWorkout(name = workoutName.ifBlank { "Workout" }, type = selectedType)
                        showEmptyDialog = false
                        navController.navigate(Screen.ActiveWorkout.route) { popUpTo(Screen.Home.route) }
                    }
                ) { Text("Start") }
            },
            dismissButton = { TextButton(onClick = { showEmptyDialog = false }) { Text("Cancel") } }
        )
    }
}
