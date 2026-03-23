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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.workout.tracker.data.entity.PushupLog
import com.workout.tracker.data.repository.WorkoutRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PushupScreen(
    navController: NavController,
    repository: WorkoutRepository
) {
    val scope = rememberCoroutineScope()
    val pushupLogs by repository.getAllPushupLogs().collectAsStateWithLifecycle(emptyList())

    var isTimerRunning by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var showCountDialog by remember { mutableStateOf(false) }
    var sessionDuration by remember { mutableIntStateOf(0) }

    // Timer effect
    LaunchedEffect(isTimerRunning) {
        if (isTimerRunning) {
            while (isTimerRunning) {
                delay(1000)
                elapsedSeconds++
            }
        }
    }

    // Today's total
    val todayLogs = pushupLogs.filter {
        val cal = Calendar.getInstance()
        val todayCal = Calendar.getInstance()
        cal.timeInMillis = it.timestamp
        cal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
            cal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)
    }
    val todayTotal = todayLogs.sumOf { it.count }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pushups") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Today's summary card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Today's Pushups",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "$todayTotal",
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        if (todayLogs.isNotEmpty()) {
                            Text(
                                "${todayLogs.size} session(s)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Timer / Start button
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (isTimerRunning) {
                            // Timer display
                            val minutes = elapsedSeconds / 60
                            val seconds = elapsedSeconds % 60
                            Text(
                                "%d:%02d".format(minutes, seconds),
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    isTimerRunning = false
                                    sessionDuration = elapsedSeconds
                                    showCountDialog = true
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Done — Log Pushups")
                            }
                        } else {
                            Text(
                                "Ready?",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    elapsedSeconds = 0
                                    isTimerRunning = true
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Start Pushup Session")
                            }
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    sessionDuration = 0
                                    showCountDialog = true
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Quick Add (no timer)")
                            }
                        }
                    }
                }
            }

            // Recent log entries
            if (pushupLogs.isNotEmpty()) {
                item {
                    Text(
                        "Recent",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                items(pushupLogs.take(20), key = { it.id }) { log ->
                    PushupLogItem(log)
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    if (showCountDialog) {
        PushupCountDialog(
            durationSeconds = sessionDuration,
            onDismiss = {
                showCountDialog = false
                elapsedSeconds = 0
            },
            onSave = { count ->
                scope.launch {
                    repository.insertPushupLog(
                        PushupLog(
                            count = count,
                            durationSeconds = if (sessionDuration > 0) sessionDuration else null
                        )
                    )
                }
                showCountDialog = false
                elapsedSeconds = 0
            }
        )
    }
}

@Composable
fun PushupLogItem(log: PushupLog) {
    val dateFormat = remember { SimpleDateFormat("EEE, MMM d  h:mm a", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.FitnessCenter,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    dateFormat.format(Date(log.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "${log.count}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            if (log.durationSeconds != null && log.durationSeconds > 0) {
                Spacer(Modifier.width(8.dp))
                Text(
                    "${log.durationSeconds}s",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun PushupCountDialog(
    durationSeconds: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var countText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("How many pushups?") },
        text = {
            Column {
                if (durationSeconds > 0) {
                    Text(
                        "Duration: ${durationSeconds}s",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                }
                OutlinedTextField(
                    value = countText,
                    onValueChange = { countText = it.filter { c -> c.isDigit() } },
                    label = { Text("Count") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val count = countText.toIntOrNull()
                    if (count != null && count > 0) onSave(count)
                },
                enabled = (countText.toIntOrNull() ?: 0) > 0
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
