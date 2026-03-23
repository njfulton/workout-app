package com.workout.tracker.ui.viewmodel

import android.content.Context
import android.media.RingtoneManager
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.workout.tracker.WorkoutApp
import com.workout.tracker.data.dao.ExerciseHistoryEntry
import com.workout.tracker.data.entity.*
import com.workout.tracker.data.repository.OverloadSuggestion
import com.workout.tracker.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ActiveExercise(
    val exerciseLogId: Long,
    val exercise: Exercise,
    val sets: List<SetLog> = emptyList(),
    val overloadSuggestion: OverloadSuggestion? = null,
    val restSeconds: Int = 90,
    val history: List<ExerciseHistoryEntry> = emptyList(),
    val supersetGroup: Int? = null,
    val targetSets: Int? = null,
    val targetReps: Int? = null,
    val isManuallyDone: Boolean = false
)

data class ExerciseGroup(
    val exercises: List<ActiveExercise>,
    val isSuperset: Boolean
) {
    val isCompleted: Boolean
        get() = exercises.all { ex ->
            ex.isManuallyDone || (ex.targetSets != null && ex.sets.count { !it.isWarmup } >= ex.targetSets)
        }

    val label: String
        get() = if (isSuperset) exercises.joinToString(" + ") { it.exercise.name }
        else exercises.first().exercise.name
}

