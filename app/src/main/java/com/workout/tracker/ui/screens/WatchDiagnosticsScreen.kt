package com.workout.tracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.workout.tracker.watch.WatchDiagnostics
import com.workout.tracker.watch.WatchSyncManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Diagnostic screen for troubleshooting the Wear OS link. Shows the list of
 * connected + capable Wear nodes, the last phone→watch send result, and a
 * "Send test ping" button that fires a round-trip ping/pong so you can
 * verify both directions of the link on-device.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchDiagnosticsScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val snapshot by WatchDiagnostics.nodeSnapshot.collectAsStateWithLifecycle()
    val lastSend by WatchDiagnostics.lastSend.collectAsStateWithLifecycle()
    val lastPongTs by WatchDiagnostics.lastPongTs.collectAsStateWithLifecycle()

    // Auto-refresh on entry
    LaunchedEffect(Unit) { WatchDiagnostics.snapshotNodes(context) }

    val dateFmt = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }
    fun fmtTs(ts: Long) = if (ts == 0L) "—" else dateFmt.format(Date(ts))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Watch connection", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { scope.launch { WatchDiagnostics.snapshotNodes(context) } }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // Connected nodes
            SectionCard(title = "Connected Wear nodes") {
                if (snapshot.snapshotError != null) {
                    Text("Error: ${snapshot.snapshotError}", color = MaterialTheme.colorScheme.error)
                } else if (snapshot.connectedNodes.isEmpty()) {
                    Text(
                        "No watches connected. Open the Wear OS / Pixel Watch companion " +
                            "app on this phone and confirm your watch is paired and nearby.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    snapshot.connectedNodes.forEach { node ->
                        NodeRow(
                            name = node.name,
                            id = node.id,
                            nearby = node.isNearby,
                            hasCapability = node.hasCapability
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Snapshot: ${fmtTs(snapshot.snapshotTs)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Capability
            val missingCapability =
                snapshot.connectedNodes.isNotEmpty() && snapshot.capableNodes.isEmpty()
            if (missingCapability) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Watch connected but app capability not detected",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "The watch companion sees your watch, but the watch app isn't " +
                                "advertising 'workout_tracker_wear'. This usually means the " +
                                "watch app isn't installed yet, or was just installed and " +
                                "needs a reboot for Google Play Services to re-scan it.\n\n" +
                                "Fix: install :wear module on the watch, then reboot the watch.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Last send
            SectionCard(title = "Last message sent to watch") {
                val rec = lastSend
                if (rec == null) {
                    Text("Nothing sent yet this session.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    KeyVal("Path", rec.path)
                    KeyVal("Time", fmtTs(rec.ts))
                    KeyVal("Targets", rec.targetCount.toString())
                    KeyVal(
                        "Result",
                        if (rec.success) "OK" else "FAIL",
                        valueColor = if (rec.success)
                            MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    rec.error?.let { KeyVal("Error", it) }
                }
            }

            // Round-trip test
            SectionCard(title = "Round-trip test") {
                Text(
                    "Sends a ping to the watch. The watch replies with a pong. If both " +
                        "timestamps update, two-way messaging is working.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                KeyVal("Last pong from watch", fmtTs(lastPongTs))
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        scope.launch {
                            WatchSyncManager.sendPing(context)
                            // Refresh node snapshot too so it stays fresh.
                            WatchDiagnostics.snapshotNodes(context)
                        }
                    }
                ) { Text("Send test ping") }
            }

            // Troubleshooting tips
            SectionCard(title = "If nothing is working") {
                Text(
                    "1. Install both :app (phone) and :wear (watch) modules from Android " +
                        "Studio, same build type.\n" +
                        "2. Reboot the watch after installing — that's when Google Play " +
                        "Services re-scans declared capabilities.\n" +
                        "3. Confirm the watch is paired in the Wear OS / Pixel Watch " +
                        "companion app on this phone and shows as 'Connected'.\n" +
                        "4. Both APKs must be signed with the same certificate. If you " +
                        "installed the watch APK from a different source, uninstall and " +
                        "reinstall from the same build.",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun NodeRow(name: String, id: String, nearby: Boolean, hasCapability: Boolean) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(name.ifBlank { "(unnamed)" }, fontWeight = FontWeight.Medium)
            Spacer(Modifier.width(8.dp))
            Badge(
                label = if (nearby) "nearby" else "far",
                color = if (nearby) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.tertiary
            )
            Spacer(Modifier.width(6.dp))
            Badge(
                label = if (hasCapability) "app ✓" else "no app",
                color = if (hasCapability) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.error
            )
        }
        Text(
            id,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun Badge(label: String, color: Color) {
    Surface(color = color.copy(alpha = 0.15f), shape = MaterialTheme.shapes.small) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun KeyVal(key: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row {
        Text(
            "$key: ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(120.dp)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = valueColor,
            fontFamily = FontFamily.Monospace
        )
    }
}
