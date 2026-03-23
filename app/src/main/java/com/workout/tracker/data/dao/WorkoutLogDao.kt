package com.workout.tracker.data.dao

import androidx.room.*
import com.workout.tracker.data.entity.ExerciseLog
import com.workout.tracker.data.entity.SetLog
import com.workout.tracker.data.entity.WorkoutLog
import com.workout.tracker.data.entity.WorkoutType
import kotlinx.coroutines.flow.Flow

data class WorkoutLogSummary(
    val id: Long,
    val name: String,
    val workoutType: WorkoutType,
    val startTime: Long,
    val endTime: Long?,
    val exerciseCount: Int
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
}
