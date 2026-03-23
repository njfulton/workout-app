package com.workout.tracker.data.dao

import androidx.room.*
import com.workout.tracker.data.entity.ScheduledWorkout
import kotlinx.coroutines.flow.Flow

data class ScheduledWorkoutWithTemplate(
    val id: Long,
    val templateId: Long?,
    val templateName: String?,
    val scheduledDate: Long,
    val isCompleted: Boolean,
    val completedWorkoutLogId: Long?,
    val label: String?,
    val isSkipped: Boolean
)

@Dao
interface ScheduleDao {
    @Query("""
        SELECT s.id, s.templateId, t.name as templateName, s.scheduledDate,
               s.isCompleted, s.completedWorkoutLogId, s.label, s.isSkipped
        FROM scheduled_workouts s
        LEFT JOIN workout_templates t ON s.templateId = t.id
        WHERE s.scheduledDate >= :fromDate
        ORDER BY s.scheduledDate ASC
    """)
    fun getUpcomingSchedule(fromDate: Long): Flow<List<ScheduledWorkoutWithTemplate>>

    @Query("""
        SELECT s.id, s.templateId, t.name as templateName, s.scheduledDate,
               s.isCompleted, s.completedWorkoutLogId, s.label, s.isSkipped
        FROM scheduled_workouts s
        LEFT JOIN workout_templates t ON s.templateId = t.id
        WHERE s.scheduledDate >= :startDate AND s.scheduledDate <= :endDate
        ORDER BY s.scheduledDate ASC
    """)
    fun getScheduleBetween(startDate: Long, endDate: Long): Flow<List<ScheduledWorkoutWithTemplate>>

    @Insert
    suspend fun insert(scheduledWorkout: ScheduledWorkout): Long

    @Update
    suspend fun update(scheduledWorkout: ScheduledWorkout)

    @Delete
    suspend fun delete(scheduledWorkout: ScheduledWorkout)
}
