package com.workout.tracker

import android.app.Application
import com.workout.tracker.data.ExerciseSeedData
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
            database.scheduleDao()
        )
    }

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            if (repository.getExerciseCount() == 0) {
                repository.insertExercises(ExerciseSeedData.getDefaultExercises())
            }
        }
    }
}
