package com.workout.tracker.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.*
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
    scheduleViewModel: ScheduleViewModel
) {
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

            // Next Workout - prominent card
            if (nextWorkout != null && !activeWorkout.isActive) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                "Next Workout",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                nextWorkout.templateName ?: nextWorkout.label ?: "Workout",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                scheduleDateFormat.format(Date(nextWorkout.scheduledDate)),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            if (nextWorkout.label != null && nextWorkout.templateName != null) {
                                Text(
                                    nextWorkout.label,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }
                            if (nextWorkout.templateId != null) {
                                Spacer(Modifier.height(12.dp))
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
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Start Workout", style = MaterialTheme.typography.titleSmall)
                                }
                            }
                        }
                    }
                }
            }

            // Quick actions row - only the essentials
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionCard(Modifier.weight(1f), Icons.Default.Sports, "Pushups") {
                        workoutViewModel.logFeatureUsage("pushups")
                        navController.navigate(Screen.Pushups.route)
                    }
                    QuickActionCard(Modifier.weight(1f), Icons.Default.History, "Recent") {
                        workoutViewModel.logFeatureUsage("history")
                        navController.navigate(Screen.History.route)
                    }
                    QuickActionCard(Modifier.weight(1f), Icons.Default.PlayArrow, "Start\nWorkout") {
                        workoutViewModel.logFeatureUsage("start_workout")
                        navController.navigate(Screen.StartWorkout.route)
                    }
                    QuickActionCard(Modifier.weight(1f), Icons.Default.MoreHoriz, "More") {
                        navController.navigate(Screen.Utilities.route)
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
                        onDayClick = { navController.navigate(Screen.Schedule.route) }
                    )
                }
            } else {
                item {
                    HomeMonthView(
                        yearMonth = currentMonth,
                        schedule = monthSchedule,
                        onNavigateMonth = { scheduleViewModel.navigateMonth(it) },
                        onDayClick = { navController.navigate(Screen.Schedule.route) }
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
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
    onDayClick: (LocalDate) -> Unit
) {
    val today = LocalDate.now()
    val weekEnd = weekStart.plusDays(6)
    val dateFormat = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }
    val startStr = dateFormat.format(Date.from(weekStart.atStartOfDay(ZoneId.systemDefault()).toInstant()))
    val endStr = dateFormat.format(Date.from(weekEnd.atStartOfDay(ZoneId.systemDefault()).toInstant()))
    val scheduleByDay = schedule.groupBy { sw ->
        Instant.ofEpochMilli(sw.scheduledDate).atZone(ZoneId.systemDefault()).toLocalDate()
    }

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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
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
                                Text(
                                    displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1
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
