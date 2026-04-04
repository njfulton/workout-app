package com.workout.tracker.data.repository

import com.workout.tracker.data.dao.*
import com.workout.tracker.data.entity.*
import com.workout.tracker.util.OneRepMaxCalculator
import kotlinx.coroutines.flow.Flow


class WorkoutRepository(
    private val exerciseDao: ExerciseDao,
    private val templateDao: WorkoutTemplateDao,
    private val workoutLogDao: WorkoutLogDao,
    private val scheduleDao: ScheduleDao,
    private val savedRoutineDao: SavedRoutineDao? = null,
    private val pushupLogDao: PushupLogDao? = null,
    private val featureUsageDao: FeatureUsageDao? = null,
    private val personalRecordDao: PersonalRecordDao? = null
) {
    // Exercises
    val allExercises: Flow<List<Exercise>> = exerciseDao.getAllExercises()

    fun searchExercises(query: String): Flow<List<Exercise>> = exerciseDao.searchExercises(query)
    fun getExercisesByCategory(category: ExerciseCategory): Flow<List<Exercise>> = exerciseDao.getExercisesByCategory(category)
    fun getExercisesByMuscleGroup(muscleGroup: MuscleGroup): Flow<List<Exercise>> = exerciseDao.getExercisesByMuscleGroup(muscleGroup)
    suspend fun getExerciseById(id: Long): Exercise? = exerciseDao.getExerciseById(id)
    suspend fun insertExercise(exercise: Exercise): Long = exerciseDao.insert(exercise)
    suspend fun insertExercises(exercises: List<Exercise>) = exerciseDao.insertAll(exercises)
    suspend fun updateExercise(exercise: Exercise) = exerciseDao.update(exercise)
    suspend fun deleteExercise(exercise: Exercise) = exerciseDao.delete(exercise)
    suspend fun getExerciseCount(): Int = exerciseDao.getCount()
    suspend fun getExerciseByName(name: String): Exercise? = exerciseDao.getExerciseByName(name)

    // Templates
    val allTemplates: Flow<List<TemplateWithExerciseCount>> = templateDao.getAllTemplatesWithCount()

    suspend fun getTemplateById(id: Long): WorkoutTemplate? = templateDao.getTemplateById(id)
    suspend fun getTemplateExercises(templateId: Long): List<TemplateExercise> = templateDao.getTemplateExercises(templateId)
    suspend fun insertTemplate(template: WorkoutTemplate): Long = templateDao.insertTemplate(template)
    suspend fun insertTemplateExercise(te: TemplateExercise): Long = templateDao.insertTemplateExercise(te)
    suspend fun insertTemplateExercises(exercises: List<TemplateExercise>) = templateDao.insertTemplateExercises(exercises)
    suspend fun updateTemplate(template: WorkoutTemplate) = templateDao.updateTemplate(template)
    suspend fun deleteTemplate(template: WorkoutTemplate) = templateDao.deleteTemplate(template)
    suspend fun deleteTemplateExercises(templateId: Long) = templateDao.deleteTemplateExercises(templateId)

    // Workout Logs
    val allWorkoutSummaries: Flow<List<WorkoutLogSummary>> = workoutLogDao.getAllWorkoutSummaries()

    fun getWorkoutsBetween(start: Long, end: Long): Flow<List<WorkoutLogSummary>> = workoutLogDao.getWorkoutsBetween(start, end)
    suspend fun getWorkoutLogById(id: Long): WorkoutLog? = workoutLogDao.getWorkoutLogById(id)
    suspend fun getExerciseLogs(workoutLogId: Long): List<ExerciseLog> = workoutLogDao.getExerciseLogs(workoutLogId)
    suspend fun getSetLogs(exerciseLogId: Long): List<SetLog> = workoutLogDao.getSetLogs(exerciseLogId)
    suspend fun getRecentSetsForExercise(exerciseId: Long, limit: Int = 30): List<SetLog> = workoutLogDao.getRecentSetsForExercise(exerciseId, limit)
    suspend fun getMaxWeightForExercise(exerciseId: Long): Double? = workoutLogDao.getMaxWeightForExercise(exerciseId)

    suspend fun insertWorkoutLog(log: WorkoutLog): Long = workoutLogDao.insertWorkoutLog(log)
    suspend fun insertExerciseLog(log: ExerciseLog): Long = workoutLogDao.insertExerciseLog(log)
    suspend fun insertSetLog(log: SetLog): Long = workoutLogDao.insertSetLog(log)
    suspend fun updateWorkoutLog(log: WorkoutLog) = workoutLogDao.updateWorkoutLog(log)
    suspend fun updateSetLog(log: SetLog) = workoutLogDao.updateSetLog(log)
    suspend fun deleteWorkoutLog(log: WorkoutLog) = workoutLogDao.deleteWorkoutLog(log)
    suspend fun insertSetLogs(logs: List<SetLog>) = workoutLogDao.insertSetLogs(logs)
    suspend fun getExerciseHistory(exerciseId: Long, limit: Int = 50): List<ExerciseHistoryEntry> = workoutLogDao.getExerciseHistory(exerciseId, limit)
    suspend fun getLatestNoteForExercise(exerciseId: Long): String? = workoutLogDao.getLatestNoteForExercise(exerciseId)
    suspend fun updateExerciseLogNote(exerciseLogId: Long, note: String?) = workoutLogDao.updateExerciseLogNote(exerciseLogId, note)
    suspend fun getAllDataForExport() = workoutLogDao.getAllDataForExport()
    suspend fun getMuscleGroupVolume(start: Long, end: Long) = workoutLogDao.getMuscleGroupVolume(start, end)
    suspend fun getExerciseProgressData(exerciseId: Long) = workoutLogDao.getExerciseProgressData(exerciseId)

    // Schedule
    fun getUpcomingSchedule(fromDate: Long): Flow<List<ScheduledWorkoutWithTemplate>> = scheduleDao.getUpcomingSchedule(fromDate)
    fun getScheduleBetween(start: Long, end: Long): Flow<List<ScheduledWorkoutWithTemplate>> = scheduleDao.getScheduleBetween(start, end)
    suspend fun getScheduleBetweenOnce(start: Long, end: Long): List<ScheduledWorkoutWithTemplate> = scheduleDao.getScheduleBetweenOnce(start, end)
    suspend fun insertScheduledWorkout(sw: ScheduledWorkout): Long = scheduleDao.insert(sw)
    suspend fun updateScheduledWorkout(sw: ScheduledWorkout) = scheduleDao.update(sw)
    suspend fun setScheduledWorkoutCompleted(id: Long, isCompleted: Boolean) = scheduleDao.setCompleted(id, isCompleted)
    suspend fun deleteScheduledWorkout(sw: ScheduledWorkout) = scheduleDao.delete(sw)

    // Saved Routines
    fun getAllSavedRoutines(): Flow<List<SavedRoutine>> = savedRoutineDao?.getAllSavedRoutines() ?: kotlinx.coroutines.flow.flowOf(emptyList())
    suspend fun getSavedRoutineById(id: Long): SavedRoutine? = savedRoutineDao?.getSavedRoutineById(id)
    suspend fun insertSavedRoutine(routine: SavedRoutine): Long = savedRoutineDao?.insert(routine) ?: 0
    suspend fun updateSavedRoutine(routine: SavedRoutine) = savedRoutineDao?.update(routine)
    suspend fun deleteSavedRoutine(routine: SavedRoutine) = savedRoutineDao?.delete(routine)
    fun getRoutineUsageHistory(routineId: Long): Flow<List<RoutineUsageHistory>> = savedRoutineDao?.getUsageHistory(routineId) ?: kotlinx.coroutines.flow.flowOf(emptyList())
    suspend fun insertRoutineUsageHistory(history: RoutineUsageHistory): Long = savedRoutineDao?.insertUsageHistory(history) ?: 0
    suspend fun updateRoutineUsageHistory(history: RoutineUsageHistory) = savedRoutineDao?.updateUsageHistory(history)
    suspend fun deleteRoutineUsageHistory(history: RoutineUsageHistory) = savedRoutineDao?.deleteUsageHistory(history)

    // Schedule - clear future
    suspend fun clearFutureSchedule() {
        val todayMillis = java.time.LocalDate.now()
            .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        // Get all upcoming non-completed workouts and delete them
        // We need to use the DAO directly since this is a bulk operation
        scheduleDao.deleteFutureIncomplete(todayMillis)
    }

    // Pushup Logs
    fun getAllPushupLogs(): Flow<List<PushupLog>> = pushupLogDao?.getAllPushupLogs() ?: kotlinx.coroutines.flow.flowOf(emptyList())
    fun getPushupLogsBetween(start: Long, end: Long): Flow<List<PushupLog>> = pushupLogDao?.getPushupLogsBetween(start, end) ?: kotlinx.coroutines.flow.flowOf(emptyList())
    suspend fun getTotalPushupsBetween(start: Long, end: Long): Int = pushupLogDao?.getTotalPushupsBetween(start, end) ?: 0
    suspend fun insertPushupLog(log: PushupLog): Long = pushupLogDao?.insert(log) ?: 0
    suspend fun deletePushupLog(log: PushupLog) = pushupLogDao?.delete(log)
    suspend fun getAllPushupLogsList(): List<PushupLog> = pushupLogDao?.getAllPushupLogsList() ?: emptyList()

    // Dashboard stats
    suspend fun getTotalCompletedWorkouts(): Int = workoutLogDao.getTotalCompletedWorkouts()
    suspend fun getCompletedWorkoutsSince(startDate: Long): Int = workoutLogDao.getCompletedWorkoutsSince(startDate)

    // Feature usage logging
    suspend fun logFeatureUsage(featureName: String) {
        featureUsageDao?.insert(FeatureUsageLog(featureName = featureName))
    }
    fun getFeatureUsageCounts(): Flow<List<FeatureUsageCount>> =
        featureUsageDao?.getUsageCounts() ?: kotlinx.coroutines.flow.flowOf(emptyList())

    // Personal Records
    fun getRecordsForExercise(exerciseId: Long) = personalRecordDao?.getRecordsForExercise(exerciseId) ?: kotlinx.coroutines.flow.flowOf(emptyList())
    fun getRecentRecords(limit: Int = 50) = personalRecordDao?.getRecentRecords(limit) ?: kotlinx.coroutines.flow.flowOf(emptyList())
    suspend fun getRecordsForWorkout(workoutLogId: Long) = personalRecordDao?.getRecordsForWorkout(workoutLogId) ?: emptyList()

    /**
     * Checks if a newly logged set is a personal record.
     * Returns list of new PRs detected.
     */
    suspend fun checkAndRecordPRs(
        exerciseId: Long,
        reps: Int?,
        weightLbs: Double?,
        workoutLogId: Long?
    ): List<PersonalRecord> {
        if (personalRecordDao == null) return emptyList()
        if (reps == null || reps <= 0) return emptyList()

        val newPRs = mutableListOf<PersonalRecord>()
        val now = System.currentTimeMillis()

        // Check MAX_WEIGHT PR
        if (weightLbs != null && weightLbs > 0) {
            val currentBest = personalRecordDao.getBestRecord(exerciseId, PRType.MAX_WEIGHT)
            if (currentBest == null || weightLbs > currentBest.value) {
                val pr = PersonalRecord(
                    exerciseId = exerciseId,
                    type = PRType.MAX_WEIGHT,
                    value = weightLbs,
                    reps = reps,
                    weightLbs = weightLbs,
                    achievedAt = now,
                    workoutLogId = workoutLogId
                )
                personalRecordDao.insert(pr)
                newPRs.add(pr)
            }
        }

        // Check MAX_VOLUME PR (single set: weight x reps)
        if (weightLbs != null && weightLbs > 0) {
            val setVolume = weightLbs * reps
            val currentBest = personalRecordDao.getBestRecord(exerciseId, PRType.MAX_VOLUME)
            if (currentBest == null || setVolume > currentBest.value) {
                val pr = PersonalRecord(
                    exerciseId = exerciseId,
                    type = PRType.MAX_VOLUME,
                    value = setVolume,
                    reps = reps,
                    weightLbs = weightLbs,
                    achievedAt = now,
                    workoutLogId = workoutLogId
                )
                personalRecordDao.insert(pr)
                newPRs.add(pr)
            }
        }

        // Check MAX_ESTIMATED_1RM PR
        if (weightLbs != null && weightLbs > 0 && reps in 1..12) {
            val estimated1RM = OneRepMaxCalculator.estimate(weightLbs, reps)
            val currentBest = personalRecordDao.getBestRecord(exerciseId, PRType.MAX_ESTIMATED_1RM)
            if (currentBest == null || estimated1RM > currentBest.value) {
                val pr = PersonalRecord(
                    exerciseId = exerciseId,
                    type = PRType.MAX_ESTIMATED_1RM,
                    value = estimated1RM,
                    reps = reps,
                    weightLbs = weightLbs,
                    achievedAt = now,
                    workoutLogId = workoutLogId
                )
                personalRecordDao.insert(pr)
                newPRs.add(pr)
            }
        }

        // Check MAX_REPS PR (bodyweight or at any weight)
        val currentBestReps = personalRecordDao.getBestRecord(exerciseId, PRType.MAX_REPS)
        if (currentBestReps == null || reps > currentBestReps.value) {
            val pr = PersonalRecord(
                exerciseId = exerciseId,
                type = PRType.MAX_REPS,
                value = reps.toDouble(),
                reps = reps,
                weightLbs = weightLbs,
                achievedAt = now,
                workoutLogId = workoutLogId
            )
            personalRecordDao.insert(pr)
            newPRs.add(pr)
        }

        return newPRs
    }

    // Progressive Overload Suggestion
    suspend fun getProgressiveOverloadSuggestion(exerciseId: Long): OverloadSuggestion? {
        val recentSets = getRecentSetsForExercise(exerciseId, 30)
            .filter { !it.isWarmup && it.weightLbs != null && it.reps != null }

        if (recentSets.size < 6) return null

        val lastWorkoutSets = recentSets.take(6)
        val allHitTarget = lastWorkoutSets.all { (it.reps ?: 0) >= 10 }
        val currentWeight = lastWorkoutSets.maxOfOrNull { it.weightLbs ?: 0.0 } ?: return null

        return if (allHitTarget) {
            val increment = if (currentWeight >= 100) 10.0 else 5.0
            OverloadSuggestion(
                exerciseId = exerciseId,
                currentWeight = currentWeight,
                suggestedWeight = currentWeight + increment,
                reason = "You've hit all your reps at ${currentWeight}lbs. Time to move up!"
            )
        } else {
            OverloadSuggestion(
                exerciseId = exerciseId,
                currentWeight = currentWeight,
                suggestedWeight = currentWeight,
                reason = "Keep working at ${currentWeight}lbs until you hit all target reps."
            )
        }
    }
}

data class OverloadSuggestion(
    val exerciseId: Long,
    val currentWeight: Double,
    val suggestedWeight: Double,
    val reason: String
)
