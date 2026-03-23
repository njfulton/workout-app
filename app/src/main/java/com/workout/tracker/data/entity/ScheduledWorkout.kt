package com.workout.tracker.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "scheduled_workouts",
    foreignKeys = [
        ForeignKey(entity = WorkoutTemplate::class, parentColumns = ["id"], childColumns = ["templateId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("templateId")]
)
data class ScheduledWorkout(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val templateId: Long,
    val scheduledDate: Long,
    val isCompleted: Boolean = false,
    val completedWorkoutLogId: Long? = null
)
