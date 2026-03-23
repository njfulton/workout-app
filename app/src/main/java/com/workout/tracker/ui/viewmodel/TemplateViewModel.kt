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

    fun importRoutineFromText(text: String, completedTodayIndex: Int? = null) {
        viewModelScope.launch {
            try {
                val result = parseAndImportRoutines(text, completedTodayIndex)
                _importResult.value = result
            } catch (e: Throwable) {
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

    private suspend fun parseAndImportRoutines(text: String, completedTodayIndex: Int? = null): String {
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
            // Extract progression text (everything after the routines)
            val progressionText = extractProgressionText(text)
            scheduleInfo = generateScheduleWithProgression(
                createdTemplateIds, weeks,
                completedToday = completedTodayIndex,
                progressionText = progressionText
            )
        }

        val summary = "Imported ${results.size} routine(s):\n${results.joinToString("\n") { "  - $it" }}"
        return if (scheduleInfo.isNotEmpty()) "$summary\n\n$scheduleInfo" else summary
    }

    private fun extractProgressionText(text: String): String {
        // Extract phase/week info — handles both multi-line and inline formats
        val phasePattern = Regex(
            "(?:Phase\\s*\\d+,?\\s*)?Weeks?\\s*\\d+(?:\\s*-\\s*\\d+)?\\s*[-–—:]+\\s*\\w+",
            RegexOption.IGNORE_CASE
        )
        val matches = phasePattern.findAll(text).map { it.value }.toList()
        return matches.joinToString("\n")
    }

    private fun splitIntoRoutineBlocks(text: String): List<String> {
        // Split on "Routine:" boundaries — works whether newline-separated or inline
        val parts = text.split(Regex("(?=Routine:)", RegexOption.IGNORE_CASE))
            .map { it.trim() }
            .filter { it.isNotEmpty() && it.startsWith("Routine", ignoreCase = true) }

        if (parts.isNotEmpty()) return parts

        // If no "Routine:" prefix found, treat entire text as one block
        return if (text.isNotBlank()) listOf(text) else emptyList()
    }

    private fun parseRoutineBlock(block: String): ParsedRoutine {
        // First, extract the routine name from the "Routine: ..." header
        var routineName = "Imported Routine"
        var description: String? = null
        var bodyText = block

        val routineHeaderMatch = Regex("^Routine:\\s*(.+?)---", RegexOption.IGNORE_CASE).find(block)
        if (routineHeaderMatch != null) {
            routineName = routineHeaderMatch.groupValues[1].trim()
            bodyText = block.substring(routineHeaderMatch.range.last + 1).trim()
        } else {
            // Try multi-line format
            val lines = block.lines()
            val firstLine = lines.firstOrNull()?.trim() ?: ""
            if (firstLine.startsWith("Routine:", ignoreCase = true) || firstLine.startsWith("Name:", ignoreCase = true)) {
                routineName = firstLine.substringAfter(":").trim()
                    .removePrefix("---").removeSuffix("---").trim()
                bodyText = lines.drop(1).joinToString("\n")
            }
        }

        // Split the body into individual exercise lines
        // The text may be all on one line, so split on exercise boundaries:
        // Each exercise starts with either a superset tag (A1, B2) or an exercise name followed by ":"
        val exerciseLines = splitIntoExerciseLines(bodyText)

        val exercises = mutableListOf<ParsedExercise>()
        for (line in exerciseLines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            if (isNonExerciseLine(trimmed)) continue

            val parsed = parseExerciseLine(trimmed)
            if (parsed != null) exercises.add(parsed)
        }

        return ParsedRoutine(routineName, description, exercises)
    }

    private fun isNonExerciseLine(line: String): Boolean {
        val lower = line.lowercase().trim()
        // Skip progression/phase/meta lines
        if (lower.startsWith("phase") || lower.startsWith("week") ||
            lower.startsWith("progression") || lower.startsWith("---") ||
            lower.startsWith("===") || lower.startsWith("#") ||
            lower.startsWith("//") || lower.startsWith("description:")) return true
        // Skip lines that start with a number followed by "-week" (e.g. "10-Week Progression")
        if (Regex("^\\d+-?\\s*week", RegexOption.IGNORE_CASE).containsMatchIn(lower)) return true
        // Skip lines mentioning RPE, RIR, deload, etc. without a sets x reps pattern
        if ((lower.contains("rpe") || lower.contains("rir") || lower.contains("deload") ||
            lower.contains("progression framework") || lower.contains("priority is") ||
            lower.contains("expect to")) &&
            !Regex("\\d+\\s*[xX×]\\s*\\d+").containsMatchIn(line)) return true
        return false
    }

    private fun splitIntoExerciseLines(text: String): List<String> {
        // First try: if text has multiple lines, use those
        val rawLines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (rawLines.size > 1) {
            // Multi-line format — but each line might still contain multiple exercises
            return rawLines.flatMap { splitSingleLineExercises(it) }
        }
        // Single line (or single block) — split on exercise boundaries
        return splitSingleLineExercises(text)
    }

    private fun splitSingleLineExercises(text: String): List<String> {
        // Split before superset tags (A1, A2, B1, B2, etc.) that start a new exercise
        // Pattern: rest time followed by a superset tag or a new exercise name with colon
        val result = mutableListOf<String>()

        // Strategy: find all positions where a new exercise starts
        // An exercise starts at: beginning, or before [A-Z]\d followed by a word
        val exerciseStartPattern = Regex(
            "(?<=\\s)([A-Z]\\d\\s+[A-Z])|" +  // superset tag like "A1 Incline"
            "(?<=\\d+s\\s)([A-Z][a-z])|" +      // after rest time like "75s B"
            "(?<=min\\s)([A-Z]\\d\\s)|" +        // after "min A1"
            "(?<=min\\s)([A-Z][a-z])"            // after "min Barbell"
        )

        // More robust: split on the pattern "rest Xs" or "rest X min" followed by next exercise
        // Each exercise ends with "rest \d+s" or "rest \d+ min"
        val restPattern = Regex("rest\\s+\\d+\\s*(?:s(?:ec)?|min)", RegexOption.IGNORE_CASE)
        val restMatches = restPattern.findAll(text).toList()

        if (restMatches.isEmpty()) {
            // No rest patterns found — try splitting on newlines or return as-is
            return text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        }

        var lastEnd = 0
        for (match in restMatches) {
            val exerciseEnd = match.range.last + 1
            val exerciseLine = text.substring(lastEnd, exerciseEnd).trim()
            if (exerciseLine.isNotEmpty()) result.add(exerciseLine)
            lastEnd = exerciseEnd
        }
        // Any remaining text after last rest
        if (lastEnd < text.length) {
            val remaining = text.substring(lastEnd).trim()
            if (remaining.isNotEmpty()) result.add(remaining)
        }

        return result
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
        return repository.getExerciseById(id)
            ?: throw IllegalStateException("Failed to create exercise: $name")
    }

    private fun detectWeekCount(text: String): Int {
        // Look for "X-Week" or "X week" patterns
        val weekMatch = Regex("(\\d+)[- ]?[Ww]eek").find(text)
        return weekMatch?.groupValues?.get(1)?.toIntOrNull() ?: 10
    }

    private suspend fun generateSchedule(templateIds: List<Long>, weeks: Int): String {
        return generateScheduleWithProgression(templateIds, weeks, completedToday = null)
    }

    /**
     * Generates a phased schedule with progression support.
     * @param completedToday index of the day completed today (0-based), or null
     * @param progressionText raw text containing phase/progression info
     */
    suspend fun generateScheduleWithProgression(
        templateIds: List<Long>,
        weeks: Int,
        completedToday: Int? = null,
        progressionText: String = ""
    ): String {
        // Parse phases from text
        val phases = parsePhases(progressionText, weeks)

        // Schedule pattern: workouts spread across the week with rest days
        val daysPattern = when (templateIds.size) {
            2 -> listOf(0, 2) // Mon, Wed
            3 -> listOf(0, 2, 4) // Mon, Wed, Fri
            4 -> listOf(0, 1, 3, 4) // Mon, Tue, Thu, Fri
            5 -> listOf(0, 1, 2, 3, 4) // Mon-Fri
            6 -> listOf(0, 1, 2, 3, 4, 5) // Mon-Sat
            else -> (0 until templateIds.size.coerceAtMost(7)).toList()
        }

        val today = java.time.LocalDate.now()
        val todayDow = today.dayOfWeek.value - 1 // 0=Mon, 6=Sun

        // Figure out this week's Monday
        val thisMonday = today.minusDays(todayDow.toLong())

        // If completedToday is set, scheduling starts from today's position
        // Otherwise start from next Monday
        val startDate = if (completedToday != null) thisMonday else
            today.with(java.time.temporal.TemporalAdjusters.next(java.time.DayOfWeek.MONDAY))

        var scheduledCount = 0
        val phaseLabels = mutableListOf<String>()

        for (week in 0 until weeks) {
            val weekStart = startDate.plusWeeks(week.toLong())
            val phase = phases.find { week + 1 in it.weekRange }
            val phaseLabel = phase?.label

            for ((patternIndex, dayOffset) in daysPattern.withIndex()) {
                val templateIndex = patternIndex % templateIds.size
                val date = weekStart.plusDays(dayOffset.toLong())

                // Skip dates in the past (before today), except for completedToday
                if (date.isBefore(today)) continue

                val millis = date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                val isToday = date.isEqual(today) && completedToday != null && templateIndex == completedToday

                val label = buildString {
                    if (phaseLabel != null) append(phaseLabel)
                }

                repository.insertScheduledWorkout(
                    com.workout.tracker.data.entity.ScheduledWorkout(
                        templateId = templateIds[templateIndex],
                        scheduledDate = millis,
                        isCompleted = isToday,
                        label = label.ifEmpty { null }
                    )
                )
                scheduledCount++
            }

            // Add rest days for 4-day split
            if (templateIds.size == 4) {
                for (restDay in listOf(2, 5, 6)) { // Wed, Sat, Sun
                    val date = weekStart.plusDays(restDay.toLong())
                    if (date.isBefore(today)) continue
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

            if (phaseLabel != null && phaseLabel !in phaseLabels) phaseLabels.add(phaseLabel)
        }

        val phaseSummary = if (phaseLabels.isNotEmpty()) {
            "\nPhases: ${phaseLabels.joinToString(", ")}"
        } else ""
        val completedNote = if (completedToday != null) "\nDay ${completedToday + 1} marked completed for today." else ""

        return "Scheduled $scheduledCount workouts across $weeks weeks (starting ${startDate})$phaseSummary$completedNote"
    }

    data class Phase(val label: String, val weekRange: IntRange)

    private fun parsePhases(text: String, totalWeeks: Int): List<Phase> {
        if (text.isBlank()) return emptyList()
        val phases = mutableListOf<Phase>()

        // Match patterns like "Phase 1, Weeks 1-3 -- Foundation" or "Week 10 -- Deload"
        val phaseRegex = Regex(
            "(?:Phase\\s*\\d+,?\\s*)?Weeks?\\s*(\\d+)(?:\\s*-\\s*(\\d+))?\\s*[-–—:]+\\s*(.+)",
            RegexOption.IGNORE_CASE
        )
        for (line in text.lines()) {
            val match = phaseRegex.find(line.trim()) ?: continue
            val startWeek = match.groupValues[1].toIntOrNull() ?: continue
            val endWeek = match.groupValues[2].toIntOrNull() ?: startWeek
            val label = match.groupValues[3].trim().split(Regex("[.:]"))[0].trim()
            phases.add(Phase(label, startWeek..endWeek))
        }
        return phases
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
