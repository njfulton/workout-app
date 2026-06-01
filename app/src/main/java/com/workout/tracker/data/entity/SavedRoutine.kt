package com.workout.tracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_routines")
data class SavedRoutine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val rawText: String,
    val dayAssignmentsJson: String, // JSON: {"0":["MONDAY","THURSDAY"],"1":["TUESDAY","FRIDAY"]}
    val weekCount: Int,
    val routineNamesJson: String, // JSON: ["Push Day","Pull Day","Leg Day"]
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "routine_usage_history")
data class RoutineUsageHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val savedRoutineId: Long,
    val startDate: Long,
    val endDate: Long? = null,
    val notes: String? = null
)
