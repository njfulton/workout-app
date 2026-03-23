package com.workout.tracker.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.workout.tracker.MainActivity
import com.workout.tracker.R
import com.workout.tracker.data.WorkoutDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar

object WorkoutNotificationHelper {

    private const val CHANNEL_ID = "workout_reminder"
    private const val NOTIFICATION_ID = 1001
    private const val ALARM_REQUEST_CODE = 2001

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Workout Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily reminders about your scheduled workouts"
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    fun scheduleDailyReminder(context: Context, hour: Int = 8, minute: Int = 0) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, WorkoutReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, ALARM_REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    fun cancelDailyReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, WorkoutReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, ALARM_REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun showNotification(context: Context, title: String, message: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }
}

class WorkoutReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = WorkoutDatabase.getDatabase(context)

                val today = LocalDate.now()
                val startMillis = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val endMillis = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1

                val todaySchedule = db.scheduleDao().getAllScheduledWorkoutsList()
                    .filter { it.scheduledDate in startMillis..endMillis && !it.isCompleted && !it.isSkipped }

                if (todaySchedule.isNotEmpty()) {
                    val workoutNames = todaySchedule.map { sw ->
                        if (sw.templateId != null) {
                            val template = db.workoutTemplateDao().getTemplateById(sw.templateId)
                            val label = sw.label
                            if (label != null) "$label: ${template?.name ?: "Workout"}" else template?.name ?: "Workout"
                        } else {
                            sw.label ?: "Workout"
                        }
                    }

                    val message = if (workoutNames.size == 1) {
                        "Today's workout: ${workoutNames.first()}"
                    } else {
                        "Today's workouts: ${workoutNames.joinToString(", ")}"
                    }

                    WorkoutNotificationHelper.showNotification(
                        context,
                        "Time to Train!",
                        message
                    )
                }
            } catch (e: Exception) {
                // Silently fail - notifications are not critical
            } finally {
                pendingResult.finish()
            }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences("workout_prefs", Context.MODE_PRIVATE)
            if (prefs.getBoolean("notifications_enabled", false)) {
                val hour = prefs.getInt("notification_hour", 8)
                val minute = prefs.getInt("notification_minute", 0)
                WorkoutNotificationHelper.createNotificationChannel(context)
                WorkoutNotificationHelper.scheduleDailyReminder(context, hour, minute)
            }
        }
    }
}
