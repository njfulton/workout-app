package com.workout.tracker.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.workout.tracker.data.entity.*
import com.workout.tracker.data.repository.WorkoutRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class BackupManager(
    private val context: Context,
    private val repository: WorkoutRepository,
    private val database: WorkoutDatabase
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.US)

    suspend fun exportToJson(): Result<File> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject()
            json.put("version", 1)
            json.put("exportDate", System.currentTimeMillis())
            json.put("appVersion", "1.0")

            // Exercises
            val exerciseDao = database.exerciseDao()
            val exercises = exerciseDao.getAllExercisesList()
            json.put("exercises", exercisesToJson(exercises))

            // Templates
            val templateDao = database.workoutTemplateDao()
            val templates = templateDao.getAllTemplatesList()
            json.put("templates", templatesToJson(templates))

            // Template exercises
            val templateExercises = mutableListOf<TemplateExercise>()
            for (template in templates) {
                templateExercises.addAll(templateDao.getTemplateExercises(template.id))
            }
            json.put("templateExercises", templateExercisesToJson(templateExercises))

            // Workout logs
            val logDao = database.workoutLogDao()
            val workoutLogs = logDao.getAllWorkoutLogsList()
            json.put("workoutLogs", workoutLogsToJson(workoutLogs))

            // Exercise logs
            val exerciseLogs = mutableListOf<ExerciseLog>()
            for (log in workoutLogs) {
                exerciseLogs.addAll(logDao.getExerciseLogs(log.id))
            }
            json.put("exerciseLogs", exerciseLogsToJson(exerciseLogs))

            // Set logs
            val setLogs = mutableListOf<SetLog>()
            for (exLog in exerciseLogs) {
                setLogs.addAll(logDao.getSetLogs(exLog.id))
            }
            json.put("setLogs", setLogsToJson(setLogs))

            // Scheduled workouts
            val scheduleDao = database.scheduleDao()
            val scheduled = scheduleDao.getAllScheduledWorkoutsList()
            json.put("scheduledWorkouts", scheduledWorkoutsToJson(scheduled))

            // Saved routines
            val savedRoutineDao = database.savedRoutineDao()
            val savedRoutines = savedRoutineDao.getAllSavedRoutinesList()
            json.put("savedRoutines", savedRoutinesToJson(savedRoutines))

            // Routine usage history
            val usageHistory = mutableListOf<RoutineUsageHistory>()
            for (routine in savedRoutines) {
                usageHistory.addAll(savedRoutineDao.getUsageHistoryList(routine.id))
            }
            json.put("routineUsageHistory", routineUsageHistoryToJson(usageHistory))

            // Pushup logs
            val pushupLogDao = database.pushupLogDao()
            val pushupLogs = pushupLogDao.getAllPushupLogsList()
            json.put("pushupLogs", pushupLogsToJson(pushupLogs))

            // Feature usage logs
            val featureUsageDao = database.featureUsageDao()
            val featureUsageLogs = featureUsageDao.getAllList()
            json.put("featureUsageLogs", featureUsageLogsToJson(featureUsageLogs))

            // Write to Downloads
            val fileName = "workout_backup_${dateFormat.format(Date())}.json"
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            downloadsDir.mkdirs()
            val file = File(downloadsDir, fileName)
            file.writeText(json.toString(2))

            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importFromJson(uri: Uri): Result<ImportSummary> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("Could not open file"))
            val jsonText = inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(jsonText)

            var exerciseCount = 0
            var templateCount = 0
            var workoutLogCount = 0
            var setLogCount = 0

            // Import exercises (use original IDs for FK references)
            val oldToNewExerciseId = mutableMapOf<Long, Long>()
            if (json.has("exercises")) {
                val exercises = jsonToExercises(json.getJSONArray("exercises"))
                for (ex in exercises) {
                    val existing = database.exerciseDao().getExerciseByName(ex.name)
                    if (existing != null) {
                        oldToNewExerciseId[ex.id] = existing.id
                    } else {
                        val newId = database.exerciseDao().insert(ex.copy(id = 0))
                        oldToNewExerciseId[ex.id] = newId
                        exerciseCount++
                    }
                }
            }

            // Import templates
            val oldToNewTemplateId = mutableMapOf<Long, Long>()
            if (json.has("templates")) {
                val templates = jsonToTemplates(json.getJSONArray("templates"))
                for (t in templates) {
                    val newId = database.workoutTemplateDao().insertTemplate(t.copy(id = 0))
                    oldToNewTemplateId[t.id] = newId
                    templateCount++
                }
            }

            // Import template exercises
            if (json.has("templateExercises")) {
                val tes = jsonToTemplateExercises(json.getJSONArray("templateExercises"))
                for (te in tes) {
                    val newTemplateId = oldToNewTemplateId[te.templateId] ?: continue
                    val newExerciseId = oldToNewExerciseId[te.exerciseId] ?: continue
                    database.workoutTemplateDao().insertTemplateExercise(
                        te.copy(id = 0, templateId = newTemplateId, exerciseId = newExerciseId)
                    )
                }
            }

            // Import workout logs
            val oldToNewLogId = mutableMapOf<Long, Long>()
            if (json.has("workoutLogs")) {
                val logs = jsonToWorkoutLogs(json.getJSONArray("workoutLogs"))
                for (log in logs) {
                    val newTemplateId = log.templateId?.let { oldToNewTemplateId[it] }
                    val newId = database.workoutLogDao().insertWorkoutLog(
                        log.copy(id = 0, templateId = newTemplateId)
                    )
                    oldToNewLogId[log.id] = newId
                    workoutLogCount++
                }
            }

            // Import exercise logs
            val oldToNewExLogId = mutableMapOf<Long, Long>()
            if (json.has("exerciseLogs")) {
                val exLogs = jsonToExerciseLogs(json.getJSONArray("exerciseLogs"))
                for (el in exLogs) {
                    val newLogId = oldToNewLogId[el.workoutLogId] ?: continue
                    val newExId = oldToNewExerciseId[el.exerciseId] ?: continue
                    val newId = database.workoutLogDao().insertExerciseLog(
                        el.copy(id = 0, workoutLogId = newLogId, exerciseId = newExId)
                    )
                    oldToNewExLogId[el.id] = newId
                }
            }

            // Import set logs
            if (json.has("setLogs")) {
                val sets = jsonToSetLogs(json.getJSONArray("setLogs"))
                for (s in sets) {
                    val newExLogId = oldToNewExLogId[s.exerciseLogId] ?: continue
                    database.workoutLogDao().insertSetLog(
                        s.copy(id = 0, exerciseLogId = newExLogId)
                    )
                    setLogCount++
                }
            }

            // Import scheduled workouts
            if (json.has("scheduledWorkouts")) {
                val scheduled = jsonToScheduledWorkouts(json.getJSONArray("scheduledWorkouts"))
                for (sw in scheduled) {
                    val newTemplateId = sw.templateId?.let { oldToNewTemplateId[it] }
                    database.scheduleDao().insert(
                        sw.copy(id = 0, templateId = newTemplateId)
                    )
                }
            }

            // Import saved routines
            val oldToNewRoutineId = mutableMapOf<Long, Long>()
            if (json.has("savedRoutines")) {
                val routines = jsonToSavedRoutines(json.getJSONArray("savedRoutines"))
                for (r in routines) {
                    val newId = database.savedRoutineDao().insert(r.copy(id = 0))
                    oldToNewRoutineId[r.id] = newId
                }
            }

            // Import routine usage history
            if (json.has("routineUsageHistory")) {
                val history = jsonToRoutineUsageHistory(json.getJSONArray("routineUsageHistory"))
                for (h in history) {
                    val newRoutineId = oldToNewRoutineId[h.savedRoutineId] ?: continue
                    database.savedRoutineDao().insertUsageHistory(
                        h.copy(id = 0, savedRoutineId = newRoutineId)
                    )
                }
            }

            // Import pushup logs
            var pushupCount = 0
            if (json.has("pushupLogs")) {
                val logs = jsonToPushupLogs(json.getJSONArray("pushupLogs"))
                for (log in logs) {
                    database.pushupLogDao().insert(log.copy(id = 0))
                    pushupCount++
                }
            }

            // Import feature usage logs
            var featureUsageCount = 0
            if (json.has("featureUsageLogs")) {
                val logs = jsonToFeatureUsageLogs(json.getJSONArray("featureUsageLogs"))
                for (log in logs) {
                    database.featureUsageDao().insert(log.copy(id = 0))
                    featureUsageCount++
                }
            }

            Result.success(ImportSummary(exerciseCount, templateCount, workoutLogCount, setLogCount, pushupCount, featureUsageCount))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Serialization helpers ---

    private fun exercisesToJson(exercises: List<Exercise>): JSONArray {
        val arr = JSONArray()
        for (e in exercises) {
            arr.put(JSONObject().apply {
                put("id", e.id)
                put("name", e.name)
                put("category", e.category.name)
                put("muscleGroup", e.muscleGroup.name)
                put("equipment", e.equipment ?: JSONObject.NULL)
                put("notes", e.notes ?: JSONObject.NULL)
                put("isCustom", e.isCustom)
            })
        }
        return arr
    }

    private fun jsonToExercises(arr: JSONArray): List<Exercise> {
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Exercise(
                id = o.getLong("id"),
                name = o.getString("name"),
                category = ExerciseCategory.valueOf(o.getString("category")),
                muscleGroup = MuscleGroup.valueOf(o.getString("muscleGroup")),
                equipment = o.optNullableString("equipment"),
                notes = o.optNullableString("notes"),
                isCustom = o.optBoolean("isCustom", false)
            )
        }
    }

    private fun templatesToJson(templates: List<WorkoutTemplate>): JSONArray {
        val arr = JSONArray()
        for (t in templates) {
            arr.put(JSONObject().apply {
                put("id", t.id)
                put("name", t.name)
                put("description", t.description ?: JSONObject.NULL)
                put("estimatedDurationMinutes", t.estimatedDurationMinutes ?: JSONObject.NULL)
            })
        }
        return arr
    }

    private fun jsonToTemplates(arr: JSONArray): List<WorkoutTemplate> {
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            WorkoutTemplate(
                id = o.getLong("id"),
                name = o.getString("name"),
                description = o.optNullableString("description"),
                estimatedDurationMinutes = o.optNullableInt("estimatedDurationMinutes")
            )
        }
    }

    private fun templateExercisesToJson(tes: List<TemplateExercise>): JSONArray {
        val arr = JSONArray()
        for (te in tes) {
            arr.put(JSONObject().apply {
                put("id", te.id)
                put("templateId", te.templateId)
                put("exerciseId", te.exerciseId)
                put("orderIndex", te.orderIndex)
                put("targetSets", te.targetSets)
                put("targetReps", te.targetReps)
                put("restSeconds", te.restSeconds)
                put("supersetGroup", te.supersetGroup ?: JSONObject.NULL)
            })
        }
        return arr
    }

    private fun jsonToTemplateExercises(arr: JSONArray): List<TemplateExercise> {
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            TemplateExercise(
                id = o.getLong("id"),
                templateId = o.getLong("templateId"),
                exerciseId = o.getLong("exerciseId"),
                orderIndex = o.getInt("orderIndex"),
                targetSets = o.optInt("targetSets", 3),
                targetReps = o.optInt("targetReps", 10),
                restSeconds = o.optInt("restSeconds", 90),
                supersetGroup = o.optNullableInt("supersetGroup")
            )
        }
    }

    private fun workoutLogsToJson(logs: List<WorkoutLog>): JSONArray {
        val arr = JSONArray()
        for (l in logs) {
            arr.put(JSONObject().apply {
                put("id", l.id)
                put("templateId", l.templateId ?: JSONObject.NULL)
                put("name", l.name)
                put("startTime", l.startTime)
                put("endTime", l.endTime ?: JSONObject.NULL)
                put("notes", l.notes ?: JSONObject.NULL)
                put("workoutType", l.workoutType.name)
            })
        }
        return arr
    }

    private fun jsonToWorkoutLogs(arr: JSONArray): List<WorkoutLog> {
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            WorkoutLog(
                id = o.getLong("id"),
                templateId = o.optNullableLong("templateId"),
                name = o.getString("name"),
                startTime = o.getLong("startTime"),
                endTime = o.optNullableLong("endTime"),
                notes = o.optNullableString("notes"),
                workoutType = WorkoutType.valueOf(o.optString("workoutType", "STRENGTH"))
            )
        }
    }

    private fun exerciseLogsToJson(logs: List<ExerciseLog>): JSONArray {
        val arr = JSONArray()
        for (l in logs) {
            arr.put(JSONObject().apply {
                put("id", l.id)
                put("workoutLogId", l.workoutLogId)
                put("exerciseId", l.exerciseId)
                put("orderIndex", l.orderIndex)
                put("supersetGroup", l.supersetGroup ?: JSONObject.NULL)
            })
        }
        return arr
    }

    private fun jsonToExerciseLogs(arr: JSONArray): List<ExerciseLog> {
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            ExerciseLog(
                id = o.getLong("id"),
                workoutLogId = o.getLong("workoutLogId"),
                exerciseId = o.getLong("exerciseId"),
                orderIndex = o.getInt("orderIndex"),
                supersetGroup = o.optNullableInt("supersetGroup")
            )
        }
    }

    private fun setLogsToJson(logs: List<SetLog>): JSONArray {
        val arr = JSONArray()
        for (s in logs) {
            arr.put(JSONObject().apply {
                put("id", s.id)
                put("exerciseLogId", s.exerciseLogId)
                put("setNumber", s.setNumber)
                put("reps", s.reps ?: JSONObject.NULL)
                put("weightLbs", s.weightLbs ?: JSONObject.NULL)
                put("durationSeconds", s.durationSeconds ?: JSONObject.NULL)
                put("distanceMiles", s.distanceMiles ?: JSONObject.NULL)
                put("isWarmup", s.isWarmup)
                put("notes", s.notes ?: JSONObject.NULL)
            })
        }
        return arr
    }

    private fun jsonToSetLogs(arr: JSONArray): List<SetLog> {
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            SetLog(
                id = o.getLong("id"),
                exerciseLogId = o.getLong("exerciseLogId"),
                setNumber = o.getInt("setNumber"),
                reps = o.optNullableInt("reps"),
                weightLbs = o.optNullableDouble("weightLbs"),
                durationSeconds = o.optNullableInt("durationSeconds"),
                distanceMiles = o.optNullableDouble("distanceMiles"),
                isWarmup = o.optBoolean("isWarmup", false),
                notes = o.optNullableString("notes")
            )
        }
    }

    private fun scheduledWorkoutsToJson(workouts: List<ScheduledWorkout>): JSONArray {
        val arr = JSONArray()
        for (sw in workouts) {
            arr.put(JSONObject().apply {
                put("id", sw.id)
                put("templateId", sw.templateId ?: JSONObject.NULL)
                put("scheduledDate", sw.scheduledDate)
                put("isCompleted", sw.isCompleted)
                put("completedWorkoutLogId", sw.completedWorkoutLogId ?: JSONObject.NULL)
                put("label", sw.label ?: JSONObject.NULL)
                put("isSkipped", sw.isSkipped)
            })
        }
        return arr
    }

    private fun jsonToScheduledWorkouts(arr: JSONArray): List<ScheduledWorkout> {
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            ScheduledWorkout(
                id = o.getLong("id"),
                templateId = o.optNullableLong("templateId"),
                scheduledDate = o.getLong("scheduledDate"),
                isCompleted = o.optBoolean("isCompleted", false),
                completedWorkoutLogId = o.optNullableLong("completedWorkoutLogId"),
                label = o.optNullableString("label"),
                isSkipped = o.optBoolean("isSkipped", false)
            )
        }
    }

    private fun savedRoutinesToJson(routines: List<SavedRoutine>): JSONArray {
        val arr = JSONArray()
        for (r in routines) {
            arr.put(JSONObject().apply {
                put("id", r.id)
                put("name", r.name)
                put("rawText", r.rawText)
                put("dayAssignmentsJson", r.dayAssignmentsJson)
                put("weekCount", r.weekCount)
                put("routineNamesJson", r.routineNamesJson)
                put("notes", r.notes ?: JSONObject.NULL)
                put("createdAt", r.createdAt)
            })
        }
        return arr
    }

    private fun jsonToSavedRoutines(arr: JSONArray): List<SavedRoutine> {
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            SavedRoutine(
                id = o.getLong("id"),
                name = o.getString("name"),
                rawText = o.getString("rawText"),
                dayAssignmentsJson = o.getString("dayAssignmentsJson"),
                weekCount = o.getInt("weekCount"),
                routineNamesJson = o.getString("routineNamesJson"),
                notes = o.optNullableString("notes"),
                createdAt = o.optLong("createdAt", System.currentTimeMillis())
            )
        }
    }

    private fun routineUsageHistoryToJson(history: List<RoutineUsageHistory>): JSONArray {
        val arr = JSONArray()
        for (h in history) {
            arr.put(JSONObject().apply {
                put("id", h.id)
                put("savedRoutineId", h.savedRoutineId)
                put("startDate", h.startDate)
                put("endDate", h.endDate ?: JSONObject.NULL)
                put("notes", h.notes ?: JSONObject.NULL)
            })
        }
        return arr
    }

    private fun jsonToRoutineUsageHistory(arr: JSONArray): List<RoutineUsageHistory> {
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            RoutineUsageHistory(
                id = o.getLong("id"),
                savedRoutineId = o.getLong("savedRoutineId"),
                startDate = o.getLong("startDate"),
                endDate = o.optNullableLong("endDate"),
                notes = o.optNullableString("notes")
            )
        }
    }

    // JSONObject extension helpers for nullable types
    private fun JSONObject.optNullableString(key: String): String? =
        if (has(key) && !isNull(key)) getString(key) else null

    private fun JSONObject.optNullableInt(key: String): Int? =
        if (has(key) && !isNull(key)) getInt(key) else null

    private fun JSONObject.optNullableLong(key: String): Long? =
        if (has(key) && !isNull(key)) getLong(key) else null

    private fun JSONObject.optNullableDouble(key: String): Double? =
        if (has(key) && !isNull(key)) getDouble(key) else null

    // Pushup log serialization
    private fun pushupLogsToJson(logs: List<PushupLog>): JSONArray {
        val arr = JSONArray()
        for (l in logs) {
            arr.put(JSONObject().apply {
                put("id", l.id)
                put("timestamp", l.timestamp)
                put("count", l.count)
                put("durationSeconds", l.durationSeconds ?: JSONObject.NULL)
            })
        }
        return arr
    }

    private fun jsonToPushupLogs(arr: JSONArray): List<PushupLog> {
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            PushupLog(
                id = o.getLong("id"),
                timestamp = o.getLong("timestamp"),
                count = o.getInt("count"),
                durationSeconds = o.optNullableInt("durationSeconds")
            )
        }
    }

    // Feature usage log serialization
    private fun featureUsageLogsToJson(logs: List<FeatureUsageLog>): JSONArray {
        val arr = JSONArray()
        for (l in logs) {
            arr.put(JSONObject().apply {
                put("id", l.id)
                put("featureName", l.featureName)
                put("timestamp", l.timestamp)
            })
        }
        return arr
    }

    private fun jsonToFeatureUsageLogs(arr: JSONArray): List<FeatureUsageLog> {
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            FeatureUsageLog(
                id = o.getLong("id"),
                featureName = o.getString("featureName"),
                timestamp = o.getLong("timestamp")
            )
        }
    }

    data class ImportSummary(
        val exercisesImported: Int,
        val templatesImported: Int,
        val workoutLogsImported: Int,
        val setLogsImported: Int,
        val pushupLogsImported: Int = 0,
        val featureUsageLogsImported: Int = 0
    )
}
