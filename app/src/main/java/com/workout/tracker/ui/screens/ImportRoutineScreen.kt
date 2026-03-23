package com.workout.tracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.workout.tracker.ui.viewmodel.TemplateViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportRoutineScreen(
    navController: NavController,
    templateViewModel: TemplateViewModel
) {
    var routineText by remember { mutableStateOf("") }
    var isImporting by remember { mutableStateOf(false) }
    val importResult by templateViewModel.importResult.collectAsStateWithLifecycle()

    LaunchedEffect(importResult) {
        if (importResult != null) isImporting = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import Routine") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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

            Button(
                onClick = {
                    isImporting = true
                    templateViewModel.clearImportResult()
                    templateViewModel.importRoutineFromText(routineText)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = routineText.isNotBlank() && !isImporting
            ) {
                if (isImporting) {
                    Text("Importing\u2026")
                } else {
                    Icon(Icons.Default.FileDownload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Import & Build Schedule")
                }
            }

            // Result
            if (importResult != null) {
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
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
