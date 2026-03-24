package com.workout.tracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
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
}

@Composable
fun WorkoutNavHost(navController: NavHostController) {
    val exerciseViewModel: ExerciseViewModel = viewModel(factory = ExerciseViewModel.Factory)
    val workoutViewModel: WorkoutViewModel = viewModel(factory = WorkoutViewModel.Factory)
    val templateViewModel: TemplateViewModel = viewModel(factory = TemplateViewModel.Factory)
    val scheduleViewModel: ScheduleViewModel = viewModel(factory = ScheduleViewModel.Factory)

    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                navController = navController,
                workoutViewModel = workoutViewModel,
                scheduleViewModel = scheduleViewModel
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
    }
        composable(Screen.Utilities.route) {
            UtilitiesScreen(
                navController = navController,
                workoutViewModel = workoutViewModel
            )
        }
    }
}
