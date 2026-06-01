package com.workout.tracker.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
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
    val currentWeekStart by scheduleViewModel.currentWeekStart.collectAsStateWithLifecycle()
    val weekSchedule by scheduleViewModel.weekSchedule.collectAsStateWithLifecycle()
    val templates by templateViewModel.templates.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var isWeekView by remember { mutableStateOf(true) } // Default to weekly view

    // Derive dialog items from live schedule so actions (unskip, mark done, etc.) update immediately
    val activeSchedule = if (isWeekView) weekSchedule else monthSchedule
    val selectedDayItems = remember(selectedDate, activeSchedule) {
        val date = selectedDate ?: return@remember emptyList()
        activeSchedule.filter { sw ->
            Instant.ofEpochMilli(sw.scheduledDate).atZone(ZoneId.systemDefault()).toLocalDate() == date
        }
    }

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
                    IconButton(onClick = { isWeekView = !isWeekView }) {
                        Icon(
                            if (isWeekView) Icons.Default.CalendarMonth else Icons.Default.ViewWeek,
                            contentDescription = if (isWeekView) "Switch to month view" else "Switch to week view"
                        )
                    }
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
            if (isWeekView) {
                WeekScheduleView(
                    weekStart = currentWeekStart,
                    schedule = weekSchedule,
                    onNavigateWeek = { scheduleViewModel.navigateWeek(it) },
                    onDayClick = { date -> selectedDate = date },
                    onReschedule = { item, newDate -> scheduleViewModel.reschedule(item, newDate) }
                )
            } else {
                // Month header with navigation
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isPastMonth = currentMonth.isBefore(YearMonth.now())
                    IconButton(onClick = { scheduleViewModel.navigateMonth(-1) }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month")
                    }
                    Text(
                        "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isPastMonth) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.onSurface
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
                    onDayClick = { date, _ ->
                        selectedDate = date
                    }
                )
            }
        }
    }

    // Day detail dialog
    if (selectedDate != null) {
        DayDetailDialog(
            date = selectedDate!!,
            items = selectedDayItems,
            scheduleViewModel = scheduleViewModel,
            workoutViewModel = workoutViewModel,
            navController = navController,
            onDismiss = { selectedDate = null }
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
            },
            onScheduleAerobic = { activityType, date, duration, distance, intensity ->
                scheduleViewModel.scheduleAerobicEvent(activityType, date, duration, distance, intensity)
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
fun WeekScheduleView(
    weekStart: LocalDate,
    schedule: List<ScheduledWorkoutWithTemplate>,
    onNavigateWeek: (Int) -> Unit,
    onDayClick: (LocalDate) -> Unit,
    onReschedule: ((ScheduledWorkoutWithTemplate, LocalDate) -> Unit)? = null
) {
    val today = LocalDate.now()
    val weekEnd = weekStart.plusDays(6)
    val dateFormat = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }
    val startStr = dateFormat.format(Date.from(weekStart.atStartOfDay(ZoneId.systemDefault()).toInstant()))
    val endStr = dateFormat.format(Date.from(weekEnd.atStartOfDay(ZoneId.systemDefault()).toInstant()))

    val scheduleByDay = schedule.groupBy { sw ->
        Instant.ofEpochMilli(sw.scheduledDate).atZone(ZoneId.systemDefault()).toLocalDate()
    }

    // Drag-to-reschedule state
    var draggedItemId by remember { mutableStateOf<Long?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    val rowCenters = remember { mutableStateMapOf<Int, Float>() }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Week header with navigation
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isPastWeek = weekEnd.isBefore(today)
            IconButton(onClick = { onNavigateWeek(-1) }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous week")
            }
            Text(
                "$startStr - $endStr",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (isPastWeek) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = { onNavigateWeek(1) }) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next week")
            }
        }

        // 7 day rows
        for (dayOffset in 0L..6L) {
            val date = weekStart.plusDays(dayOffset)
            val dayItems = scheduleByDay[date] ?: emptyList()
            val isToday = date == today
            val isPast = date.isBefore(today)
            val workoutItems = dayItems.filter { it.label?.lowercase()?.contains("rest") != true }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coords ->
                        rowCenters[dayOffset.toInt()] =
                            coords.positionInParent().y + coords.size.height / 2f
                    }
                    .clickable { onDayClick(date) }
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                shape = RoundedCornerShape(8.dp),
                color = if (isToday) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface,
                border = if (isToday) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Day name + date
                    Column(modifier = Modifier.width(72.dp)) {
                        Text(
                            date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (isToday) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${date.dayOfMonth}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (isToday) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Scheduled items
                    if (dayItems.isEmpty()) {
                        Text(
                            "No workouts",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    } else {
                        Column(modifier = Modifier.weight(1f)) {
                            dayItems.forEach { item ->
                                val displayName = item.templateName ?: item.label ?: "Workout"
                                val isRestDay = item.label?.lowercase()?.contains("rest") == true
                                val isAerobic = item.activityType != null
                                val icon = when {
                                    isRestDay -> Icons.Default.Hotel
                                    isAerobic -> when (item.activityType) {
                                        "Running" -> Icons.AutoMirrored.Filled.DirectionsRun
                                        "Cycling" -> Icons.AutoMirrored.Filled.DirectionsBike
                                        "Swimming" -> Icons.Default.Pool
                                        "Walking" -> Icons.AutoMirrored.Filled.DirectionsWalk
                                        else -> Icons.Default.FitnessCenter
                                    }
                                    item.templateId != null -> Icons.Default.FitnessCenter
                                    else -> Icons.Default.Event
                                }
                                val isDragging = draggedItemId == item.id
                                val currentDayOffset = dayOffset.toInt()
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .padding(vertical = 1.dp)
                                        .graphicsLayer {
                                            if (isDragging) {
                                                translationY = dragOffsetY
                                                shadowElevation = 16f
                                                scaleX = 1.03f; scaleY = 1.03f
                                                alpha = 0.9f
                                            }
                                        }
                                        .then(
                                            if (onReschedule != null && !item.isCompleted && !item.isSkipped) {
                                                Modifier.pointerInput(item.id) {
                                                    detectDragGesturesAfterLongPress(
                                                        onDragStart = {
                                                            draggedItemId = item.id
                                                            dragOffsetY = 0f
                                                        },
                                                        onDrag = { change, amount ->
                                                            change.consume()
                                                            dragOffsetY += amount.y
                                                        },
                                                        onDragEnd = {
                                                            val sourceCenter = rowCenters[currentDayOffset] ?: 0f
                                                            val targetCenter = sourceCenter + dragOffsetY
                                                            val targetDay = rowCenters.entries
                                                                .minByOrNull { abs(it.value - targetCenter) }
                                                                ?.key ?: currentDayOffset
                                                            if (targetDay != currentDayOffset) {
                                                                val targetDate = weekStart.plusDays(targetDay.toLong())
                                                                onReschedule(item, targetDate)
                                                            }
                                                            draggedItemId = null
                                                            dragOffsetY = 0f
                                                        },
                                                        onDragCancel = {
                                                            draggedItemId = null
                                                            dragOffsetY = 0f
                                                        }
                                                    )
                                                }
                                            } else Modifier
                                        )
                                ) {
                                    Icon(
                                        icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            displayName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        if (isAerobic) {
                                            val details = listOfNotNull(
                                                item.plannedDurationMinutes?.let { "${it}min" },
                                                item.plannedDistanceMiles?.let { "${"%.1f".format(it)}mi" },
                                                item.plannedIntensity
                                            )
                                            if (details.isNotEmpty()) {
                                                Text(
                                                    details.joinToString(" · "),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Status icon
                    Spacer(Modifier.width(8.dp))
                    if (workoutItems.isNotEmpty()) {
                        val allDone = workoutItems.all { it.isCompleted }
                        val anyMissed = isPast && workoutItems.any { !it.isCompleted && !it.isSkipped }
                        val anySkipped = workoutItems.any { it.isSkipped }
                        when {
                            allDone -> Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Completed",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            anyMissed || anySkipped -> Icon(
                                Icons.Default.Cancel,
                                contentDescription = "Missed",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
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
    var showMoveDateDialog by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf<List<com.workout.tracker.ui.viewmodel.WorkoutViewModel.TemplateExercisePreview>>(emptyList()) }

    LaunchedEffect(expanded, item.templateId) {
        if (expanded && item.templateId != null && preview.isEmpty()) {
            preview = workoutViewModel.getTemplatePreview(item.templateId)
        }
    }

    val isAerobic = item.activityType != null
    val icon = when {
        isRestDay -> Icons.Default.Hotel
        isAerobic -> when (item.activityType) {
            "Running" -> Icons.AutoMirrored.Filled.DirectionsRun
            "Cycling" -> Icons.AutoMirrored.Filled.DirectionsBike
            "Swimming" -> Icons.Default.Pool
            "Walking" -> Icons.AutoMirrored.Filled.DirectionsWalk
            else -> Icons.Default.FitnessCenter
        }
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = if (item.templateId != null) Modifier.clickable { expanded = !expanded } else Modifier
            ) {
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
                if (item.templateId != null) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Expanded exercise preview
            if (expanded && preview.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                @Suppress("DEPRECATION") Divider()
                Spacer(Modifier.height(8.dp))
                preview.forEachIndexed { index, ex ->
                    val isSupersetWithPrev = ex.supersetGroup != null &&
                        index > 0 && preview[index - 1].supersetGroup == ex.supersetGroup
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp, horizontal = if (isSupersetWithPrev) 16.dp else 0.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (ex.supersetGroup != null) {
                            Icon(
                                Icons.Default.Link,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(
                            ex.exerciseName,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "${ex.targetSets} × ${ex.targetReps}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (ex.lastWeightLbs != null && ex.lastWeightLbs > 0) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "${ex.lastWeightLbs.toInt()} lb",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                @Suppress("DEPRECATION") Divider()
            }

            // Aerobic details
            if (isAerobic) {
                val details = listOfNotNull(
                    item.plannedDurationMinutes?.let { "${it} min" },
                    item.plannedDistanceMiles?.let { "${"%.1f".format(it)} mi" },
                    item.plannedIntensity
                )
                if (details.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        details.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Undo button for completed/skipped items
            if (isDone && !isRestDay) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { scheduleViewModel.markUncompleted(item) },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Mark Incomplete", style = MaterialTheme.typography.labelSmall)
                }
            }

            // Action buttons for non-completed items
            if (!isDone && !isRestDay) {
                Spacer(Modifier.height(8.dp))

                if (item.templateId != null) {
                    Button(
                        onClick = {
                            workoutViewModel.startWorkout(
                                name = item.templateName ?: "Workout",
                                type = WorkoutType.STRENGTH,
                                templateId = item.templateId,
                                scheduledWorkoutId = item.id
                            )
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

                Spacer(Modifier.height(4.dp))

                OutlinedButton(
                    onClick = { showMoveDateDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.EditCalendar, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Move", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }

    if (showMoveDateDialog) {
        MoveDateDialog(
            currentDateMillis = item.scheduledDate,
            onDismiss = { showMoveDateDialog = false },
            onMove = { newDate ->
                scheduleViewModel.reschedule(item, newDate)
                showMoveDateDialog = false
                onDismissDialog()
            }
        )
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
                        selectedDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
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
    // Convert stored local-midnight millis to UTC midnight for the DatePicker
    val initialUtcMillis = remember(currentDateMillis) {
        Instant.ofEpochMilli(currentDateMillis)
            .atZone(ZoneId.systemDefault()).toLocalDate()
            .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialUtcMillis
    )

    // DatePicker returns UTC midnight millis - must convert with UTC, not local timezone
    val selectedDate = datePickerState.selectedDateMillis?.let { millis ->
        Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
    }

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                if (selectedDate != null) {
                    onMove(selectedDate)
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
    onScheduleLabel: (String, LocalDate) -> Unit,
    onScheduleAerobic: ((String, LocalDate, Int?, Double?, String?) -> Unit)? = null
) {
    var selectedTemplate by remember { mutableStateOf<TemplateWithExerciseCount?>(null) }
    var selectedLabel by remember { mutableStateOf<String?>(null) }
    var customLabel by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    // Aerobic fields
    var selectedAerobic by remember { mutableStateOf<String?>(null) }
    var aerobicDuration by remember { mutableStateOf("") }
    var aerobicDistance by remember { mutableStateOf("") }
    var aerobicIntensity by remember { mutableStateOf<String?>(null) }

    val quickOptions = listOf("Rest Day", "Mobility")
    val aerobicOptions = listOf("Running", "Cycling", "Swimming", "Walking", "Rowing", "Hiking")
    val intensityOptions = listOf("Easy", "Moderate", "Tempo", "Intervals", "Hard")
    val hasSelection = selectedTemplate != null || selectedLabel != null || customLabel.isNotBlank() || selectedAerobic != null
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
                                selectedTemplate = null; customLabel = ""; selectedAerobic = null
                            },
                            label = { Text(option) }
                        )
                    }
                }

                // Aerobic activity options
                Text("Aerobic:", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    aerobicOptions.forEach { activity ->
                        FilterChip(
                            selected = selectedAerobic == activity,
                            onClick = {
                                selectedAerobic = if (selectedAerobic == activity) null else activity
                                selectedTemplate = null; selectedLabel = null; customLabel = ""
                            },
                            label = { Text(activity) },
                            leadingIcon = {
                                val icon = when (activity) {
                                    "Running" -> Icons.AutoMirrored.Filled.DirectionsRun
                                    "Cycling" -> Icons.AutoMirrored.Filled.DirectionsBike
                                    "Swimming" -> Icons.Default.Pool
                                    "Walking" -> Icons.AutoMirrored.Filled.DirectionsWalk
                                    else -> Icons.Default.FitnessCenter
                                }
                                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        )
                    }
                }

                // Aerobic detail fields (shown when an aerobic activity is selected)
                if (selectedAerobic != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = aerobicDuration,
                            onValueChange = { aerobicDuration = it },
                            label = { Text("Min") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = aerobicDistance,
                            onValueChange = { aerobicDistance = it },
                            label = { Text("Miles") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        intensityOptions.forEach { intensity ->
                            FilterChip(
                                selected = aerobicIntensity == intensity,
                                onClick = { aerobicIntensity = if (aerobicIntensity == intensity) null else intensity },
                                label = { Text(intensity, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = customLabel,
                    onValueChange = {
                        customLabel = it
                        if (it.isNotBlank()) { selectedLabel = null; selectedTemplate = null; selectedAerobic = null }
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
                                    selectedLabel = null; customLabel = ""; selectedAerobic = null
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

                @Suppress("DEPRECATION") Divider()

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
                        selectedAerobic != null -> onScheduleAerobic?.invoke(
                            selectedAerobic!!,
                            selectedDate,
                            aerobicDuration.toIntOrNull(),
                            aerobicDistance.toDoubleOrNull(),
                            aerobicIntensity
                        )
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
                        selectedDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
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
