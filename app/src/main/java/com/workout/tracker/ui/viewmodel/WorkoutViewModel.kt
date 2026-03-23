package com.workout.tracker.ui.viewmodel

import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.workout.tracker.WorkoutApp
import com.workout.tracker.data.entity.*
import com.workout.tracker.data.repository.OverloadSuggestion
import com.workout.tracker.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ActiveExercise(
    val exerciseLogId: Long,
    val exercise: Exercise,
    val sets: List<SetLog> = emptyList(),
    val overloadSuggestion: OverloadSuggestion? = null
)

data class ActiveWorkoutState(
    val workoutLog: WorkoutLog? = null,
    val exercises: List<ActiveExercise> = emptyList(),
    val isActive: Boolean = false
)

class WorkoutViewModel(private val repository: WorkoutRepository) : ViewModel() {

    private val _activeWorkout = MutableStateFlow(ActiveWorkoutState())
    val activeWorkout: StateFlow<ActiveWorkoutState> = _activeWorkout

    val workoutHistory = repository.allWorkoutSummaries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Rest timer state
    private val _restTimerSeconds = MutableStateFlow(0)
    val restTimerSeconds: StateFlow<Int> = _restTimerSeconds

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning

    fun startWorkout(name: String, type: WorkoutType, templateId: Long? = null) {
        viewModelScope.launch {
            val log = WorkoutLog(
                name = name,
                startTime = System.currentTimeMillis(),
                workoutType = type,
                templateId = templateId
            )
            val logId = repository.insertWorkoutLog(log)
            val savedLog = repository.getWorkoutLogById(logId) ?: return@launch

            // If from template, pre-populate exercises
            if (templateId != null) {
                val templateExercises = repository.getTemplateExercises(templateId)
                val activeExercises = templateExercises.mapNotNull { te ->
                    val exercise = repository.getExerciseById(te.exerciseId) ?: return@mapNotNull null
                    val elId = repository.insertExerciseLog(
                        ExerciseLog(workoutLogId = logId, exerciseId = te.exerciseId, orderIndex = te.orderIndex)
                    )
                    val suggestion = repository.getProgressiveOverloadSuggestion(te.exerciseId)
                    ActiveExercise(exerciseLogId = elId, exercise = exercise, overloadSuggestion = suggestion)
                }
                _activeWorkout.value = ActiveWorkoutState(workoutLog = savedLog, exercises = activeExercises, isActive = true)
            } else {
                _activeWorkout.value = ActiveWorkoutState(workoutLog = savedLog, isActive = true)
            }
        }
    }

    fun addExerciseToWorkout(exercise: Exercise) {
        viewModelScope.launch {
            val state = _activeWorkout.value
            val workoutLog = state.workoutLog ?: return@launch
            val orderIndex = state.exercises.size
            val elId = repository.insertExerciseLog(
                ExerciseLog(workoutLogId = workoutLog.id, exerciseId = exercise.id, orderIndex = orderIndex)
            )
            val suggestion = repository.getProgressiveOverloadSuggestion(exercise.id)
            val newExercise = ActiveExercise(exerciseLogId = elId, exercise = exercise, overloadSuggestion = suggestion)
            _activeWorkout.value = state.copy(exercises = state.exercises + newExercise)
        }
    }

    fun logSet(exerciseLogId: Long, setNumber: Int, reps: Int, weight: Double?, isWarmup: Boolean = false) {
        viewModelScope.launch {
            val setLog = SetLog(
                exerciseLogId = exerciseLogId,
                setNumber = setNumber,
                reps = reps,
                weightLbs = weight,
                isWarmup = isWarmup
            )
            val setId = repository.insertSetLog(setLog)
            val savedSet = setLog.copy(id = setId)

            _activeWorkout.value = _activeWorkout.value.let { state ->
                state.copy(exercises = state.exercises.map { ae ->
                    if (ae.exerciseLogId == exerciseLogId) {
                        ae.copy(sets = ae.sets + savedSet)
                    } else ae
                })
            }
        }
    }

    fun finishWorkout() {
        viewModelScope.launch {
            val log = _activeWorkout.value.workoutLog ?: return@launch
            repository.updateWorkoutLog(log.copy(endTime = System.currentTimeMillis()))
            _activeWorkout.value = ActiveWorkoutState()
        }
    }

    // Quick log for bodyweight / random exercises
    fun quickLog(exercise: Exercise, reps: Int, weight: Double? = null) {
        viewModelScope.launch {
            val log = WorkoutLog(
                name = "Quick: ${exercise.name}",
                startTime = System.currentTimeMillis(),
                endTime = System.currentTimeMillis(),
                workoutType = WorkoutType.BODYWEIGHT_QUICK
            )
            val logId = repository.insertWorkoutLog(log)
            val elId = repository.insertExerciseLog(
                ExerciseLog(workoutLogId = logId, exerciseId = exercise.id, orderIndex = 0)
            )
            repository.insertSetLog(
                SetLog(exerciseLogId = elId, setNumber = 1, reps = reps, weightLbs = weight)
            )
        }
    }

    fun startRestTimer(seconds: Int) {
        _restTimerSeconds.value = seconds
        _isTimerRunning.value = true
        viewModelScope.launch {
            while (_restTimerSeconds.value > 0 && _isTimerRunning.value) {
                kotlinx.coroutines.delay(1000)
                _restTimerSeconds.value = _restTimerSeconds.value - 1
            }
            _isTimerRunning.value = false
        }
    }

    fun stopRestTimer() {
        _isTimerRunning.value = false
        _restTimerSeconds.value = 0
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as WorkoutApp
                WorkoutViewModel(app.repository)
            }
        }
    }
}
