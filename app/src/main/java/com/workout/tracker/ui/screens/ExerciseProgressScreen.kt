package com.workout.tracker.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.workout.tracker.data.dao.ExerciseHistoryEntry
import com.workout.tracker.data.dao.ExerciseProgressEntry
import com.workout.tracker.ui.viewmodel.WorkoutViewModel
import com.workout.tracker.util.OneRepMaxCalculator
import java.text.SimpleDateFormat
import java.util.*

private enum class ChartMetric(val label: String) {
    OneRM("Est 1RM"),
    Weight("Top weight"),
    Volume("Volume"),
    Reps("Top reps")
}

private enum class ChartRange(val label: String, val days: Int) {
    OneMonth("1M", 30),
    ThreeMonths("3M", 90),
    SixMonths("6M", 180),
    OneYear("1Y", 365),
    All("ALL", Int.MAX_VALUE)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseProgressScreen(
    navController: NavController,
    workoutViewModel: WorkoutViewModel,
    exerciseId: Long
) {
    val progressData by workoutViewModel.exerciseProgress.collectAsStateWithLifecycle()
    val history by workoutViewModel.exerciseHistory.collectAsStateWithLifecycle()
    val exerciseName by workoutViewModel.progressExerciseName.collectAsStateWithLifecycle()
    val est1RM by workoutViewModel.estimated1RM.collectAsStateWithLifecycle()

    // Ensure data loads when coming from routes that didn't pre-load it.
    LaunchedEffect(exerciseId) {
        if (progressData.isEmpty() && history.isEmpty()) {
            workoutViewModel.loadExerciseProgress(exerciseId, exerciseName)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (progressData.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No data yet", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Complete some workouts with this exercise to see progress",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            return@Scaffold
        }

        // Group the raw history by session start time, preserving original set order.
        val sessions: List<SessionDetail> = remember(history) {
            history.filter { it.reps != null }
                .groupBy { it.startTime }
                .entries
                .map { (ts, entries) ->
                    val sorted = entries.sortedBy { it.setNumber }
                    val best1RM = sorted
                        .filter { !it.isWarmup && (it.weightLbs ?: 0.0) > 0 && it.reps!! in 1..12 }
                        .maxOfOrNull { OneRepMaxCalculator.estimate(it.weightLbs!!, it.reps!!) }
                    val topReps = sorted.filter { !it.isWarmup }.maxOfOrNull { it.reps!! } ?: 0
                    val topWeight = sorted.filter { !it.isWarmup }
                        .mapNotNull { it.weightLbs }.filter { it > 0 }.maxOrNull() ?: 0.0
                    SessionDetail(ts, sorted, best1RM, topReps, topWeight)
                }
                .sortedBy { it.startTime }
        }

        // Determine PR (personal record) sessions — each session where topWeight is an all-time high
        val prSessionTimes: Set<Long> = remember(sessions) {
            val result = mutableSetOf<Long>()
            var allTimeBest = 0.0
            for (session in sessions) {
                if (session.topWeight > allTimeBest) {
                    allTimeBest = session.topWeight
                    result.add(session.startTime)
                }
            }
            result
        }

        var selectedMetric by remember { mutableStateOf(ChartMetric.OneRM) }
        var selectedRange by remember { mutableStateOf(ChartRange.All) }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // -- Custom header row --
            item {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Circular back button
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        onClick = {
                            workoutViewModel.clearExerciseProgress()
                            navController.popBackStack()
                        }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "PROGRESS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                        Text(
                            exerciseName.ifEmpty { "Progress" },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    // Optional more button
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        onClick = { /* reserved for future menu */ }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "More",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // -- Hero 1RM card --
            item {
                est1RM?.let { oneRM ->
                    val firstSession1RM = sessions.firstOrNull()?.best1RM
                    val change = if (firstSession1RM != null && firstSession1RM > 0)
                        (oneRM - firstSession1RM) else null
                    val daySpan = if (sessions.size >= 2) {
                        val days = ((sessions.last().startTime - sessions.first().startTime) / 86400000).toInt()
                        days.coerceAtLeast(1)
                    } else null

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                "ESTIMATED 1RM",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Text(
                                    "${oneRM.toInt()}",
                                    style = TextStyle(
                                        fontSize = 52.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "lb",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 10.dp)
                                )
                            }
                            // Change indicator
                            if (change != null && daySpan != null) {
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (change >= 0) {
                                        Icon(
                                            Icons.Default.ArrowUpward,
                                            contentDescription = "Up",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                    }
                                    Text(
                                        "${if (change >= 0) "+" else ""}${change.toInt()} lb · ${daySpan}d",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(Modifier.height(16.dp))
                            // Hairline divider
                            @Suppress("DEPRECATION") Divider(
                                color = MaterialTheme.colorScheme.outline
                            )
                            Spacer(Modifier.height(16.dp))

                            // Rep max grid: 3RM, 5RM, 8RM, 10RM
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                listOf(3, 5, 8, 10).forEach { reps ->
                                    val weight = OneRepMaxCalculator.weightForReps(oneRM, reps)
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            "${reps}RM",
                                            style = TextStyle(
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                letterSpacing = 0.5.sp
                                            )
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            "${weight.toInt()}",
                                            style = TextStyle(
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // -- Chart card --
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Metric pills row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ChartMetric.values().forEach { metric ->
                                val isSelected = selectedMetric == metric
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedMetric = metric },
                                    label = {
                                        Text(
                                            metric.label,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))

                        // Chart — filter data by selected range
                        val now = System.currentTimeMillis()
                        val cutoff = if (selectedRange.days == Int.MAX_VALUE) 0L
                        else now - selectedRange.days.toLong() * 86400000L

                        val filteredProgress = progressData.filter { it.workoutDate >= cutoff }
                        val filteredSessions = sessions.filter { it.startTime >= cutoff }

                        MetricChart(
                            metric = selectedMetric,
                            progressData = filteredProgress,
                            sessions = filteredSessions,
                            modifier = Modifier.fillMaxWidth().height(220.dp)
                        )

                        Spacer(Modifier.height(12.dp))

                        // Range selector row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ChartRange.values().forEach { range ->
                                val isSelected = selectedRange == range
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clickable { selectedRange = range }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        range.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (isSelected) {
                                        Spacer(Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .width(24.dp)
                                                .height(2.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.primary,
                                                    RoundedCornerShape(1.dp)
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // -- Session history header --
            item {
                Text(
                    "SESSION HISTORY",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "${sessions.size} session${if (sessions.size == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // -- Detailed per-session rows, newest first --
            items(sessions.asReversed()) { session ->
                SessionRow(
                    session = session,
                    isPR = prSessionTimes.contains(session.startTime)
                )
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

private data class SessionDetail(
    val startTime: Long,
    val sets: List<ExerciseHistoryEntry>,
    val best1RM: Double?,
    val topReps: Int,
    val topWeight: Double
)

@Composable
private fun MetricChart(
    metric: ChartMetric,
    progressData: List<ExerciseProgressEntry>,
    sessions: List<SessionDetail>,
    modifier: Modifier = Modifier
) {
    when (metric) {
        ChartMetric.Weight -> WeightChart(
            data = progressData,
            modifier = modifier,
            lineColor = MaterialTheme.colorScheme.primary,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
        ChartMetric.Volume -> VolumeChart(
            data = progressData,
            modifier = modifier,
            barColor = MaterialTheme.colorScheme.primary,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
        ChartMetric.OneRM -> {
            val points = sessions.mapNotNull { s -> s.best1RM?.let { s.startTime to it } }
            LineMetricChart(
                points = points,
                unit = "lbs",
                modifier = modifier,
                lineColor = MaterialTheme.colorScheme.primary,
                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        ChartMetric.Reps -> {
            val points = sessions.map { it.startTime to it.topReps.toDouble() }
            LineMetricChart(
                points = points,
                unit = "reps",
                modifier = modifier,
                lineColor = MaterialTheme.colorScheme.primary,
                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SessionRow(session: SessionDetail, isPR: Boolean) {
    var expanded by remember(session.startTime) { mutableStateOf(false) }
    val dayFmt = remember { SimpleDateFormat("d", Locale.US) }
    val monthFmt = remember { SimpleDateFormat("MMM", Locale.US) }
    val working = session.sets.filter { !it.isWarmup }
    val warmups = session.sets.filter { it.isWarmup }

    val summary = when {
        working.all { (it.weightLbs ?: 0.0) > 0 } && working.isNotEmpty() ->
            working.joinToString(", ") { "${it.reps}×${it.weightLbs!!.toInt()}" }
        working.isNotEmpty() ->
            "${working.size} set${if (working.size == 1) "" else "s"}, " +
                working.joinToString(", ") { "${it.reps}" } + " reps"
        else -> "—"
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
    val prTintBg = MaterialTheme.colorScheme.primaryContainer

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isPR) {
                    Modifier.drawBehind {
                        // Lime left border for PR sessions
                        drawRoundRect(
                            color = primaryColor,
                            topLeft = Offset(0f, 0f),
                            size = Size(3.dp.toPx(), size.height),
                            cornerRadius = CornerRadius(2.dp.toPx())
                        )
                    }
                } else Modifier
            )
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPR) prTintBg else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Date block on the left: month abbrev above, day number bold
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(40.dp)
                ) {
                    Text(
                        monthFmt.format(Date(session.startTime)).uppercase(),
                        style = TextStyle(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Text(
                        dayFmt.format(Date(session.startTime)),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(12.dp))

                // Middle: best weight + sets summary
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        if (session.topWeight > 0) {
                            Text(
                                "${session.topWeight.toInt()}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                "lb",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 1.dp)
                            )
                        }
                        if (isPR) {
                            Spacer(Modifier.width(8.dp))
                            // PR pill
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = primaryColor
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        Icons.Default.EmojiEvents,
                                        contentDescription = "PR",
                                        modifier = Modifier.size(12.dp),
                                        tint = onPrimaryColor
                                    )
                                    Spacer(Modifier.width(3.dp))
                                    Text(
                                        "PR",
                                        style = TextStyle(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = onPrimaryColor
                                        )
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (expanded) Int.MAX_VALUE else 2
                    )
                }

                // Right side: e1RM value + expand icon
                Column(horizontalAlignment = Alignment.End) {
                    session.best1RM?.let { oneRM ->
                        Text(
                            "e1RM",
                            style = TextStyle(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Text(
                            "${oneRM.toInt()}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            AnimatedVisibility(expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    if (warmups.isNotEmpty()) {
                        Text(
                            "WARMUP",
                            style = TextStyle(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 0.5.sp
                            )
                        )
                        warmups.forEach { SessionSetLine(it, isWarmup = true) }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "WORKING",
                            style = TextStyle(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                    working.forEach { SessionSetLine(it, isWarmup = false) }
                    session.best1RM?.let { oneRM ->
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Best estimated 1RM this session: ${oneRM.toInt()} lbs",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionSetLine(entry: ExerciseHistoryEntry, isWarmup: Boolean) {
    val weight = entry.weightLbs
    val detail = buildString {
        append(entry.reps ?: 0)
        append(" reps")
        if (weight != null && weight > 0) {
            append(" @ ")
            append(if (weight == weight.toLong().toDouble()) weight.toLong().toString() else weight.toString())
            append(" lb")
        }
    }
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            "${entry.setNumber}.",
            modifier = Modifier.width(24.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            detail,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isWarmup) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ProgressStatCard(modifier: Modifier = Modifier, label: String, value: String) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface)
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun WeightChart(
    data: List<ExerciseProgressEntry>,
    modifier: Modifier = Modifier,
    lineColor: Color,
    labelColor: Color
) {
    val textMeasurer = rememberTextMeasurer()
    val dateFormat = remember { SimpleDateFormat("M/d", Locale.getDefault()) }

    Canvas(modifier = modifier.padding(16.dp)) {
        if (data.size < 2) {
            // Single point - just show the value
            val label = "${data.first().maxWeight.toInt()} lbs"
            drawText(textMeasurer, label, topLeft = Offset(size.width / 2 - 30, size.height / 2 - 10))
            return@Canvas
        }

        val paddingLeft = 50f
        val paddingBottom = 30f
        val paddingTop = 10f
        val paddingRight = 10f
        val chartWidth = size.width - paddingLeft - paddingRight
        val chartHeight = size.height - paddingBottom - paddingTop

        val weights = data.map { it.maxWeight }
        val minW = (weights.min() - 5).coerceAtLeast(0.0)
        val maxW = weights.max() + 5
        val range = (maxW - minW).coerceAtLeast(10.0)

        // Draw Y-axis labels
        val ySteps = 4
        for (i in 0..ySteps) {
            val value = minW + range * i / ySteps
            val y = paddingTop + chartHeight - (chartHeight * i / ySteps)
            drawLine(
                color = labelColor.copy(alpha = 0.2f),
                start = Offset(paddingLeft, y),
                end = Offset(paddingLeft + chartWidth, y),
                strokeWidth = 1f
            )
            drawText(
                textMeasurer,
                "${value.toInt()}",
                topLeft = Offset(0f, y - 8),
                style = TextStyle(fontSize = 10.sp, color = labelColor)
            )
        }

        // Draw line
        val path = Path()
        data.forEachIndexed { index, entry ->
            val x = paddingLeft + chartWidth * index / (data.size - 1)
            val y = paddingTop + chartHeight - (chartHeight * ((entry.maxWeight - minW) / range)).toFloat()
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, lineColor, style = Stroke(width = 3f, cap = StrokeCap.Round))

        // Draw dots
        data.forEachIndexed { index, entry ->
            val x = paddingLeft + chartWidth * index / (data.size - 1)
            val y = paddingTop + chartHeight - (chartHeight * ((entry.maxWeight - minW) / range)).toFloat()
            drawCircle(lineColor, radius = 4f, center = Offset(x, y))
        }

        // X-axis labels (show first, last, and ~2 in middle)
        val labelIndices = when {
            data.size <= 4 -> data.indices.toList()
            else -> listOf(0, data.size / 3, 2 * data.size / 3, data.size - 1)
        }
        for (i in labelIndices) {
            val x = paddingLeft + chartWidth * i / (data.size - 1)
            drawText(
                textMeasurer,
                dateFormat.format(Date(data[i].workoutDate)),
                topLeft = Offset(x - 15, paddingTop + chartHeight + 5),
                style = TextStyle(fontSize = 10.sp, color = labelColor)
            )
        }
    }
}

@Composable
fun VolumeChart(
    data: List<ExerciseProgressEntry>,
    modifier: Modifier = Modifier,
    barColor: Color,
    labelColor: Color
) {
    val textMeasurer = rememberTextMeasurer()
    val dateFormat = remember { SimpleDateFormat("M/d", Locale.getDefault()) }

    Canvas(modifier = modifier.padding(16.dp)) {
        if (data.isEmpty()) return@Canvas

        val paddingLeft = 50f
        val paddingBottom = 30f
        val paddingTop = 10f
        val paddingRight = 10f
        val chartWidth = size.width - paddingLeft - paddingRight
        val chartHeight = size.height - paddingBottom - paddingTop

        val maxVol = data.maxOf { it.totalVolume }.coerceAtLeast(1.0)
        val barWidth = (chartWidth / data.size * 0.7f).coerceAtMost(40f)

        data.forEachIndexed { index, entry ->
            val x = paddingLeft + chartWidth * (index + 0.5f) / data.size
            val barH = (chartHeight * (entry.totalVolume / maxVol)).toFloat()
            val y = paddingTop + chartHeight - barH

            drawRoundRect(
                color = barColor,
                topLeft = Offset(x - barWidth / 2, y),
                size = Size(barWidth, barH),
                cornerRadius = CornerRadius(4f, 4f)
            )
        }

        // Y-axis labels
        val ySteps = 4
        for (i in 0..ySteps) {
            val value = maxVol * i / ySteps
            val y = paddingTop + chartHeight - (chartHeight * i / ySteps)
            drawLine(
                color = labelColor.copy(alpha = 0.2f),
                start = Offset(paddingLeft, y),
                end = Offset(paddingLeft + chartWidth, y),
                strokeWidth = 1f
            )
            val label = if (value >= 1000) "${(value / 1000).toInt()}k" else "${value.toInt()}"
            drawText(
                textMeasurer,
                label,
                topLeft = Offset(0f, y - 8),
                style = TextStyle(fontSize = 10.sp, color = labelColor)
            )
        }

        // X-axis labels
        val labelIndices = when {
            data.size <= 5 -> data.indices.toList()
            else -> listOf(0, data.size / 3, 2 * data.size / 3, data.size - 1)
        }
        for (i in labelIndices) {
            val x = paddingLeft + chartWidth * (i + 0.5f) / data.size
            drawText(
                textMeasurer,
                dateFormat.format(Date(data[i].workoutDate)),
                topLeft = Offset(x - 15, paddingTop + chartHeight + 5),
                style = TextStyle(fontSize = 10.sp, color = labelColor)
            )
        }
    }
}

/**
 * Generic line chart over arbitrary (timestamp, value) points. Used for the
 * estimated-1RM and top-reps metric toggles on ExerciseProgress where the
 * data isn't a [ExerciseProgressEntry] row.
 */
@Composable
fun LineMetricChart(
    points: List<Pair<Long, Double>>,
    unit: String,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    if (points.size < 2) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                if (points.isEmpty()) "No data yet" else "Need 2+ sessions to chart",
                style = MaterialTheme.typography.bodySmall,
                color = labelColor
            )
        }
        return
    }
    val textMeasurer = rememberTextMeasurer()
    val dateFormat = remember { SimpleDateFormat("MM/dd", Locale.US) }
    Canvas(modifier = modifier) {
        val paddingLeft = 40f
        val paddingBottom = 30f
        val paddingTop = 10f
        val paddingRight = 10f
        val chartWidth = size.width - paddingLeft - paddingRight
        val chartHeight = size.height - paddingBottom - paddingTop

        val values = points.map { it.second }
        val minV = (values.min() * 0.95).coerceAtLeast(0.0)
        val maxV = values.max() * 1.05
        val range = (maxV - minV).coerceAtLeast(1.0)

        val ySteps = 4
        for (i in 0..ySteps) {
            val value = minV + range * i / ySteps
            val y = paddingTop + chartHeight - (chartHeight * i / ySteps)
            drawLine(
                color = labelColor.copy(alpha = 0.2f),
                start = Offset(paddingLeft, y),
                end = Offset(paddingLeft + chartWidth, y),
                strokeWidth = 1f
            )
            drawText(
                textMeasurer,
                "${value.toInt()}",
                topLeft = Offset(0f, y - 8),
                style = TextStyle(fontSize = 10.sp, color = labelColor)
            )
        }

        val stepX = chartWidth / (points.size - 1)
        val path = Path()
        points.forEachIndexed { i, (_, v) ->
            val x = paddingLeft + i * stepX
            val y = paddingTop + chartHeight - ((v - minV) / range * chartHeight).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = lineColor, style = Stroke(width = 4f, cap = StrokeCap.Round))

        // Dots at each point
        points.forEachIndexed { i, (_, v) ->
            val x = paddingLeft + i * stepX
            val y = paddingTop + chartHeight - ((v - minV) / range * chartHeight).toFloat()
            drawCircle(color = lineColor, radius = 5f, center = Offset(x, y))
        }

        // X-axis labels at first, middle, last
        val labelIndices = when {
            points.size <= 5 -> points.indices.toList()
            else -> listOf(0, points.size / 3, 2 * points.size / 3, points.size - 1)
        }
        for (i in labelIndices) {
            val x = paddingLeft + i * stepX
            drawText(
                textMeasurer,
                dateFormat.format(Date(points[i].first)),
                topLeft = Offset(x - 15, paddingTop + chartHeight + 5),
                style = TextStyle(fontSize = 10.sp, color = labelColor)
            )
        }

        // Unit label in top-right
        drawText(
            textMeasurer,
            unit,
            topLeft = Offset(size.width - paddingRight - 30, paddingTop),
            style = TextStyle(fontSize = 10.sp, color = labelColor)
        )
    }
}
