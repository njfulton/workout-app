package com.workout.tracker.data.dao

import androidx.room.*
import com.workout.tracker.data.entity.FeatureUsageLog
import kotlinx.coroutines.flow.Flow

data class FeatureUsageCount(
    val featureName: String,
    val useCount: Int,
    val lastUsed: Long
)

@Dao
interface FeatureUsageDao {
    @Insert
    suspend fun insert(log: FeatureUsageLog)

    @Query("""
        SELECT featureName, COUNT(*) as useCount, MAX(timestamp) as lastUsed
        FROM feature_usage_logs
        GROUP BY featureName
        ORDER BY useCount DESC
    """)
    fun getUsageCounts(): Flow<List<FeatureUsageCount>>

    @Query("DELETE FROM feature_usage_logs")
    suspend fun clearAll()
}
