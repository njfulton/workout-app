package com.workout.tracker.data

import com.workout.tracker.data.entity.Exercise
import com.workout.tracker.data.entity.ExerciseCategory
import com.workout.tracker.data.entity.MuscleGroup

object ExerciseSeedData {

    fun getDefaultExercises(): List<Exercise> = listOf(
        // Chest
        Exercise(name = "Bench Press", category = ExerciseCategory.STRENGTH, muscleGroup = MuscleGroup.CHEST, equipment = "Barbell"),
        Exercise(name = "Incline Bench Press", category = ExerciseCategory.STRENGTH, muscleGroup = MuscleGroup.CHEST, equipment = "Barbell"),
        Exercise(name = "Decline Bench Press", category = ExerciseCategory.STRENGTH, muscleGroup = MuscleGroup.CHEST, equipment = "Barbell"),
        Exercise(name = "Dumbbell Bench Press", category = ExerciseCategory.STRENGTH, muscleGroup = MuscleGroup.CHEST, equipment = "Dumbbells"),
        Exercise(name = "Incline Dumbbell Press", category = ExerciseCategory.STRENGTH, muscleGroup = MuscleGroup.CHEST, equipment = "Dumbbells"),
        Exercise(name = "Dumbbell Fly", category = ExerciseCategory.STRENGTH, muscleGroup = MuscleGroup.CHEST, equipment = "Dumbbells"),
        Exercise(name = "Cable Fly", category = ExerciseCategory.STRENGTH, muscleGroup = MuscleGroup.CHEST, equipment = "Cable"),
        Exercise(name = "Chest Dip", category = ExerciseCategory.BODYWEIGHT, muscleGroup = MuscleGroup.CHEST, equipment = "Dip Station"),
        Exercise(name = "Push-Up", category = ExerciseCategory.BODYWEIGHT, muscleGroup = MuscleGroup.CHEST),
        Exercise(name = "Pec Deck", category = ExerciseCategory.STRENGTH, muscleGroup = MuscleGroup.CHEST, equipment = "Machine"),

        // Back
        Exercise(name = "Deadlift", category = ExerciseCategory.STRENGTH, muscleGroup = MuscleGroup.BACK, equipment = "Barbell"),
        Exercise(name = "Barbell Row", category = ExerciseCategory.STRENGTH, muscleGroup = MuscleGroup.BACK, equipment = "Barbell"),
        Exercise(name = "Dumbbell Row", category = ExerciseCategory.STRENGTH, muscleGroup = MuscleGroup.BACK, equipment = "Dumbbell"),
        Exercise(name = "Pull-Up", category = ExerciseCategory.BODYWEIGHT, muscleGroup = MuscleGroup.BACK, equipment = "Pull-Up Bar"),
        Exercise(name = "Chin-Up", category = ExerciseCategory.BODYWEIGHT, muscleGroup = MuscleGroup.BACK, equipment = "Pull-Up Bar"),
        Exercise(name = "Lat Pulldown", category = ExerciseCategory.STRENGTH, muscleGroup = MuscleGroup.BACK, equipment = "Cable"),
        Exercise(name = "Seated Cable Row", category = ExerciseCategory.STRENGTH, muscleGroup = MuscleGroup.BACK, equipment = "Cable"),
        Exercise(name = "T-Bar Row", category = ExerciseCategory.STRENGTH, muscleGroup = MuscleGroup.BACK, equipment = "Barbell"),
        Exercise(name = "Face Pull", category = ExerciseCategory.STRENGTH, muscleGroup = MuscleGroup.BACK, equipment = "Cable"),

        // Shoulders
        Exercise(name = "Overhead Press", category = ExerciseCategory.STRENGTH, muscleGroup = MuscleGroup.SHOULDERS, equipment = "Barbell"),
        Exercise(name = "Dumbbell Shoulder Press", category = ExerciseCategory.STRENGTH, muscleGroup = MuscleGroup.SHOULDERS, equipment = "Dumbbells"),
        Exercise(name = "Lateral Raise", category = ExerciseCategory.STRENGTH, muscleGroup = MuscleGroup.SHOULDERS, equipment = "Dumbbells"),
        Exercise(name = "Front Raise", category = ExerciseCategory.STRENGTH, muscleGroup = MuscleGroup.SHOULDERS, equipment = "Dumbbells"),
        Exercise(name = "Rear Delt Fly", category = ExerciseCategory.STRENGTH, muscleGroup = MuscleGroup.SHOULDERS, equipment = "Dumbbells"),
        Exercise(name = "Arnold Press", category = ExerciseCategory.STRENGTH, muscleGroup = MuscleGroup.SHOULDERS, equipment = "Dumbbells"),
        Exercise(name = "Upright Row", category = ExerciseCategory.STRENGTH, muscleGroup = MuscleGroup.SHOULDERS, equipment = "Barbell"),

        // Biceps
        Exercise(name = "Barbell Curl", category = ExerciseCategory.STRENGTH, muscleGroup = MuscleGroup.BICEPS, equipment = "Barbell"),
        Exercise(name = "Dumbbell Curl", category = ExerciseCategory.STRENGTH, muscleGroup = MuscleGroup.BICEPS, equipment = "Dumbbells"),
        Exercise(name = "Hammer Curl", category = ExerciseCategory.STRENGTH, muscleGroup = MuscleGroup.BICEPS, equipment = "Dumbbells"),
        Exercise(name = "Preacher Curl", category = ExerciseCategory.STRENGTH, muscleGroup = MuscleGroup.BICEPS, equipment = "Barbell"),
        Exercise(name = "Incline Dumbbell Curl", category = ExerciseCategory.STRENGTH, muscleGroup = MuscleGroup.BICEPS, equipment = "Dumbbells"),
        Exercise(name = "Cable Curl", category = ExerciseCategory.STRENGTH, muscleGroup = MuscleGroup.BICEPS, equipment = "Cable"),
        Exercise(name = "Concentration Curl", category = ExerciseCategory.STRENGTH, muscleGroup = MuscleGroup.BICEPS, equipment = "Dumbbell"),

        // Triceps
        Exercise(name = "Tricep Pushdown", category = ExerciseCategory.STRENGTH, muscleGroup = MuscleGroup.TRICEPS, equipment = "Cable"),
        Exercise(name = "Overhead Tricep Extension", category = ExerciseCategory.STRENGTH, muscleGroup = MuscleGroup.TRICEPS, equipment = "Dumbbell"),
        Exercise(name = "Skull Crusher", category = ExerciseCategory.STRENGTH, muscleGroup = MuscleGroup.TRICEPS, equipment = "Barbell"),
        Exercise(name = "Close Grip Bench Press", category = ExerciseCategory.STRENGTH, muscleGroup = MuscleGroup.TRICEPS, equipment = "Barbell"),
        Exercise(name = "Tricep Dip", category = ExerciseCategory.BODYWEIGHT, muscleGroup = MuscleGroup.TRICEPS, equipment = "Dip Station"),
        Exercise(name = "Diamond Push-Up", category = ExerciseCategory.BODYWEIGHT, muscleGroup = MuscleGroup.TRICEPS),

        // Quadriceps
        Exercise(name = "Squat", category = ExerciseCategory.STRENGTH, muscleGroup = MuscleGroup.QUADRICEPS, equipment = "Barbell"),
        Exercise(name = "Front Squat", category = ExerciseCategory.STRENGTH, muscleGroup = MuscleGroup.QUADRICEPS, equipment = "Barbell"),
        Exercise(name = "Leg Press", category = ExerciseCategory.STRENGTH, muscleGroup = MuscleGroup.QUADRICEPS, equipment = "Machine"),
        Exercise(name = "Leg Extension", category = ExerciseCategory.STRENGTH, muscleGroup = MuscleGroup.QUADRICEPS, equipment = "Machine"),
        Exercise(name = "Bulgarian Split Squat", category = ExerciseCategory.STRENGTH, muscleGroup = MuscleGroup.QUADRICEPS, equipment = "Dumbbells"),
        Exercise(name = "Goblet Squat", category = ExerciseCategory.STRENGTH, muscleGroup = MuscleGroup.QUADRICEPS, equipment = "Dumbbell"),
        Exercise(name = "Lunge", category = ExerciseCategory.STRENGTH, muscleGroup = MuscleGroup.QUADRICEPS, equipment = "Dumbbells"),

        // Hamstrings
        Exercise(name = "Romanian Deadlift", category = ExerciseCategory.STRENGTH, muscleGroup = MuscleGroup.HAMSTRINGS, equipment = "Barbell"),
        Exercise(name = "Leg Curl", category = ExerciseCategory.STRENGTH, muscleGroup = MuscleGroup.HAMSTRINGS, equipment = "Machine"),
        Exercise(name = "Stiff Leg Deadlift", category = ExerciseCategory.STRENGTH, muscleGroup = MuscleGroup.HAMSTRINGS, equipment = "Barbell"),
        Exercise(name = "Good Morning", category = ExerciseCategory.STRENGTH, muscleGroup = MuscleGroup.HAMSTRINGS, equipment = "Barbell"),

        // Glutes
        Exercise(name = "Hip Thrust", category = ExerciseCategory.STRENGTH, muscleGroup = MuscleGroup.GLUTES, equipment = "Barbell"),
        Exercise(name = "Glute Bridge", category = ExerciseCategory.BODYWEIGHT, muscleGroup = MuscleGroup.GLUTES),
        Exercise(name = "Cable Kickback", category = ExerciseCategory.STRENGTH, muscleGroup = MuscleGroup.GLUTES, equipment = "Cable"),

        // Calves
        Exercise(name = "Standing Calf Raise", category = ExerciseCategory.STRENGTH, muscleGroup = MuscleGroup.CALVES, equipment = "Machine"),
        Exercise(name = "Seated Calf Raise", category = ExerciseCategory.STRENGTH, muscleGroup = MuscleGroup.CALVES, equipment = "Machine"),

        // Abs
        Exercise(name = "Crunch", category = ExerciseCategory.BODYWEIGHT, muscleGroup = MuscleGroup.ABS),
        Exercise(name = "Plank", category = ExerciseCategory.BODYWEIGHT, muscleGroup = MuscleGroup.ABS),
        Exercise(name = "Hanging Leg Raise", category = ExerciseCategory.BODYWEIGHT, muscleGroup = MuscleGroup.ABS, equipment = "Pull-Up Bar"),
        Exercise(name = "Ab Wheel Rollout", category = ExerciseCategory.BODYWEIGHT, muscleGroup = MuscleGroup.ABS, equipment = "Ab Wheel"),
        Exercise(name = "Cable Crunch", category = ExerciseCategory.STRENGTH, muscleGroup = MuscleGroup.ABS, equipment = "Cable"),
        Exercise(name = "Russian Twist", category = ExerciseCategory.BODYWEIGHT, muscleGroup = MuscleGroup.ABS),
        Exercise(name = "Mountain Climber", category = ExerciseCategory.BODYWEIGHT, muscleGroup = MuscleGroup.ABS),

        // Cardio
        Exercise(name = "Running", category = ExerciseCategory.CARDIO, muscleGroup = MuscleGroup.CARDIO),
        Exercise(name = "Treadmill", category = ExerciseCategory.CARDIO, muscleGroup = MuscleGroup.CARDIO, equipment = "Treadmill"),
        Exercise(name = "Peloton Ride", category = ExerciseCategory.CARDIO, muscleGroup = MuscleGroup.CARDIO, equipment = "Peloton"),
        Exercise(name = "Peloton Run", category = ExerciseCategory.CARDIO, muscleGroup = MuscleGroup.CARDIO, equipment = "Peloton"),
        Exercise(name = "Peloton Strength", category = ExerciseCategory.STRENGTH, muscleGroup = MuscleGroup.FULL_BODY, equipment = "Peloton"),
        Exercise(name = "Peloton Yoga", category = ExerciseCategory.FLEXIBILITY, muscleGroup = MuscleGroup.FULL_BODY, equipment = "Peloton"),
        Exercise(name = "Peloton HIIT", category = ExerciseCategory.CARDIO, muscleGroup = MuscleGroup.FULL_BODY, equipment = "Peloton"),
        Exercise(name = "Stationary Bike", category = ExerciseCategory.CARDIO, muscleGroup = MuscleGroup.CARDIO, equipment = "Bike"),
        Exercise(name = "Elliptical", category = ExerciseCategory.CARDIO, muscleGroup = MuscleGroup.CARDIO, equipment = "Elliptical"),
        Exercise(name = "Rowing Machine", category = ExerciseCategory.CARDIO, muscleGroup = MuscleGroup.FULL_BODY, equipment = "Rower"),
        Exercise(name = "Jump Rope", category = ExerciseCategory.CARDIO, muscleGroup = MuscleGroup.FULL_BODY, equipment = "Jump Rope"),
        Exercise(name = "Stair Climber", category = ExerciseCategory.CARDIO, muscleGroup = MuscleGroup.CARDIO, equipment = "Machine"),

        // Bodyweight / Full Body
        Exercise(name = "Burpee", category = ExerciseCategory.BODYWEIGHT, muscleGroup = MuscleGroup.FULL_BODY),
        Exercise(name = "Jumping Jack", category = ExerciseCategory.BODYWEIGHT, muscleGroup = MuscleGroup.FULL_BODY),
        Exercise(name = "Box Jump", category = ExerciseCategory.BODYWEIGHT, muscleGroup = MuscleGroup.FULL_BODY, equipment = "Box"),
        Exercise(name = "Bodyweight Squat", category = ExerciseCategory.BODYWEIGHT, muscleGroup = MuscleGroup.QUADRICEPS),
        Exercise(name = "Wall Sit", category = ExerciseCategory.BODYWEIGHT, muscleGroup = MuscleGroup.QUADRICEPS),

        // Forearms
        Exercise(name = "Wrist Curl", category = ExerciseCategory.STRENGTH, muscleGroup = MuscleGroup.FOREARMS, equipment = "Barbell"),
        Exercise(name = "Reverse Wrist Curl", category = ExerciseCategory.STRENGTH, muscleGroup = MuscleGroup.FOREARMS, equipment = "Barbell"),
        Exercise(name = "Farmer's Walk", category = ExerciseCategory.STRENGTH, muscleGroup = MuscleGroup.FOREARMS, equipment = "Dumbbells"),
    )
}
