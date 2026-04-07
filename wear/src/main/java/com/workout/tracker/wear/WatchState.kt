package com.workout.tracker.wear

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Shared state for the watch UI, fed by [WearDataService] messages from the phone.
 */
data class WatchWorkoutState(
    val isActive: Boolean = false,
    val workoutName: String = "",
    val exerciseName: String = "",
    val setsDone: Int = 0,
    val totalSets: Int = 0,
    val lastSet: String = "",
    val restSeconds: Int = 0,
    val restRunning: Boolean = false,
    val restStartedAtMillis: Long = 0L
)

object WatchState {
    private val _state = MutableStateFlow(WatchWorkoutState())
    val state: StateFlow<WatchWorkoutState> = _state

    // Fires a new value every time the rest timer finishes (for haptic feedback).
    private val _restFinishedTick = MutableStateFlow(0L)
    val restFinishedTick: StateFlow<Long> = _restFinishedTick

    fun onWorkoutStart(name: String, exercise: String, setsDone: Int, totalSets: Int) {
        _state.value = _state.value.copy(
            isActive = true,
            workoutName = name,
            exerciseName = exercise,
            setsDone = setsDone,
            totalSets = totalSets,
            lastSet = "",
            restSeconds = 0,
            restRunning = false
        )
    }

    fun onWorkoutUpdate(exercise: String, setsDone: Int, totalSets: Int, lastSet: String) {
        _state.value = _state.value.copy(
            isActive = true,
            exerciseName = exercise,
            setsDone = setsDone,
            totalSets = totalSets,
            lastSet = lastSet
        )
    }

    fun onRestTimer(seconds: Int, running: Boolean) {
        val prev = _state.value
        if (prev.restRunning && !running) {
            _restFinishedTick.value = System.currentTimeMillis()
        }
        _state.value = prev.copy(
            restSeconds = seconds,
            restRunning = running,
            restStartedAtMillis = if (running) System.currentTimeMillis() else 0L
        )
    }

    fun onWorkoutEnd() {
        _state.value = WatchWorkoutState()
    }
}
