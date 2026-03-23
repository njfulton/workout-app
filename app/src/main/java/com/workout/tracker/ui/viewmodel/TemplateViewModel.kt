package com.workout.tracker.ui.viewmodel

import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.workout.tracker.WorkoutApp
import com.workout.tracker.data.dao.TemplateWithExerciseCount
import com.workout.tracker.data.entity.Exercise
import com.workout.tracker.data.entity.TemplateExercise
import com.workout.tracker.data.entity.WorkoutTemplate
import com.workout.tracker.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class TemplateExerciseDetail(
    val templateExercise: TemplateExercise,
    val exercise: Exercise
)

class TemplateViewModel(private val repository: WorkoutRepository) : ViewModel() {

    val templates: StateFlow<List<TemplateWithExerciseCount>> = repository.allTemplates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentTemplateExercises = MutableStateFlow<List<TemplateExerciseDetail>>(emptyList())
    val currentTemplateExercises: StateFlow<List<TemplateExerciseDetail>> = _currentTemplateExercises

    fun loadTemplateExercises(templateId: Long) {
        viewModelScope.launch {
            val exercises = repository.getTemplateExercises(templateId)
            _currentTemplateExercises.value = exercises.mapNotNull { te ->
                val exercise = repository.getExerciseById(te.exerciseId) ?: return@mapNotNull null
                TemplateExerciseDetail(te, exercise)
            }
        }
    }

    fun createTemplate(name: String, description: String?, exercises: List<Pair<Long, TemplateExerciseConfig>>) {
        viewModelScope.launch {
            val templateId = repository.insertTemplate(
                WorkoutTemplate(name = name, description = description)
            )
            val templateExercises = exercises.mapIndexed { index, (exerciseId, config) ->
                TemplateExercise(
                    templateId = templateId,
                    exerciseId = exerciseId,
                    orderIndex = index,
                    targetSets = config.sets,
                    targetReps = config.reps,
                    restSeconds = config.restSeconds
                )
            }
            repository.insertTemplateExercises(templateExercises)
        }
    }

    fun deleteTemplate(template: WorkoutTemplate) {
        viewModelScope.launch { repository.deleteTemplate(template) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as WorkoutApp
                TemplateViewModel(app.repository)
            }
        }
    }
}

data class TemplateExerciseConfig(
    val sets: Int = 3,
    val reps: Int = 10,
    val restSeconds: Int = 90
)
