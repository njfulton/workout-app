package com.workout.tracker.data

import android.content.Context
import android.util.Log
import com.workout.tracker.data.entity.*
import com.workout.tracker.data.repository.WorkoutRepository

class JefitImporter(
    private val context: Context,
    private val repository: WorkoutRepository
) {

    companion object {
        private const val TAG = "JefitImporter"
    }

    suspend fun importFromAssets(filename: String = "jefit_backup.csv") {
        val content = context.assets.open(filename).bufferedReader().use { it.readText() }
        importData(content)
    }

    private suspend fun importData(content: String) {
        val sections = parseSections(content)

        // Step 1: Import exercises (from exercise logs - these are the ones actually used)
        val exerciseNameMap = importExercises(sections)
        Log.d(TAG, "Imported ${exerciseNameMap.size} exercises")

        // Step 2: Import workout sessions and their exercise logs
        val sessionCount = importWorkoutSessions(sections, exerciseNameMap)
        Log.d(TAG, "Imported $sessionCount workout sessions")
    }

    private fun parseSections(content: String): Map<String, String> {
        val sections = mutableMapOf<String, String>()
        val sectionPattern = Regex("### (.+?) #+")
        val parts = content.split(Regex("(?=### )"))

        for (part in parts) {
            val match = sectionPattern.find(part)
            if (match != null) {
                val sectionName = match.groupValues[1].trim()
                sections[sectionName] = part.substringAfter('\n')
            }
        }
        return sections
    }

    private suspend fun importExercises(sections: Map<String, String>): Map<String, Long> {
        val exerciseNameToId = mutableMapOf<String, Long>()

        // Parse exercise logs to find all unique exercises with their JEFIT body part
        val exerciseLogSection = sections["EXERCISE LOGS"] ?: return exerciseNameToId
        val exerciseInfoMap = mutableMapOf<String, ExerciseInfo>()

        parseExerciseLogRows(exerciseLogSection) { row ->
            if (row.size >= 9) {
                val ename = row[8].trim()
                if (ename.isNotEmpty() && ename !in exerciseInfoMap) {
                    exerciseInfoMap[ename] = ExerciseInfo(name = ename)
                }
            }
        }

        // Also parse custom exercises for body part info
        val customSection = sections["CUSTOM EXERCISES"] ?: ""
        parseCustomExercises(customSection, exerciseInfoMap)

        // Insert each exercise, checking for existing ones first
        for ((name, info) in exerciseInfoMap) {
            val existing = repository.getExerciseByName(name)
            if (existing != null) {
                exerciseNameToId[name] = existing.id
            } else {
                // Try to match common name variations
                val matchedExisting = findExistingExercise(name)
                if (matchedExisting != null) {
                    exerciseNameToId[name] = matchedExisting.id
                } else {
                    val exercise = Exercise(
                        name = name,
                        category = info.category,
                        muscleGroup = info.muscleGroup,
                        equipment = info.equipment,
                        isCustom = true
                    )
                    val id = repository.insertExercise(exercise)
                    exerciseNameToId[name] = id
                }
            }
        }
        return exerciseNameToId
    }

    private suspend fun findExistingExercise(jefitName: String): Exercise? {
        val nameMap = mapOf(
            // --- Identity mappings (seed names) ---
            "Push-Up" to "Push-Up",
            "Pull-Up" to "Pull-Up",
            "Chin-Up" to "Chin-Up",
            "Barbell Curl" to "Barbell Curl",
            "Cable Crunch" to "Cable Crunch",
            "Leg Extension" to "Leg Extension",
            "Leg Press" to "Leg Press",
            "Seated Calf Raise" to "Seated Calf Raise",
            "Running" to "Running",
            "Stationary Bike" to "Stationary Bike",
            "Jump Rope" to "Jump Rope",
            "Hanging Leg Raise" to "Hanging Leg Raise",
            "Dumbbell Fly" to "Dumbbell Fly",
            "Dumbbell Bench Press" to "Dumbbell Bench Press",
            "Plank" to "Plank",
            "Dumbbell Shoulder Press" to "Dumbbell Shoulder Press",
            "Russian Twist" to "Russian Twist",
            "Mountain Climber" to "Mountain Climber",

            // --- Push-Up variants ---
            "Push Up" to "Push-Up",
            "Bench Pushups" to "Push-Up",

            // --- Pull-Up / Chin-Up variants ---
            "Pullups" to "Pull-Up",
            "Pull Ups" to "Pull-Up",
            "Weighted Pull Ups" to "Pull-Up",
            "Chin Up" to "Chin-Up",

            // --- Bench Press ---
            "Barbell Bench Press" to "Bench Press",
            "Barbell Incline Bench Press" to "Incline Bench Press",
            "Barbell Decline Bench Press" to "Decline Bench Press",
            "Dumbbell Incline Bench Press" to "Incline Dumbbell Press",
            "Dumbbell One Arm Incline Chest Press" to "Incline Dumbbell Press",
            "Dumbbell One-Arm Incline Chest Press" to "Incline Dumbbell Press",
            "Hammer Strength MTS Incline" to "Incline Bench Press",
            "Barbell Bench Press (Close Grip)" to "Close Grip Bench Press",
            "Barbell Close Grip Bench Press" to "Close Grip Bench Press",
            "Smith Machine Close Grip Bench Press" to "Close Grip Bench Press",

            // --- Curls / Biceps ---
            "Dumbbell Bicep Curl" to "Dumbbell Curl",
            "Dumbbell Hammer Curl" to "Hammer Curl",
            "Dumbbell Hammer Curls" to "Hammer Curl",
            "Hammer Curls with Rope" to "Hammer Curl",
            "EZ-Bar Curl" to "Barbell Curl",
            "Barbell Bicep Curl (Wide Grip)" to "Barbell Curl",
            "Barbell Drag Curl" to "Barbell Curl",
            "Dumbbell Alternate Bicep Curl" to "Dumbbell Curl",
            "Dumbbell Alternating Bicep Curl" to "Dumbbell Curl",
            "Dumbbell Alternating Seated Curl" to "Dumbbell Curl",
            "Dumbbell Seated Bicep Curl" to "Dumbbell Curl",
            "Dumbbell Zottman Curl" to "Dumbbell Curl",
            "One Arm Standing Dumbbell Curl" to "Dumbbell Curl",
            "Alternate Seated Dumbbell Curl" to "Dumbbell Curl",
            "Alternate Hammer Curl" to "Hammer Curl",
            "Dumbbell Alternating Hammer Curl" to "Hammer Curl",
            "Dumbbell Concentration Curl" to "Concentration Curl",
            "Dumbbell Concentration Curls" to "Concentration Curl",
            "Dumbbell Preacher Curl" to "Preacher Curl",
            "Barbell Preacher Curl" to "Preacher Curl",
            "Dumbbell One-Arm Preacher Curl" to "Preacher Curl",
            "Alternate Dumbbell Preacher Curl" to "Preacher Curl",
            "Barbell Close Grip Preacher Curl" to "Preacher Curl",
            "Hammer Strength Preacher Curl" to "Preacher Curl",
            "Preacher Curl Machine" to "Preacher Curl",
            "Dumbbell Incline Bench Curl" to "Incline Dumbbell Curl",
            "Dumbbell Incline Curl" to "Incline Dumbbell Curl",
            "Dumbbell Alternate Incline Curl" to "Incline Dumbbell Curl",
            "Cable Standing Biceps Curl" to "Cable Curl",

            // --- Triceps ---
            "Cable Rope Triceps Pushdown" to "Tricep Pushdown",
            "Cable Rope Overhead Triceps Extension" to "Overhead Tricep Extension",
            "Cable Rope Overhead Tricep Extension" to "Overhead Tricep Extension",
            "Cable Tricep Pushdown" to "Tricep Pushdown",
            "Cable Triceps Pushdown" to "Tricep Pushdown",
            "Cable Standing Triceps Extension" to "Tricep Pushdown",
            "Reverse Grip Triceps Pushdown" to "Tricep Pushdown",
            "Barbell Lying Triceps Extension" to "Skull Crusher",
            "Barbell Lying Triceps Extension " to "Skull Crusher",
            "Barbell Lying Triceps Press" to "Skull Crusher",
            "Barbell Skull Crusher (Reverse Grip)" to "Skull Crusher",
            "Barbell Reverse Grip Skullcrusher" to "Skull Crusher",
            "EZ Bar Decline Close Grip Skull Crusher" to "Skull Crusher",
            "Barbell Seated Overhead Triceps Extension" to "Overhead Tricep Extension",
            "Barbell Triceps Extension" to "Overhead Tricep Extension",
            "Dumbbell One Arm Triceps Extension" to "Overhead Tricep Extension",
            "Dumbbell Standing Triceps Extension" to "Overhead Tricep Extension",
            "Dumbbell Tricep Extension" to "Overhead Tricep Extension",
            "Dumbbell Alternating Tricep Kickback" to "Overhead Tricep Extension",
            "Bench Dip" to "Tricep Dip",
            "Weighted Tricep Dips" to "Tricep Dip",
            "Weighted Tricep Dip" to "Tricep Dip",
            "Hammer Strength Seated Dip" to "Tricep Dip",
            "Dip" to "Chest Dip",
            "Machine Assisted Dip" to "Chest Dip",
            "Close Hand Pushup" to "Diamond Push-Up",

            // --- Shoulders ---
            "Barbell Military Press" to "Overhead Press",
            "Barbell Standing Military Press" to "Overhead Press",
            "Standing Military Press" to "Overhead Press",
            "Barbell Shoulder Press" to "Overhead Press",
            "Barbell Overhead Press" to "Overhead Press",
            "Barbell Alternating Press" to "Overhead Press",
            "Barbell Push Press Behind the Neck" to "Overhead Press",
            "Barbell Rear Press" to "Overhead Press",
            "Behind The Head Military Press" to "Overhead Press",
            "Barbell Upright Row" to "Upright Row",
            "Dumbbell Arnold Press" to "Arnold Press",
            "Alternate Seated Palms In Dumbbell Press" to "Arnold Press",
            "Dumbbell Lateral Raise" to "Lateral Raise",
            "Alternate Standing Dumbbell Lateral Raise" to "Lateral Raise",
            "Cable Lateral Raise" to "Lateral Raise",
            "Cable Standing Deltoid Raise" to "Lateral Raise",
            "Dumbbell Alternate Standing Lateral Raise" to "Lateral Raise",
            "Dumbbell One-Arm Lateral Raise" to "Lateral Raise",
            "Dumbbell Front Raise" to "Front Raise",
            "Dumbbell Alternating Front Raise" to "Front Raise",
            "Weight Plate Front Raise" to "Front Raise",
            "Alternate Seated Dumbbell Press" to "Dumbbell Shoulder Press",
            "Dumbbell Seated Shoulder Press" to "Dumbbell Shoulder Press",
            "Dumbbell Standing Press" to "Dumbbell Shoulder Press",
            "Machine Shoulder Press" to "Dumbbell Shoulder Press",

            // --- Rear Delts ---
            "Dumbbell Reverse Fly" to "Rear Delt Fly",
            "Dumbbell Bent-Over Reverse Fly" to "Rear Delt Fly",
            "Dumbbell Bent Over Reverse Fly" to "Rear Delt Fly",
            "Cable Reverse Fly" to "Rear Delt Fly",
            "Alternate Bent Over Dumbbell Reverse Fly" to "Rear Delt Fly",
            "Bent Over Dumbbell Reverse Fly" to "Rear Delt Fly",
            "Dumbbell Bent Over Delt Raise" to "Rear Delt Fly",
            "Dumbbell Bent Over Delt  Raise" to "Rear Delt Fly",
            "Dumbbell Bent-Over Raise" to "Rear Delt Fly",
            "Reverse Machine Flyes" to "Rear Delt Fly",

            // --- Back / Rows ---
            "Barbell Bent Over Row" to "Barbell Row",
            "Barbell Bent-Over Row" to "Barbell Row",
            "Barbell Body Row" to "Barbell Row",
            "Barbell Row (Underhand Grip)" to "Barbell Row",
            "Pendlay Row" to "Barbell Row",
            "One Arm Dumbell Row" to "Dumbbell Row",
            "One-Arm Dumbell Row" to "Dumbbell Row",
            "Dumbbell One-Arm Row" to "Dumbbell Row",
            "Dumbbell Bent Over Row" to "Dumbbell Row",
            "Dumbbell Bent-Over Row" to "Dumbbell Row",
            "Bent Over Two Dumbbell Row" to "Dumbbell Row",
            "Alternating Renegade Row" to "Dumbbell Row",
            "Dumbbell Incline Bench Row" to "Dumbbell Row",
            "Dumbbell Palm Rotational Row" to "Dumbbell Row",
            "Dumbbell Renegade Row" to "Dumbbell Row",
            "Cable Seated Row" to "Seated Cable Row",
            "Life Fitness Rows" to "Seated Cable Row",
            "Cable Row to Neck" to "Face Pull",
            "T Bar Row" to "T-Bar Row",
            "Full Range Of Motion Lat Pulldown" to "Lat Pulldown",
            "Wide Grip Lat Pulldown" to "Lat Pulldown",
            "Close Grip Front Lat Pulldown" to "Lat Pulldown",
            "Underhand Pull down" to "Lat Pulldown",
            "Underhand Pull Down" to "Lat Pulldown",
            "Machine Lat Pulldown" to "Lat Pulldown",
            "Cable Straight Arm Push Down" to "Lat Pulldown",

            // --- Chest Fly ---
            "Dumbbell Incline Fly" to "Dumbbell Fly",
            "Dumbbell Decline Fly" to "Cable Fly",
            "Cable Incline Fly" to "Cable Fly",
            "Cable Cross Over" to "Cable Fly",
            "Cable Cross-Over" to "Cable Fly",
            "Machine Fly" to "Pec Deck",

            // --- Squat / Legs ---
            "Barbell Full Squat" to "Squat",
            "Barbell Squat" to "Squat",
            "Barbell Bench Squat" to "Squat",
            "Barbell Hack Squat" to "Squat",
            "Barbell Wide Stance Squat" to "Squat",
            "Smith Machine Squat" to "Squat",
            "Barbell Front Squat" to "Front Squat",
            "Kettlebell Goblet Squat" to "Goblet Squat",
            "Dumbbell Bench Squat" to "Goblet Squat",
            "Dumbbell Plie Squat" to "Goblet Squat",
            "Pile Squat" to "Goblet Squat",
            "Dumbbell Squat" to "Goblet Squat",
            "Sit Squat" to "Bodyweight Squat",
            "Prisoner Squat" to "Bodyweight Squat",
            "Barbell One Leg Squat" to "Bulgarian Split Squat",
            "Dumbbell Bulgarian Split Squat" to "Bulgarian Split Squat",
            "Barbell Bulgarian Split Squat" to "Bulgarian Split Squat",
            "Leg Press with Wide Stance" to "Leg Press",
            "Life Fitness Seated Leg Press" to "Leg Press",
            "Leg Extensions" to "Leg Extension",
            "Hammer Strength Unilateral Leg Extensions" to "Leg Extension",

            // --- Deadlift / Hamstrings ---
            "Barbell Deadlift" to "Deadlift",
            "Dumbbell Deadlift" to "Deadlift",
            "Barbell Romanian Deadlift" to "Romanian Deadlift",
            "Dumbbell Romanian Deadlift" to "Romanian Deadlift",
            "Barbell Stiff-Leg Deadlift" to "Stiff Leg Deadlift",
            "Stiff-Legged Barbell Deadlift" to "Stiff Leg Deadlift",
            "Barbell Good Morning" to "Good Morning",
            "Lying Leg Curls" to "Leg Curl",
            "Prone Leg Curl" to "Leg Curl",
            "Seated Leg Curl" to "Leg Curl",
            "Kneeling Leg Curl" to "Leg Curl",
            "Dumbbell Hamstring Curl" to "Leg Curl",

            // --- Lunges ---
            "Dumbbell Lunges" to "Lunge",
            "Dumbbell Lunge" to "Lunge",
            "Dumbbell Walking Lunge" to "Lunge",
            "Dumbbell Reverse Lunge" to "Lunge",
            "Dumbbell Step Ups" to "Lunge",
            "Bodyweight Lunge" to "Lunge",
            "Rear Bodyweight Lunge" to "Lunge",
            "Crossover Reverse Lunge" to "Lunge",
            "Barbell Lunge" to "Lunge",

            // --- Glutes ---
            "Barbell Hip Thrust" to "Hip Thrust",
            "Barbell Glute Bridge" to "Glute Bridge",

            // --- Calves ---
            "Standing Calf Raises" to "Standing Calf Raise",
            "Standing Barbell Calf Raise" to "Standing Calf Raise",
            "Barbell Standing Calf Raise" to "Standing Calf Raise",
            "Barbell Seated Calf Raise" to "Seated Calf Raise",
            "Bodyweight Standing Calf Raise" to "Standing Calf Raise",
            "Calf Press On Leg Press" to "Standing Calf Raise",
            "Calf Press on Leg Press Machine" to "Standing Calf Raise",
            "Dumbbell Calf Raise" to "Standing Calf Raise",
            "Dumbbell Seated Calf Raise" to "Seated Calf Raise",
            "Floor Barbell Calf Raise" to "Standing Calf Raise",
            "Life Fitness Leg Press Calf Raises" to "Standing Calf Raise",

            // --- Abs / Core ---
            "Crunches" to "Crunch",
            "Sit Up" to "Crunch",
            "Sit-Up" to "Crunch",
            "Cross Body Crunch" to "Crunch",
            "Cross-Body Crunch" to "Crunch",
            "Decline Crunch" to "Crunch",
            "Exercise Ball Crunch" to "Crunch",
            "Oblique Crunches" to "Crunch",
            "Reverse Crunch" to "Crunch",
            "V-Up" to "Crunch",
            "Bench Leg Pull-In" to "Crunch",
            "Weighted Crunch" to "Crunch",
            "Weighted Decline Crunch" to "Crunch",
            "Weight Plate Decline Crunch" to "Crunch",
            "Ab Crunch Machine" to "Cable Crunch",
            "Hammer Strength Ab Crunch" to "Cable Crunch",
            "Barbell Ab Rollout" to "Ab Wheel Rollout",
            "Hanging Knee Raise" to "Hanging Leg Raise",
            "Weighted Hanging Knee Raise" to "Hanging Leg Raise",
            "Decline Bench Leg Raise" to "Hanging Leg Raise",
            "Knee Hip Raise On Parallel Bars" to "Hanging Leg Raise",
            "Leg Raise" to "Hanging Leg Raise",
            "Parallel Bar Hip Raise" to "Hanging Leg Raise",
            "Weight Plate Russian Twist" to "Russian Twist",
            "Weight Plate Twist" to "Russian Twist",
            "Decline Bench Weighted Twist" to "Russian Twist",
            "Rotational Crunch" to "Russian Twist",
            "Standing Bodyweight Twists" to "Russian Twist",
            "Cable Wood Chops" to "Russian Twist",
            "Wood Chops with Cable" to "Russian Twist",
            "Weight Plate Rotation" to "Russian Twist",
            "Weighted Decline Rotation" to "Russian Twist",
            "Forearm Plank with Hip Abduction" to "Plank",

            // --- Cardio ---
            "Treadmill Running" to "Treadmill",
            "Elliptical Training" to "Elliptical",
            "Rowing" to "Rowing Machine",
            "Spin" to "Stationary Bike",

            // --- Forearms ---
            "Seated Palm Up Barbell Wrist Curl" to "Wrist Curl",
            "Seated Palms Down Barbell Wrist Curl" to "Reverse Wrist Curl",

            // --- Misc ---
            "Barbell Shrug" to "Farmer's Walk",
            "Freehand Jump Squat" to "Box Jump",
            "Jump Squat" to "Box Jump",
        )
        val mappedName = nameMap[jefitName] ?: return null
        return repository.getExerciseByName(mappedName)
    }

    private fun parseCustomExercises(
        section: String,
        exerciseInfoMap: MutableMap<String, ExerciseInfo>
    ) {
        // header: row_id,USERID,TIMESTAMP,rating,name,description,image2,image1,bodypart,...
        val lines = section.lines()
        for (line in lines) {
            if (line.isEmpty() || !line[0].isDigit()) continue
            try {
                val row = parseCsvLine(line)
                if (row.size >= 10) {
                    val name = row[4].trim()
                    val bodypartCode = row[8].trim().toIntOrNull() ?: -1
                    if (name in exerciseInfoMap) {
                        val info = exerciseInfoMap[name]!!
                        exerciseInfoMap[name] = info.copy(
                            muscleGroup = mapJefitBodyPart(bodypartCode),
                            category = categorizeExercise(name, bodypartCode)
                        )
                    }
                }
            } catch (e: Exception) {
                // Skip malformed lines
            }
        }
    }

    private suspend fun importWorkoutSessions(
        sections: Map<String, String>,
        exerciseNameMap: Map<String, Long>
    ): Int {
        // Parse workout sessions
        val sessionSection = sections["WORKOUT SESSIONS"] ?: return 0
        val sessions = mutableListOf<WorkoutSessionData>()

        val lines = sessionSection.lines()
        for (line in lines) {
            if (line.isEmpty() || !line[0].isDigit()) continue
            try {
                val row = parseCsvLine(line)
                // header: rowid,_id,USERID,edit_time,day_id,total_time,workout_time,rest_time,
                //         wasted_time,total_exercise,total_weight,recordbreak,starttime,endtime,
                //         workout_mode,TIMESTAMP,calories,avg_heart_rate
                if (row.size >= 14 && row[2] == "941173") {
                    val sessionId = row[1].trim().toIntOrNull() ?: continue
                    val startTime = row[12].trim().toLongOrNull() ?: continue
                    val endTime = row[13].trim().toLongOrNull() ?: continue
                    val totalExercises = row[9].trim().toIntOrNull() ?: 0

                    if (totalExercises > 0 && startTime > 0) {
                        sessions.add(
                            WorkoutSessionData(
                                jefitId = sessionId,
                                startTime = startTime * 1000, // Convert seconds to millis
                                endTime = endTime * 1000
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error parsing workout session line: ${e.message}")
            }
        }

        // Parse exercise logs and group by session
        val exerciseLogSection = sections["EXERCISE LOGS"] ?: return 0
        val exerciseLogsBySession = mutableMapOf<Int, MutableList<ExerciseLogData>>()

        parseExerciseLogRows(exerciseLogSection) { row ->
            // header: USERID,TIMESTAMP,belongSys,logs,_id,record,mydate,eid,ename,
            //         day_item_id,belongsession,logTime,interval_logs,auto_generated
            if (row.size >= 11) {
                val ename = row[8].trim()
                val sessionId = row[10].trim().toIntOrNull() ?: return@parseExerciseLogRows
                val logsStr = row[3].trim()

                if (ename.isNotEmpty() && sessionId > 0) {
                    val sets = parseJefitLogSets(logsStr)
                    exerciseLogsBySession.getOrPut(sessionId) { mutableListOf() }
                        .add(ExerciseLogData(exerciseName = ename, sets = sets))
                }
            }
        }

        // Now insert workout sessions with their exercise logs
        var importedCount = 0
        for (session in sessions) {
            val exerciseLogs = exerciseLogsBySession[session.jefitId] ?: continue
            if (exerciseLogs.isEmpty()) continue

            // Determine workout type based on exercises
            val hasCardio = exerciseLogs.any { isCardioExercise(it.exerciseName) }
            val hasStrength = exerciseLogs.any { !isCardioExercise(it.exerciseName) }
            val workoutType = when {
                hasCardio && !hasStrength -> WorkoutType.CARDIO
                else -> WorkoutType.STRENGTH
            }

            // Build workout name from date
            val dateStr = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("America/New_York")
            }.format(java.util.Date(session.startTime))
            val workoutName = "JEFIT Workout - $dateStr"

            val workoutLogId = repository.insertWorkoutLog(
                WorkoutLog(
                    name = workoutName,
                    startTime = session.startTime,
                    endTime = session.endTime,
                    workoutType = workoutType
                )
            )

            for ((orderIndex, exLog) in exerciseLogs.withIndex()) {
                val exerciseId = exerciseNameMap[exLog.exerciseName] ?: continue

                val exerciseLogId = repository.insertExerciseLog(
                    ExerciseLog(
                        workoutLogId = workoutLogId,
                        exerciseId = exerciseId,
                        orderIndex = orderIndex
                    )
                )

                val setLogs = exLog.sets.mapIndexed { setIndex, setData ->
                    SetLog(
                        exerciseLogId = exerciseLogId,
                        setNumber = setIndex + 1,
                        reps = setData.reps,
                        weightLbs = if (setData.weight > 0.0) setData.weight else null,
                        durationSeconds = setData.durationSeconds,
                        distanceMiles = setData.distanceMiles
                    )
                }
                if (setLogs.isNotEmpty()) {
                    repository.insertSetLogs(setLogs)
                }
            }
            importedCount++
        }
        return importedCount
    }

    private fun parseJefitLogSets(logsStr: String): List<SetData> {
        // Format: "weight x reps" comma separated, e.g. "115.0x12,125.0x12,115.0x6"
        // Cardio format: "0x684,0x5.46,0x0,0x0,0x0" (calories, distance, time, laps, speed)
        if (logsStr.isBlank() || logsStr == "0x0") return emptyList()

        val parts = logsStr.replace("\"", "").split(",")
        val sets = mutableListOf<SetData>()

        for (part in parts) {
            val trimmed = part.trim()
            if (trimmed.isEmpty()) continue

            val xIndex = trimmed.indexOf('x')
            if (xIndex < 0) continue

            val weightStr = trimmed.substring(0, xIndex)
            val repsStr = trimmed.substring(xIndex + 1)

            val weight = weightStr.toDoubleOrNull() ?: 0.0
            val reps = repsStr.toDoubleOrNull()?.toInt()

            if (reps != null && reps > 0) {
                sets.add(SetData(weight = weight, reps = reps))
            }
        }
        return sets
    }

    private fun parseExerciseLogRows(section: String, handler: (List<String>) -> Unit) {
        val lines = section.lines()
        for (line in lines) {
            if (!line.startsWith("941173")) continue
            try {
                val row = parseCsvLine(line)
                handler(row)
            } catch (e: Exception) {
                // Skip malformed lines
            }
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false

        for (char in line) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current = StringBuilder()
                }
                else -> current.append(char)
            }
        }
        result.add(current.toString())
        return result
    }

    private fun isCardioExercise(name: String): Boolean {
        val cardioKeywords = listOf(
            "Running", "Treadmill", "Bike", "Elliptical", "Jump Rope",
            "Walking", "Cardio", "Spin", "Hiking", "Interval training"
        )
        return cardioKeywords.any { name.contains(it, ignoreCase = true) }
    }

    private fun mapJefitBodyPart(code: Int): MuscleGroup {
        return when (code) {
            0 -> MuscleGroup.ABS
            1 -> MuscleGroup.BACK
            2 -> MuscleGroup.BICEPS
            3 -> MuscleGroup.CHEST
            4 -> MuscleGroup.FOREARMS
            5 -> MuscleGroup.GLUTES
            6 -> MuscleGroup.SHOULDERS
            7 -> MuscleGroup.TRICEPS
            8 -> MuscleGroup.QUADRICEPS   // JEFIT "Legs"
            9 -> MuscleGroup.CALVES
            10 -> MuscleGroup.CARDIO
            else -> MuscleGroup.OTHER
        }
    }

    private fun categorizeExercise(name: String, bodypartCode: Int): ExerciseCategory {
        if (bodypartCode == 10 || isCardioExercise(name)) return ExerciseCategory.CARDIO
        val bodyweightKeywords = listOf(
            "Push-Up", "Push Up", "Pull-Up", "Pull Up", "Pullup", "Chin Up",
            "Dip", "Plank", "Crunch", "Sit Up", "Sit-Up", "Burpee",
            "Bodyweight", "Bridge", "Superman", "V-Up"
        )
        if (bodyweightKeywords.any { name.contains(it, ignoreCase = true) }) {
            return ExerciseCategory.BODYWEIGHT
        }
        val flexKeywords = listOf("Stretch", "Yoga", "Pose", "Child Pose")
        if (flexKeywords.any { name.contains(it, ignoreCase = true) }) {
            return ExerciseCategory.FLEXIBILITY
        }
        return ExerciseCategory.STRENGTH
    }

    private data class ExerciseInfo(
        val name: String,
        val muscleGroup: MuscleGroup = MuscleGroup.OTHER,
        val category: ExerciseCategory = ExerciseCategory.STRENGTH,
        val equipment: String? = null
    )

    private data class WorkoutSessionData(
        val jefitId: Int,
        val startTime: Long,
        val endTime: Long
    )

    private data class ExerciseLogData(
        val exerciseName: String,
        val sets: List<SetData>
    )

    private data class SetData(
        val weight: Double = 0.0,
        val reps: Int = 0,
        val durationSeconds: Int? = null,
        val distanceMiles: Double? = null
    )
}
