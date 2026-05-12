package com.workout.tracker.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
    var selectedDate by remember { mutableStateOf<java.time.LocalDate?>(null) }
    var showScheduleDialog by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var moveItem by remember { mutableStateOf<com.workout.tracker.data.dao.ScheduledWorkoutWithTemplate?>(null) }
    val templates by templateViewModel.templates.collectAsStateWithLifecycle()

    val todayFormatted = remember {
        val cal = Calendar.getInstance()
        val dayName = SimpleDateFormat("EEEE", Locale.getDefault()).format(cal.time)
        val monthDay = SimpleDateFormat("MMMM d", Locale.getDefault()).format(cal.time)
        "$dayName, $monthDay"
    }
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> "Good morning."
            hour < 17 -> "Good afternoon."
            else -> "Good evening."
        }
    }

    Scaffold(
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Pushup FAB
                SmallFloatingActionButton(
                    onClick = {
                        workoutViewModel.logFeatureUsage("pushups")
                        navController.navigate(Screen.Pushups.route)
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(100)
                ) {
                    Icon(Icons.Default.Sports, contentDescription = "Pushups", modifier = Modifier.size(20.dp))
                }
                // Schedule FAB
                FloatingActionButton(
                    onClick = { showScheduleDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Schedule workout")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Custom header — greeting + icon buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            todayFormatted.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                        Text(
                            greeting,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalIconButton(
                            onClick = { navController.navigate(Screen.WeeklySummary.route) },
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Icon(Icons.Default.BarChart, contentDescription = "Stats", modifier = Modifier.size(20.dp))
                        }
                        FilledTonalIconButton(
                            onClick = { navController.navigate(Screen.Utilities.route) },
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "More", modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            // Routine block + week ribbon (combined, top of page)
            routineOverview?.let { data ->
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate(Screen.RoutineOverview.route) },
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            // Current block header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "CURRENT BLOCK",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        letterSpacing = 1.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(3.dp))
                                    Text(
                                        data.routine.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    "W${data.currentWeek}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "/W${data.totalWeeks}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Week progress ticks
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                for (i in 1..data.totalWeeks) {
                                    val filled = i < data.currentWeek
                                    val current = i == data.currentWeek
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                when {
                                                    filled -> MaterialTheme.colorScheme.primary
                                                    current -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                                                    else -> MaterialTheme.colorScheme.outline
                                                }
                                            )
                                    )
                                }
                            }

                            // This Week label + schedule toggle
                            Spacer(Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "THIS WEEK",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Row(
                                    modifier = Modifier.clickable { isWeekView = !isWeekView },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        if (isWeekView) "Month" else "Week",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Compact 7-day ribbon
                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                val today = LocalDate.now()
                                val weekStart = today.with(java.time.DayOfWeek.MONDAY)
                                val scheduleByDay = weekSchedule.groupBy { sw ->
                                    Instant.ofEpochMilli(sw.scheduledDate).atZone(ZoneId.systemDefault()).toLocalDate()
                                }
                                for (offset in 0L..6L) {
                                    val date = weekStart.plusDays(offset)
                                    val isToday = date == today
                                    val dayItems = scheduleByDay[date] ?: emptyList()
                                    val workoutItems = dayItems.filter { it.label?.lowercase()?.contains("rest") != true }
                                    val allDone = workoutItems.isNotEmpty() && workoutItems.all { it.isCompleted }
                                    val isPast = date.isBefore(today)
                                    val anyMissed = isPast && workoutItems.any { !it.isCompleted && !it.isSkipped }
                                    val hasWorkout = workoutItems.isNotEmpty()
                                    val label = when {
                                        allDone -> null
                                        anyMissed -> null
                                        hasWorkout -> dayItems.firstOrNull()?.let { it.templateName?.take(4) ?: it.label?.take(4) }
                                        else -> if (dayItems.any { it.label?.lowercase()?.contains("rest") == true }) "Rest" else null
                                    }

                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isToday) MaterialTheme.colorScheme.primary
                                                else if (allDone) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.10f)
                                                else androidx.compose.ui.graphics.Color.Transparent
                                            )
                                            .then(
                                                if (!isToday) Modifier.border(
                                                    1.dp,
                                                    MaterialTheme.colorScheme.outline,
                                                    RoundedCornerShape(12.dp)
                                                ) else Modifier
                                            )
                                            .clickable { selectedDate = date }
                                            .padding(vertical = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            date.dayOfWeek.getDisplayName(JavaTextStyle.SHORT, Locale.getDefault()).take(2).uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isToday) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            "${date.dayOfMonth}",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isToday) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurface
                                        )
                                        when {
                                            allDone -> Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(11.dp),
                                                tint = MaterialTheme.colorScheme.tertiary
                                            )
                                            anyMissed -> Icon(
                                                Icons.Default.Close,
                                                contentDescription = null,
                                                modifier = Modifier.size(11.dp),
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                            label != null -> Text(
                                                label,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 9.sp,
                                                color = if (isToday) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                            else -> Spacer(Modifier.height(11.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // HERO: Today's workout card
            item {
                val workoutName: String
                val workoutDate: String
                val canStart: Boolean
                val templateId: Long?
                val scheduledId: Long?

                if (activeWorkout.isActive) {
                    workoutName = activeWorkout.workoutLog?.name ?: "Workout"
                    workoutDate = "IN PROGRESS"
                    canStart = true
                    templateId = null
                    scheduledId = null
                } else if (nextWorkout != null) {
                    workoutName = nextWorkout.templateName ?: nextWorkout.label ?: "Workout"
                    workoutDate = "TODAY"
                    canStart = nextWorkout.templateId != null
                    templateId = nextWorkout.templateId
                    scheduledId = nextWorkout.id
                } else {
                    workoutName = "No workout scheduled"
                    workoutDate = "TODAY"
                    canStart = false
                    templateId = null
                    scheduledId = null
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row {
                        // Lime left accent bar
                        Box(
                            modifier = Modifier
                                .width(6.dp)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Column(modifier = Modifier.padding(22.dp).weight(1f)) {
                            // Pill
                            Surface(
                                shape = RoundedCornerShape(100),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                            ) {
                                Text(
                                    if (activeWorkout.isActive) "IN PROGRESS" else workoutDate,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(
                                workoutName,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                lineHeight = 34.sp
                            )

                            Spacer(Modifier.height(20.dp))
                            if (activeWorkout.isActive) {
                                Button(
                                    onClick = { navController.navigate(Screen.ActiveWorkout.route) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(100),
                                    contentPadding = PaddingValues(16.dp)
                                ) {
                                    Text("Resume workout", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            } else if (canStart && templateId != null) {
                                Button(
                                    onClick = {
                                        workoutViewModel.logFeatureUsage("start_workout")
                                        workoutViewModel.startWorkout(
                                            name = workoutName,
                                            type = com.workout.tracker.data.entity.WorkoutType.STRENGTH,
                                            templateId = templateId,
                                            scheduledWorkoutId = scheduledId
                                        )
                                        navController.navigate(Screen.ActiveWorkout.route) {
                                            popUpTo(Screen.Home.route)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(100),
                                    contentPadding = PaddingValues(16.dp)
                                ) {
                                    Text("Start workout", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { navController.navigate(Screen.StartWorkout.route) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(100),
                                    contentPadding = PaddingValues(16.dp)
                                ) {
                                    Text("Quick start", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Stat chips: Streak + This Week
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SweatStatChip(
                        modifier = Modifier.weight(1f),
                        kicker = "STREAK",
                        value = dashboardStats.currentStreak.toString(),
                        unit = "days",
                        filled = true,
                        onClick = { showLifetimeStats = true }
                    )
                    SweatStatChip(
                        modifier = Modifier.weight(1f),
                        kicker = "THIS WEEK",
                        value = dashboardStats.workoutsThisWeek.toString(),
                        unit = "of 5",
                        filled = false,
                        onClick = { navController.navigate(Screen.WeeklySummary.route) }
                    )
                }
            }

            // Schedule section (full week/month view)
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

            item { Spacer(Modifier.height(80.dp)) }
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
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        dayItems.forEach { item ->
                            val name = item.templateName ?: item.label ?: "Workout"
                            val isDone = item.isCompleted || item.isSkipped
                            val isRestDay = item.label?.lowercase()?.contains("rest") == true

                            Column {
                                // Name + status
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val statusIcon = when {
                                        item.isCompleted -> Icons.Default.CheckCircle
                                        item.isSkipped -> Icons.Default.Cancel
                                        else -> Icons.Default.Circle
                                    }
                                    val statusColor = when {
                                        item.isCompleted -> MaterialTheme.colorScheme.primary
                                        item.isSkipped -> MaterialTheme.colorScheme.error
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                    Icon(statusIcon, null, modifier = Modifier.size(16.dp), tint = statusColor)
                                    Spacer(Modifier.width(8.dp))
                                    Text(name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                }

                                Spacer(Modifier.height(6.dp))

                                // Completed: View + Undo
                                if (item.isCompleted && item.completedWorkoutLogId != null) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = {
                                                selectedDate = null
                                                navController.navigate(Screen.WorkoutDetail.createRoute(item.completedWorkoutLogId))
                                            },
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(vertical = 6.dp)
                                        ) { Text("View") }
                                        OutlinedButton(
                                            onClick = { scheduleViewModel.markUncompleted(item) },
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(vertical = 6.dp)
                                        ) { Text("Undo") }
                                    }
                                }

                                // Skipped: Undo
                                if (item.isSkipped && !isRestDay) {
                                    OutlinedButton(
                                        onClick = { scheduleViewModel.markUncompleted(item) },
                                        modifier = Modifier.fillMaxWidth(),
                                        contentPadding = PaddingValues(vertical = 6.dp)
                                    ) { Text("Mark Incomplete") }
                                }

                                // Not done: Start / Done / Skip / Move
                                if (!isDone && !isRestDay) {
                                    if (item.templateId != null) {
                                        Button(
                                            onClick = {
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
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            contentPadding = PaddingValues(vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Start Workout")
                                        }
                                        Spacer(Modifier.height(4.dp))
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FilledTonalButton(
                                            onClick = { scheduleViewModel.markCompleted(item) },
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Done")
                                        }
                                        OutlinedButton(
                                            onClick = { scheduleViewModel.markSkipped(item) },
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Default.SkipNext, null, modifier = Modifier.size(14.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Skip")
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            moveItem = item
                                            selectedDate = null
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        contentPadding = PaddingValues(vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.EditCalendar, null, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Move")
                                    }
                                }
                            }
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedDate = null }) { Text("Close") }
            },
            confirmButton = {}
        )
    }

    // Move date dialog
    moveItem?.let { item ->
        MoveDateDialog(
            currentDateMillis = item.scheduledDate,
            onDismiss = { moveItem = null },
            onMove = { newDate ->
                scheduleViewModel.reschedule(item, newDate)
                moveItem = null
            }
        )
    }

    // Schedule workout dialog (from FAB)
    if (showScheduleDialog) {
        ScheduleWorkoutDialog(
            templates = templates,
            onDismiss = { showScheduleDialog = false },
            onScheduleTemplate = { templateId, date ->
                scheduleViewModel.scheduleWorkout(templateId, date)
                showScheduleDialog = false
            },
            onScheduleLabel = { label, date ->
                scheduleViewModel.scheduleNonTemplate(label, date)
                showScheduleDialog = false
            },
            onScheduleAerobic = { activityType, date, duration, distance, intensity ->
                scheduleViewModel.scheduleAerobicEvent(activityType, date, duration, distance, intensity)
                showScheduleDialog = false
            }
        )
    }

    // Clear future schedule confirmation
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear Future Schedule?") },
            text = { Text("This will remove all upcoming workouts that haven't been completed. Completed workout history will not be affected.") },
            confirmButton = {
                TextButton(onClick = {
                    scheduleViewModel.clearFutureSchedule()
                    showClearConfirm = false
                }) { Text("Clear", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
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

@Composable
private fun SweatStatChip(
    modifier: Modifier = Modifier,
    kicker: String,
    value: String,
    unit: String,
    filled: Boolean,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier.then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(18.dp),
        color = if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        border = if (!filled) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(
                kicker,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = if (filled) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (filled) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    unit,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (filled) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                if (onClick != null) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (filled) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
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
