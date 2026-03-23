package com.workout.tracker.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.workout.tracker.data.entity.WorkoutType
import com.workout.tracker.ui.navigation.Screen
import com.workout.tracker.ui.viewmodel.WorkoutViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    navController: NavController,
    viewModel: WorkoutViewModel
) {
    val workouts by viewModel.workoutHistory.collectAsStateWithLifecycle()
    val dateFormat = remember { SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workout History") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (workouts.isNotEmpty()) {
                        IconButton(onClick = {
                            viewModel.exportToCsv { csv ->
                                val fileName = "workout_export_${SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())}.csv"
                                val file = File(context.cacheDir, fileName)
                                file.writeText(csv)
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/csv"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Export Workout History"))
                            }
                        }) {
                            Icon(Icons.Default.FileDownload, contentDescription = "Export CSV")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (workouts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    Text("No workouts yet", style = MaterialTheme.typography.titleMedium)
                    Text("Your completed workouts will appear here", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Group by date
                val grouped = workouts.groupBy { dateFormat.format(Date(it.startTime)) }
                grouped.forEach { (date, dayWorkouts) ->
                    item {
                        Text(date, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 4.dp))
                    }
                    items(dayWorkouts) { workout ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { navController.navigate(Screen.WorkoutDetail.createRoute(workout.id)) }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    when (workout.workoutType) {
                                        WorkoutType.STRENGTH -> Icons.Default.FitnessCenter
                                        WorkoutType.CARDIO -> Icons.Default.DirectionsRun
                                        WorkoutType.PELOTON -> Icons.Default.PedalBike
                                        WorkoutType.BODYWEIGHT_QUICK -> Icons.Default.Bolt
                                        WorkoutType.OTHER -> Icons.Default.SportsGymnastics
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(workout.name, style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        buildString {
                                            append("${workout.exerciseCount} exercises")
                                            append(" • ${timeFormat.format(Date(workout.startTime))}")
                                            if (workout.endTime != null) {
                                                val totalMin = (workout.endTime - workout.startTime) / 60000
                                                if (totalMin >= 60) {
                                                    append(" • ${totalMin / 60}h ${totalMin % 60}m")
                                                } else {
                                                    append(" • ${totalMin}m")
                                                }
                                            }
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}
