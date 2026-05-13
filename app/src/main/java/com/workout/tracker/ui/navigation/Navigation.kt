package com.workout.tracker.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.workout.tracker.ui.screens.*
import com.workout.tracker.ui.viewmodel.*

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Exercises : Screen("exercises")
    object Templates : Screen("templates")
    object CreateTemplate : Screen("create_template")
    object ActiveWorkout : Screen("active_workout")
    object QuickLog : Screen("quick_log")
    object History : Screen("history")
    object Schedule : Screen("schedule")
    object StartWorkout : Screen("start_workout")
    object WorkoutDetail : Screen("workout_detail/{workoutLogId}") {
        fun createRoute(workoutLogId: Long) = "workout_detail/$workoutLogId"
    }
    object ImportRoutine : Screen("import_routine")
    object EditTemplate : Screen("edit_template/{templateId}") {
        fun createRoute(templateId: Long) = "edit_template/$templateId"
    }
    object SavedRoutines : Screen("saved_routines")
    object BackupRestore : Screen("backup_restore")
    object Pushups : Screen("pushups")
    object WeeklySummary : Screen("weekly_summary")
    object RoutineBuilder : Screen("routine_builder")
    object ExerciseProgress : Screen("exercise_progress/{exerciseId}") {
        fun createRoute(exerciseId: Long) = "exercise_progress/$exerciseId"
    }
    object Utilities : Screen("utilities")
    object WorkoutSummary : Screen("workout_summary")
    object PlateCalculator : Screen("plate_calculator")
    object HealthConnect : Screen("health_connect")
    object RoutineOverview : Screen("routine_overview")
    object WatchDiagnostics : Screen("watch_diagnostics")
}

