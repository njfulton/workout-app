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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.workout.tracker.data.dao.MuscleGroupVolume
import com.workout.tracker.data.dao.WorkoutLogSummary
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import com.workout.tracker.data.repository.WorkoutRepository
import com.workout.tracker.ui.navigation.Screen
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklySummaryScreen(
    navController: NavController,
    repository: WorkoutRepository
) {
    var weekOffset by remember { mutableIntStateOf(0) } // 0 = this week, -1 = last week, etc.

    val today = LocalDate.now()
    val weekStart = today.plusWeeks(weekOffset.toLong())
        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val weekEnd = weekStart.plusDays(6)

    val startMillis = weekStart.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val endMillis = weekEnd.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1

    val workouts by repository.getWorkoutsBetween(startMillis, endMillis)
        .collectAsStateWithLifecycle(emptyList())

    val pushupLogs by repository.getPushupLogsBetween(startMillis, endMillis)
        .collectAsStateWithLifecycle(emptyList())

    val totalPushups = pushupLogs.sumOf { it.count }
    val pushupSessions = pushupLogs.size

    val totalWorkoutMinutes = workouts.sumOf { w ->
        val end = w.endTime ?: w.startTime
        ((end - w.startTime) / 60000).toInt()
    }

    var muscleGroupVolume by remember { mutableStateOf<List<MuscleGroupVolume>>(emptyList()) }
    LaunchedEffect(startMillis, endMillis) {
        muscleGroupVolume = repository.getMuscleGroupVolume(startMillis, endMillis)
    }

    val dateFormatter = DateTimeFormatter.ofPattern("MMM d")
    val weekLabel = "${weekStart.format(dateFormatter)} - ${weekEnd.format(dateFormatter)}, ${weekEnd.year}"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Weekly Summary") },
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
            // Week navigation
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { weekOffset-- }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous week")
                    }
                    Text(
                        weekLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { weekOffset++ },
                        enabled = weekOffset < 0
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next week")
                    }
                }
            }

            // Stats cards row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.FitnessCenter,
                        value = "${workouts.size}",
                        label = "Workouts"
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Timer,
                        value = "${totalWorkoutMinutes}m",
                        label = "Time"
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Sports,
                        value = "$totalPushups",
                        label = "Pushups"
                    )
                }
            }

            // Muscle group volume
            if (muscleGroupVolume.isNotEmpty()) {
                item {
                    Text(
                        "Volume by Muscle Group",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            val maxSets = muscleGroupVolume.maxOf { it.totalSets }
                            muscleGroupVolume.forEach { mgv ->
                                val name = mgv.muscleGroup.lowercase().replace("_", " ")
                                    .replaceFirstChar { it.uppercase() }
                                val fraction = mgv.totalSets.toFloat() / maxSets.coerceAtLeast(1)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        name,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.width(90.dp)
                                    )
                                    Box(modifier = Modifier.weight(1f).height(20.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(fraction)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(MaterialTheme.colorScheme.primary)
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "${mgv.totalSets} sets",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.width(52.dp),
                                        textAlign = TextAlign.End
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Workouts list
            if (workouts.isNotEmpty()) {
                item {
                    Text(
                        "Workouts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                items(workouts, key = { it.id }) { workout ->
                    WorkoutSummaryCard(workout, navController)
                }
            }

            // Pushup sessions
            if (pushupLogs.isNotEmpty()) {
                item {
                    Text(
                        "Pushup Sessions ($pushupSessions)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                items(pushupLogs, key = { it.id }) { log ->
                    PushupLogItem(log)
                }
            }

            // Empty state
            if (workouts.isEmpty() && pushupLogs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.EventBusy,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "No activity this week",
                                style = MaterialTheme.typography.bodyLarge,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(4.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutSummaryCard(
    workout: WorkoutLogSummary,
    navController: NavController
) {
    val dateFormat = remember { SimpleDateFormat("EEE h:mm a", Locale.getDefault()) }
    val durationMin = workout.endTime?.let { ((it - workout.startTime) / 60000).toInt() }

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
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(workout.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
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
