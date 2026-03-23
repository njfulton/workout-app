package com.workout.tracker.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exercise_logs",
    foreignKeys = [
        ForeignKey(entity = WorkoutLog::class, parentColumns = ["id"], childColumns = ["workoutLogId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Exercise::class, parentColumns = ["id"], childColumns = ["exerciseId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("workoutLogId"), Index("exerciseId")]
)
data class ExerciseLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutLogId: Long,
    val exerciseId: Long,
    val orderIndex: Int,
    val supersetGroup: Int? = null
)
