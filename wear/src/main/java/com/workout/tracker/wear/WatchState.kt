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
    val targetWeight: Double? = null,
    val targetReps: Int? = null,
    val restInitialSeconds: Int = 0,
    val restRunning: Boolean = false,
    val restEndTimeMillis: Long = 0L,
    val lastReceivedPath: String = "",
    val lastReceivedTs: Long = 0L
)

object WatchState {
    private val _state = MutableStateFlow(WatchWorkoutState())
    val state: StateFlow<WatchWorkoutState> = _state

    // Fires a new value every time the rest timer finishes (for haptic feedback).
    private val _restFinishedTick = MutableStateFlow(0L)
    val restFinishedTick: StateFlow<Long> = _restFinishedTick

    fun onWorkoutStart(
        name: String, exercise: String, setsDone: Int, totalSets: Int,
        targetWeight: Double? = null, targetReps: Int? = null
    ) {
        _state.value = _state.value.copy(
            isActive = true,
            workoutName = name,
            exerciseName = exercise,
            setsDone = setsDone,
            totalSets = totalSets,
            lastSet = "",
            targetWeight = targetWeight,
            targetReps = targetReps,
            restInitialSeconds = 0,
            restRunning = false,
            restEndTimeMillis = 0L
        )
    }

    fun onWorkoutUpdate(
        exercise: String, setsDone: Int, totalSets: Int, lastSet: String,
        targetWeight: Double? = null, targetReps: Int? = null
    ) {
        _state.value = _state.value.copy(
            isActive = true,
            exerciseName = exercise,
            setsDone = setsDone,
            totalSets = totalSets,
            lastSet = lastSet,
            targetWeight = targetWeight,
            targetReps = targetReps
        )
    }

    fun onRestTimer(seconds: Int, running: Boolean) {
        val prev = _state.value
        if (prev.restRunning && !running) {
            _restFinishedTick.value = System.currentTimeMillis()
        }
        _state.value = prev.copy(
            restInitialSeconds = seconds,
            restRunning = running,
            restEndTimeMillis = if (running) System.currentTimeMillis() + seconds * 1000L else 0L
        )
    }

    /** Called by the UI when its local countdown reaches zero. */
    fun markRestFinishedLocally() {
        val prev = _state.value
        if (prev.restRunning) {
            _restFinishedTick.value = System.currentTimeMillis()
            _state.value = prev.copy(restRunning = false, restEndTimeMillis = 0L)
        }
    }

    fun onWorkoutEnd() {
        // Preserve diagnostic trail so user can still see last-received after a workout ends
        val prev = _state.value
        _state.value = WatchWorkoutState(
            lastReceivedPath = prev.lastReceivedPath,
            lastReceivedTs = prev.lastReceivedTs
        )
    }

    fun onAnyMessage(path: String) {
        _state.value = _state.value.copy(
            lastReceivedPath = path,
            lastReceivedTs = System.currentTimeMillis()
        )
    }
}
