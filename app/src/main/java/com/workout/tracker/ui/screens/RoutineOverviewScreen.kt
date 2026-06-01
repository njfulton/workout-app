package com.workout.tracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.workout.tracker.ui.viewmodel.TemplateViewModel
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineOverviewScreen(
    navController: NavController,
    templateViewModel: TemplateViewModel
) {
    val overview by templateViewModel.routineOverview.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        templateViewModel.loadRoutineOverview()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Routine") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        val data = overview
        if (data == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No active routine.\nImport a routine from the Templates screen.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    data.routine.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Week ${data.currentWeek} of ${data.totalWeeks}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Progress bar
            item {
                LinearProgressIndicator(
                    progress = data.currentWeek.toFloat() / data.totalWeeks.toFloat(),
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                )
            }

            // Weeks
            items(data.weeks.size) { idx ->
                val week = data.weeks[idx]
                RoutineWeekCard(week)
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun RoutineWeekCard(week: TemplateViewModel.RoutineWeek) {
    val dateFormat = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }
    val weekStartStr = dateFormat.format(
        Date.from(week.startDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
    )
    val weekEndStr = dateFormat.format(
        Date.from(week.startDate.plusDays(6).atStartOfDay(ZoneId.systemDefault()).toInstant())
    )

    val bgColor = when {
        week.isCurrent -> MaterialTheme.colorScheme.primaryContainer
        week.isPast -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Week ${week.weekNumber}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "$weekStartStr – $weekEndStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                if (week.isCurrent) {
                    Text(
                        "CURRENT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (week.isPhaseStart && week.weekNumber > 1) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "New phase — workload changes",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            if (week.items.isEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "No workouts scheduled",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            } else {
                Spacer(Modifier.height(6.dp))
                week.items.forEach { item ->
                    val isRestDay = item.label?.lowercase()?.contains("rest") == true
                    val icon = when {
                        isRestDay -> Icons.Default.Hotel
                        item.isCompleted -> Icons.Default.CheckCircle
                        else -> Icons.Default.FitnessCenter
                    }
                    val tint = when {
                        item.isCompleted -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = tint)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            item.templateName ?: item.label ?: "Workout",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            item.dayOfWeek(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun com.workout.tracker.data.dao.ScheduledWorkoutWithTemplate.dayOfWeek(): String {
    return java.time.Instant.ofEpochMilli(this.scheduledDate)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalDate()
        .dayOfWeek
        .getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault())
}
