package com.workout.tracker.ui.viewmodel

import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.workout.tracker.WorkoutApp
import com.workout.tracker.data.dao.TemplateWithExerciseCount
import com.workout.tracker.data.entity.*
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

    private val _importResult = MutableStateFlow<String?>(null)
    val importResult: StateFlow<String?> = _importResult

    fun clearImportResult() {
        _importResult.value = null
    }

    fun importRoutineFromText(text: String) {
        viewModelScope.launch {
            try {
                val result = parseAndImportRoutines(text)
                _importResult.value = result
            } catch (e: Exception) {
                _importResult.value = "Import failed: ${e.message}"
            }
        }
    }

    private data class ParsedExercise(
        val name: String,
        val sets: Int,
        val reps: Int,
        val restSeconds: Int,
        val supersetTag: String? // e.g. "A", "B" — exercises with same tag are superseted
    )

    private data class ParsedRoutine(
        val name: String,
        val description: String?,
        val exercises: List<ParsedExercise>
    )

    private suspend fun parseAndImportRoutines(text: String): String {
        // Split into multiple routines if present
        val routineBlocks = splitIntoRoutineBlocks(text)
        if (routineBlocks.isEmpty()) return "No routines found"

        val parsedRoutines = routineBlocks.map { parseRoutineBlock(it) }
        val createdTemplateIds = mutableListOf<Long>()
        val results = mutableListOf<String>()

        for (routine in parsedRoutines) {
            if (routine.exercises.isEmpty()) continue
            val templateId = createTemplateFromParsed(routine)
            createdTemplateIds.add(templateId)
            results.add("\"${routine.name}\" (${routine.exercises.size} exercises)")
        }

        // Auto-generate schedule if multiple routines (program import)
        var scheduleInfo = ""
        if (createdTemplateIds.size >= 2) {
            val weeks = detectWeekCount(text)
            scheduleInfo = generateSchedule(createdTemplateIds, weeks)
        }

        val summary = "Imported ${results.size} routine(s):\n${results.joinToString("\n") { "  - $it" }}"
        return if (scheduleInfo.isNotEmpty()) "$summary\n\n$scheduleInfo" else summary
    }

    private fun splitIntoRoutineBlocks(text: String): List<String> {
        // Split on "Routine:" boundaries
        val parts = text.split(Regex("(?=Routine:)", RegexOption.IGNORE_CASE))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        // If no "Routine:" prefix found, treat entire text as one block
        if (parts.isEmpty() || !parts[0].startsWith("Routine", ignoreCase = true)) {
            return if (text.isNotBlank()) listOf(text) else emptyList()
        }
        return parts
    }

    private fun parseRoutineBlock(block: String): ParsedRoutine {
        val lines = block.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return ParsedRoutine("Imported Routine", null, emptyList())

        var routineName = "Imported Routine"
        var description: String? = null
        val exercises = mutableListOf<ParsedExercise>()

        for (line in lines) {
            val lower = line.lowercase()
            when {
                lower.startsWith("routine:") || lower.startsWith("name:") -> {
                    routineName = line.substringAfter(":").trim()
                        .removePrefix("---").removeSuffix("---").trim()
                }
                lower.startsWith("description:") -> {
                    description = line.substringAfter(":").trim()
                }
                line.startsWith("---") || line.startsWith("===") -> continue
                line.startsWith("#") || line.startsWith("//") -> continue
                lower.startsWith("phase") || lower.startsWith("week") || lower.startsWith("progression") -> continue
                else -> {
                    val parsed = parseExerciseLine(line)
                    if (parsed != null) exercises.add(parsed)
                }
            }
        }

        return ParsedRoutine(routineName, description, exercises)
    }

    private fun parseExerciseLine(line: String): ParsedExercise? {
        var cleaned = line
            .removePrefix("-").removePrefix("*")
            .replace(Regex("^\\d+\\.\\s*"), "") // Remove "1. " numbering
            .trim()

        if (cleaned.isEmpty()) return null

        // Detect superset tag: A1, A2, B1, B2, etc.
        var supersetTag: String? = null
        val supersetMatch = Regex("^([A-Z])\\d\\s+", RegexOption.IGNORE_CASE).find(cleaned)
        if (supersetMatch != null) {
            supersetTag = supersetMatch.groupValues[1].uppercase()
            cleaned = cleaned.substring(supersetMatch.range.last + 1).trim()
        }

        // Split name from config at colon
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

        if (namePart.isEmpty()) return null

        // Parse sets x reps (handles ranges like 4x6-8)
        var sets = 3
        var reps = 10
        var rest = 90

        val setsRepsMatch = Regex("(\\d+)\\s*[xX×]\\s*(\\d+)(?:-(\\d+))?").find(configPart)
        if (setsRepsMatch != null) {
            sets = setsRepsMatch.groupValues[1].toIntOrNull() ?: 3
            // For rep ranges like 6-8, use the top of range as target
            val lowReps = setsRepsMatch.groupValues[2].toIntOrNull() ?: 10
            val highReps = setsRepsMatch.groupValues[3].toIntOrNull()
            reps = highReps ?: lowReps
        }

        // "each" modifier (e.g., "3x10 each") — keep reps as-is, just note it

        // Parse rest time - supports: "rest 2 min", "rest 90s", "rest 0s", "75s", "2 min"
        val restMinMatch = Regex("rest\\s+(\\d+)\\s*min", RegexOption.IGNORE_CASE).find(configPart)
        val restSecMatch = Regex("rest\\s+(\\d+)\\s*s(?:ec)?", RegexOption.IGNORE_CASE).find(configPart)
        val restSecAlt = Regex("(\\d+)\\s*s(?:ec)?\\s*rest", RegexOption.IGNORE_CASE).find(configPart)

        rest = when {
            restMinMatch != null -> (restMinMatch.groupValues[1].toIntOrNull() ?: 2) * 60
            restSecMatch != null -> restSecMatch.groupValues[1].toIntOrNull() ?: 90
            restSecAlt != null -> restSecAlt.groupValues[1].toIntOrNull() ?: 90
            else -> 90
        }

        return ParsedExercise(namePart, sets, reps, rest, supersetTag)
    }

    private suspend fun createTemplateFromParsed(routine: ParsedRoutine): Long {
        val templateId = repository.insertTemplate(
            WorkoutTemplate(name = routine.name, description = routine.description)
        )

        // Assign superset group numbers from tags (A→1, B→2, etc.)
        val tagToGroup = mutableMapOf<String, Int>()
        var nextGroup = 1

        val templateExercises = routine.exercises.mapIndexed { index, ex ->
            val supersetGroup = if (ex.supersetTag != null) {
                tagToGroup.getOrPut(ex.supersetTag) { nextGroup++ }
            } else null

            val exercise = findOrCreateExercise(ex.name)

            TemplateExercise(
                templateId = templateId,
                exerciseId = exercise.id,
                orderIndex = index,
                targetSets = ex.sets,
                targetReps = ex.reps,
                restSeconds = ex.restSeconds,
                supersetGroup = supersetGroup
            )
        }

        repository.insertTemplateExercises(templateExercises)
        return templateId
    }

    private suspend fun findOrCreateExercise(name: String): Exercise {
        // Try exact match
        repository.getExerciseByName(name)?.let { return it }
        // Try with hyphens replaced
        repository.getExerciseByName(name.replace("-", " "))?.let { return it }
        // Try common aliases
        val aliases = mapOf(
            "DB" to "Dumbbell", "BB" to "Barbell", "KB" to "Kettlebell",
            "OH" to "Overhead", "RDL" to "Romanian Deadlift",
            "Barbell Back Squat" to "Squat", "Barbell RDL" to "Romanian Deadlift",
            "DB Curl" to "Dumbbell Curl", "DB Lateral Raise" to "Lateral Raise",
            "DB Overhead Tricep Extension" to "Overhead Tricep Extension",
            "DB Walking Lunge" to "Lunge", "DB Incline Curl" to "Incline Dumbbell Curl",
            "DB Skull Crusher" to "Skull Crusher", "DB Chest-Supported Row" to "Dumbbell Row",
            "Single-Arm DB Row" to "Dumbbell Row", "Hammer Curl" to "Hammer Curl",
            "Incline DB Press" to "Incline Dumbbell Press",
            "Barbell Bent-Over Row" to "Barbell Row",
        )
        val aliasedName = aliases[name]
        if (aliasedName != null) {
            repository.getExerciseByName(aliasedName)?.let { return it }
        }

        // Create new exercise
        val id = repository.insertExercise(
            Exercise(
                name = name,
                category = ExerciseCategory.STRENGTH,
                muscleGroup = MuscleGroup.OTHER,
                isCustom = true
            )
        )
        return repository.getExerciseById(id)!!
    }

    private fun detectWeekCount(text: String): Int {
        // Look for "X-Week" or "X week" patterns
        val weekMatch = Regex("(\\d+)[- ]?[Ww]eek").find(text)
        return weekMatch?.groupValues?.get(1)?.toIntOrNull() ?: 10
    }

    private suspend fun generateSchedule(templateIds: List<Long>, weeks: Int): String {
        // Schedule pattern: workouts spread across the week with rest days
        // For 4 templates: Mon/Tue/Thu/Fri with Wed/Sat/Sun as rest
        val daysPattern = when (templateIds.size) {
            2 -> listOf(0, 2) // Mon, Wed
            3 -> listOf(0, 2, 4) // Mon, Wed, Fri
            4 -> listOf(0, 1, 3, 4) // Mon, Tue, Thu, Fri
            5 -> listOf(0, 1, 2, 3, 4) // Mon-Fri
            6 -> listOf(0, 1, 2, 3, 4, 5) // Mon-Sat
            else -> (0 until templateIds.size.coerceAtMost(7)).toList()
        }

        val today = java.time.LocalDate.now()
        // Start on next Monday
        val startDate = today.with(java.time.temporal.TemporalAdjusters.next(java.time.DayOfWeek.MONDAY))

        var scheduledCount = 0
        for (week in 0 until weeks) {
            val weekStart = startDate.plusWeeks(week.toLong())
            for ((patternIndex, dayOffset) in daysPattern.withIndex()) {
                val templateIndex = patternIndex % templateIds.size
                val date = weekStart.plusDays(dayOffset.toLong())
                val millis = date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

                repository.insertScheduledWorkout(
                    com.workout.tracker.data.entity.ScheduledWorkout(
                        templateId = templateIds[templateIndex],
                        scheduledDate = millis
                    )
                )
                scheduledCount++
            }

            // Add rest days (Wed and weekends for 4-day split)
            if (templateIds.size == 4) {
                for (restDay in listOf(2, 5, 6)) { // Wed, Sat, Sun
                    val date = weekStart.plusDays(restDay.toLong())
                    val millis = date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                    repository.insertScheduledWorkout(
                        com.workout.tracker.data.entity.ScheduledWorkout(
                            templateId = null,
                            scheduledDate = millis,
                            label = "Rest Day"
                        )
                    )
                }
            }
        }

        return "Scheduled $scheduledCount workouts across $weeks weeks (starting ${startDate})"
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
