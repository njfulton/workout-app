package com.workout.tracker.ui.screens

import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.workout.tracker.data.entity.SetLog
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
            TopAppBar(
                title = {
                    Column {
                        Text(activeWorkout.workoutLog?.name ?: "Workout")
                        Text(
                            elapsedStr,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showExerciseList = true }) {
                        Icon(Icons.Default.List, contentDescription = "Exercise List")
                    }
                    TextButton(onClick = { showFinishConfirm = true }) {
                        Text("Finish", color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showExercisePicker = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Exercise")
            }
        },
        bottomBar = {
            if (groups.isNotEmpty()) {
                Surface(tonalElevation = 3.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { workoutViewModel.previousGroup() },
                            enabled = currentGroupIndex > 0
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Back")
                        }
                        Text(
                            "Exercise ${currentGroupIndex + 1} of ${groups.size}",
                            style = MaterialTheme.typography.labelLarge
                        )
                        TextButton(
                            onClick = { workoutViewModel.nextGroup() },
                            enabled = currentGroupIndex < groups.size - 1
                        ) {
                            Text("Next")
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)
        ) {
            // Rest timer - prominent when active, hidden when not
            if (isTimerRunning) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("REST", style = MaterialTheme.typography.labelLarge)
                        Text(
                            "${restTimer / 60}:${(restTimer % 60).toString().padStart(2, '0')}",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { workoutViewModel.skipRestTimer() }) {
                            Text("Skip")
                        }
                    }
                }
            }

            // Current exercise/superset content
            if (groups.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Tap + to add exercises", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else if (currentGroup != null) {
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                ) {
                    Spacer(Modifier.height(8.dp))
                    if (currentGroup.isSuperset) {
                        FocusedSupersetCard(
                            exercises = currentGroup.exercises,
                            isFromTemplate = activeWorkout.isFromTemplate,
                            onLogSet = { exerciseLogId, setNum, reps, weight, isWarmup ->
                                workoutViewModel.logSet(exerciseLogId, setNum, reps, weight, isWarmup)
                            },
                            onUpdateSet = { workoutViewModel.updateSet(it) },
                            onStartTimer = { workoutViewModel.startRestTimer(it) },
                            onMarkDone = { workoutViewModel.markExerciseDone(it) }
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
                            defaultRestSeconds = currentGroup.exercises.first().restSeconds
                        )
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
                    workoutViewModel.finishWorkout()
                    showFinishConfirm = false
                    navController.popBackStack(route = "home", inclusive = false)
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
}

/**
 * Resolves the pre-fill weight: last set in this session, or last weight from history.
 */
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
    defaultRestSeconds: Int = 90
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

            // Last workout summary
            if (activeExercise.history.isNotEmpty()) {
                val lastWorkoutTime = activeExercise.history.first().startTime
                val lastSets = activeExercise.history.filter { it.startTime == lastWorkoutTime }
                val dateFormat = java.text.SimpleDateFormat("MMM d", java.util.Locale.US)
                val dateStr = dateFormat.format(java.util.Date(lastWorkoutTime))
                val lastWeight = lastSets.mapNotNull { it.weightLbs }.maxOrNull()
                val repsStr = lastSets.mapNotNull { it.reps }.joinToString(", ")
                val summary = if (lastWeight != null && lastWeight > 0) {
                    "Last ($dateStr): ${lastWeight.toInt()}lb x $repsStr"
                } else if (repsStr.isNotEmpty()) {
                    "Last ($dateStr): $repsStr reps"
                } else null
                if (summary != null) {
                    Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }

            // Overload suggestion
            activeExercise.overloadSuggestion?.let { suggestion ->
                if (suggestion.suggestedWeight > suggestion.currentWeight) {
                    Spacer(Modifier.height(4.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(suggestion.reason, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Logged sets - tappable for editing
            if (activeExercise.sets.isNotEmpty()) {
                Text("Logged Sets", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                activeExercise.sets.forEach { set ->
                    EditableSetRow(set = set, onUpdateSet = onUpdateSet)
                }
                Spacer(Modifier.height(8.dp))
                Divider()
            }

            // New set input (show even if "done" so user can add extra sets)
            Spacer(Modifier.height(8.dp))
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
                        onStartTimer(defaultRestSeconds)
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
    onMarkDone: (Long) -> Unit
) {
    var activeTab by remember { mutableStateOf(0) }
    val restSeconds = exercises.firstOrNull()?.restSeconds ?: 90

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
                        onClick = { activeTab = index },
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

                // Last workout summary
                if (currentExercise.history.isNotEmpty()) {
                    val lastWorkoutTime = currentExercise.history.first().startTime
                    val lastSets = currentExercise.history.filter { it.startTime == lastWorkoutTime }
                    val dateFormat = java.text.SimpleDateFormat("MMM d", java.util.Locale.US)
                    val dateStr = dateFormat.format(java.util.Date(lastWorkoutTime))
                    val lastWeight = lastSets.mapNotNull { it.weightLbs }.maxOrNull()
                    val repsStr = lastSets.mapNotNull { it.reps }.joinToString(", ")
                    val summary = if (lastWeight != null && lastWeight > 0) {
                        "Last ($dateStr): ${lastWeight.toInt()}lb x $repsStr"
                    } else if (repsStr.isNotEmpty()) {
                        "Last ($dateStr): $repsStr reps"
                    } else null
                    if (summary != null) {
                        Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(4.dp))
                    }
                }

                // Overload suggestion
                currentExercise.overloadSuggestion?.let { suggestion ->
                    if (suggestion.suggestedWeight > suggestion.currentWeight) {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(suggestion.reason, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }

                // Logged sets - tappable
                if (currentExercise.sets.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    currentExercise.sets.forEach { set ->
                        EditableSetRow(set = set, onUpdateSet = onUpdateSet)
                    }
                    Spacer(Modifier.height(8.dp))
                    Divider()
                }

                Spacer(Modifier.height(8.dp))

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
                            // Auto-advance to next exercise in superset, or start rest timer if last
                            if (activeTab < exercises.size - 1) {
                                activeTab++
                            } else {
                                activeTab = 0
                                onStartTimer(restSeconds)
                            }
                        },
                        enabled = repsText.toIntOrNull() != null
                    ) { Text("Log & Next") }
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
