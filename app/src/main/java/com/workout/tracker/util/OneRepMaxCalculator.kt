package com.workout.tracker.util

/**
 * Calculates estimated one-rep max using standard formulas.
 */
object OneRepMaxCalculator {

    /**
     * Epley formula: 1RM = weight × (1 + reps/30)
     * Most widely used and accurate for moderate rep ranges (2-12).
     */
    fun epley(weight: Double, reps: Int): Double {
        if (reps <= 0 || weight <= 0) return 0.0
        if (reps == 1) return weight
        return weight * (1 + reps / 30.0)
    }

    /**
     * Brzycki formula: 1RM = weight × (36 / (37 - reps))
     * More conservative for higher rep ranges.
     */
    fun brzycki(weight: Double, reps: Int): Double {
        if (reps <= 0 || weight <= 0) return 0.0
        if (reps == 1) return weight
        if (reps >= 37) return weight * 36.0 // cap at theoretical max
        return weight * (36.0 / (37.0 - reps))
    }

    /**
     * Average of Epley and Brzycki for a balanced estimate.
     */
    fun estimate(weight: Double, reps: Int): Double {
        return (epley(weight, reps) + brzycki(weight, reps)) / 2.0
    }

    /**
     * Calculate the weight needed for a target rep count given a 1RM.
     */
    fun weightForReps(oneRepMax: Double, targetReps: Int): Double {
        if (targetReps <= 0 || oneRepMax <= 0) return 0.0
        if (targetReps == 1) return oneRepMax
        return oneRepMax / (1 + targetReps / 30.0)
    }
}
