package com.workout.tracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: ExerciseCategory,
    val muscleGroup: MuscleGroup,
    val equipment: String? = null,
    val notes: String? = null,
    val isCustom: Boolean = false
)

enum class ExerciseCategory {
    STRENGTH, CARDIO, FLEXIBILITY, BODYWEIGHT, OTHER
}

enum class MuscleGroup {
    CHEST, BACK, SHOULDERS, BICEPS, TRICEPS, FOREARMS,
    QUADRICEPS, HAMSTRINGS, GLUTES, CALVES, ABS, FULL_BODY, CARDIO, OTHER
}
