package com.workout.tracker.util

data class PlateLoadout(
    val targetWeight: Double,
    val barWeight: Double,
    val platesPerSide: List<Pair<Double, Int>>,
    val achievedWeight: Double,
    val isExact: Boolean
) {
    val totalPlates: Int get() = platesPerSide.sumOf { it.second } * 2
}

object PlateCalculator {

    val STANDARD_PLATES = listOf(45.0, 35.0, 25.0, 10.0, 5.0, 2.5)
    val STANDARD_BAR_WEIGHT = 45.0

    fun calculate(
        targetWeight: Double,
        barWeight: Double = STANDARD_BAR_WEIGHT,
        availablePlates: List<Double> = STANDARD_PLATES
    ): PlateLoadout {
        val sortedPlates = availablePlates.sortedDescending()
        var remaining = (targetWeight - barWeight) / 2.0 // per side

        if (remaining <= 0) {
            return PlateLoadout(
                targetWeight = targetWeight,
                barWeight = barWeight,
                platesPerSide = emptyList(),
                achievedWeight = barWeight,
                isExact = targetWeight <= barWeight
            )
        }

        val platesPerSide = mutableListOf<Pair<Double, Int>>()

        for (plate in sortedPlates) {
            if (remaining >= plate) {
                val count = (remaining / plate).toInt()
                platesPerSide.add(plate to count)
                remaining -= plate * count
            }
        }

        val achievedPerSide = platesPerSide.sumOf { it.first * it.second }
        val achievedWeight = barWeight + achievedPerSide * 2

        return PlateLoadout(
            targetWeight = targetWeight,
            barWeight = barWeight,
            platesPerSide = platesPerSide,
            achievedWeight = achievedWeight,
            isExact = kotlin.math.abs(achievedWeight - targetWeight) < 0.01
        )
    }

    fun formatPlate(weight: Double): String {
        return if (weight == weight.toLong().toDouble()) {
            weight.toLong().toString()
        } else {
            weight.toString()
        }
    }
}
