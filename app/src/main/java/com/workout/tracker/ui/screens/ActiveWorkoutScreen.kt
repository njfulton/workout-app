package com.workout.tracker.ui.screens

import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.workout.tracker.data.entity.PRType
import com.workout.tracker.data.entity.PersonalRecord
import com.workout.tracker.data.entity.SetLog
import com.workout.tracker.ui.navigation.Screen
import com.workout.tracker.ui.viewmodel.ActiveExercise
import com.workout.tracker.ui.viewmodel.ExerciseGroup
import com.workout.tracker.ui.viewmodel.ExerciseViewModel
import com.workout.tracker.ui.viewmodel.WorkoutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    navController: NavController,
    workoutViewModel: WorkoutViewModel,
    exerciseViewModel: ExerciseViewModel
) {
    val activeWorkout by workoutViewModel.activeWorkout.collectAsStateWithLifecycle()
    val restTimer by workoutViewModel.restTimerSeconds.collectAsStateWithLifecycle()
    val isTimerRunning by workoutViewModel.isTimerRunning.collectAsStateWithLifecycle()
    var showExercisePicker by remember { mutableStateOf(false) }
    var showFinishConfirm by remember { mutableStateOf(false) }
    var showExerciseList by remember { mutableStateOf(false) }
    val allExercisesCompleted by workoutViewModel.allExercisesCompleted.collectAsStateWithLifecycle()
    var showPRCelebration by remember { mutableStateOf<List<PersonalRecord>?>(null) }

    val context = LocalContext.current
    val groups = activeWorkout.groups
    val currentGroupIndex = activeWorkout.currentGroupIndex
    val currentGroup = activeWorkout.currentGroup

    // Keep screen on
    DisposableEffect(Unit) {
        val window = (context as? ComponentActivity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Play sound when timer finishes
    LaunchedEffect(Unit) {
        workoutViewModel.timerFinishedEvent.collect {
            workoutViewModel.playTimerSound(context)
        }
    }

    // PR celebration
    LaunchedEffect(Unit) {
        workoutViewModel.newPRs.collect { prs ->
            if (prs.isNotEmpty()) {
                showPRCelebration = prs
            }
        }
    }

    // Elapsed workout timer
    val startTime = activeWorkout.workoutLog?.startTime ?: System.currentTimeMillis()
    var elapsedSeconds by remember { mutableStateOf(0L) }
    LaunchedEffect(startTime) {
        while (true) {
            elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000
            kotlinx.coroutines.delay(1000)
        }
    }
    val elapsedMinutes = elapsedSeconds / 60
    val elapsedSecs = elapsedSeconds % 60
    val elapsedStr = "${elapsedMinutes}:${elapsedSecs.toString().padStart(2, '0')}"

    Scaffold(
        topBar = {
            // Top row: back + workout name/timer + Finish
            Surface(
                color = MaterialTheme.colorScheme.background,
                shadowElevation = if (isTimerRunning) 0.dp else 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { showExerciseList = true },
                        colors = IconButtonDefaults.outlinedIconButtonColors()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Exercise List")
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            (activeWorkout.workoutLog?.name ?: "Workout").uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp,
                            maxLines = 1
                        )
                        Text(
                            "$elapsedStr · ${currentGroupIndex + 1}/${groups.size}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    OutlinedButton(
                        onClick = { showFinishConfirm = true },
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(100)
                    ) {
                        Text("Finish", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
        floatingActionButton = {
            if (!isTimerRunning) {
                FloatingActionButton(onClick = { showExercisePicker = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Exercise")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            // Rest timer - full-screen overlay with ring
            if (isTimerRunning) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    ) {
                        // Rest pill
                        Surface(
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(100),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                        ) {
                            Text(
                                "REST",
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                letterSpacing = 1.sp
                            )
                        }

                        Spacer(Modifier.height(24.dp))
                        Text(
                            "${restTimer / 60}:${(restTimer % 60).toString().padStart(2, '0')}",
                            fontSize = 80.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "REMAINING",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )

                        Spacer(Modifier.height(24.dp))
                        OutlinedButton(
                            onClick = { workoutViewModel.skipRestTimer() },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(100),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Skip rest · start now", style = MaterialTheme.typography.titleMedium)
                        }

                        currentGroup?.let { group ->
                            val groupNotStartedYet = group.exercises.all { ex ->
                                ex.sets.none { !it.isWarmup }
                            }
                            if (groupNotStartedYet) {
                                Spacer(Modifier.height(24.dp))
                                UpNextCard(group)
                            }
                        }
                    }
                }
            } else if (groups.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Tap + to add exercises", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else if (currentGroup != null) {
                // Exercise pager: prev/name/next — pinned
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { workoutViewModel.previousGroup() },
                        enabled = currentGroupIndex > 0
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous exercise")
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            currentGroup.label,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                    IconButton(
                        onClick = { workoutViewModel.nextGroup() },
                        enabled = currentGroupIndex < groups.size - 1
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next exercise")
                    }
                }

                // Scrollable content below pager
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
                ) {
                    if (currentGroup.isSuperset) {
                        FocusedSupersetCard(
                            exercises = currentGroup.exercises,
                            isFromTemplate = activeWorkout.isFromTemplate,
                            onLogSet = { exerciseLogId, setNum, reps, weight, isWarmup ->
                                workoutViewModel.logSet(exerciseLogId, setNum, reps, weight, isWarmup)
                            },
                            onUpdateSet = { workoutViewModel.updateSet(it) },
                            onStartTimer = { workoutViewModel.startRestTimer(it) },
                            onMarkDone = { workoutViewModel.markExerciseDone(it) },
                            onTabChanged = { workoutViewModel.setSupersetTab(it) },
                            defaultRestSeconds = minOf(currentGroup.exercises.first().restSeconds, 60),
                            onRestSecondsChanged = { seconds ->
                                currentGroup.exercises.forEach { ex ->
                                    workoutViewModel.updateExerciseRestSeconds(ex.exerciseLogId, seconds)
                                }
                            },
                            onNoteChanged = { exerciseLogId, note ->
                                workoutViewModel.updateExerciseNote(exerciseLogId, note)
                            },
                            onOpenProgress = { exerciseId ->
                                navController.navigate(Screen.ExerciseProgress.createRoute(exerciseId))
                            }
                        )
                    } else {
                        FocusedExerciseCard(
                            activeExercise = currentGroup.exercises.first(),
                            isFromTemplate = activeWorkout.isFromTemplate,
                            onLogSet = { setNum, reps, weight, isWarmup ->
                                workoutViewModel.logSet(currentGroup.exercises.first().exerciseLogId, setNum, reps, weight, isWarmup)
                            },
                            onUpdateSet = { workoutViewModel.updateSet(it) },
                            onStartTimer = { workoutViewModel.startRestTimer(it) },
                            onMarkDone = { workoutViewModel.markExerciseDone(currentGroup.exercises.first().exerciseLogId) },
                            defaultRestSeconds = currentGroup.exercises.first().restSeconds,
                            onRestSecondsChanged = { workoutViewModel.updateExerciseRestSeconds(currentGroup.exercises.first().exerciseLogId, it) },
                            onNoteChanged = { workoutViewModel.updateExerciseNote(currentGroup.exercises.first().exerciseLogId, it) },
                            onOpenProgress = {
                                val ex = currentGroup.exercises.first().exercise
                                navController.navigate(Screen.ExerciseProgress.createRoute(ex.id))
                            }
                        )
                    }
                    // Up next preview
                    if (currentGroupIndex + 1 < groups.size) {
                        val nextGroup = groups[currentGroupIndex + 1]
                        Spacer(Modifier.height(16.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(18.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.FitnessCenter,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.width(14.dp))
                                Column {
                                    Text(
                                        "UP NEXT",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        letterSpacing = 1.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        nextGroup.label,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // All exercises complete banner
                    if (allExercisesCompleted) {
                        Spacer(Modifier.height(16.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { showFinishConfirm = true },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "All exercises complete!",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        "Tap to finish workout",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(80.dp))
                }
            }
        }
    }

    // Exercise list bottom sheet
    if (showExerciseList) {
        ExerciseListSheet(
            groups = groups,
            currentIndex = currentGroupIndex,
            onJumpTo = { index ->
                workoutViewModel.navigateToGroup(index)
                showExerciseList = false
            },
            onDismiss = { showExerciseList = false }
        )
    }

    if (showExercisePicker) {
        ExercisePickerDialog(
            exerciseViewModel = exerciseViewModel,
            onDismiss = { showExercisePicker = false },
            onSelect = { exercise ->
                workoutViewModel.addExerciseToWorkout(exercise)
                showExercisePicker = false
            }
        )
    }

    if (showFinishConfirm) {
        AlertDialog(
            onDismissRequest = { showFinishConfirm = false },
            title = { Text("End Workout") },
            text = { Text("Save this workout to your history, or discard it?") },
            confirmButton = {
                TextButton(onClick = {
                    // Sync to Health Connect before finishing
                    val workoutLog = activeWorkout.workoutLog
                    if (workoutLog != null) {
                        workoutViewModel.syncWorkoutToHealthConnect(
                            context, workoutLog.name, workoutLog.workoutType,
                            workoutLog.startTime, System.currentTimeMillis()
                        )
                    }
                    workoutViewModel.finishWorkout()
                    showFinishConfirm = false
                    navController.navigate(Screen.WorkoutSummary.route) {
                        popUpTo(Screen.Home.route)
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { showFinishConfirm = false }) { Text("Cancel") }
                    TextButton(onClick = {
                        workoutViewModel.discardWorkout()
                        showFinishConfirm = false
                        navController.popBackStack(route = "home", inclusive = false)
                    }) { Text("Discard", color = MaterialTheme.colorScheme.error) }
                }
            }
        )
    }

    // PR Celebration Dialog
    showPRCelebration?.let { prs ->
        AlertDialog(
            onDismissRequest = { showPRCelebration = null },
            icon = {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    "New Personal Record!",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column {
                    prs.forEach { pr ->
                        val description = when (pr.type) {
                            PRType.MAX_WEIGHT -> "Max Weight: ${pr.weightLbs?.toInt()} lbs"
                            PRType.MAX_VOLUME -> "Best Set Volume: ${pr.value.toInt()} lbs"
                            PRType.MAX_ESTIMATED_1RM -> "Est. 1RM: ${pr.value.toInt()} lbs"
                            PRType.MAX_REPS -> "Max Reps: ${pr.reps}"
                        }
                        Text(
                            description,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPRCelebration = null }) {
                    Text("Nice!")
                }
            }
        )
    }
}

/**
 * Resolves the pre-fill weight: last set in this session, or last weight from history.
 */
/**
 * Compact progress view shown inline on the exercise card during a workout.
 * Left side: sparkline of estimated 1RM across the most recent sessions.
 * Right side: the last 3 sessions written out as "Apr 5: 185×8, 185×8".
 * Whole card is tappable to open the full ExerciseProgress screen.
 */
@Composable
private fun MiniProgressWidget(
    history: List<com.workout.tracker.data.dao.ExerciseHistoryEntry>,
    onClick: (() -> Unit)? = null
) {
    data class Session(
        val startTime: Long,
        val bestWeight: Double?,
        val bestReps: Int?,
        val estimated1RM: Double?,
        val setsSummary: String
    )

    // Group non-warmup sets by session startTime and compute per-session bests.
    val sessions: List<Session> = remember(history) {
        history.filter { !it.isWarmup && it.reps != null && it.reps > 0 }
            .groupBy { it.startTime }
            .map { (ts, sets) ->
                val sorted = sets.sortedBy { it.setNumber }
                val bestW = sorted.mapNotNull { it.weightLbs }.filter { it > 0 }.maxOrNull()
                val bestR = sorted.maxOf { it.reps!! }
                val best1RM = sorted.filter { (it.weightLbs ?: 0.0) > 0 && it.reps!! in 1..12 }
                    .maxOfOrNull {
                        com.workout.tracker.util.OneRepMaxCalculator.estimate(it.weightLbs!!, it.reps!!)
                    }
                val summary = sorted.take(3).joinToString(", ") { s ->
                    val w = s.weightLbs
                    if (w != null && w > 0) "${s.reps}×${w.toInt()}" else "${s.reps}"
                }
                Session(ts, bestW, bestR, best1RM, summary)
            }
            .sortedBy { it.startTime }
    }

    if (sessions.isEmpty()) return

    val recentSessions = sessions.takeLast(15)
    val lastThree = sessions.takeLast(3).reversed()
    val dateFormat = remember { java.text.SimpleDateFormat("MMM d", java.util.Locale.US) }
    val lineColor = MaterialTheme.colorScheme.primary
    val pointColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Sparkline
        Canvas(
            modifier = Modifier
                .width(100.dp)
                .height(40.dp)
        ) {
            val points = recentSessions.mapNotNull { it.estimated1RM }
                .takeIf { it.isNotEmpty() } ?: return@Canvas
            val minV = points.min()
            val maxV = points.max()
            val range = (maxV - minV).takeIf { it > 0.0 } ?: 1.0
            val w = size.width
            val h = size.height
            val stepX = if (points.size > 1) w / (points.size - 1) else 0f
            val path = Path()
            points.forEachIndexed { i, v ->
                val x = i * stepX
                val y = h - ((v - minV) / range * h).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, color = lineColor, style = Stroke(width = 3f))
            // Dot at the latest point
            val lastX = (points.size - 1) * stepX
            val lastY = h - ((points.last() - minV) / range * h).toFloat()
            drawCircle(color = pointColor, radius = 4f, center = Offset(lastX, lastY))
        }
        Spacer(Modifier.width(12.dp))
        // Last 3 sessions as text
        Column(modifier = Modifier.weight(1f)) {
            lastThree.forEach { s ->
                Row {
                    Text(
                        dateFormat.format(java.util.Date(s.startTime)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(52.dp)
                    )
                    Text(
                        s.setsSummary,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * Shown below the rest countdown when the rest follows the last set of an
 * exercise. Tells the user what's coming next so they can mentally prep.
 */
@Composable
private fun UpNextCard(group: ExerciseGroup) {
    Card(
        modifier = Modifier.fillMaxWidth(fraction = 0.9f),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "UP NEXT",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                group.label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            group.exercises.forEach { ex ->
                val sets = ex.targetSets
                val reps = ex.targetReps
                val weight = getPreFillWeight(ex)
                val detail = buildString {
                    if (sets != null && reps != null) append("$sets × $reps")
                    else if (sets != null) append("$sets sets")
                    if (weight.isNotBlank()) {
                        if (isNotEmpty()) append(" @ ")
                        append("${weight} lb")
                    }
                }
                if (detail.isNotBlank()) {
                    Text(
                        if (group.isSuperset) "${ex.exercise.name}: $detail" else detail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun getPreFillWeight(activeExercise: ActiveExercise): String {
    // First: last set logged in this session
    val lastSessionWeight = activeExercise.sets.lastOrNull { !it.isWarmup }?.weightLbs
    if (lastSessionWeight != null) return lastSessionWeight.let {
        if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString()
    }

    // Second: most recent weight from history
    if (activeExercise.history.isNotEmpty()) {
        val lastWorkoutTime = activeExercise.history.first().startTime
        val lastWeight = activeExercise.history
            .filter { it.startTime == lastWorkoutTime }
            .mapNotNull { it.weightLbs }
            .maxOrNull()
        if (lastWeight != null && lastWeight > 0) {
            return if (lastWeight == lastWeight.toLong().toDouble()) lastWeight.toLong().toString() else lastWeight.toString()
        }
    }
    return ""
}

@Composable
fun FocusedExerciseCard(
    activeExercise: ActiveExercise,
    isFromTemplate: Boolean,
    onLogSet: (Int, Int, Double?, Boolean) -> Unit,
    onUpdateSet: (SetLog) -> Unit,
    onStartTimer: (Int) -> Unit,
    onMarkDone: () -> Unit,
    defaultRestSeconds: Int = 120,
    onRestSecondsChanged: ((Int) -> Unit)? = null,
    onNoteChanged: ((String) -> Unit)? = null,
    onOpenProgress: (() -> Unit)? = null
) {
    val nextSetNumber = activeExercise.sets.size + 1
    val targetSets = activeExercise.targetSets
    val targetReps = activeExercise.targetReps
    val nonWarmupSets = activeExercise.sets.count { !it.isWarmup }
    val isDone = activeExercise.isManuallyDone || (targetSets != null && nonWarmupSets >= targetSets)

    // Pre-fill values - key on exerciseLogId + set count so they reset after logging
    var repsText by remember(activeExercise.exerciseLogId, activeExercise.sets.size) {
        mutableStateOf(if (isFromTemplate && targetReps != null) targetReps.toString() else "")
    }
    var weightText by remember(activeExercise.exerciseLogId, activeExercise.sets.size) {
        mutableStateOf(if (isFromTemplate) getPreFillWeight(activeExercise) else "")
    }
    var isWarmup by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(activeExercise.exercise.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                if (isDone) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Done", tint = MaterialTheme.colorScheme.primary)
                }
            }

            // Set progress
            if (targetSets != null) {
                Text(
                    "Set $nextSetNumber of $targetSets${if (targetReps != null) " ($targetReps reps)" else ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text("${activeExercise.sets.size} sets logged", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Compact progress widget: sparkline of estimated 1RM + last 3 sessions.
            // Tapping opens the full ExerciseProgress screen.
            if (activeExercise.history.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                MiniProgressWidget(
                    history = activeExercise.history,
                    onClick = onOpenProgress
                )
            }

            // Overload suggestion
            activeExercise.overloadSuggestion?.let { suggestion ->
                if (suggestion.suggestedWeight > suggestion.currentWeight) {
                    Spacer(Modifier.height(4.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(suggestion.reason, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // Exercise notes
            if (activeExercise.lastNote != null) {
                Spacer(Modifier.height(4.dp))
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.StickyNote2, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        Spacer(Modifier.width(6.dp))
                        Text("Last time: ${activeExercise.lastNote}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
            }
            var noteText by remember(activeExercise.exerciseLogId) { mutableStateOf(activeExercise.currentNote) }
            OutlinedTextField(
                value = noteText,
                onValueChange = {
                    noteText = it
                    onNoteChanged?.invoke(it)
                },
                label = { Text("Note for this session") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall
            )

            Spacer(Modifier.height(12.dp))

            // Logged sets - tappable for editing
            if (activeExercise.sets.isNotEmpty()) {
                Text("Logged Sets", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                activeExercise.sets.forEach { set ->
                    EditableSetRow(set = set, onUpdateSet = onUpdateSet)
                }
                Spacer(Modifier.height(8.dp))
                @Suppress("DEPRECATION") Divider()
            }

            // Rest time adjuster
            var currentRestSeconds by remember(activeExercise.exerciseLogId) { mutableStateOf(defaultRestSeconds) }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(4.dp))
                Text("Rest: ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                IconButton(onClick = {
                    currentRestSeconds = (currentRestSeconds - 15).coerceAtLeast(0)
                    onRestSecondsChanged?.invoke(currentRestSeconds)
                }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease rest", modifier = Modifier.size(16.dp))
                }
                Text("${currentRestSeconds}s", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                IconButton(onClick = {
                    currentRestSeconds = (currentRestSeconds + 15).coerceAtMost(300)
                    onRestSecondsChanged?.invoke(currentRestSeconds)
                }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Add, contentDescription = "Increase rest", modifier = Modifier.size(16.dp))
                }
            }

            Spacer(Modifier.height(4.dp))

            // New set input (show even if "done" so user can add extra sets)
            Text("Set $nextSetNumber", style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = repsText,
                    onValueChange = { repsText = it },
                    label = { Text("Reps") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    label = { Text("Weight") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isWarmup, onCheckedChange = { isWarmup = it })
                Text("Warmup", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.weight(1f))
                if (!isFromTemplate && !isDone) {
                    OutlinedButton(onClick = onMarkDone, modifier = Modifier.padding(end = 8.dp)) {
                        Text("Mark Done")
                    }
                }
                Button(
                    onClick = {
                        val reps = repsText.toIntOrNull() ?: return@Button
                        val weight = weightText.toDoubleOrNull()
                        onLogSet(nextSetNumber, reps, weight, isWarmup)
                        isWarmup = false
                        onStartTimer(currentRestSeconds)
                    },
                    enabled = repsText.toIntOrNull() != null
                ) { Text("Log Set") }
            }
        }
    }
}

@Composable
fun EditableSetRow(set: SetLog, onUpdateSet: (SetLog) -> Unit) {
    var isEditing by remember { mutableStateOf(false) }
    var editReps by remember(set) { mutableStateOf(set.reps?.toString() ?: "") }
    var editWeight by remember(set) { mutableStateOf(set.weightLbs?.let {
        if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString()
    } ?: "") }

    if (isEditing) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Set ${set.setNumber}:", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(48.dp))
            OutlinedTextField(
                value = editReps,
                onValueChange = { editReps = it },
                label = { Text("Reps") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = editWeight,
                onValueChange = { editWeight = it },
                label = { Text("Weight") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            IconButton(onClick = {
                val newReps = editReps.toIntOrNull()
                val newWeight = editWeight.toDoubleOrNull()
                if (newReps != null) {
                    onUpdateSet(set.copy(reps = newReps, weightLbs = newWeight))
                }
                isEditing = false
            }) {
                Icon(Icons.Default.Check, contentDescription = "Save", tint = MaterialTheme.colorScheme.primary)
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { isEditing = true }.padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Set ${set.setNumber}: ${set.reps} reps${if (set.weightLbs != null) " @ ${set.weightLbs}lbs" else ""}${if (set.isWarmup) " (warmup)" else ""}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusedSupersetCard(
    exercises: List<ActiveExercise>,
    isFromTemplate: Boolean,
    onLogSet: (Long, Int, Int, Double?, Boolean) -> Unit,
    onUpdateSet: (SetLog) -> Unit,
    onStartTimer: (Int) -> Unit,
    onMarkDone: (Long) -> Unit,
    onTabChanged: ((Int) -> Unit)? = null,
    defaultRestSeconds: Int = 120,
    onRestSecondsChanged: ((Int) -> Unit)? = null,
    onNoteChanged: ((Long, String) -> Unit)? = null,
    onOpenProgress: ((Long) -> Unit)? = null
) {
    var activeTab by remember { mutableStateOf(0) }
    var currentRestSeconds by remember { mutableStateOf(defaultRestSeconds) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column {
            // Superset header
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("SUPERSET", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }

            // Tabs
            TabRow(selectedTabIndex = activeTab) {
                exercises.forEachIndexed { index, ex ->
                    Tab(
                        selected = activeTab == index,
                        onClick = { activeTab = index; onTabChanged?.invoke(index) },
                        text = { Text(ex.exercise.name, maxLines = 1) }
                    )
                }
            }

            val currentExercise = exercises.getOrNull(activeTab) ?: return@Card
            val nextSetNumber = currentExercise.sets.size + 1
            val targetSets = currentExercise.targetSets
            val targetReps = currentExercise.targetReps
            val nonWarmupSets = currentExercise.sets.count { !it.isWarmup }
            val isDone = currentExercise.isManuallyDone || (targetSets != null && nonWarmupSets >= targetSets)

            var repsText by remember(activeTab, currentExercise.sets.size) {
                mutableStateOf(if (isFromTemplate && targetReps != null) targetReps.toString() else "")
            }
            var weightText by remember(activeTab, currentExercise.sets.size) {
                mutableStateOf(if (isFromTemplate) getPreFillWeight(currentExercise) else "")
            }

            Column(modifier = Modifier.padding(16.dp)) {
                // Set progress
                if (targetSets != null) {
                    Text(
                        "Set $nextSetNumber of $targetSets${if (targetReps != null) " ($targetReps reps)" else ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Compact progress widget for this tab's exercise. Tappable
                // to open the full ExerciseProgress screen.
                if (currentExercise.history.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    MiniProgressWidget(
                        history = currentExercise.history,
                        onClick = onOpenProgress?.let { cb ->
                            { cb(currentExercise.exercise.id) }
                        }
                    )
                    Spacer(Modifier.height(4.dp))
                }

                // Overload suggestion
                currentExercise.overloadSuggestion?.let { suggestion ->
                    if (suggestion.suggestedWeight > suggestion.currentWeight) {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(suggestion.reason, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }

                // Exercise note
                if (currentExercise.lastNote != null) {
                    Spacer(Modifier.height(4.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.StickyNote2, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Last time: ${currentExercise.lastNote}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                var noteText by remember(activeTab, currentExercise.exerciseLogId) { mutableStateOf(currentExercise.currentNote) }
                OutlinedTextField(
                    value = noteText,
                    onValueChange = {
                        noteText = it
                        onNoteChanged?.invoke(currentExercise.exerciseLogId, it)
                    },
                    label = { Text("Note") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall
                )

                // Logged sets - tappable
                if (currentExercise.sets.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    currentExercise.sets.forEach { set ->
                        EditableSetRow(set = set, onUpdateSet = onUpdateSet)
                    }
                    Spacer(Modifier.height(8.dp))
                    @Suppress("DEPRECATION") Divider()
                }

                // Rest time adjuster
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Text("Rest: ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    IconButton(onClick = {
                        currentRestSeconds = (currentRestSeconds - 15).coerceAtLeast(0)
                        onRestSecondsChanged?.invoke(currentRestSeconds)
                    }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease rest", modifier = Modifier.size(16.dp))
                    }
                    Text("${currentRestSeconds}s", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    IconButton(onClick = {
                        currentRestSeconds = (currentRestSeconds + 15).coerceAtMost(300)
                        onRestSecondsChanged?.invoke(currentRestSeconds)
                    }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Add, contentDescription = "Increase rest", modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Set input
                Text("Set $nextSetNumber", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = repsText,
                        onValueChange = { repsText = it },
                        label = { Text("Reps") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f), singleLine = true
                    )
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { weightText = it },
                        label = { Text("Weight") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f), singleLine = true
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.weight(1f))
                    if (!isFromTemplate && !isDone) {
                        OutlinedButton(
                            onClick = { onMarkDone(currentExercise.exerciseLogId) },
                            modifier = Modifier.padding(end = 8.dp)
                        ) { Text("Mark Done") }
                    }
                    Button(
                        onClick = {
                            val reps = repsText.toIntOrNull() ?: return@Button
                            val weight = weightText.toDoubleOrNull()
                            onLogSet(currentExercise.exerciseLogId, nextSetNumber, reps, weight, false)
                            if (activeTab < exercises.size - 1) {
                                activeTab++
                                onTabChanged?.invoke(activeTab)
                            } else {
                                activeTab = 0
                                onTabChanged?.invoke(0)
                                onStartTimer(currentRestSeconds)
                            }
                        },
                        enabled = repsText.toIntOrNull() != null
                    ) { Text(if (activeTab < exercises.size - 1) "Log & Next" else "Log & Rest") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseListSheet(
    groups: List<ExerciseGroup>,
    currentIndex: Int,
    onJumpTo: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text("Exercises", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            groups.forEachIndexed { index, group ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        .clickable { onJumpTo(index) },
                    colors = if (index == currentIndex) {
                        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    } else CardDefaults.cardColors()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                group.label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (index == currentIndex) FontWeight.Bold else FontWeight.Normal
                            )
                            // Show set progress
                            val exercise = group.exercises.first()
                            val loggedSets = exercise.sets.count { !it.isWarmup }
                            val target = exercise.targetSets
                            if (target != null) {
                                Text(
                                    "$loggedSets / $target sets",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    "${exercise.sets.size} sets",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (group.isCompleted) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Completed",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        if (group.isSuperset) {
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Default.Link, contentDescription = "Superset", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
