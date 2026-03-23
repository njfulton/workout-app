package com.workout.tracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
                            Icon(Icons.Default.FitnessCenter, contentDescription = null)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Workout in Progress", style = MaterialTheme.typography.titleMedium)
                                Text(activeWorkout.workoutLog?.name ?: "", style = MaterialTheme.typography.bodySmall)
                            }
                            Icon(Icons.Default.ArrowForward, contentDescription = "Continue")
                        }
                    }
                }
            }

            // Quick actions
            item {
                Spacer(Modifier.height(4.dp))
                Text("Quick Actions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.PlayArrow,
                        label = "Start\nWorkout",
                        onClick = { navController.navigate(Screen.StartWorkout.route) }
                    )
                    QuickActionCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Bolt,
                        label = "Quick\nLog",
                        onClick = { navController.navigate(Screen.QuickLog.route) }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.FitnessCenter,
                        label = "Exercises",
                        onClick = { navController.navigate(Screen.Exercises.route) }
                    )
                    QuickActionCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.ViewList,
                        label = "Templates",
                        onClick = { navController.navigate(Screen.Templates.route) }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.History,
                        label = "History",
                        onClick = { navController.navigate(Screen.History.route) }
                    )
                    QuickActionCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.CalendarMonth,
                        label = "Schedule",
                        onClick = { navController.navigate(Screen.Schedule.route) }
                    )
                }
            }

            // Upcoming schedule
            if (upcomingSchedule.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text("Upcoming", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                items(upcomingSchedule.take(5)) { scheduled ->
                    val displayName = scheduled.templateName ?: scheduled.label ?: "Unknown"
                    val isRestDay = scheduled.label?.lowercase()?.contains("rest") == true
                    val icon = when {
                        isRestDay -> Icons.Default.Hotel
                        scheduled.label?.lowercase()?.contains("cardio") == true -> Icons.Default.DirectionsRun
                        scheduled.templateId != null -> Icons.Default.FitnessCenter
                        else -> Icons.Default.Event
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            if (scheduled.templateId != null && !scheduled.isCompleted && !scheduled.isSkipped) {
                                workoutViewModel.startWorkout(
                                    name = scheduled.templateName ?: "Workout",
                                    type = com.workout.tracker.data.entity.WorkoutType.STRENGTH,
                                    templateId = scheduled.templateId
                                )
                                scheduleViewModel.markCompleted(scheduled)
                                navController.navigate(Screen.ActiveWorkout.route) {
                                    popUpTo(Screen.Home.route)
                                }
                            } else {
                                navController.navigate(Screen.Schedule.route)
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(displayName, style = MaterialTheme.typography.bodyLarge)
                                Text(dateFormat.format(Date(scheduled.scheduledDate)), style = MaterialTheme.typography.bodySmall)
                            }
                            if (scheduled.templateId != null && !scheduled.isCompleted && !scheduled.isSkipped) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Start", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            // Recent workouts
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
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                when (workout.workoutType) {
                                    com.workout.tracker.data.entity.WorkoutType.STRENGTH -> Icons.Default.FitnessCenter
                                    com.workout.tracker.data.entity.WorkoutType.CARDIO -> Icons.Default.DirectionsRun
                                    com.workout.tracker.data.entity.WorkoutType.PELOTON -> Icons.Default.PedalBike
                                    com.workout.tracker.data.entity.WorkoutType.BODYWEIGHT_QUICK -> Icons.Default.Bolt
                                    com.workout.tracker.data.entity.WorkoutType.OTHER -> Icons.Default.SportsGymnastics
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(workout.name, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "${workout.exerciseCount} exercises • ${dateFormat.format(Date(workout.startTime))}",
                                    style = MaterialTheme.typography.bodySmall
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
        modifier = modifier.height(100.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}
