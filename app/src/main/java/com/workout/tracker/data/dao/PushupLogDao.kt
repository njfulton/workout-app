package com.workout.tracker.data.dao

import androidx.room.*
import com.workout.tracker.data.entity.PushupLog
import kotlinx.coroutines.flow.Flow

@Dao
interface PushupLogDao {
    @Query("SELECT * FROM pushup_logs ORDER BY timestamp DESC")
    fun getAllPushupLogs(): Flow<List<PushupLog>>

    @Query("SELECT * FROM pushup_logs ORDER BY timestamp DESC")
    suspend fun getAllPushupLogsList(): List<PushupLog>

    @Query("SELECT * FROM pushup_logs WHERE timestamp >= :startDate AND timestamp <= :endDate ORDER BY timestamp DESC")
    fun getPushupLogsBetween(startDate: Long, endDate: Long): Flow<List<PushupLog>>

    @Query("SELECT * FROM pushup_logs WHERE timestamp >= :startDate AND timestamp <= :endDate ORDER BY timestamp DESC")
    suspend fun getPushupLogsBetweenList(startDate: Long, endDate: Long): List<PushupLog>

    @Query("SELECT COALESCE(SUM(count), 0) FROM pushup_logs WHERE timestamp >= :startDate AND timestamp <= :endDate")
    suspend fun getTotalPushupsBetween(startDate: Long, endDate: Long): Int

    @Insert
    suspend fun insert(log: PushupLog): Long

    @Delete
    suspend fun delete(log: PushupLog)
}
