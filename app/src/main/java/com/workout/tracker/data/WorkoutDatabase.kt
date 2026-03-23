package com.workout.tracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.workout.tracker.data.dao.ExerciseDao
import com.workout.tracker.data.dao.PushupLogDao
import com.workout.tracker.data.dao.SavedRoutineDao
import com.workout.tracker.data.dao.ScheduleDao
import com.workout.tracker.data.dao.WorkoutLogDao
import com.workout.tracker.data.dao.WorkoutTemplateDao
import com.workout.tracker.data.entity.*

@Database(
    entities = [
        Exercise::class,
        WorkoutTemplate::class,
        TemplateExercise::class,
        WorkoutLog::class,
        ExerciseLog::class,
        SetLog::class,
        ScheduledWorkout::class,
        SavedRoutine::class,
        RoutineUsageHistory::class,
        PushupLog::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class WorkoutDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutTemplateDao(): WorkoutTemplateDao
    abstract fun workoutLogDao(): WorkoutLogDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun savedRoutineDao(): SavedRoutineDao
    abstract fun pushupLogDao(): PushupLogDao

    companion object {
        @Volatile
        private var INSTANCE: WorkoutDatabase? = null

        fun getDatabase(context: Context): WorkoutDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WorkoutDatabase::class.java,
                    "workout_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
