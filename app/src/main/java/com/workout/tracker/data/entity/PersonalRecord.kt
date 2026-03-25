package com.workout.tracker.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "personal_records",
    foreignKeys = [
        ForeignKey(entity = Exercise::class, parentColumns = ["id"], childColumns = ["exerciseId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("exerciseId")]
)
data class PersonalRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseId: Long,
    val type: PRType,
    val value: Double,
    val reps: Int? = null,
    val weightLbs: Double? = null,
    val achievedAt: Long = System.currentTimeMillis(),
    val workoutLogId: Long? = null
)

enum class PRType {
    MAX_WEIGHT,      // Heaviest weight lifted (any reps)
    MAX_REPS,        // Most reps at any weight
    MAX_VOLUME,      // Highest single-set volume (weight x reps)
    MAX_ESTIMATED_1RM // Highest estimated 1RM
}
