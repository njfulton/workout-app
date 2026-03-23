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

    val savedRoutines: StateFlow<List<com.workout.tracker.data.entity.SavedRoutine>> =
        repository.getAllSavedRoutines()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clearImportResult() {
        _importResult.value = null
    }

    // Phase 1: Parse text and return routine info for user configuration
    data class ParsedRoutineInfo(
        val name: String,
        val exerciseCount: Int
    )
    data class ParseResult(
        val routines: List<ParsedRoutineInfo>,
        val detectedWeeks: Int,
        val defaultDayAssignments: Map<Int, List<java.time.DayOfWeek>>
    )

    private var _lastParsedText: String? = null
    private var _lastParsedRoutines: List<ParsedRoutine>? = null

    fun parseRoutineText(text: String): ParseResult? {
        val routineBlocks = splitIntoRoutineBlocks(text)
        if (routineBlocks.isEmpty()) return null

        val parsedRoutines = routineBlocks.map { parseRoutineBlock(it) }
            .filter { it.exercises.isNotEmpty() }
        if (parsedRoutines.isEmpty()) return null

        _lastParsedText = text
        _lastParsedRoutines = parsedRoutines

        val weeks = detectWeekCount(text)
        val defaultDays = getDefaultDayAssignments(parsedRoutines.size)

        return ParseResult(
            routines = parsedRoutines.map { ParsedRoutineInfo(it.name, it.exercises.size) },
            detectedWeeks = weeks,
            defaultDayAssignments = defaultDays
        )
    }

    fun getDefaultDayAssignmentsPublic(routineCount: Int) = getDefaultDayAssignments(routineCount)

    private fun getDefaultDayAssignments(routineCount: Int): Map<Int, List<java.time.DayOfWeek>> {
        val patterns = when (routineCount) {
            1 -> listOf(listOf(java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.WEDNESDAY, java.time.DayOfWeek.FRIDAY))
            2 -> listOf(
                listOf(java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.THURSDAY),
                listOf(java.time.DayOfWeek.TUESDAY, java.time.DayOfWeek.FRIDAY)
            )
            3 -> listOf(
                listOf(java.time.DayOfWeek.MONDAY),
                listOf(java.time.DayOfWeek.WEDNESDAY),
                listOf(java.time.DayOfWeek.FRIDAY)
            )
            4 -> listOf(
                listOf(java.time.DayOfWeek.MONDAY),
                listOf(java.time.DayOfWeek.TUESDAY),
                listOf(java.time.DayOfWeek.THURSDAY),
                listOf(java.time.DayOfWeek.FRIDAY)
            )
            5 -> listOf(
                listOf(java.time.DayOfWeek.MONDAY),
                listOf(java.time.DayOfWeek.TUESDAY),
                listOf(java.time.DayOfWeek.WEDNESDAY),
                listOf(java.time.DayOfWeek.THURSDAY),
                listOf(java.time.DayOfWeek.FRIDAY)
            )
            6 -> listOf(
                listOf(java.time.DayOfWeek.MONDAY),
                listOf(java.time.DayOfWeek.TUESDAY),
                listOf(java.time.DayOfWeek.WEDNESDAY),
                listOf(java.time.DayOfWeek.THURSDAY),
                listOf(java.time.DayOfWeek.FRIDAY),
                listOf(java.time.DayOfWeek.SATURDAY)
            )
            else -> (0 until routineCount).map { listOf(java.time.DayOfWeek.of((it % 7) + 1)) }
        }
        return patterns.mapIndexed { index, days -> index to days }.toMap()
    }

    // Phase 2: Import with user-configured settings
    fun importWithSchedule(
        startDate: java.time.LocalDate,
        dayAssignments: Map<Int, List<java.time.DayOfWeek>>,
        weeks: Int,
        routineName: String? = null
    ) {
        val parsedRoutines = _lastParsedRoutines ?: return
        val text = _lastParsedText ?: return
        viewModelScope.launch {
            try {
                val result = createTemplatesAndSchedule(parsedRoutines, text, startDate, dayAssignments, weeks)

                // Save the routine for future re-import
                val dayAssignmentsJson = buildDayAssignmentsJson(dayAssignments)
                val routineNamesJson = buildRoutineNamesJson(parsedRoutines)
                val name = routineName
                    ?: if (parsedRoutines.size == 1) parsedRoutines[0].name
                    else "${parsedRoutines.size}-Day Program"

                val savedRoutine = com.workout.tracker.data.entity.SavedRoutine(
                    name = name,
                    rawText = text,
                    dayAssignmentsJson = dayAssignmentsJson,
                    weekCount = weeks,
                    routineNamesJson = routineNamesJson
                )
                val savedId = repository.insertSavedRoutine(savedRoutine)

                // Record this usage
                val startMillis = startDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                val endDate = startDate.plusWeeks(weeks.toLong())
                val endMillis = endDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                repository.insertRoutineUsageHistory(
                    com.workout.tracker.data.entity.RoutineUsageHistory(
                        savedRoutineId = savedId,
                        startDate = startMillis,
                        endDate = endMillis
                    )
                )

                _importResult.value = result
            } catch (e: Throwable) {
                _importResult.value = "Import failed: ${e.message}"
            }
        }
    }

    // Re-import a saved routine
    fun reimportSavedRoutine(
        savedRoutine: com.workout.tracker.data.entity.SavedRoutine,
        startDate: java.time.LocalDate,
        dayAssignments: Map<Int, List<java.time.DayOfWeek>>,
        weeks: Int
    ) {
        viewModelScope.launch {
            try {
                // Parse the saved text
                val routineBlocks = splitIntoRoutineBlocks(savedRoutine.rawText)
                val parsedRoutines = routineBlocks.map { parseRoutineBlock(it) }
                    .filter { it.exercises.isNotEmpty() }

                val result = createTemplatesAndSchedule(parsedRoutines, savedRoutine.rawText, startDate, dayAssignments, weeks)

                // Record usage
                val startMillis = startDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                val endDate = startDate.plusWeeks(weeks.toLong())
                val endMillis = endDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                repository.insertRoutineUsageHistory(
                    com.workout.tracker.data.entity.RoutineUsageHistory(
                        savedRoutineId = savedRoutine.id,
                        startDate = startMillis,
                        endDate = endMillis
                    )
                )

                _importResult.value = result
            } catch (e: Throwable) {
                _importResult.value = "Import failed: ${e.message}"
            }
        }
    }

    fun updateSavedRoutineNotes(routineId: Long, notes: String) {
        viewModelScope.launch {
            val routine = repository.getSavedRoutineById(routineId) ?: return@launch
            repository.updateSavedRoutine(routine.copy(notes = notes))
        }
    }

    fun deleteSavedRoutine(routine: com.workout.tracker.data.entity.SavedRoutine) {
        viewModelScope.launch {
            repository.deleteSavedRoutine(routine)
        }
    }

    fun buildRoutineFromTemplates(
        routineName: String,
        templateIds: List<Long>,
        templateNames: List<String>,
        dayAssignments: Map<Int, List<java.time.DayOfWeek>>,
        weeks: Int,
        startDate: java.time.LocalDate,
        clearFutureFirst: Boolean,
        deloadEveryNWeeks: Int? = null
    ) {
        viewModelScope.launch {
            try {
                if (clearFutureFirst) {
                    repository.clearFutureSchedule()
                }

                val today = java.time.LocalDate.now()
                var scheduledCount = 0

                var deloadWeekCount = 0
                for (week in 1..weeks) {
                    val isDeloadWeek = deloadEveryNWeeks != null && week > 1 && week % deloadEveryNWeeks == 0
                    if (isDeloadWeek) deloadWeekCount++

                    val weekMonday = startDate.plusWeeks((week - 1).toLong())
                        .with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))

                    for ((routineIndex, templateId) in templateIds.withIndex()) {
                        val days = dayAssignments[routineIndex] ?: continue
                        for (dow in days) {
                            val date = weekMonday.with(dow)
                            if (week == 1 && date.isBefore(startDate)) continue
                            if (date.isBefore(today)) continue

                            val millis = date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                            repository.insertScheduledWorkout(
                                com.workout.tracker.data.entity.ScheduledWorkout(
                                    templateId = templateId,
                                    scheduledDate = millis,
                                    label = if (isDeloadWeek) "DELOAD" else null
                                )
                            )
                            scheduledCount++
                        }
                    }
                }

                // Save the routine for future re-use
                val dayAssignmentsJson = buildString {
                    append("{")
                    append(dayAssignments.entries.joinToString(",") { (k, v) ->
                        "\"$k\":[${v.joinToString(",") { "\"${it.name}\"" }}]"
                    })
                    append("}")
                }
                val routineNamesJson = "[${templateNames.joinToString(",") { "\"${it.replace("\"", "\\\"")}\"" }}]"

                val savedRoutine = SavedRoutine(
                    name = routineName,
                    rawText = "",
                    dayAssignmentsJson = dayAssignmentsJson,
                    weekCount = weeks,
                    routineNamesJson = routineNamesJson
                )
                val savedId = repository.insertSavedRoutine(savedRoutine)

                val startMillis = startDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                val endDate = startDate.plusWeeks(weeks.toLong())
                val endMillis = endDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                repository.insertRoutineUsageHistory(
                    RoutineUsageHistory(
                        savedRoutineId = savedId,
                        startDate = startMillis,
                        endDate = endMillis
                    )
                )

                val deloadMsg = if (deloadEveryNWeeks != null && deloadWeekCount > 0) " ($deloadWeekCount deload weeks)" else ""
                _importResult.value = "Scheduled $scheduledCount workouts across $weeks weeks$deloadMsg for \"$routineName\""
            } catch (e: Throwable) {
                _importResult.value = "Failed to build routine: ${e.message}"
            }
        }
    }

    fun getUsageHistory(routineId: Long): Flow<List<com.workout.tracker.data.entity.RoutineUsageHistory>> {
        return repository.getRoutineUsageHistory(routineId)
    }

    private fun buildDayAssignmentsJson(dayAssignments: Map<Int, List<java.time.DayOfWeek>>): String {
        val entries = dayAssignments.entries.joinToString(",") { (k, v) ->
            "\"$k\":[${v.joinToString(",") { "\"${it.name}\"" }}]"
        }
        return "{$entries}"
    }

    private fun buildRoutineNamesJson(routines: List<ParsedRoutine>): String {
        return "[${routines.joinToString(",") { "\"${it.name.replace("\"", "\\\"")}\"" }}]"
    }

    fun parseDayAssignmentsJson(json: String): Map<Int, List<java.time.DayOfWeek>> {
        val result = mutableMapOf<Int, List<java.time.DayOfWeek>>()
        // Simple JSON parsing: {"0":["MONDAY","THURSDAY"],"1":["TUESDAY"]}
        val entryPattern = Regex("\"(\\d+)\":\\[([^]]*)]")
        for (match in entryPattern.findAll(json)) {
            val key = match.groupValues[1].toInt()
            val daysStr = match.groupValues[2]
            val days = Regex("\"(\\w+)\"").findAll(daysStr)
                .map { java.time.DayOfWeek.valueOf(it.groupValues[1]) }
                .toList()
            result[key] = days
        }
        return result
    }

    fun parseRoutineNamesJson(json: String): List<String> {
        return Regex("\"([^\"]+)\"").findAll(json).map { it.groupValues[1] }.toList()
    }

    // Legacy import (no config step)
    fun importRoutineFromText(text: String, completedTodayIndex: Int? = null) {
        viewModelScope.launch {
            try {
                val result = parseAndImportRoutinesLegacy(text, completedTodayIndex)
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

    private suspend fun createTemplatesAndSchedule(
        parsedRoutines: List<ParsedRoutine>,
        text: String,
        startDate: java.time.LocalDate,
        dayAssignments: Map<Int, List<java.time.DayOfWeek>>,
        weeks: Int
    ): String {
        val progressionText = extractProgressionText(text)
        val fullProgressionText = extractFullProgressionText(text)
        val phases = parsePhasesWithModifications(fullProgressionText, weeks)
        val results = mutableListOf<String>()

        // Create base templates (used for phases with no structural changes)
        val baseTemplateIds = mutableListOf<Long>()
        for (routine in parsedRoutines) {
            val templateId = createTemplateFromParsed(routine)
            baseTemplateIds.add(templateId)
            results.add("\"${routine.name}\" (${routine.exercises.size} exercises)")
        }

        // Determine which phases need variant templates
        // Group phases by their modification signature to avoid duplicate templates
        data class ModSignature(val anchorSets: Int?, val accessorySets: Int?, val affectedDays: List<Int>?)
        val modPhases = phases.filter { it.anchorSetsOverride != null || it.accessorySetsOverride != null }
        val signatureToPhases = modPhases.groupBy {
            ModSignature(it.anchorSetsOverride, it.accessorySetsOverride, it.affectedDays)
        }

        // Create variant templates for each unique modification
        // Map: phaseLabel -> list of templateIds (parallel to baseTemplateIds)
        val phaseTemplateMap = mutableMapOf<String, List<Long>>()

        // All unmodified phases use base templates
        for (phase in phases) {
            if (phase.anchorSetsOverride == null && phase.accessorySetsOverride == null) {
                phaseTemplateMap[phase.label] = baseTemplateIds
            }
        }

        for ((sig, sigPhases) in signatureToPhases) {
            val variantIds = mutableListOf<Long>()
            for ((routineIndex, routine) in parsedRoutines.withIndex()) {
                val needsModification = sig.affectedDays == null || routineIndex in sig.affectedDays
                if (needsModification && (sig.anchorSets != null || sig.accessorySets != null)) {
                    val modifiedRoutine = applyPhaseModifications(
                        routine, sig.anchorSets, sig.accessorySets,
                        sigPhases.first().label
                    )
                    val templateId = createTemplateFromParsed(modifiedRoutine)
                    variantIds.add(templateId)
                    results.add("\"${modifiedRoutine.name}\" (${modifiedRoutine.exercises.size} exercises)")
                } else {
                    variantIds.add(baseTemplateIds[routineIndex])
                }
            }
            for (phase in sigPhases) {
                phaseTemplateMap[phase.label] = variantIds
            }
        }

        // Build week-to-templateIds mapping
        val weekTemplateMap = mutableMapOf<Int, List<Long>>()
        for (week in 1..weeks) {
            val phase = phases.find { week in it.weekRange }
            weekTemplateMap[week] = if (phase != null) {
                phaseTemplateMap[phase.label] ?: baseTemplateIds
            } else {
                baseTemplateIds
            }
        }

        val scheduleInfo = generateScheduleWithPhaseTemplates(
            weekTemplateMap, weeks, startDate, dayAssignments, phases
        )

        val templateCount = (baseTemplateIds + phaseTemplateMap.values.flatten()).distinct().size
        val summary = "Imported ${templateCount} template(s):\n${results.joinToString("\n") { "  - $it" }}"
        return "$summary\n\n$scheduleInfo"
    }

    private fun applyPhaseModifications(
        routine: ParsedRoutine,
        anchorSetsOverride: Int?,
        accessorySetsOverride: Int?,
        phaseLabel: String
    ): ParsedRoutine {
        val modifiedExercises = routine.exercises.mapIndexed { index, ex ->
            val isAnchor = index == 0 // First exercise is the anchor/main lift
            when {
                isAnchor && anchorSetsOverride != null -> ex.copy(sets = anchorSetsOverride)
                !isAnchor && accessorySetsOverride != null -> ex.copy(sets = accessorySetsOverride)
                else -> ex
            }
        }
        return routine.copy(
            name = "${routine.name} ($phaseLabel)",
            exercises = modifiedExercises
        )
    }

    private suspend fun parseAndImportRoutinesLegacy(text: String, completedTodayIndex: Int? = null): String {
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

        var scheduleInfo = ""
        if (createdTemplateIds.size >= 2) {
            val weeks = detectWeekCount(text)
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

        // Strip trailing progression/phase text from the body before parsing exercises
        bodyText = stripProgressionText(bodyText)

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

    private fun stripProgressionText(body: String): String {
        // Find where progression/phase text begins and cut it off
        val markers = listOf(
            Regex("\\d+-?\\s*Week\\s+Progression", RegexOption.IGNORE_CASE),
            Regex("Progression\\s+Framework", RegexOption.IGNORE_CASE)
        )
        var cutoff = body.length
        for (marker in markers) {
            val match = marker.find(body)
            if (match != null && match.range.first < cutoff) {
                cutoff = match.range.first
            }
        }
        return body.substring(0, cutoff).trim()
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
        val result = mutableListOf<String>()

        // Split on the pattern "rest Xs" or "rest X min" — each exercise ends with a rest time
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

    private suspend fun generateScheduleWithPhaseTemplates(
        weekTemplateMap: Map<Int, List<Long>>,
        weeks: Int,
        startDate: java.time.LocalDate,
        dayAssignments: Map<Int, List<java.time.DayOfWeek>>,
        phases: List<Phase>
    ): String {
        val today = java.time.LocalDate.now()
        var scheduledCount = 0
        val phaseLabels = mutableListOf<String>()

        for (week in 1..weeks) {
            val weekMonday = startDate.plusWeeks((week - 1).toLong())
                .with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
            val phase = phases.find { week in it.weekRange }
            val phaseLabel = phase?.label
            val templateIds = weekTemplateMap[week] ?: continue

            for ((routineIndex, templateId) in templateIds.withIndex()) {
                val days = dayAssignments[routineIndex] ?: continue
                for (dow in days) {
                    val date = weekMonday.with(dow)
                    if (week == 1 && date.isBefore(startDate)) continue
                    if (date.isBefore(today)) continue

                    val millis = date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                    repository.insertScheduledWorkout(
                        com.workout.tracker.data.entity.ScheduledWorkout(
                            templateId = templateId,
                            scheduledDate = millis,
                            label = phaseLabel
                        )
                    )
                    scheduledCount++
                }
            }

            if (phaseLabel != null && phaseLabel !in phaseLabels) phaseLabels.add(phaseLabel)
        }

        val phaseSummary = if (phaseLabels.isNotEmpty()) {
            "\nPhases: ${phaseLabels.joinToString(", ")}"
        } else ""

        return "Scheduled $scheduledCount workouts across $weeks weeks (starting $startDate)$phaseSummary"
    }

    private suspend fun generateScheduleWithDayAssignments(
        templateIds: List<Long>,
        weeks: Int,
        startDate: java.time.LocalDate,
        dayAssignments: Map<Int, List<java.time.DayOfWeek>>,
        progressionText: String = ""
    ): String {
        val phases = parsePhases(progressionText, weeks)
        val today = java.time.LocalDate.now()
        var scheduledCount = 0
        val phaseLabels = mutableListOf<String>()

        for (week in 0 until weeks) {
            val weekMonday = startDate.plusWeeks(week.toLong())
                .with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
            val phase = phases.find { week + 1 in it.weekRange }
            val phaseLabel = phase?.label

            for ((routineIndex, templateId) in templateIds.withIndex()) {
                val days = dayAssignments[routineIndex] ?: continue
                for (dow in days) {
                    val date = weekMonday.with(dow)
                    // For the first week, only schedule from startDate onward
                    if (week == 0 && date.isBefore(startDate)) continue
                    if (date.isBefore(today)) continue

                    val millis = date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                    val label = if (phaseLabel != null) phaseLabel else null

                    repository.insertScheduledWorkout(
                        com.workout.tracker.data.entity.ScheduledWorkout(
                            templateId = templateId,
                            scheduledDate = millis,
                            label = label
                        )
                    )
                    scheduledCount++
                }
            }

            if (phaseLabel != null && phaseLabel !in phaseLabels) phaseLabels.add(phaseLabel)
        }

        val phaseSummary = if (phaseLabels.isNotEmpty()) {
            "\nPhases: ${phaseLabels.joinToString(", ")}"
        } else ""

        return "Scheduled $scheduledCount workouts across $weeks weeks (starting $startDate)$phaseSummary"
    }

    data class Phase(
        val label: String,
        val weekRange: IntRange,
        val anchorSetsOverride: Int? = null,
        val accessorySetsOverride: Int? = null,
        val affectedDays: List<Int>? = null // null = all days
    )

    private fun parsePhases(text: String, totalWeeks: Int): List<Phase> {
        return parsePhasesWithModifications(text, totalWeeks)
    }

    private fun parsePhasesWithModifications(text: String, totalWeeks: Int): List<Phase> {
        if (text.isBlank()) return emptyList()
        val phases = mutableListOf<Phase>()

        // Split into phase blocks — each starts with a week/phase header
        val phaseRegex = Regex(
            "(?:Phase\\s*\\d+,?\\s*)?Weeks?\\s*(\\d+)(?:\\s*-\\s*(\\d+))?\\s*[-–—:]+\\s*(.+)",
            RegexOption.IGNORE_CASE
        )

        for (match in phaseRegex.findAll(text)) {
            val startWeek = match.groupValues[1].toIntOrNull() ?: continue
            val endWeek = match.groupValues[2].toIntOrNull() ?: startWeek
            val fullText = match.groupValues[3].trim()
            val label = fullText.split(Regex("[.:]"))[0].trim()

            // Parse structural modifications from the phase description
            var anchorSetsOverride: Int? = null
            var accessorySetsOverride: Int? = null
            var affectedDays: List<Int>? = null

            val lowerText = fullText.lowercase()

            // "anchor sets to N" or "anchor sets to NxM"
            val anchorSetsMatch = Regex("anchor\\s+sets\\s+to\\s+(\\d+)", RegexOption.IGNORE_CASE).find(fullText)
            if (anchorSetsMatch != null) {
                anchorSetsOverride = anchorSetsMatch.groupValues[1].toIntOrNull()
            }
            // "Drop anchor sets to NxM"
            val dropAnchorMatch = Regex("(?:drop|reduce|cut)\\s+anchor\\s+sets\\s+to\\s+(\\d+)", RegexOption.IGNORE_CASE).find(fullText)
            if (dropAnchorMatch != null) {
                anchorSetsOverride = dropAnchorMatch.groupValues[1].toIntOrNull()
            }
            // "Increase anchor sets to N"
            val increaseAnchorMatch = Regex("(?:increase|raise|bump)\\s+anchor\\s+sets\\s+to\\s+(\\d+)", RegexOption.IGNORE_CASE).find(fullText)
            if (increaseAnchorMatch != null) {
                anchorSetsOverride = increaseAnchorMatch.groupValues[1].toIntOrNull()
            }

            // "Accessories cut to N sets" or "Accessories... N sets"
            val accessorySetsMatch = Regex("accessor(?:ies|y)\\s+(?:cut\\s+to|drop\\s+to|at|stay\\s+at)?\\s*(\\d+)\\s+sets", RegexOption.IGNORE_CASE).find(fullText)
            if (accessorySetsMatch != null) {
                accessorySetsOverride = accessorySetsMatch.groupValues[1].toIntOrNull()
            }

            // "on Day 1 and Day 2" or "on Day 1, Day 2"
            val dayPattern = Regex("on\\s+Day\\s+(\\d+)(?:\\s*(?:and|,)\\s*Day\\s+(\\d+))*", RegexOption.IGNORE_CASE)
            val dayMatch = dayPattern.find(fullText)
            if (dayMatch != null) {
                val days = Regex("Day\\s+(\\d+)", RegexOption.IGNORE_CASE).findAll(fullText)
                    .map { (it.groupValues[1].toIntOrNull() ?: 1) - 1 } // 0-indexed
                    .toList()
                if (days.isNotEmpty()) affectedDays = days
            }

            phases.add(Phase(label, startWeek..endWeek, anchorSetsOverride, accessorySetsOverride, affectedDays))
        }

        return phases
    }

    private fun extractFullProgressionText(text: String): String {
        // Extract everything after the routines — the full progression section
        val markers = listOf(
            Regex("\\d+-?\\s*Week\\s+Progression", RegexOption.IGNORE_CASE),
            Regex("Progression\\s+Framework", RegexOption.IGNORE_CASE),
            Regex("Phase\\s+1", RegexOption.IGNORE_CASE)
        )
        for (marker in markers) {
            val match = marker.find(text)
            if (match != null) {
                return text.substring(match.range.first).trim()
            }
        }
        return ""
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
