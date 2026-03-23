package com.workout.tracker.data.dao

import androidx.room.*
import com.workout.tracker.data.entity.ExerciseLog
import com.workout.tracker.data.entity.SetLog
import com.workout.tracker.data.entity.WorkoutLog
import com.workout.tracker.data.entity.WorkoutType
import kotlinx.coroutines.flow.Flow

data class ExportRow(
    val workoutId: Long,
    val workoutName: String,
    val workoutType: WorkoutType,
    val startTime: Long,
    val endTime: Long?,
    val exerciseName: String,
    val orderIndex: Int,
    val setNumber: Int,
    val reps: Int?,
    val weightLbs: Double?,
    val durationSeconds: Int?,
    val distanceMiles: Double?,
    val isWarmup: Boolean
)

data class ExerciseHistoryEntry(
    val startTime: Long,
    val setNumber: Int,
    val reps: Int?,
    val weightLbs: Double?,
    val isWarmup: Boolean
)

data class WorkoutLogSummary(
    val id: Long,
    val name: String,
    val workoutType: WorkoutType,
    val startTime: Long,
    val endTime: Long?,
    val exerciseCount: Int
)

data class ExerciseProgressEntry(
    val workoutDate: Long,
    val maxWeight: Double,
    val totalVolume: Double,
    val totalSets: Int
)

data class MuscleGroupVolume(
    val muscleGroup: String,
    val totalSets: Int,
    val totalVolume: Double
)

@Dao
interface WorkoutLogDao {
    @Query("""
        SELECT w.id, w.name, w.workoutType, w.startTime, w.endTime,
               COUNT(DISTINCT el.id) as exerciseCount
        FROM workout_logs w
        LEFT JOIN exercise_logs el ON w.id = el.workoutLogId
        GROUP BY w.id
        ORDER BY w.startTime DESC
    """)
    fun getAllWorkoutSummaries(): Flow<List<WorkoutLogSummary>>

    @Query("""
        SELECT w.id, w.name, w.workoutType, w.startTime, w.endTime,
               COUNT(DISTINCT el.id) as exerciseCount
        FROM workout_logs w
        LEFT JOIN exercise_logs el ON w.id = el.workoutLogId
        WHERE w.startTime >= :startDate AND w.startTime <= :endDate
        GROUP BY w.id
        ORDER BY w.startTime DESC
    """)
    fun getWorkoutsBetween(startDate: Long, endDate: Long): Flow<List<WorkoutLogSummary>>

    @Query("SELECT * FROM workout_logs WHERE id = :id")
    suspend fun getWorkoutLogById(id: Long): WorkoutLog?

    @Query("SELECT * FROM exercise_logs WHERE workoutLogId = :workoutLogId ORDER BY orderIndex ASC")
    suspend fun getExerciseLogs(workoutLogId: Long): List<ExerciseLog>

    @Query("SELECT * FROM set_logs WHERE exerciseLogId = :exerciseLogId ORDER BY setNumber ASC")
    suspend fun getSetLogs(exerciseLogId: Long): List<SetLog>

    @Query("""
        SELECT sl.* FROM set_logs sl
        INNER JOIN exercise_logs el ON sl.exerciseLogId = el.id
        WHERE el.exerciseId = :exerciseId
        ORDER BY sl.id DESC
        LIMIT :limit
    """)
    suspend fun getRecentSetsForExercise(exerciseId: Long, limit: Int = 30): List<SetLog>

    @Query("""
        SELECT MAX(sl.weightLbs) FROM set_logs sl
        INNER JOIN exercise_logs el ON sl.exerciseLogId = el.id
        WHERE el.exerciseId = :exerciseId AND sl.isWarmup = 0
    """)
    suspend fun getMaxWeightForExercise(exerciseId: Long): Double?

    @Query("""
        SELECT w.startTime, sl.setNumber, sl.reps, sl.weightLbs, sl.isWarmup
        FROM set_logs sl
        INNER JOIN exercise_logs el ON sl.exerciseLogId = el.id
        INNER JOIN workout_logs w ON el.workoutLogId = w.id
        WHERE el.exerciseId = :exerciseId AND sl.isWarmup = 0
        ORDER BY w.startTime DESC, sl.setNumber ASC
        LIMIT :limit
    """)
    suspend fun getExerciseHistory(exerciseId: Long, limit: Int = 50): List<ExerciseHistoryEntry>

    @Query("""
        SELECT w.id as workoutId, w.name as workoutName, w.workoutType, w.startTime, w.endTime,
               e.name as exerciseName, el.orderIndex,
               sl.setNumber, sl.reps, sl.weightLbs, sl.durationSeconds, sl.distanceMiles, sl.isWarmup
        FROM workout_logs w
        INNER JOIN exercise_logs el ON w.id = el.workoutLogId
        INNER JOIN exercises e ON el.exerciseId = e.id
        INNER JOIN set_logs sl ON el.id = sl.exerciseLogId
        ORDER BY w.startTime DESC, el.orderIndex ASC, sl.setNumber ASC
    """)
    suspend fun getAllDataForExport(): List<ExportRow>

    @Query("""
        SELECT w.startTime as workoutDate,
               MAX(sl.weightLbs) as maxWeight,
               SUM(sl.weightLbs * sl.reps) as totalVolume,
               COUNT(sl.id) as totalSets
        FROM set_logs sl
        INNER JOIN exercise_logs el ON sl.exerciseLogId = el.id
        INNER JOIN workout_logs w ON el.workoutLogId = w.id
        WHERE el.exerciseId = :exerciseId AND sl.isWarmup = 0 AND sl.weightLbs IS NOT NULL
        GROUP BY w.id
        ORDER BY w.startTime ASC
    """)
    suspend fun getExerciseProgressData(exerciseId: Long): List<ExerciseProgressEntry>

    @Query("""
        SELECT e.muscleGroup as muscleGroup,
               COUNT(sl.id) as totalSets,
               COALESCE(SUM(sl.weightLbs * sl.reps), 0.0) as totalVolume
        FROM set_logs sl
        INNER JOIN exercise_logs el ON sl.exerciseLogId = el.id
        INNER JOIN workout_logs w ON el.workoutLogId = w.id
        INNER JOIN exercises e ON el.exerciseId = e.id
        WHERE w.startTime >= :startDate AND w.startTime <= :endDate AND sl.isWarmup = 0
        GROUP BY e.muscleGroup
        ORDER BY totalSets DESC
    """)
    suspend fun getMuscleGroupVolume(startDate: Long, endDate: Long): List<MuscleGroupVolume>

    @Insert
    suspend fun insertWorkoutLog(workoutLog: WorkoutLog): Long

    @Insert
    suspend fun insertExerciseLog(exerciseLog: ExerciseLog): Long

    @Insert
    suspend fun insertSetLog(setLog: SetLog): Long

    @Insert
    suspend fun insertSetLogs(setLogs: List<SetLog>)

    @Update
    suspend fun updateWorkoutLog(workoutLog: WorkoutLog)

    @Update
    suspend fun updateSetLog(setLog: SetLog)

    @Delete
    suspend fun deleteWorkoutLog(workoutLog: WorkoutLog)

    @Delete
    suspend fun deleteExerciseLog(exerciseLog: ExerciseLog)

    @Delete
    suspend fun deleteSetLog(setLog: SetLog)

    @Query("SELECT * FROM workout_logs ORDER BY startTime DESC")
    suspend fun getAllWorkoutLogsList(): List<WorkoutLog>
}
