package com.workout.tracker.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_logs",
    foreignKeys = [
        ForeignKey(entity = WorkoutTemplate::class, parentColumns = ["id"], childColumns = ["templateId"], onDelete = ForeignKey.SET_NULL)
    ],
    indices = [Index("templateId")]
)
data class WorkoutLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val templateId: Long? = null,
    val name: String,
    val startTime: Long,
    val endTime: Long? = null,
    val notes: String? = null,
    val workoutType: WorkoutType = WorkoutType.STRENGTH
)

enum class WorkoutType {
    STRENGTH, CARDIO, PELOTON, BODYWEIGHT_QUICK, OTHER
}