@Composable
fun WorkoutNavHost(navController: NavHostController) {
    val exerciseViewModel: ExerciseViewModel = viewModel(factory = ExerciseViewModel.Factory)
    val workoutViewModel: WorkoutViewModel = viewModel(factory = WorkoutViewModel.Factory)
    val templateViewModel: TemplateViewModel = viewModel(factory = TemplateViewModel.Factory)
    val scheduleViewModel: ScheduleViewModel = viewModel(factory = ScheduleViewModel.Factory)

    val activeWorkout by workoutViewModel.activeWorkout.collectAsStateWithLifecycle()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val showWorkoutBar = activeWorkout.isActive && currentRoute != Screen.ActiveWorkout.route

    Column(modifier = Modifier.fillMaxSize()) {
        if (showWorkoutBar) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable {
                        navController.navigate(Screen.ActiveWorkout.route) {
                            launchSingleTop = true
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    activeWorkout.workoutLog?.name ?: "Workout in progress",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
                Text(
                    "Return",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Return to workout",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                navController = navController,
                workoutViewModel = workoutViewModel,
                scheduleViewModel = scheduleViewModel,
                templateViewModel = templateViewModel
            )
        }
        composable(Screen.Exercises.route) {
            ExerciseListScreen(
                navController = navController,
                viewModel = exerciseViewModel,
                onExerciseProgressClick = { exerciseId, exerciseName ->
                    workoutViewModel.loadExerciseProgress(exerciseId, exerciseName)
                    navController.navigate(Screen.ExerciseProgress.createRoute(exerciseId))
                }
            )
        }
        composable(Screen.Templates.route) {
            TemplateListScreen(
                navController = navController,
                viewModel = templateViewModel
            )
        }
        composable(Screen.CreateTemplate.route) {
            CreateTemplateScreen(
                navController = navController,
                templateViewModel = templateViewModel,
                exerciseViewModel = exerciseViewModel
            )
        }
        composable(
            Screen.EditTemplate.route,
            arguments = listOf(navArgument("templateId") { type = NavType.LongType })
        ) { backStackEntry ->
            val templateId = backStackEntry.arguments?.getLong("templateId") ?: return@composable
            CreateTemplateScreen(
                navController = navController,
                templateViewModel = templateViewModel,
                exerciseViewModel = exerciseViewModel,
                editTemplateId = templateId
            )
        }
        composable(Screen.StartWorkout.route) {
            StartWorkoutScreen(
                navController = navController,
                templateViewModel = templateViewModel,
                workoutViewModel = workoutViewModel
            )
        }
        composable(Screen.ActiveWorkout.route) {
            ActiveWorkoutScreen(
                navController = navController,
                workoutViewModel = workoutViewModel,
                exerciseViewModel = exerciseViewModel
            )
        }
        composable(Screen.QuickLog.route) {
            QuickLogScreen(
                navController = navController,
                workoutViewModel = workoutViewModel,
                exerciseViewModel = exerciseViewModel
            )
        }
        composable(Screen.History.route) {
            HistoryScreen(
                navController = navController,
                viewModel = workoutViewModel
            )
        }
        composable(
            Screen.WorkoutDetail.route,
            arguments = listOf(navArgument("workoutLogId") { type = NavType.LongType })
        ) { backStackEntry ->
            val workoutLogId = backStackEntry.arguments?.getLong("workoutLogId") ?: return@composable
            WorkoutDetailScreen(
                navController = navController,
                viewModel = workoutViewModel,
                workoutLogId = workoutLogId
            )
        }
        composable(Screen.ImportRoutine.route) {
            ImportRoutineScreen(
                navController = navController,
                templateViewModel = templateViewModel
            )
        }
        composable(Screen.Schedule.route) {
            ScheduleScreen(
                navController = navController,
                scheduleViewModel = scheduleViewModel,
                templateViewModel = templateViewModel,
                workoutViewModel = workoutViewModel
            )
        }
        composable(Screen.SavedRoutines.route) {
            SavedRoutinesScreen(
                navController = navController,
                templateViewModel = templateViewModel,
                scheduleViewModel = scheduleViewModel
            )
        }
        composable(Screen.BackupRestore.route) {
            val context = LocalContext.current
            val app = context.applicationContext as com.workout.tracker.WorkoutApp
            val backupManager = remember {
                com.workout.tracker.data.BackupManager(context, app.repository, app.database)
            }
            BackupRestoreScreen(
                navController = navController,
                backupManager = backupManager
            )
        }
        composable(Screen.Pushups.route) {
            val context = LocalContext.current
            val app = context.applicationContext as com.workout.tracker.WorkoutApp
            PushupScreen(
                navController = navController,
                repository = app.repository
            )
        }
        composable(Screen.RoutineBuilder.route) {
            RoutineBuilderScreen(
                navController = navController,
                templateViewModel = templateViewModel
            )
        }
        composable(Screen.WeeklySummary.route) {
            val context = LocalContext.current
            val app = context.applicationContext as com.workout.tracker.WorkoutApp
            WeeklySummaryScreen(
                navController = navController,
                repository = app.repository
            )
        }
        composable(
            Screen.ExerciseProgress.route,
            arguments = listOf(navArgument("exerciseId") { type = NavType.LongType })
        ) { backStackEntry ->
            val exerciseId = backStackEntry.arguments?.getLong("exerciseId") ?: return@composable
            ExerciseProgressScreen(
                navController = navController,
                workoutViewModel = workoutViewModel,
                exerciseId = exerciseId
            )
        }
        composable(Screen.Utilities.route) {
            UtilitiesScreen(
                navController = navController,
                workoutViewModel = workoutViewModel
            )
        }
        composable(Screen.WorkoutSummary.route) {
            WorkoutSummaryScreen(
                navController = navController,
                workoutViewModel = workoutViewModel
            )
        }
        composable(Screen.PlateCalculator.route) {
            PlateCalculatorScreen(navController = navController)
        }
        composable(Screen.HealthConnect.route) {
            HealthConnectScreen(navController = navController)
        }
        composable(Screen.RoutineOverview.route) {
            RoutineOverviewScreen(navController = navController, templateViewModel = templateViewModel)
        }
        composable(Screen.WatchDiagnostics.route) {
            WatchDiagnosticsScreen(navController = navController)
        }
    }
    } // Column
}
