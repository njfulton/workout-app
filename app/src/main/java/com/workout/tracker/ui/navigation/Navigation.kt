package com.workout.tracker.ui.navigation

import androidx.compose.runtime.Composable
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
                viewModel = exerciseViewModel
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
    }
}
