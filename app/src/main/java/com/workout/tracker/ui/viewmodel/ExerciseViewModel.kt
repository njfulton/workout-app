package com.workout.tracker.ui.viewmodel

import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.workout.tracker.WorkoutApp
import com.workout.tracker.data.entity.Exercise
import com.workout.tracker.data.entity.ExerciseCategory
import com.workout.tracker.data.entity.MuscleGroup
import com.workout.tracker.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ExerciseViewModel(private val repository: WorkoutRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedCategory = MutableStateFlow<ExerciseCategory?>(null)
    val selectedCategory: StateFlow<ExerciseCategory?> = _selectedCategory

    private val _selectedMuscleGroup = MutableStateFlow<MuscleGroup?>(null)
    val selectedMuscleGroup: StateFlow<MuscleGroup?> = _selectedMuscleGroup

    val exercises: StateFlow<List<Exercise>> = combine(
        _searchQuery, _selectedCategory, _selectedMuscleGroup
    ) { query, category, muscleGroup ->
        Triple(query, category, muscleGroup)
    }.flatMapLatest { (query, category, muscleGroup) ->
        when {
            query.isNotBlank() -> repository.searchExercises(query)
            category != null -> repository.getExercisesByCategory(category)
            muscleGroup != null -> repository.getExercisesByMuscleGroup(muscleGroup)
            else -> repository.allExercises
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setCategory(category: ExerciseCategory?) { _selectedCategory.value = category }
    fun setMuscleGroup(muscleGroup: MuscleGroup?) { _selectedMuscleGroup.value = muscleGroup }

    fun addExercise(name: String, category: ExerciseCategory, muscleGroup: MuscleGroup, equipment: String?) {
        viewModelScope.launch {
            repository.insertExercise(
                Exercise(name = name, category = category, muscleGroup = muscleGroup, equipment = equipment, isCustom = true)
            )
        }
    }

    fun deleteExercise(exercise: Exercise) {
        viewModelScope.launch { repository.deleteExercise(exercise) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as WorkoutApp
                ExerciseViewModel(app.repository)
            }
        }
    }
}
