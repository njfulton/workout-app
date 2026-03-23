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

    fun scheduleNonTemplate(label: String, date: LocalDate) {
        viewModelScope.launch {
            val millis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            repository.insertScheduledWorkout(
                ScheduledWorkout(templateId = null, scheduledDate = millis, label = label)
            )
        }
    }

    fun markCompleted(sw: ScheduledWorkoutWithTemplate) {
        viewModelScope.launch {
            repository.updateScheduledWorkout(sw.toEntity().copy(isCompleted = true, isSkipped = false))
        }
    }

    fun markSkipped(sw: ScheduledWorkoutWithTemplate) {
        viewModelScope.launch {
            repository.updateScheduledWorkout(sw.toEntity().copy(isSkipped = true, isCompleted = false))
        }
    }

    fun reschedule(sw: ScheduledWorkoutWithTemplate, newDate: LocalDate) {
        viewModelScope.launch {
            val millis = newDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            repository.updateScheduledWorkout(sw.toEntity().copy(scheduledDate = millis, isSkipped = false, isCompleted = false))
        }
    }

    fun deleteScheduledWorkout(sw: ScheduledWorkoutWithTemplate) {
        viewModelScope.launch {
            repository.deleteScheduledWorkout(sw.toEntity())
        }
    }

    private fun ScheduledWorkoutWithTemplate.toEntity() = ScheduledWorkout(
        id = id,
        templateId = templateId,
        scheduledDate = scheduledDate,
        isCompleted = isCompleted,
        completedWorkoutLogId = completedWorkoutLogId,
        label = label,
        isSkipped = isSkipped
    )

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as WorkoutApp
                ScheduleViewModel(app.repository)
            }
        }
    }
}
