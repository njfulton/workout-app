package com.workout.tracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.workout.tracker.data.entity.WorkoutType
import com.workout.tracker.ui.viewmodel.WorkoutDetailExercise
import com.workout.tracker.ui.viewmodel.WorkoutViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailScreen(
    navController: NavController,
    viewModel: WorkoutViewModel,
    workoutLogId: Long
) {
    LaunchedEffect(workoutLogId) {
        viewModel.loadWorkoutDetail(workoutLogId)
    }
    DisposableEffect(Unit) {
        onDispose { viewModel.clearWorkoutDetail() }
    }

    val detail by viewModel.workoutDetail.collectAsStateWithLifecycle()
    val hasError by viewModel.workoutDetailError.collectAsStateWithLifecycle()
    val dateFormat = remember { SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(detail?.workoutLog?.name ?: "Workout") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when {
            hasError -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(12.dp))
                        Text("Workout not found", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { navController.popBackStack() }) {
                            Text("Go Back")
                        }
                    }
                }
            }
            detail == null -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            else -> {
                val workout = detail!!
                val log = workout.workoutLog
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Summary card
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    when (log.workoutType) {
                                        WorkoutType.STRENGTH -> Icons.Default.FitnessCenter
                                        WorkoutType.CARDIO -> Icons.AutoMirrored.Filled.DirectionsRun
                                        WorkoutType.PELOTON -> Icons.Default.PedalBike
                                        WorkoutType.BODYWEIGHT_QUICK -> Icons.Default.Bolt
                                        WorkoutType.OTHER -> Icons.Default.SportsGymnastics
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(dateFormat.format(Date(log.startTime)), style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        buildString {
                                            append(timeFormat.format(Date(log.startTime)))
                                            if (log.endTime != null) {
                                                append(" - ${timeFormat.format(Date(log.endTime))}")
                                            }
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            // Stats row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                // Duration
                                if (log.endTime != null) {
                                    val totalMin = (log.endTime - log.startTime) / 60000
                                    StatItem(
                                        label = "Duration",
                                        value = if (totalMin >= 60) "${totalMin / 60}h ${totalMin % 60}m" else "${totalMin}m"
                                    )
                                }
                                StatItem(label = "Exercises", value = "${workout.exercises.size}")
                                StatItem(label = "Sets", value = "${workout.totalSets}")
                                if (workout.totalVolume > 0) {
                                    StatItem(
                                        label = "Volume",
                                        value = "${(workout.totalVolume / 1000).let { if (it >= 1) "%.1fk".format(it) else "%.0f".format(workout.totalVolume) }} lbs"
                                    )
                                }
                            }
                        }
                    }

                    // Notes
                    if (!log.notes.isNullOrBlank()) {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Notes", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(4.dp))
                                Text(log.notes, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    // Exercises
                    Text("Exercises", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                    if (workout.exercises.isEmpty()) {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("No exercises logged", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // Group supersets
                    val groups = mutableListOf<List<WorkoutDetailExercise>>()
                    var i = 0
                    while (i < workout.exercises.size) {
                        val ex = workout.exercises[i]
                        if (ex.supersetGroup != null) {
                            val group = mutableListOf(ex)
                            while (i + 1 < workout.exercises.size && workout.exercises[i + 1].supersetGroup == ex.supersetGroup) {
                                i++
                                group.add(workout.exercises[i])
                            }
                            groups.add(group)
                        } else {
                            groups.add(listOf(ex))
                        }
                        i++
                    }

                    groups.forEach { group ->
                        if (group.size > 1) {
                            // Superset
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("SUPERSET", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    group.forEach { exercise ->
                                        ExerciseDetailContent(exercise)
                                        Spacer(Modifier.height(8.dp))
                                    }
                                }
                            }
                        } else {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    ExerciseDetailContent(group.first())
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun ExerciseDetailContent(exercise: WorkoutDetailExercise) {
    Text(exercise.exerciseName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(4.dp))

    if (exercise.sets.isEmpty()) {
        Text("No sets logged", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }

    // Header row
    Row(modifier = Modifier.fillMaxWidth()) {
        Text("Set", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(36.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Reps", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Weight", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Spacer(Modifier.height(2.dp))

    exercise.sets.forEach { set ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${set.setNumber}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.width(36.dp),
                color = if (set.isWarmup) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
            )
            Text(
                "${set.reps ?: "-"}${if (set.isWarmup) " (W)" else ""}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                color = if (set.isWarmup) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
            )
            val weightStr = set.weightLbs?.let {
                if (it == it.toLong().toDouble()) "${it.toLong()} lbs" else "$it lbs"
            } ?: if (set.durationSeconds != null) "${set.durationSeconds}s" else "BW"
            Text(
                weightStr,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                color = if (set.isWarmup) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
            )
        }
    }

    // Best set highlight
    val bestSet = exercise.sets.filter { !it.isWarmup && it.weightLbs != null }
        .maxByOrNull { (it.weightLbs ?: 0.0) * (it.reps ?: 0) }
    if (bestSet != null) {
        Spacer(Modifier.height(4.dp))
        Text(
            "Best: ${bestSet.reps} x ${bestSet.weightLbs?.let { if (it == it.toLong().toDouble()) "${it.toLong()}" else "$it" }} lbs",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
