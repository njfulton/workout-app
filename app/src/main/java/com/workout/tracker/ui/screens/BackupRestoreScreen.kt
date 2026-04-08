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
import androidx.compose.ui.graphics.Color
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
    var importMode by remember { mutableStateOf(BackupManager.ImportMode.REPLACE) }

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
                        "Import data from a previously exported JSON file. Replace All wipes the current database first; Merge keeps current data and skips duplicates.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                importMode = BackupManager.ImportMode.REPLACE
                                filePickerLauncher.launch(arrayOf("application/json", "*/*"))
                            },
                            enabled = !isExporting && !isImporting
                        ) {
                            if (isImporting && importMode == BackupManager.ImportMode.REPLACE) {
                                Text("Importing\u2026")
                            } else {
                                Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Replace All")
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                importMode = BackupManager.ImportMode.MERGE
                                filePickerLauncher.launch(arrayOf("application/json", "*/*"))
                            },
                            enabled = !isExporting && !isImporting
                        ) {
                            if (isImporting && importMode == BackupManager.ImportMode.MERGE) {
                                Text("Merging\u2026")
                            } else {
                                Icon(Icons.Default.MergeType, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Merge")
                            }
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
        val isReplace = importMode == BackupManager.ImportMode.REPLACE
        AlertDialog(
            onDismissRequest = {
                showImportConfirm = false
                pendingImportUri = null
            },
            title = { Text(if (isReplace) "Replace All Data?" else "Merge Backup?") },
            text = {
                Text(
                    if (isReplace)
                        "This will WIPE all existing data — exercises, templates, workout history, schedule, saved routines, pushup logs — and replace it with the contents of the backup file. This cannot be undone. Continue?"
                    else
                        "This will add data from the backup to your existing data. Duplicates (same workout name + start time, same scheduled day + template, same pushup timestamp + count, same template name) will be skipped."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showImportConfirm = false
                        val uri = pendingImportUri!!
                        pendingImportUri = null
                        val mode = importMode
                        isImporting = true
                        statusMessage = null
                        scope.launch {
                            val result = backupManager.importFromJson(uri, mode)
                            isImporting = false
                            result.fold(
                                onSuccess = { summary ->
                                    val verb = if (mode == BackupManager.ImportMode.REPLACE) "Replaced" else "Merged"
                                    statusMessage = "$verb: ${summary.exercisesImported} exercises, ${summary.templatesImported} templates, ${summary.workoutLogsImported} workouts, ${summary.setLogsImported} sets, ${summary.pushupLogsImported} pushup logs"
                                },
                                onFailure = { e ->
                                    statusMessage = "Import failed: ${e.message}"
                                }
                            )
                        }
                    }
                ) {
                    Text(
                        if (isReplace) "Replace All" else "Merge",
                        color = if (isReplace) MaterialTheme.colorScheme.error else Color.Unspecified
                    )
                }
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
