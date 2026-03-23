package com.workout.tracker.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "set_logs",
    foreignKeys = [
        ForeignKey(entity = ExerciseLog::class, parentColumns = ["id"], childColumns = ["exerciseLogId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("exerciseLogId")]
)
data class SetLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseLogId: Long,
    val setNumber: Int,
    val reps: Int? = null,
    val weightLbs: Double? = null,
    val durationSeconds: Int? = null,
    val distanceMiles: Double? = null,
    val isWarmup: Boolean = false,
    val notes: String? = null
)
