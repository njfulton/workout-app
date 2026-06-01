package com.workout.tracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.workout.tracker.data.dao.FeatureUsageCount
import com.workout.tracker.ui.navigation.Screen
import com.workout.tracker.ui.viewmodel.WorkoutViewModel

private data class UtilityItem(
    val icon: ImageVector,
    val label: String,
    val description: String,
    val featureKey: String,
    val onClick: () -> Unit
)

@Composable
fun UtilitiesScreen(
    navController: NavController,
    workoutViewModel: WorkoutViewModel
) {
    val usageCounts by workoutViewModel.featureUsageCounts.collectAsStateWithLifecycle()
    val usageMap = remember(usageCounts) { usageCounts.associateBy { it.featureName } }

    val workoutsSection = remember {
        listOf(
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
            }
        )
    }

    val routinesSection = remember {
        listOf(
            UtilityItem(Icons.Default.Build, "Build Routine", "Create a training routine", "build_routine") {
                workoutViewModel.logFeatureUsage("build_routine")
                navController.navigate(Screen.RoutineBuilder.route)
            },
            UtilityItem(Icons.Default.FolderOpen, "Saved Routines", "View saved routines", "saved_routines") {
                workoutViewModel.logFeatureUsage("saved_routines")
                navController.navigate(Screen.SavedRoutines.route)
            }
        )
    }

    val toolsSection = remember {
        listOf(
            UtilityItem(Icons.Default.Calculate, "Plate Calculator", "Calculate plate loading", "plate_calculator") {
                workoutViewModel.logFeatureUsage("plate_calculator")
                navController.navigate(Screen.PlateCalculator.route)
            },
            UtilityItem(Icons.Default.Bolt, "Quick Log", "Log a single exercise fast", "quick_log") {
                workoutViewModel.logFeatureUsage("quick_log")
                navController.navigate(Screen.QuickLog.route)
            }
        )
    }

    val dataSection = remember {
        listOf(
            UtilityItem(Icons.Default.Backup, "Backup & Restore", "Export or restore data", "backup") {
                workoutViewModel.logFeatureUsage("backup")
                navController.navigate(Screen.BackupRestore.route)
            },
            UtilityItem(Icons.Default.FileDownload, "Import", "Import routine data", "import") {
                workoutViewModel.logFeatureUsage("import")
                navController.navigate(Screen.ImportRoutine.route)
            },
            UtilityItem(Icons.Default.HealthAndSafety, "Health Connect", "Sync with Google Fit", "health_connect") {
                workoutViewModel.logFeatureUsage("health_connect")
                navController.navigate(Screen.HealthConnect.route)
            }
        )
    }

    val devicesSection = remember {
        listOf(
            UtilityItem(Icons.Default.Watch, "Watch Connection", "Diagnose Wear OS pairing", "watch_diagnostics") {
                workoutViewModel.logFeatureUsage("watch_diagnostics")
                navController.navigate(Screen.WatchDiagnostics.route)
            }
        )
    }

    val cardShape = RoundedCornerShape(20.dp)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // ---- Custom header ----
            item {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable { navController.popBackStack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "UTILITIES",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            // ---- 01 WORKOUTS ----
            item {
                SectionHeader(number = "01", title = "WORKOUTS", icon = Icons.Default.FitnessCenter)
                GroupedCard(items = workoutsSection, usageMap = usageMap, cardShape = cardShape)
                Spacer(Modifier.height(24.dp))
            }

            // ---- 02 ROUTINES ----
            item {
                SectionHeader(number = "02", title = "ROUTINES", icon = Icons.Default.Build)
                GroupedCard(items = routinesSection, usageMap = usageMap, cardShape = cardShape)
                Spacer(Modifier.height(24.dp))
            }

            // ---- 03 TOOLS ----
            item {
                SectionHeader(number = "03", title = "TOOLS", icon = Icons.Default.Bolt)
                GroupedCard(items = toolsSection, usageMap = usageMap, cardShape = cardShape)
                Spacer(Modifier.height(24.dp))
            }

            // ---- 04 DATA ----
            item {
                SectionHeader(number = "04", title = "DATA", icon = Icons.Default.Backup)
                GroupedCard(items = dataSection, usageMap = usageMap, cardShape = cardShape)
                Spacer(Modifier.height(24.dp))
            }

            // ---- 05 DEVICES ----
            item {
                SectionHeader(number = "05", title = "DEVICES", icon = Icons.Default.Watch)
                GroupedCard(items = devicesSection, usageMap = usageMap, cardShape = cardShape)
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(
    number: String,
    title: String,
    icon: ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Text(
            text = number,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(Modifier.width(8.dp))
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.5.sp
        )
        Spacer(Modifier.width(12.dp))
        @Suppress("DEPRECATION") Divider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun GroupedCard(
    items: List<UtilityItem>,
    usageMap: Map<String, FeatureUsageCount>,
    cardShape: RoundedCornerShape
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = cardShape
            ),
        shape = cardShape,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column {
            items.forEachIndexed { index, item ->
                val usage = usageMap[item.featureKey]
                UtilityRow(
                    icon = item.icon,
                    label = item.label,
                    description = item.description,
                    usage = usage,
                    onClick = item.onClick
                )
                if (index < items.lastIndex) {
                    @Suppress("DEPRECATION") Divider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
private fun UtilityRow(
    icon: ImageVector,
    label: String,
    description: String,
    usage: FeatureUsageCount?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (usage != null && usage.useCount > 0) {
            Text(
                "${usage.useCount}x",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp)
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}
