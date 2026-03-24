package com.workout.tracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
    val dashboardStats by workoutViewModel.dashboardStats.collectAsStateWithLifecycle()
    val scheduleDateFormat = remember { SimpleDateFormat("EEE, MMM d", Locale.getDefault()) }

    // Find the next non-completed, non-skipped, non-rest workout
    val nextWorkout = upcomingSchedule.firstOrNull {
        !it.isCompleted && !it.isSkipped && it.label?.lowercase()?.contains("rest") != true
    }
    // Remaining upcoming after the next workout (skip rest days and completed)
    val upcomingAfterNext = upcomingSchedule
        .filter { it != nextWorkout }
        .take(4)

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

            // Dashboard stats row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DashboardStatCard(
                        modifier = Modifier.weight(1f),
                        value = dashboardStats.currentStreak.toString(),
                        label = "Streak",
                        icon = Icons.Default.LocalFireDepartment
                    )
                    DashboardStatCard(
                        modifier = Modifier.weight(1f),
                        value = dashboardStats.workoutsThisWeek.toString(),
                        label = "This Week",
                        icon = Icons.Default.DateRange
                    )
                    DashboardStatCard(
                        modifier = Modifier.weight(1f),
                        value = dashboardStats.totalWorkouts.toString(),
                        label = "Total",
                        icon = Icons.Default.EmojiEvents
                    )
                }
            }

            // Next Workout - prominent card
            if (nextWorkout != null && !activeWorkout.isActive) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                "Next Workout",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                nextWorkout.templateName ?: nextWorkout.label ?: "Workout",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                scheduleDateFormat.format(Date(nextWorkout.scheduledDate)),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            if (nextWorkout.label != null && nextWorkout.templateName != null) {
                                Text(
                                    nextWorkout.label,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }
                            if (nextWorkout.templateId != null) {
                                Spacer(Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        workoutViewModel.logFeatureUsage("start_workout")
                                        workoutViewModel.startWorkout(
                                            name = nextWorkout.templateName ?: "Workout",
                                            type = com.workout.tracker.data.entity.WorkoutType.STRENGTH,
                                            templateId = nextWorkout.templateId,
                                            scheduledWorkoutId = nextWorkout.id
                                        )
                                        navController.navigate(Screen.ActiveWorkout.route) {
                                            popUpTo(Screen.Home.route)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        contentColor = MaterialTheme.colorScheme.primaryContainer
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Start Workout", style = MaterialTheme.typography.titleSmall)
                                }
                            }
                        }
                    }
                }
            }

            // Quick actions row - only the essentials
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionCard(Modifier.weight(1f), Icons.Default.Sports, "Pushups") {
                        workoutViewModel.logFeatureUsage("pushups")
                        navController.navigate(Screen.Pushups.route)
                    }
                    QuickActionCard(Modifier.weight(1f), Icons.Default.CalendarMonth, "Schedule") {
                        workoutViewModel.logFeatureUsage("schedule")
                        navController.navigate(Screen.Schedule.route)
                    }
                    QuickActionCard(Modifier.weight(1f), Icons.Default.PlayArrow, "Start\nWorkout") {
                        workoutViewModel.logFeatureUsage("start_workout")
                        navController.navigate(Screen.StartWorkout.route)
                    }
                    QuickActionCard(Modifier.weight(1f), Icons.Default.MoreHoriz, "More") {
                        navController.navigate(Screen.Utilities.route)
                    }
                }
            }

            // Upcoming schedule - compact list
            if (upcomingAfterNext.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Upcoming", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { navController.navigate(Screen.Schedule.route) }) {
                            Text("See all", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                items(upcomingAfterNext) { scheduled ->
                    UpcomingWorkoutRow(scheduled, scheduleDateFormat)
                }
            }

            // Recent workouts - compact
            if (recentWorkouts.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(4.dp))
                    Text("Recent", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                }
                items(recentWorkouts.take(3)) { workout ->
                    val dateFormat = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }
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
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(workout.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            val durationMin = workout.endTime?.let { ((it - workout.startTime) / 60000).toInt() }
                            Text(
                                buildString {
                                    append(dateFormat.format(Date(workout.startTime)))
                                    if (durationMin != null && durationMin > 0) append(" \u2022 ${durationMin}m")
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun DashboardStatCard(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun UpcomingWorkoutRow(
    scheduled: com.workout.tracker.data.dao.ScheduledWorkoutWithTemplate,
    dateFormat: SimpleDateFormat
) {
    val displayName = scheduled.templateName ?: scheduled.label ?: "Unknown"
    val isRestDay = scheduled.label?.lowercase()?.contains("rest") == true

    val statusIcon = when {
        scheduled.isCompleted -> Icons.Default.CheckCircle
        scheduled.isSkipped -> Icons.Default.Cancel
        isRestDay -> Icons.Default.Hotel
        else -> Icons.Default.Circle
    }
    val statusColor = when {
        scheduled.isCompleted -> MaterialTheme.colorScheme.primary
        scheduled.isSkipped -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(statusIcon, contentDescription = null, tint = statusColor, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(12.dp))
        Text(displayName, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            dateFormat.format(Date(scheduled.scheduledDate)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
        modifier = modifier.height(72.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 2)
        }
    }
}