data class ActiveWorkoutState(
    val workoutLog: WorkoutLog? = null,
    val exercises: List<ActiveExercise> = emptyList(),
    val isActive: Boolean = false,
    val currentGroupIndex: Int = 0,
    val isFromTemplate: Boolean = false
) {
    val groups: List<ExerciseGroup>
        get() {
            val result = mutableListOf<ExerciseGroup>()
            var i = 0
            while (i < exercises.size) {
                val ex = exercises[i]
                if (ex.supersetGroup != null) {
                    val group = mutableListOf(ex)
                    while (i + 1 < exercises.size && exercises[i + 1].supersetGroup == ex.supersetGroup) {
                        i++
                        group.add(exercises[i])
                    }
                    result.add(ExerciseGroup(group, isSuperset = true))
                } else {
                    result.add(ExerciseGroup(listOf(ex), isSuperset = false))
                }
                i++
            }
            return result
        }

    val currentGroup: ExerciseGroup?
        get() = groups.getOrNull(currentGroupIndex)
}

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

    // Timer completion event
    private val _timerFinishedEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val timerFinishedEvent: SharedFlow<Unit> = _timerFinishedEvent

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

            if (templateId != null) {
                val templateExercises = repository.getTemplateExercises(templateId)
                val activeExercises = templateExercises.mapNotNull { te ->
                    val exercise = repository.getExerciseById(te.exerciseId) ?: return@mapNotNull null
                    val elId = repository.insertExerciseLog(
                        ExerciseLog(workoutLogId = logId, exerciseId = te.exerciseId, orderIndex = te.orderIndex, supersetGroup = te.supersetGroup)
                    )
                    val suggestion = repository.getProgressiveOverloadSuggestion(te.exerciseId)
                    val history = repository.getExerciseHistory(te.exerciseId)
                    ActiveExercise(
                        exerciseLogId = elId, exercise = exercise, overloadSuggestion = suggestion,
                        restSeconds = te.restSeconds, history = history, supersetGroup = te.supersetGroup,
                        targetSets = te.targetSets, targetReps = te.targetReps
                    )
                }
                _activeWorkout.value = ActiveWorkoutState(
                    workoutLog = savedLog, exercises = activeExercises,
                    isActive = true, isFromTemplate = true
                )
            } else {
                _activeWorkout.value = ActiveWorkoutState(workoutLog = savedLog, isActive = true, isFromTemplate = false)
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
            val history = repository.getExerciseHistory(exercise.id)
            val newExercise = ActiveExercise(exerciseLogId = elId, exercise = exercise, overloadSuggestion = suggestion, history = history)
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

            // Check if current group is now completed and auto-advance
            val state = _activeWorkout.value
            val currentGroup = state.currentGroup
            if (currentGroup != null && currentGroup.isCompleted) {
                val nextIndex = state.currentGroupIndex + 1
                if (nextIndex < state.groups.size) {
                    _activeWorkout.value = state.copy(currentGroupIndex = nextIndex)
                }
            }
        }
    }

    fun updateSet(updatedSet: SetLog) {
        viewModelScope.launch {
            repository.updateSetLog(updatedSet)
            _activeWorkout.value = _activeWorkout.value.let { state ->
                state.copy(exercises = state.exercises.map { ae ->
                    if (ae.exerciseLogId == updatedSet.exerciseLogId) {
                        ae.copy(sets = ae.sets.map { s -> if (s.id == updatedSet.id) updatedSet else s })
                    } else ae
                })
            }
        }
    }

    fun markExerciseDone(exerciseLogId: Long) {
        _activeWorkout.value = _activeWorkout.value.let { state ->
            val newExercises = state.exercises.map { ae ->
                if (ae.exerciseLogId == exerciseLogId) ae.copy(isManuallyDone = true) else ae
            }
            state.copy(exercises = newExercises)
        }
        // Auto-advance if current group is now done
        val state = _activeWorkout.value
        val currentGroup = state.currentGroup
        if (currentGroup != null && currentGroup.isCompleted) {
            val nextIndex = state.currentGroupIndex + 1
            if (nextIndex < state.groups.size) {
                _activeWorkout.value = state.copy(currentGroupIndex = nextIndex)
            }
        }
    }

    fun navigateToGroup(index: Int) {
        val state = _activeWorkout.value
        if (index in state.groups.indices) {
            _activeWorkout.value = state.copy(currentGroupIndex = index)
        }
    }

    fun nextGroup() {
        val state = _activeWorkout.value
        val nextIndex = state.currentGroupIndex + 1
        if (nextIndex < state.groups.size) {
            _activeWorkout.value = state.copy(currentGroupIndex = nextIndex)
        }
    }

    fun previousGroup() {
        val state = _activeWorkout.value
        val prevIndex = state.currentGroupIndex - 1
        if (prevIndex >= 0) {
            _activeWorkout.value = state.copy(currentGroupIndex = prevIndex)
        }
    }

    fun discardWorkout() {
        viewModelScope.launch {
            val log = _activeWorkout.value.workoutLog ?: return@launch
            repository.deleteWorkoutLog(log)
            _activeWorkout.value = ActiveWorkoutState()
        }
    }

    fun finishWorkout() {
        viewModelScope.launch {
            val log = _activeWorkout.value.workoutLog ?: return@launch
            repository.updateWorkoutLog(log.copy(endTime = System.currentTimeMillis()))
            _activeWorkout.value = ActiveWorkoutState()
        }
    }

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
            if (_restTimerSeconds.value == 0) {
                _timerFinishedEvent.tryEmit(Unit)
            }
            _isTimerRunning.value = false
        }
    }

    fun skipRestTimer() {
        _isTimerRunning.value = false
        _restTimerSeconds.value = 0
    }

    fun stopRestTimer() {
        _isTimerRunning.value = false
        _restTimerSeconds.value = 0
    }

    fun playTimerSound(context: Context) {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context, uri)
            ringtone?.play()
        } catch (_: Exception) { }
    }

    fun exportToCsv(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val rows = repository.getAllDataForExport()
            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US)
            val sb = StringBuilder()
            sb.appendLine("Date,Workout Name,Type,Duration (min),Exercise,Set,Reps,Weight (lbs),Duration (sec),Distance (mi),Warmup")
            for (row in rows) {
                val date = dateFormat.format(java.util.Date(row.startTime))
                val duration = if (row.endTime != null) (row.endTime - row.startTime) / 60000 else ""
                val weight = row.weightLbs?.let { "%.1f".format(it) } ?: ""
                val dist = row.distanceMiles?.let { "%.2f".format(it) } ?: ""
                val dur = row.durationSeconds?.toString() ?: ""
                val reps = row.reps?.toString() ?: ""
                val warmup = if (row.isWarmup) "Yes" else ""
                val name = "\"${row.workoutName}\""
                val exercise = "\"${row.exerciseName}\""
                sb.appendLine("$date,$name,${row.workoutType},$duration,$exercise,${row.setNumber},$reps,$weight,$dur,$dist,$warmup")
            }
            onResult(sb.toString())
        }
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
