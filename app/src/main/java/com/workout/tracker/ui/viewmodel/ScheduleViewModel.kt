package com.workout.tracker.ui.viewmodel

import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.workout.tracker.WorkoutApp
import com.workout.tracker.data.dao.ScheduledWorkoutWithTemplate
import com.workout.tracker.data.entity.ScheduledWorkout
import com.workout.tracker.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

class ScheduleViewModel(private val repository: WorkoutRepository) : ViewModel() {

    val upcomingSchedule: StateFlow<List<ScheduledWorkoutWithTemplate>> =
        repository.getUpcomingSchedule(
            LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun scheduleWorkout(templateId: Long, date: LocalDate) {
        viewModelScope.launch {
            val millis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            repository.insertScheduledWorkout(
                ScheduledWorkout(templateId = templateId, scheduledDate = millis)
            )
        }
    }

    fun markCompleted(scheduledWorkout: ScheduledWorkoutWithTemplate, workoutLogId: Long) {
        viewModelScope.launch {
            repository.updateScheduledWorkout(
                ScheduledWorkout(
                    id = scheduledWorkout.id,
                    templateId = scheduledWorkout.templateId,
                    scheduledDate = scheduledWorkout.scheduledDate,
                    isCompleted = true,
                    completedWorkoutLogId = workoutLogId
                )
            )
        }
    }

    fun deleteScheduledWorkout(sw: ScheduledWorkoutWithTemplate) {
        viewModelScope.launch {
            repository.deleteScheduledWorkout(
                ScheduledWorkout(
                    id = sw.id,
                    templateId = sw.templateId,
                    scheduledDate = sw.scheduledDate,
                    isCompleted = sw.isCompleted,
                    completedWorkoutLogId = sw.completedWorkoutLogId
                )
            )
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as WorkoutApp
                ScheduleViewModel(app.repository)
            }
        }
    }
}
