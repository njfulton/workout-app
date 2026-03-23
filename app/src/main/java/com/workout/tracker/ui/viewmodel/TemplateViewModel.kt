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

    suspend fun getTemplateById(id: Long): WorkoutTemplate? = repository.getTemplateById(id)

    suspend fun getTemplateExerciseDetails(templateId: Long): List<TemplateExerciseDetail> {
        val exercises = repository.getTemplateExercises(templateId)
        return exercises.mapNotNull { te ->
            val exercise = repository.getExerciseById(te.exerciseId) ?: return@mapNotNull null
            TemplateExerciseDetail(te, exercise)
        }
    }

    fun updateTemplate(templateId: Long, name: String, description: String?, exercises: List<Pair<Long, TemplateExerciseConfig>>) {
        viewModelScope.launch {
            repository.updateTemplate(WorkoutTemplate(id = templateId, name = name, description = description))
            repository.deleteTemplateExercises(templateId)
            val templateExercises = exercises.mapIndexed { index, (exerciseId, config) ->
                TemplateExercise(
                    templateId = templateId,
                    exerciseId = exerciseId,
                    orderIndex = index,
                    targetSets = config.sets,
                    targetReps = config.reps,
                    restSeconds = config.restSeconds,
                    supersetGroup = config.supersetGroup
                )
            }
            repository.insertTemplateExercises(templateExercises)
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
                    restSeconds = config.restSeconds,
                    supersetGroup = config.supersetGroup
                )
            }
            repository.insertTemplateExercises(templateExercises)
        }
    }

    fun deleteTemplate(template: WorkoutTemplate) {
        viewModelScope.launch { repository.deleteTemplate(template) }
    }

    /**
     * Import a routine from text. Supports format:
     *
     * Routine: Push Day
     * ---
     * Bench Press: 4x8 rest 90s
     * Incline Dumbbell Press: 3x10 rest 60s
     * Cable Fly: 3x12 rest 60s
     *
     * Or a simpler format:
     * Bench Press 4x8
     * Incline Dumbbell Press 3x10
     */
    private val _importResult = MutableStateFlow<String?>(null)
    val importResult: StateFlow<String?> = _importResult

    fun clearImportResult() {
        _importResult.value = null
    }

    fun importRoutineFromText(text: String) {
        viewModelScope.launch {
            try {
                val result = parseAndImportRoutine(text)
                _importResult.value = result
            } catch (e: Exception) {
                _importResult.value = "Import failed: ${e.message}"
            }
        }
    }

    private suspend fun parseAndImportRoutine(text: String): String {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return "No content to import"

        // Extract routine name
        var routineName = "Imported Routine"
        var description: String? = null
        val exerciseLines = mutableListOf<String>()
        var pastHeader = false

        for (line in lines) {
            val lower = line.lowercase()
            when {
                lower.startsWith("routine:") || lower.startsWith("name:") -> {
                    routineName = line.substringAfter(":").trim()
                }
                lower.startsWith("description:") -> {
                    description = line.substringAfter(":").trim()
                }
                line.startsWith("---") || line.startsWith("===") -> {
                    pastHeader = true
                }
                line.startsWith("#") || line.startsWith("//") -> {
                    // Skip comments
                }
                else -> exerciseLines.add(line)
            }
        }

        if (exerciseLines.isEmpty()) return "No exercises found in text"

        // Parse exercise lines
        data class ParsedExercise(val name: String, val sets: Int, val reps: Int, val restSeconds: Int)
        val parsed = mutableListOf<ParsedExercise>()

        for (line in exerciseLines) {
            // Try formats:
            // "Bench Press: 4x8 rest 90s"
            // "Bench Press: 4 sets x 8 reps, 90s rest"
            // "Bench Press 4x8 90s"
            // "- Bench Press: 4x8 rest 90s"
            // "1. Bench Press: 4x8 rest 90s"
            val cleaned = line
                .removePrefix("-").removePrefix("*")
                .replace(Regex("^\\d+\\.\\s*"), "") // Remove "1. " numbering
                .trim()

            // Split name from sets/reps config
            val colonSplit = cleaned.split(":")
            val (namePart, configPart) = if (colonSplit.size >= 2) {
                colonSplit[0].trim() to colonSplit.subList(1, colonSplit.size).joinToString(":").trim()
            } else {
                // Try to find NxM pattern and split there
                val match = Regex("(.+?)\\s+(\\d+)\\s*[xX×]\\s*(\\d+)").find(cleaned)
                if (match != null) {
                    match.groupValues[1].trim() to cleaned.substring(match.range.first + match.groupValues[1].length).trim()
                } else {
                    cleaned to ""
                }
            }

            if (namePart.isEmpty()) continue

            // Parse sets x reps
            var sets = 3
            var reps = 10
            var rest = 90

            val setsRepsMatch = Regex("(\\d+)\\s*[xX×]\\s*(\\d+)").find(configPart)
            if (setsRepsMatch != null) {
                sets = setsRepsMatch.groupValues[1].toIntOrNull() ?: 3
                reps = setsRepsMatch.groupValues[2].toIntOrNull() ?: 10
            }

            // Parse rest time
            val restMatch = Regex("rest\\s*(\\d+)\\s*s", RegexOption.IGNORE_CASE).find(configPart)
                ?: Regex("(\\d+)\\s*s(?:ec)?\\s*rest", RegexOption.IGNORE_CASE).find(configPart)
                ?: Regex("(\\d+)\\s*seconds?", RegexOption.IGNORE_CASE).find(configPart)
            if (restMatch != null) {
                rest = restMatch.groupValues[1].toIntOrNull() ?: 90
            }

            parsed.add(ParsedExercise(namePart, sets, reps, rest))
        }

        if (parsed.isEmpty()) return "Could not parse any exercises"

        // Create template
        val templateId = repository.insertTemplate(
            WorkoutTemplate(name = routineName, description = description)
        )

        // Match exercises by name or create new ones
        val templateExercises = parsed.mapIndexed { index, ex ->
            var exercise = repository.getExerciseByName(ex.name)
            if (exercise == null) {
                // Try fuzzy match - search for exercises containing the name
                exercise = repository.getExerciseByName(ex.name.replace("-", " "))
            }
            if (exercise == null) {
                // Create new exercise
                val id = repository.insertExercise(
                    com.workout.tracker.data.entity.Exercise(
                        name = ex.name,
                        category = com.workout.tracker.data.entity.ExerciseCategory.STRENGTH,
                        muscleGroup = com.workout.tracker.data.entity.MuscleGroup.OTHER,
                        isCustom = true
                    )
                )
                exercise = repository.getExerciseById(id)
            }

            TemplateExercise(
                templateId = templateId,
                exerciseId = exercise!!.id,
                orderIndex = index,
                targetSets = ex.sets,
                targetReps = ex.reps,
                restSeconds = ex.restSeconds
            )
        }

        repository.insertTemplateExercises(templateExercises)
        return "Imported \"$routineName\" with ${parsed.size} exercises"
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
    val restSeconds: Int = 90,
    val supersetGroup: Int? = null
)
