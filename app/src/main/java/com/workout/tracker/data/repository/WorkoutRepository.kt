package com.workout.tracker.data.repository

import com.workout.tracker.data.dao.*
import com.workout.tracker.data.entity.*
import kotlinx.coroutines.flow.Flow

class WorkoutRepository(
    private val exerciseDao: ExerciseDao,
    private val templateDao: WorkoutTemplateDao,
    private val workoutLogDao: WorkoutLogDao,
    private val scheduleDao: ScheduleDao
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

    // Schedule
    fun getUpcomingSchedule(fromDate: Long): Flow<List<ScheduledWorkoutWithTemplate>> = scheduleDao.getUpcomingSchedule(fromDate)
    fun getScheduleBetween(start: Long, end: Long): Flow<List<ScheduledWorkoutWithTemplate>> = scheduleDao.getScheduleBetween(start, end)
    suspend fun insertScheduledWorkout(sw: ScheduledWorkout): Long = scheduleDao.insert(sw)
    suspend fun updateScheduledWorkout(sw: ScheduledWorkout) = scheduleDao.update(sw)
    suspend fun deleteScheduledWorkout(sw: ScheduledWorkout) = scheduleDao.delete(sw)

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
