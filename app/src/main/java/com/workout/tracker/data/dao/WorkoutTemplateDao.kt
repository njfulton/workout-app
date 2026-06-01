package com.workout.tracker.data.dao

import androidx.room.*
import com.workout.tracker.data.entity.TemplateExercise
import com.workout.tracker.data.entity.WorkoutTemplate
import kotlinx.coroutines.flow.Flow

data class TemplateWithExerciseCount(
    val id: Long,
    val name: String,
    val description: String?,
    val estimatedDurationMinutes: Int?,
    val exerciseCount: Int
)

@Dao
interface WorkoutTemplateDao {
    @Query("""
        SELECT t.id, t.name, t.description, t.estimatedDurationMinutes,
               COUNT(te.id) as exerciseCount
        FROM workout_templates t
        LEFT JOIN template_exercises te ON t.id = te.templateId
        GROUP BY t.id
        ORDER BY t.name ASC
    """)
    fun getAllTemplatesWithCount(): Flow<List<TemplateWithExerciseCount>>

    @Query("SELECT * FROM workout_templates WHERE id = :id")
    suspend fun getTemplateById(id: Long): WorkoutTemplate?

    @Query("SELECT * FROM template_exercises WHERE templateId = :templateId ORDER BY orderIndex ASC")
    suspend fun getTemplateExercises(templateId: Long): List<TemplateExercise>

    @Insert
    suspend fun insertTemplate(template: WorkoutTemplate): Long

    @Insert
    suspend fun insertTemplateExercise(templateExercise: TemplateExercise): Long

    @Insert
    suspend fun insertTemplateExercises(exercises: List<TemplateExercise>)

    @Update
    suspend fun updateTemplate(template: WorkoutTemplate)

    @Update
    suspend fun updateTemplateExercise(templateExercise: TemplateExercise)

    @Delete
    suspend fun deleteTemplate(template: WorkoutTemplate)

    @Query("DELETE FROM template_exercises WHERE templateId = :templateId")
    suspend fun deleteTemplateExercises(templateId: Long)

    @Delete
    suspend fun deleteTemplateExercise(templateExercise: TemplateExercise)

    @Query("SELECT * FROM workout_templates ORDER BY name ASC")
    suspend fun getAllTemplatesList(): List<WorkoutTemplate>
}
