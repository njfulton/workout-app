package com.workout.tracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.workout.tracker.data.dao.FeatureUsageCount
import com.workout.tracker.ui.navigation.Screen
import com.workout.tracker.ui.viewmodel.WorkoutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UtilitiesScreen(
    navController: NavController,
    workoutViewModel: WorkoutViewModel
) {
    val usageCounts by workoutViewModel.featureUsageCounts.collectAsStateWithLifecycle()
    val usageMap = remember(usageCounts) { usageCounts.associateBy { it.featureName } }

    data class UtilityItem(
        val icon: androidx.compose.ui.graphics.vector.ImageVector,
        val label: String,
        val description: String,
        val featureKey: String,
        val onClick: () -> Unit
    )

    val utilities = listOf(
        UtilityItem(Icons.Default.History, "History", "View past workouts", "history") {
            workoutViewModel.logFeatureUsage("history")
            navController.navigate(Screen.History.route)
        },
        UtilityItem(Icons.Default.Assessment, "Weekly Summary", "Performance breakdown", "weekly_summary") {
            workoutViewModel.logFeatureUsage("weekly_summary")
            navController.navigate(Screen.WeeklySummary.route)
        },
        UtilityItem(Icons.Default.FitnessCenter, "Exercises", "Browse exercise library", "exercises") {
            workoutViewModel.logFeatureUsage("exercises")
            navController.navigate(Screen.Exercises.route)
        },
        UtilityItem(Icons.AutoMirrored.Filled.ViewList, "Templates", "Manage workout templates", "templates") {
            workoutViewModel.logFeatureUsage("templates")
            navController.navigate(Screen.Templates.route)
        },
        UtilityItem(Icons.Default.Build, "Build Routine", "Create a training routine", "build_routine") {
            workoutViewModel.logFeatureUsage("build_routine")
            navController.navigate(Screen.RoutineBuilder.route)
        },
        UtilityItem(Icons.Default.FolderOpen, "Saved Routines", "View saved routines", "saved_routines") {
            workoutViewModel.logFeatureUsage("saved_routines")
            navController.navigate(Screen.SavedRoutines.route)
        },
        UtilityItem(Icons.Default.FileDownload, "Import", "Import routine data", "import") {
            workoutViewModel.logFeatureUsage("import")
            navController.navigate(Screen.ImportRoutine.route)
        },
        UtilityItem(Icons.Default.Backup, "Backup & Restore", "Export or restore data", "backup") {
            workoutViewModel.logFeatureUsage("backup")
            navController.navigate(Screen.BackupRestore.route)
        },
        UtilityItem(Icons.Default.Bolt, "Quick Log", "Log a single exercise fast", "quick_log") {
            workoutViewModel.logFeatureUsage("quick_log")
            navController.navigate(Screen.QuickLog.route)
        },
        UtilityItem(Icons.Default.Calculate, "Plate Calculator", "Calculate plate loading", "plate_calculator") {
            workoutViewModel.logFeatureUsage("plate_calculator")
            navController.navigate(Screen.PlateCalculator.route)
        },
        UtilityItem(Icons.Default.HealthAndSafety, "Health Connect", "Sync with Google Fit", "health_connect") {
            workoutViewModel.logFeatureUsage("health_connect")
            navController.navigate(Screen.HealthConnect.route)
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Utilities", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(utilities) { item ->
                val count = usageMap[item.featureKey]
                UtilityRow(item.icon, item.label, item.description, count, item.onClick)
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UtilityRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    description: String,
    usage: FeatureUsageCount?,
    onClick: () -> Unit
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (usage != null && usage.useCount > 0) {
                Text(
                    "${usage.useCount}x",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
