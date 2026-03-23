package com.workout.tracker.data

import androidx.room.TypeConverter
import com.workout.tracker.data.entity.ExerciseCategory
import com.workout.tracker.data.entity.MuscleGroup
import com.workout.tracker.data.entity.WorkoutType

class Converters {
    @TypeConverter
    fun fromExerciseCategory(value: ExerciseCategory): String = value.name
    @TypeConverter
    fun toExerciseCategory(value: String): ExerciseCategory = ExerciseCategory.valueOf(value)

    @TypeConverter
    fun fromMuscleGroup(value: MuscleGroup): String = value.name
    @TypeConverter
    fun toMuscleGroup(value: String): MuscleGroup = MuscleGroup.valueOf(value)

    @TypeConverter
    fun fromWorkoutType(value: WorkoutType): String = value.name
    @TypeConverter
    fun toWorkoutType(value: String): WorkoutType = WorkoutType.valueOf(value)
}
