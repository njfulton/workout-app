package com.workout.tracker.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.workout.tracker.data.BackupManager
import com.workout.tracker.data.WorkoutDatabase
import com.workout.tracker.data.repository.WorkoutRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
    navController: NavController,
    backupManager: BackupManager
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isExporting by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var showImportConfirm by remember { mutableStateOf(false) }
    var pendingImportUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingImportUri = uri
            showImportConfirm = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup & Restore") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Export section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Upload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Export Backup",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Exports all data (exercises, templates, workout history, schedule, saved routines) to a JSON file in your Downloads folder.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                isExporting = true
                                statusMessage = null
                                scope.launch {
                                    val result = backupManager.exportToJson()
                                    isExporting = false
                                    result.fold(
                                        onSuccess = { file ->
                                            statusMessage = "Exported to Downloads/${file.name}"
                                        },
                                        onFailure = { e ->
                                            statusMessage = "Export failed: ${e.message}"
                                        }
                                    )
                                }
                            },
                            enabled = !isExporting && !isImporting
                        ) {
                            if (isExporting) {
                                Text("Exporting\u2026")
                            } else {
                                Icon(Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Export to Downloads")
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                isExporting = true
                                statusMessage = null
                                scope.launch {
                                    val result = backupManager.exportToJson()
                                    isExporting = false
                                    result.fold(
                                        onSuccess = { file ->
                                            try {
                                                val uri = FileProvider.getUriForFile(
                                                    context,
                                                    "${context.packageName}.fileprovider",
                                                    file
                                                )
                                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "application/json"
                                                    putExtra(Intent.EXTRA_STREAM, uri)
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                context.startActivity(Intent.createChooser(shareIntent, "Share backup"))
                                            } catch (e: Exception) {
                                                statusMessage = "Exported to Downloads/${file.name} (share failed: ${e.message})"
                                            }
                                        },
                                        onFailure = { e ->
                                            statusMessage = "Export failed: ${e.message}"
                                        }
                                    )
                                }
                            },
                            enabled = !isExporting && !isImporting
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Export & Share")
                        }
                    }
                }
            }

            // Import section
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Restore from Backup",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Import data from a previously exported JSON file. Exercises with matching names will be merged; everything else will be added.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = {
                            filePickerLauncher.launch(arrayOf("application/json", "*/*"))
                        },
                        enabled = !isExporting && !isImporting
                    ) {
                        if (isImporting) {
                            Text("Importing\u2026")
                        } else {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Select Backup File")
                        }
                    }
                }
            }

            // Status message
            if (statusMessage != null) {
                val isError = statusMessage!!.contains("failed", ignoreCase = true)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isError) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (isError) Icons.Default.Error else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (isError) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(statusMessage!!, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }

    // Import confirmation dialog
    if (showImportConfirm && pendingImportUri != null) {
        AlertDialog(
            onDismissRequest = {
                showImportConfirm = false
                pendingImportUri = null
            },
            title = { Text("Restore Backup") },
            text = {
                Text("This will import all data from the backup file. Existing exercises with matching names will be merged. Templates, workout logs, and schedule entries will be added alongside existing data.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showImportConfirm = false
                        val uri = pendingImportUri!!
                        pendingImportUri = null
                        isImporting = true
                        statusMessage = null
                        scope.launch {
                            val result = backupManager.importFromJson(uri)
                            isImporting = false
                            result.fold(
                                onSuccess = { summary ->
                                    statusMessage = "Restored: ${summary.exercisesImported} exercises, ${summary.templatesImported} templates, ${summary.workoutLogsImported} workouts, ${summary.setLogsImported} sets, ${summary.pushupLogsImported} pushup logs"
                                },
                                onFailure = { e ->
                                    statusMessage = "Import failed: ${e.message}"
                                }
                            )
                        }
                    }
                ) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showImportConfirm = false
                    pendingImportUri = null
                }) { Text("Cancel") }
            }
        )
    }
}
