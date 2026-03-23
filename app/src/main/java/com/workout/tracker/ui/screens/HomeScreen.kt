package com.workout.tracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.workout.tracker.ui.navigation.Screen
import com.workout.tracker.ui.viewmodel.ScheduleViewModel
import com.workout.tracker.ui.viewmodel.WorkoutViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    workoutViewModel: WorkoutViewModel,
    scheduleViewModel: ScheduleViewModel
) {
    val activeWorkout by workoutViewModel.activeWorkout.collectAsStateWithLifecycle()
    val recentWorkouts by workoutViewModel.workoutHistory.collectAsStateWithLifecycle()
    val upcomingSchedule by scheduleViewModel.upcomingSchedule.collectAsStateWithLifecycle()
    val dateFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
    val scheduleDateFormat = remember { SimpleDateFormat("EEE, MMM d", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workout Tracker", fontWeight = FontWeight.Bold) }
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
            // Active workout banner
            if (activeWorkout.isActive) {
                item {
                    Card(
                        onClick = { navController.navigate(Screen.ActiveWorkout.route) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.FitnessCenter, contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Workout in Progress", style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text(activeWorkout.workoutLog?.name ?: "", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Continue",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }

            // Quick actions - row 1
            item {
                Spacer(Modifier.height(4.dp))
                Text("Quick Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickActionCard(Modifier.weight(1f), Icons.Default.PlayArrow, "Start\nWorkout") { navController.navigate(Screen.StartWorkout.route) }
                    QuickActionCard(Modifier.weight(1f), Icons.Default.Sports, "Pushups") { navController.navigate(Screen.Pushups.route) }
                    QuickActionCard(Modifier.weight(1f), Icons.Default.Assessment, "Weekly\nSummary") { navController.navigate(Screen.WeeklySummary.route) }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickActionCard(Modifier.weight(1f), Icons.Default.CalendarMonth, "Schedule") { navController.navigate(Screen.Schedule.route) }
                    QuickActionCard(Modifier.weight(1f), Icons.Default.History, "History") { navController.navigate(Screen.History.route) }
                    QuickActionCard(Modifier.weight(1f), Icons.Default.Bolt, "Quick\nLog") { navController.navigate(Screen.QuickLog.route) }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickActionCard(Modifier.weight(1f), Icons.Default.FitnessCenter, "Exercises") { navController.navigate(Screen.Exercises.route) }
                    QuickActionCard(Modifier.weight(1f), Icons.AutoMirrored.Filled.ViewList, "Templates") { navController.navigate(Screen.Templates.route) }
                    QuickActionCard(Modifier.weight(1f), Icons.Default.FileDownload, "Import") { navController.navigate(Screen.ImportRoutine.route) }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickActionCard(Modifier.weight(1f), Icons.Default.FolderOpen, "Saved\nRoutines") { navController.navigate(Screen.SavedRoutines.route) }
                    QuickActionCard(Modifier.weight(1f), Icons.Default.Build, "Build\nRoutine") { navController.navigate(Screen.RoutineBuilder.route) }
                    QuickActionCard(Modifier.weight(1f), Icons.Default.Backup, "Backup") { navController.navigate(Screen.BackupRestore.route) }
                }
            }

            // Upcoming schedule with status indicators
            if (upcomingSchedule.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text("Upcoming", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                items(upcomingSchedule.take(5)) { scheduled ->
                    val displayName = scheduled.templateName ?: scheduled.label ?: "Unknown"
                    val isRestDay = scheduled.label?.lowercase()?.contains("rest") == true

                    val statusIcon = when {
                        scheduled.isCompleted -> Icons.Default.CheckCircle
                        scheduled.isSkipped -> Icons.Default.Cancel
                        isRestDay -> Icons.Default.Hotel
                        else -> Icons.Default.Schedule
                    }
                    val statusColor = when {
                        scheduled.isCompleted -> MaterialTheme.colorScheme.primary
                        scheduled.isSkipped -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { navController.navigate(Screen.Schedule.route) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(statusIcon, contentDescription = null, tint = statusColor, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(displayName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                Row {
                                    Text(
                                        scheduleDateFormat.format(Date(scheduled.scheduledDate)),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (scheduled.label != null && scheduled.templateName != null) {
                                        Text(
                                            " \u2022 ${scheduled.label}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                            if (scheduled.isCompleted || scheduled.isSkipped) {
                                IconButton(onClick = { scheduleViewModel.markUncompleted(scheduled) }) {
                                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Mark Incomplete", modifier = Modifier.size(20.dp))
                                }
                            } else if (scheduled.templateId != null) {
                                FilledTonalButton(
                                    onClick = {
                                        workoutViewModel.startWorkout(
                                            name = scheduled.templateName ?: "Workout",
                                            type = com.workout.tracker.data.entity.WorkoutType.STRENGTH,
                                            templateId = scheduled.templateId,
                                            scheduledWorkoutId = scheduled.id
                                        )
                                        navController.navigate(Screen.ActiveWorkout.route) {
                                            popUpTo(Screen.Home.route)
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Start", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }

            // Recent workouts - clickable with status
            if (recentWorkouts.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text("Recent Workouts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                items(recentWorkouts.take(5)) { workout ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { navController.navigate(Screen.WorkoutDetail.createRoute(workout.id)) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Completed",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(workout.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                val durationMin = workout.endTime?.let { ((it - workout.startTime) / 60000).toInt() }
                                Text(
                                    buildString {
                                        append(dateFormat.format(Date(workout.startTime)))
                                        append(" \u2022 ${workout.exerciseCount} exercises")
                                        if (durationMin != null && durationMin > 0) append(" \u2022 ${durationMin}m")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickActionCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 2)
        }
    }
}
