package com.workout.tracker

import android.app.Application
import android.util.Log
import com.workout.tracker.data.ExerciseSeedData
import com.workout.tracker.data.JefitImporter
import com.workout.tracker.data.WorkoutDatabase
import com.workout.tracker.data.repository.WorkoutRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WorkoutApp : Application() {
    val applicationScope = CoroutineScope(SupervisorJob())
    val database by lazy { WorkoutDatabase.getDatabase(this) }
    val repository by lazy {
        WorkoutRepository(
            database.exerciseDao(),
            database.workoutTemplateDao(),
            database.workoutLogDao(),
            database.scheduleDao(),
            database.savedRoutineDao()
        )
    }

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            if (repository.getExerciseCount() == 0) {
                // Seed default exercises first
                repository.insertExercises(ExerciseSeedData.getDefaultExercises())

                // Import JEFIT history data
                try {
                    val importer = JefitImporter(this@WorkoutApp, repository)
                    importer.importFromAssets()
                    Log.d("WorkoutApp", "JEFIT data import completed successfully")
                } catch (e: Exception) {
                    Log.e("WorkoutApp", "Failed to import JEFIT data", e)
                }
            }
        }
    }
}
