package com.workout.tracker.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.health.connect.client.PermissionController
import androidx.navigation.NavController
import com.workout.tracker.health.HealthConnectManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthConnectScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val healthManager = remember { HealthConnectManager(context) }
    var isAvailable by remember { mutableStateOf(HealthConnectManager.isAvailable(context)) }
    var hasPermissions by remember { mutableStateOf(false) }
    var syncStatus by remember { mutableStateOf<String?>(null) }
    var bodyWeight by remember { mutableStateOf<Double?>(null) }

    // Check permissions on load
    LaunchedEffect(Unit) {
        if (isAvailable) {
            hasPermissions = healthManager.hasPermissions()
            if (hasPermissions) {
                bodyWeight = healthManager.readRecentBodyWeight()
            }
        }
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        scope.launch {
            hasPermissions = healthManager.hasPermissions()
            if (hasPermissions) {
                bodyWeight = healthManager.readRecentBodyWeight()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Health Connect", fontWeight = FontWeight.Bold) },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (!isAvailable) {
                // Health Connect not available
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Health Connect Not Available",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Health Connect requires Android 14+ or the Health Connect app installed.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = {
                                try {
                                    context.startActivity(HealthConnectManager.getInstallIntent())
                                } catch (_: Exception) { }
                            }
                        ) {
                            Text("Install Health Connect")
                        }
                    }
                }
            } else {
                // Status card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (hasPermissions) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (hasPermissions) Icons.Default.CheckCircle else Icons.Default.HealthAndSafety,
                                contentDescription = null,
                                tint = if (hasPermissions) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    if (hasPermissions) "Connected" else "Not Connected",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    if (hasPermissions) "Workouts will sync to Health Connect"
                                    else "Grant permissions to sync workout data",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (!hasPermissions) {
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    permissionLauncher.launch(HealthConnectManager.PERMISSIONS)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Link, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Connect to Health Connect")
                            }
                        }
                    }
                }

                if (hasPermissions) {
                    Spacer(Modifier.height(16.dp))

                    // Sync options
                    Text("Sync Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))

                    // Auto-sync toggle
                    val prefs = remember { context.getSharedPreferences("workout_prefs", 0) }
                    var autoSync by remember { mutableStateOf(prefs.getBoolean("health_connect_auto_sync", true)) }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Auto-sync workouts", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                Text(
                                    "Automatically sync completed workouts to Health Connect",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = autoSync,
                                onCheckedChange = {
                                    autoSync = it
                                    prefs.edit().putBoolean("health_connect_auto_sync", it).apply()
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Body weight from Health Connect
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Body Weight", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            if (bodyWeight != null) {
                                Text(
                                    "%.1f lbs".format(bodyWeight),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "From Health Connect",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    "No recent weight data found",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Sync status
                    syncStatus?.let {
                        Spacer(Modifier.height(12.dp))
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    }

                    Spacer(Modifier.height(16.dp))

                    // Data synced info
                    Text("What Gets Synced", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))

                    listOf(
                        "Exercise sessions" to "Workout name, type, duration",
                        "Body weight" to "Read/write body weight measurements"
                    ).forEach { (title, desc) ->
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Sync,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
