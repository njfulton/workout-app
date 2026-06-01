package com.workout.tracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.workout.tracker.data.entity.WorkoutTemplate
import com.workout.tracker.ui.navigation.Screen
import com.workout.tracker.ui.viewmodel.TemplateViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateListScreen(
    navController: NavController,
    viewModel: TemplateViewModel
) {
    val templates by viewModel.templates.collectAsStateWithLifecycle()
    var showImportDialog by remember { mutableStateOf(false) }
    val importResult by viewModel.importResult.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workout Templates") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showImportDialog = true }) {
                        Icon(Icons.Default.FileUpload, contentDescription = "Import Routine")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Screen.CreateTemplate.route) }) {
                Icon(Icons.Default.Add, contentDescription = "Create Template")
            }
        }
    ) { padding ->
        if (templates.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.AutoMirrored.Filled.ViewList, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    Text("No templates yet", style = MaterialTheme.typography.titleMedium)
                    Text("Create a template or import one", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(templates) { template ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { navController.navigate(Screen.EditTemplate.createRoute(template.id)) }
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(template.name, style = MaterialTheme.typography.titleMedium)
                                if (template.description != null) {
                                    Text(template.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("${template.exerciseCount} exercises", style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = {
                                viewModel.deleteTemplate(WorkoutTemplate(id = template.id, name = template.name, description = template.description, estimatedDurationMinutes = template.estimatedDurationMinutes))
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showImportDialog) {
        ImportRoutineDialog(
            onDismiss = { showImportDialog = false },
            onImport = { text ->
                viewModel.importRoutineFromText(text)
                showImportDialog = false
            }
        )
    }

    // Show import result snackbar
    importResult?.let { result ->
        AlertDialog(
            onDismissRequest = { viewModel.clearImportResult() },
            title = { Text("Import Result") },
            text = { Text(result) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearImportResult() }) { Text("OK") }
            }
        )
    }
}

@Composable
fun ImportRoutineDialog(
    onDismiss: () -> Unit,
    onImport: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import Routine") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Paste routines from AI. Supports supersets (A1/A2), rep ranges (4x6-8), rest times (rest 2 min), and multi-routine programs with auto-scheduling.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Paste routine(s) here") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp),
                    maxLines = 30
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onImport(text) },
                enabled = text.isNotBlank()
            ) { Text("Import") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
