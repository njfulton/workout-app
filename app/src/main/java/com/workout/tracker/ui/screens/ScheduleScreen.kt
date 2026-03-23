package com.workout.tracker.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.workout.tracker.data.dao.ScheduledWorkoutWithTemplate
import com.workout.tracker.data.dao.TemplateWithExerciseCount
import com.workout.tracker.data.entity.WorkoutType
import com.workout.tracker.ui.navigation.Screen
import com.workout.tracker.ui.viewmodel.ScheduleViewModel
import com.workout.tracker.ui.viewmodel.TemplateViewModel
import com.workout.tracker.ui.viewmodel.WorkoutViewModel
import java.time.*
import java.time.format.TextStyle
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    navController: NavController,
    scheduleViewModel: ScheduleViewModel,
    templateViewModel: TemplateViewModel,
    workoutViewModel: WorkoutViewModel
) {
    val currentMonth by scheduleViewModel.currentMonth.collectAsStateWithLifecycle()
    val monthSchedule by scheduleViewModel.monthSchedule.collectAsStateWithLifecycle()
    val templates by templateViewModel.templates.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var selectedDayItems by remember { mutableStateOf<List<ScheduledWorkoutWithTemplate>?>(null) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Schedule") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showClearConfirm = true }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear future schedule")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Schedule")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Month header with navigation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { scheduleViewModel.navigateMonth(-1) }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month")
                }
                Text(
                    "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { scheduleViewModel.navigateMonth(1) }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next month")
                }
            }

            // Day-of-week headers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val daysOfWeek = listOf(
                    DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
                )
                daysOfWeek.forEach { dow ->
                    Text(
                        dow.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // Calendar grid
            CalendarGrid(
                yearMonth = currentMonth,
                schedule = monthSchedule,
                onDayClick = { date, items ->
                    selectedDate = date
                    selectedDayItems = items
                }
            )
        }
    }

    // Day detail dialog
    if (selectedDayItems != null && selectedDate != null) {
        DayDetailDialog(
            date = selectedDate!!,
            items = selectedDayItems!!,
            scheduleViewModel = scheduleViewModel,
            workoutViewModel = workoutViewModel,
            navController = navController,
            onDismiss = {
                selectedDayItems = null
                selectedDate = null
            }
        )
    }

    if (showAddDialog) {
        ScheduleWorkoutDialog(
            templates = templates,
            onDismiss = { showAddDialog = false },
            onScheduleTemplate = { templateId, date ->
                scheduleViewModel.scheduleWorkout(templateId, date)
                showAddDialog = false
            },
            onScheduleLabel = { label, date ->
                scheduleViewModel.scheduleNonTemplate(label, date)
                showAddDialog = false
            }
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear Future Schedule") },
            text = { Text("This will remove all upcoming workouts that haven't been completed. Completed workout history will not be affected.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scheduleViewModel.clearFutureSchedule()
                        showClearConfirm = false
                    }
                ) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun CalendarGrid(
    yearMonth: YearMonth,
    schedule: List<ScheduledWorkoutWithTemplate>,
    onDayClick: (LocalDate, List<ScheduledWorkoutWithTemplate>) -> Unit
) {
    val today = LocalDate.now()
    val firstOfMonth = yearMonth.atDay(1)
    // Monday=1, so offset is (dayOfWeek.value - 1) to make Monday=0
    val startOffset = (firstOfMonth.dayOfWeek.value - 1) // 0=Mon
    val daysInMonth = yearMonth.lengthOfMonth()

    // Group schedule by day-of-month
    val scheduleByDay = schedule.groupBy { sw ->
        Instant.ofEpochMilli(sw.scheduledDate).atZone(ZoneId.systemDefault()).toLocalDate()
    }

    // Build weeks
    val totalCells = startOffset + daysInMonth
    val numWeeks = (totalCells + 6) / 7

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        for (week in 0 until numWeeks) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (dayInWeek in 0 until 7) {
                    val cellIndex = week * 7 + dayInWeek
                    val dayOfMonth = cellIndex - startOffset + 1

                    if (dayOfMonth in 1..daysInMonth) {
                        val date = yearMonth.atDay(dayOfMonth)
                        val dayItems = scheduleByDay[date] ?: emptyList()
                        val isToday = date == today

                        CalendarDayCell(
                            day = dayOfMonth,
                            date = date,
                            today = today,
                            isToday = isToday,
                            items = dayItems,
                            modifier = Modifier.weight(1f),
                            onClick = { onDayClick(date, dayItems) }
                        )
                    } else {
                        // Empty cell for padding
                        Box(modifier = Modifier.weight(1f).aspectRatio(0.75f))
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarDayCell(
    day: Int,
    date: LocalDate,
    today: LocalDate,
    isToday: Boolean,
    items: List<ScheduledWorkoutWithTemplate>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isPast = date.isBefore(today)
    val hasWorkouts = items.isNotEmpty()
    val workoutItems = items.filter { it.label?.lowercase()?.contains("rest") != true }
    val hasCompletedAll = workoutItems.isNotEmpty() && workoutItems.all { it.isCompleted }
    val hasMissed = isPast && workoutItems.any { !it.isCompleted && !it.isSkipped }
    val hasSkipped = workoutItems.any { it.isSkipped }

    val backgroundColor = when {
        isToday -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }

    val borderColor = if (isToday) MaterialTheme.colorScheme.primary else null

    Surface(
        modifier = modifier
            .aspectRatio(0.75f)
            .padding(1.dp),
        shape = RoundedCornerShape(6.dp),
        color = backgroundColor,
        border = borderColor?.let { BorderStroke(2.dp, it) },
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Day number
            Text(
                "$day",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color = if (isToday) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface
            )

            if (hasWorkouts) {
                Spacer(Modifier.height(1.dp))

                if (isPast && hasCompletedAll) {
                    // Green checkmark for completed
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Completed",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                } else if (isPast && (hasMissed || hasSkipped)) {
                    // Red X for missed/skipped
                    Icon(
                        Icons.Default.Cancel,
                        contentDescription = "Missed",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                } else if (!isPast || isToday) {
                    // Future/today: show template name
                    val displayName = workoutItems.firstOrNull()?.let {
                        it.templateName ?: it.label
                    }
                    if (displayName != null) {
                        Text(
                            displayName,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 9.sp
                        )
                    }
                }

                // If partially completed (some done, some not)
                if (isPast && !hasCompletedAll && workoutItems.any { it.isCompleted }) {
                    // Show partial indicator
                    Text(
                        "${workoutItems.count { it.isCompleted }}/${workoutItems.size}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailDialog(
    date: LocalDate,
    items: List<ScheduledWorkoutWithTemplate>,
    scheduleViewModel: ScheduleViewModel,
    workoutViewModel: WorkoutViewModel,
    navController: NavController,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("EEEE, MMM d", Locale.getDefault()) }
    val displayDate = dateFormat.format(
        Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant())
    )
    val today = LocalDate.now()
    val isPast = date.isBefore(today)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(displayDate) },
        text = {
            if (items.isEmpty()) {
                Text("No workouts scheduled", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items.forEach { item ->
                        DayDetailItem(
                            item = item,
                            isPast = isPast,
                            scheduleViewModel = scheduleViewModel,
                            workoutViewModel = workoutViewModel,
                            navController = navController,
                            onDismissDialog = onDismiss
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun DayDetailItem(
    item: ScheduledWorkoutWithTemplate,
    isPast: Boolean,
    scheduleViewModel: ScheduleViewModel,
    workoutViewModel: WorkoutViewModel,
    navController: NavController,
    onDismissDialog: () -> Unit
) {
    val displayName = item.templateName ?: item.label ?: "Unknown"
    val isRestDay = item.label?.lowercase()?.contains("rest") == true
    val isDone = item.isCompleted || item.isSkipped

    val icon = when {
        isRestDay -> Icons.Default.Hotel
        item.templateId != null -> Icons.Default.FitnessCenter
        else -> Icons.Default.Event
    }

    val statusColor = when {
        item.isCompleted -> MaterialTheme.colorScheme.primary
        item.isSkipped -> MaterialTheme.colorScheme.error
        isPast -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val statusText = when {
        item.isCompleted -> "Completed"
        item.isSkipped -> "Skipped"
        isPast && !isRestDay -> "Missed"
        else -> null
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                item.isCompleted -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                if (statusText != null) {
                    Text(
                        statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor
                    )
                }
            }

            // Action buttons for non-completed items
            if (!isDone && !isRestDay) {
                Spacer(Modifier.height(8.dp))

                if (item.templateId != null && !isPast) {
                    Button(
                        onClick = {
                            workoutViewModel.startWorkout(
                                name = item.templateName ?: "Workout",
                                type = WorkoutType.STRENGTH,
                                templateId = item.templateId
                            )
                            scheduleViewModel.markCompleted(item)
                            onDismissDialog()
                            navController.navigate(Screen.ActiveWorkout.route) {
                                popUpTo(Screen.Home.route)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Start Workout", style = MaterialTheme.typography.labelMedium)
                    }
                    Spacer(Modifier.height(4.dp))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = {
                            scheduleViewModel.markCompleted(item)
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Done", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = {
                            scheduleViewModel.markSkipped(item)
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Skip", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkCompletedDialog(
    item: ScheduledWorkoutWithTemplate,
    onDismiss: () -> Unit,
    onComplete: (LocalDate) -> Unit,
    onCompleteWithWorkout: (LocalDate) -> Unit
) {
    val displayName = item.templateName ?: item.label ?: "Workout"
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("EEE, MMM d", Locale.getDefault()) }
    val displayDate = dateFormat.format(
        Date.from(selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mark Completed") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("When did you complete \"$displayName\"?")

                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(displayDate)
                }

                if (item.templateId != null) {
                    Text(
                        "Want to log your weights and reps?",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = { onCompleteWithWorkout(selectedDate) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Log Weights & Reps")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onComplete(selectedDate) }) {
                Text("Mark Done (Skip Logging)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        selectedDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoveDateDialog(
    currentDateMillis: Long,
    onDismiss: () -> Unit,
    onMove: (LocalDate) -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = currentDateMillis
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                    val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                    onMove(date)
                }
            }) { Text("Move") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ScheduleWorkoutDialog(
    templates: List<TemplateWithExerciseCount>,
    onDismiss: () -> Unit,
    onScheduleTemplate: (Long, LocalDate) -> Unit,
    onScheduleLabel: (String, LocalDate) -> Unit
) {
    var selectedTemplate by remember { mutableStateOf<TemplateWithExerciseCount?>(null) }
    var selectedLabel by remember { mutableStateOf<String?>(null) }
    var customLabel by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    val quickOptions = listOf("Rest Day", "Cardio", "Mobility")
    val hasSelection = selectedTemplate != null || selectedLabel != null || customLabel.isNotBlank()
    val dateFormat = remember { SimpleDateFormat("EEE, MMM d", Locale.getDefault()) }
    val displayDate = dateFormat.format(
        Date.from(selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schedule") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Quick options - wrapped
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    quickOptions.forEach { option ->
                        FilterChip(
                            selected = selectedLabel == option,
                            onClick = {
                                selectedLabel = if (selectedLabel == option) null else option
                                selectedTemplate = null
                                customLabel = ""
                            },
                            label = { Text(option) }
                        )
                    }
                }

                OutlinedTextField(
                    value = customLabel,
                    onValueChange = {
                        customLabel = it
                        if (it.isNotBlank()) { selectedLabel = null; selectedTemplate = null }
                    },
                    label = { Text("Or type custom...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (templates.isNotEmpty()) {
                    Text("Template:", style = MaterialTheme.typography.labelMedium)
                    LazyColumn(modifier = Modifier.heightIn(max = 120.dp)) {
                        items(templates) { template ->
                            TextButton(
                                onClick = {
                                    selectedTemplate = if (selectedTemplate == template) null else template
                                    selectedLabel = null; customLabel = ""
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = selectedTemplate == template, onClick = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("${template.name} (${template.exerciseCount} ex.)")
                                }
                            }
                        }
                    }
                }

                Divider()

                // Date selector
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(displayDate)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when {
                        selectedTemplate != null -> onScheduleTemplate(selectedTemplate!!.id, selectedDate)
                        selectedLabel != null -> onScheduleLabel(selectedLabel!!, selectedDate)
                        customLabel.isNotBlank() -> onScheduleLabel(customLabel, selectedDate)
                    }
                },
                enabled = hasSelection
            ) { Text("Schedule") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        selectedDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
