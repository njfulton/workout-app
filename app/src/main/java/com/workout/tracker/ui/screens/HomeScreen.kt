package com.workout.tracker.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.foundation.layout.*
import kotlin.math.abs
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.workout.tracker.ui.navigation.Screen
import com.workout.tracker.ui.viewmodel.ScheduleViewModel
import com.workout.tracker.ui.viewmodel.TemplateViewModel
import com.workout.tracker.ui.viewmodel.WorkoutViewModel
import java.text.SimpleDateFormat
import java.time.*
import java.time.format.TextStyle as JavaTextStyle
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    workoutViewModel: WorkoutViewModel,
    scheduleViewModel: ScheduleViewModel,
    templateViewModel: TemplateViewModel
) {
    val routineOverview by templateViewModel.routineOverview.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { templateViewModel.loadRoutineOverview() }
    val activeWorkout by workoutViewModel.activeWorkout.collectAsStateWithLifecycle()
    val upcomingSchedule by scheduleViewModel.upcomingSchedule.collectAsStateWithLifecycle()
    val currentWeekStart by scheduleViewModel.currentWeekStart.collectAsStateWithLifecycle()
    val weekSchedule by scheduleViewModel.weekSchedule.collectAsStateWithLifecycle()
    val currentMonth by scheduleViewModel.currentMonth.collectAsStateWithLifecycle()
    val monthSchedule by scheduleViewModel.monthSchedule.collectAsStateWithLifecycle()
    val dashboardStats by workoutViewModel.dashboardStats.collectAsStateWithLifecycle()
    val scheduleDateFormat = remember { SimpleDateFormat("EEE, MMM d", Locale.getDefault()) }

    // Find the next non-completed, non-skipped, non-rest workout
    val nextWorkout = upcomingSchedule.firstOrNull {
        !it.isCompleted && !it.isSkipped && it.label?.lowercase()?.contains("rest") != true
    }

    var showLifetimeStats by remember { mutableStateOf(false) }
    var isWeekView by remember { mutableStateOf(true) }
    // When a day is tapped, show a quick-action dialog right here instead of
    // navigating to the full Schedule screen.
    var selectedDate by remember { mutableStateOf<java.time.LocalDate?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workout Tracker", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Active workout banner
            if (activeWorkout.isActive) {
                item {
                    Card(
                        onClick = { navController.navigate(Screen.ActiveWorkout.route) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.FitnessCenter, contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Workout in Progress", style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text(activeWorkout.workoutLog?.name ?: "", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Continue",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }

            // Dashboard stats row - tappable tiles
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DashboardStatCard(
                        modifier = Modifier.weight(1f),
                        value = dashboardStats.currentStreak.toString(),
                        label = "Streak",
                        icon = Icons.Default.LocalFireDepartment,
                        onClick = { navController.navigate(Screen.Schedule.route) }
                    )
                    DashboardStatCard(
                        modifier = Modifier.weight(1f),
                        value = dashboardStats.workoutsThisWeek.toString(),
                        label = "This Week",
                        icon = Icons.Default.DateRange,
                        onClick = { navController.navigate(Screen.WeeklySummary.route) }
                    )
                    DashboardStatCard(
                        modifier = Modifier.weight(1f),
                        value = dashboardStats.totalWorkouts.toString(),
                        label = "Total Workouts",
                        icon = Icons.Default.EmojiEvents,
                        onClick = { showLifetimeStats = true }
                    )
                }
            }

            // Next Workout card + Pushups/More buttons (single row)
            if (!activeWorkout.isActive) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (nextWorkout != null) {
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        "Next Workout",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        nextWorkout.templateName ?: nextWorkout.label ?: "Workout",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        maxLines = 2
                                    )
                                    Text(
                                        scheduleDateFormat.format(Date(nextWorkout.scheduledDate)),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    )
                                    if (nextWorkout.templateId != null) {
                                        Spacer(Modifier.height(8.dp))
                                        Button(
                                            onClick = {
                                                workoutViewModel.logFeatureUsage("start_workout")
                                                workoutViewModel.startWorkout(
                                                    name = nextWorkout.templateName ?: "Workout",
                                                    type = com.workout.tracker.data.entity.WorkoutType.STRENGTH,
                                                    templateId = nextWorkout.templateId,
                                                    scheduledWorkoutId = nextWorkout.id
                                                )
                                                navController.navigate(Screen.ActiveWorkout.route) {
                                                    popUpTo(Screen.Home.route)
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                                contentColor = MaterialTheme.colorScheme.primaryContainer
                                            ),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Start", style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }
                            }
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                        // Right column: Pushups + More stacked
                        Column(
                            modifier = Modifier.width(100.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            QuickActionCard(Modifier.fillMaxWidth(), Icons.Default.Sports, "Pushups") {
                                workoutViewModel.logFeatureUsage("pushups")
                                navController.navigate(Screen.Pushups.route)
                            }
                            QuickActionCard(Modifier.fillMaxWidth(), Icons.Default.MoreHoriz, "More") {
                                navController.navigate(Screen.Utilities.route)
                            }
                        }
                    }
                }
            }

            // Routine progress indicator
            routineOverview?.let { data ->
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate(Screen.RoutineOverview.route) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Timeline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    data.routine.name,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1
                                )
                                Text(
                                    "Week ${data.currentWeek} of ${data.totalWeeks}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = data.currentWeek.toFloat() / data.totalWeeks.toFloat(),
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                trackColor = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.25f)
                            )
                        }
                    }
                }
            }

            // Schedule section with week/month toggle
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (isWeekView) "This Week" else "This Month",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.weight(1f))
                    SegmentedToggle(
                        isWeekView = isWeekView,
                        onToggle = { isWeekView = it }
                    )
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { navController.navigate(Screen.Schedule.route) }) {
                        Text("Open", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            if (isWeekView) {
                item {
                    HomeWeekView(
                        weekStart = currentWeekStart,
                        schedule = weekSchedule,
                        onNavigateWeek = { scheduleViewModel.navigateWeek(it) },
                        onDayClick = { date -> selectedDate = date },
                        onReschedule = { item, newDate -> scheduleViewModel.reschedule(item, newDate) }
                    )
                }
            } else {
                item {
                    HomeMonthView(
                        yearMonth = currentMonth,
                        schedule = monthSchedule,
                        onNavigateMonth = { scheduleViewModel.navigateMonth(it) },
                        onDayClick = { date -> selectedDate = date }
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    // Day-tap dialog: lets you start a workout or view details directly
    // from the home screen, without a detour through the Schedule screen.
    selectedDate?.let { date ->
        val schedule = if (isWeekView) weekSchedule else monthSchedule
        val dayEpoch = date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        val nextDayEpoch = date.plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        val dayItems = schedule.filter { it.scheduledDate in dayEpoch until nextDayEpoch }
        val dateStr = date.format(java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d"))

        AlertDialog(
            onDismissRequest = { selectedDate = null },
            title = { Text(dateStr) },
            text = {
                if (dayItems.isEmpty()) {
                    Text("Nothing scheduled", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        dayItems.forEach { item ->
                            val name = item.templateName ?: item.label ?: "Workout"
                            val status = when {
                                item.isCompleted -> " (done)"
                                item.isSkipped -> " (skipped)"
                                else -> ""
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "$name$status",
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (item.isCompleted && item.completedWorkoutLogId != null) {
                                    TextButton(onClick = {
                                        selectedDate = null
                                        navController.navigate(Screen.WorkoutDetail.createRoute(item.completedWorkoutLogId))
                                    }) { Text("View") }
                                } else if (!item.isCompleted && !item.isSkipped && item.templateId != null) {
                                    TextButton(onClick = {
                                        selectedDate = null
                                        workoutViewModel.startWorkout(
                                            name = item.templateName ?: "Workout",
                                            type = com.workout.tracker.data.entity.WorkoutType.STRENGTH,
                                            templateId = item.templateId,
                                            scheduledWorkoutId = item.id
                                        )
                                        navController.navigate(Screen.ActiveWorkout.route) {
                                            popUpTo(Screen.Home.route)
                                        }
                                    }) { Text("Start") }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    selectedDate = null
                    navController.navigate(Screen.Schedule.route)
                }) { Text("Full calendar") }
            },
            dismissButton = {
                TextButton(onClick = { selectedDate = null }) { Text("Close") }
            }
        )
    }

    // Lifetime stats dialog
    if (showLifetimeStats) {
        AlertDialog(
            onDismissRequest = { showLifetimeStats = false },
            title = { Text("Lifetime Stats") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    LifetimeStatRow(Icons.Default.EmojiEvents, "Total Workouts", "${dashboardStats.totalWorkouts}")
                    LifetimeStatRow(Icons.Default.DateRange, "This Week", "${dashboardStats.workoutsThisWeek}")
                    LifetimeStatRow(Icons.Default.LocalFireDepartment, "Current Streak", "${dashboardStats.currentStreak}")
                }
            },
            confirmButton = {
                TextButton(onClick = { showLifetimeStats = false }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun LifetimeStatRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardStatCard(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: (() -> Unit)? = null
) {
    Card(
        onClick = { onClick?.invoke() },
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SegmentedToggle(isWeekView: Boolean, onToggle: (Boolean) -> Unit) {
    Row {
        FilterChip(
            selected = isWeekView,
            onClick = { onToggle(true) },
            label = { Text("Week", style = MaterialTheme.typography.labelSmall) }
        )
        Spacer(Modifier.width(4.dp))
        FilterChip(
            selected = !isWeekView,
            onClick = { onToggle(false) },
            label = { Text("Month", style = MaterialTheme.typography.labelSmall) }
        )
    }
}

@Composable
private fun HomeWeekView(
    weekStart: LocalDate,
    schedule: List<com.workout.tracker.data.dao.ScheduledWorkoutWithTemplate>,
    onNavigateWeek: (Int) -> Unit,
    onDayClick: (LocalDate) -> Unit,
    onReschedule: (com.workout.tracker.data.dao.ScheduledWorkoutWithTemplate, LocalDate) -> Unit = { _, _ -> }
) {
    val today = LocalDate.now()
    val weekEnd = weekStart.plusDays(6)
    val dateFormat = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }
    val startStr = dateFormat.format(Date.from(weekStart.atStartOfDay(ZoneId.systemDefault()).toInstant()))
    val endStr = dateFormat.format(Date.from(weekEnd.atStartOfDay(ZoneId.systemDefault()).toInstant()))
    val scheduleByDay = schedule.groupBy { sw ->
        Instant.ofEpochMilli(sw.scheduledDate).atZone(ZoneId.systemDefault()).toLocalDate()
    }
    var draggedItemId by remember { mutableStateOf<Long?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    val rowCenters = remember { mutableStateMapOf<Int, Float>() }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isPastWeek = weekEnd.isBefore(today)
                IconButton(onClick = { onNavigateWeek(-1) }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous week")
                }
                Text(
                    "$startStr - $endStr",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isPastWeek) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = { onNavigateWeek(1) }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next week")
                }
            }
            for (dayOffset in 0L..6L) {
                val date = weekStart.plusDays(dayOffset)
                val dayItems = scheduleByDay[date] ?: emptyList()
                val isToday = date == today
                val isPast = date.isBefore(today)
                val workoutItems = dayItems.filter { it.label?.lowercase()?.contains("rest") != true }
                val currentDayOffset = dayOffset.toInt()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coords ->
                            rowCenters[currentDayOffset] =
                                coords.positionInParent().y + coords.size.height / 2f
                        }
                        .clickable { onDayClick(date) }
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.width(56.dp)) {
                        Text(
                            date.dayOfWeek.getDisplayName(JavaTextStyle.SHORT, Locale.getDefault()),
                            style = MaterialTheme.typography.labelSmall,
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
                    if (dayItems.isEmpty()) {
                        Text(
                            "—",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    } else {
                        Column(modifier = Modifier.weight(1f)) {
                            dayItems.forEach { item ->
                                val displayName = item.templateName ?: item.label ?: "Workout"
                                val isDragging = draggedItemId == item.id
                                val canDrag = !item.isCompleted && !item.isSkipped
                                Text(
                                    displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    modifier = Modifier
                                        .graphicsLayer {
                                            if (isDragging) {
                                                translationY = dragOffsetY
                                                shadowElevation = 16f
                                                scaleX = 1.03f; scaleY = 1.03f
                                                alpha = 0.9f
                                            }
                                        }
                                        .then(
                                            if (canDrag) Modifier.pointerInput(item.id) {
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
                                            } else Modifier
                                        )
                                )
                            }
                        }
                    }
                    if (workoutItems.isNotEmpty()) {
                        val allDone = workoutItems.all { it.isCompleted }
                        val anyMissed = isPast && workoutItems.any { !it.isCompleted && !it.isSkipped }
                        val anySkipped = workoutItems.any { it.isSkipped }
                        when {
                            allDone -> Icon(
                                Icons.Default.CheckCircle, contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)
                            )
                            anyMissed || anySkipped -> Icon(
                                Icons.Default.Cancel, contentDescription = null,
                                tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeMonthView(
    yearMonth: YearMonth,
    schedule: List<com.workout.tracker.data.dao.ScheduledWorkoutWithTemplate>,
    onNavigateMonth: (Int) -> Unit,
    onDayClick: (LocalDate) -> Unit
) {
    val today = LocalDate.now()
    val firstOfMonth = yearMonth.atDay(1)
    val startOffset = firstOfMonth.dayOfWeek.value - 1
    val daysInMonth = yearMonth.lengthOfMonth()
    val scheduleByDay = schedule.groupBy { sw ->
        Instant.ofEpochMilli(sw.scheduledDate).atZone(ZoneId.systemDefault()).toLocalDate()
    }
    val totalCells = startOffset + daysInMonth
    val numWeeks = (totalCells + 6) / 7

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isPastMonth = yearMonth.isBefore(YearMonth.now())
                IconButton(onClick = { onNavigateMonth(-1) }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month")
                }
                Text(
                    "${yearMonth.month.getDisplayName(JavaTextStyle.FULL, Locale.getDefault())} ${yearMonth.year}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isPastMonth) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = { onNavigateMonth(1) }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next month")
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
                    DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY).forEach { dow ->
                    Text(
                        dow.getDisplayName(JavaTextStyle.SHORT, Locale.getDefault()).take(1),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            for (week in 0 until numWeeks) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (dayInWeek in 0 until 7) {
                        val cellIndex = week * 7 + dayInWeek
                        val dayOfMonth = cellIndex - startOffset + 1
                        if (dayOfMonth in 1..daysInMonth) {
                            val date = yearMonth.atDay(dayOfMonth)
                            val dayItems = scheduleByDay[date] ?: emptyList()
                            val isToday = date == today
                            val isPast = date.isBefore(today)
                            val workoutItems = dayItems.filter { it.label?.lowercase()?.contains("rest") != true }
                            val allDone = workoutItems.isNotEmpty() && workoutItems.all { it.isCompleted }
                            val anyMissed = isPast && workoutItems.any { !it.isCompleted && !it.isSkipped }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .clickable { onDayClick(date) },
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (isToday) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surface,
                                    border = if (isToday) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            "$dayOfMonth",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                                        )
                                        when {
                                            allDone -> Icon(
                                                Icons.Default.CheckCircle, contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(10.dp)
                                            )
                                            anyMissed -> Icon(
                                                Icons.Default.Cancel, contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(10.dp)
                                            )
                                            workoutItems.isNotEmpty() -> Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                                    .background(MaterialTheme.colorScheme.primary)
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickActionCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(72.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 2)
        }
    }
}
