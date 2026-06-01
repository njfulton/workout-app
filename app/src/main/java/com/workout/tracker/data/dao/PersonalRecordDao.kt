package com.workout.tracker.data.dao

import androidx.room.*
import com.workout.tracker.data.entity.PRType
import com.workout.tracker.data.entity.PersonalRecord
import kotlinx.coroutines.flow.Flow

data class PersonalRecordWithExercise(
    val id: Long,
    val exerciseId: Long,
    val exerciseName: String,
    val type: PRType,
    val value: Double,
    val reps: Int?,
    val weightLbs: Double?,
    val achievedAt: Long
)

@Dao
interface PersonalRecordDao {

    @Query("SELECT * FROM personal_records WHERE exerciseId = :exerciseId ORDER BY achievedAt DESC")
    fun getRecordsForExercise(exerciseId: Long): Flow<List<PersonalRecord>>

    @Query("SELECT * FROM personal_records WHERE exerciseId = :exerciseId AND type = :type ORDER BY value DESC LIMIT 1")
    suspend fun getBestRecord(exerciseId: Long, type: PRType): PersonalRecord?

    @Query("SELECT COUNT(*) FROM personal_records WHERE exerciseId = :exerciseId")
    suspend fun countForExercise(exerciseId: Long): Int

    @Query("""
        SELECT pr.id, pr.exerciseId, e.name as exerciseName, pr.type, pr.value, pr.reps, pr.weightLbs, pr.achievedAt
        FROM personal_records pr
        INNER JOIN exercises e ON pr.exerciseId = e.id
        ORDER BY pr.achievedAt DESC
        LIMIT :limit
    """)
    fun getRecentRecords(limit: Int = 50): Flow<List<PersonalRecordWithExercise>>

    @Query("""
        SELECT pr.id, pr.exerciseId, e.name as exerciseName, pr.type, pr.value, pr.reps, pr.weightLbs, pr.achievedAt
        FROM personal_records pr
        INNER JOIN exercises e ON pr.exerciseId = e.id
        WHERE pr.workoutLogId = :workoutLogId
        ORDER BY pr.type ASC
    """)
    suspend fun getRecordsForWorkout(workoutLogId: Long): List<PersonalRecordWithExercise>

    @Insert
    suspend fun insert(record: PersonalRecord): Long

    @Delete
    suspend fun delete(record: PersonalRecord)

    @Query("DELETE FROM personal_records WHERE exerciseId = :exerciseId")
    suspend fun deleteAllForExercise(exerciseId: Long)
}
