package com.workout.tracker

import android.app.Application
import android.util.Log
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Wearable
import com.workout.tracker.data.ExerciseSeedData
import com.workout.tracker.data.JefitImporter
import com.workout.tracker.data.WorkoutDatabase
import com.workout.tracker.data.repository.WorkoutRepository
import com.workout.tracker.watch.WatchDiagnostics
import com.workout.tracker.watch.WatchEventBus
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
            database.savedRoutineDao(),
            database.pushupLogDao(),
            database.featureUsageDao(),
            database.personalRecordDao()
        )
    }

    private val wearMessageListener = MessageClient.OnMessageReceivedListener { event ->
        Log.d("WorkoutApp", "Wear runtime listener: ${event.path}")
        when (event.path) {
            "/workout/log_set" -> WatchEventBus.emit(WatchEventBus.Event.LogSetRequested)
            "/workout/pong" -> WatchDiagnostics.recordPong()
        }
    }

    override fun onCreate() {
        super.onCreate()

        // Register a runtime MessageClient listener so wear->phone messages
        // don't depend on the manifest WearableListenerService binding,
        // which is blocked by permission denial on some Wear OS / GMS
        // builds (BIND_WEARABLE_LISTENER_SERVICE).
        try {
            Wearable.getMessageClient(this).addListener(wearMessageListener)
        } catch (e: Exception) {
            Log.w("WorkoutApp", "Failed to register wear listener: ${e.message}")
        }

        // Set up notifications
        com.workout.tracker.notification.WorkoutNotificationHelper.createNotificationChannel(this)
        val prefs = getSharedPreferences("workout_prefs", MODE_PRIVATE)
        if (prefs.getBoolean("notifications_enabled", true)) {
            val hour = prefs.getInt("notification_hour", 8)
            val minute = prefs.getInt("notification_minute", 0)
            com.workout.tracker.notification.WorkoutNotificationHelper.scheduleDailyReminder(this, hour, minute)
            if (!prefs.contains("notifications_enabled")) {
                prefs.edit().putBoolean("notifications_enabled", true).apply()
            }
        }

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
