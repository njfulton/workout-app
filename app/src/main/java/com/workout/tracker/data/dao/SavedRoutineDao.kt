package com.workout.tracker.data.dao

import androidx.room.*
import com.workout.tracker.data.entity.RoutineUsageHistory
import com.workout.tracker.data.entity.SavedRoutine
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedRoutineDao {
    @Query("SELECT * FROM saved_routines ORDER BY createdAt DESC")
    fun getAllSavedRoutines(): Flow<List<SavedRoutine>>

    @Query("SELECT * FROM saved_routines WHERE id = :id")
    suspend fun getSavedRoutineById(id: Long): SavedRoutine?

    @Query("SELECT * FROM saved_routines ORDER BY createdAt DESC")
    suspend fun getAllSavedRoutinesList(): List<SavedRoutine>

    @Query("SELECT * FROM routine_usage_history WHERE savedRoutineId = :routineId ORDER BY startDate DESC")
    suspend fun getUsageHistoryList(routineId: Long): List<RoutineUsageHistory>

    @Insert
    suspend fun insert(routine: SavedRoutine): Long

    @Update
    suspend fun update(routine: SavedRoutine)

    @Delete
    suspend fun delete(routine: SavedRoutine)

    // Usage history
    @Query("SELECT * FROM routine_usage_history WHERE savedRoutineId = :routineId ORDER BY startDate DESC")
    fun getUsageHistory(routineId: Long): Flow<List<RoutineUsageHistory>>

    @Insert
    suspend fun insertUsageHistory(history: RoutineUsageHistory): Long

    @Update
    suspend fun updateUsageHistory(history: RoutineUsageHistory)

    @Delete
    suspend fun deleteUsageHistory(history: RoutineUsageHistory)
}
