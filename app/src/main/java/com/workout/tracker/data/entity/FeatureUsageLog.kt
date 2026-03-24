package com.workout.tracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "feature_usage_logs")
data class FeatureUsageLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val featureName: String,
    val timestamp: Long = System.currentTimeMillis()
)
