package com.workout.tracker.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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
    Weight("Weight"),
    Volume("Volume"),
    OneRM("Est. 1RM"),
    Reps("Top reps")
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
        topBar = {
            TopAppBar(
                title = { Text(exerciseName.ifEmpty { "Progress" }) },
                navigationIcon = {
                    IconButton(onClick = {
                        workoutViewModel.clearExerciseProgress()
                        navController.popBackStack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
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

        var selectedMetric by remember { mutableStateOf(ChartMetric.Weight) }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }

            // Summary stats
            item {
                val maxWeight = progressData.maxOf { it.maxWeight }
                val latestWeight = progressData.last().maxWeight
                val firstWeight = progressData.first().maxWeight
                val weightChange = latestWeight - firstWeight

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProgressStatCard(modifier = Modifier.weight(1f), label = "Best", value = "${maxWeight.toInt()} lbs")
                    ProgressStatCard(modifier = Modifier.weight(1f), label = "Current", value = "${latestWeight.toInt()} lbs")
                    ProgressStatCard(
                        modifier = Modifier.weight(1f),
                        label = "Change",
                        value = "${if (weightChange >= 0) "+" else ""}${weightChange.toInt()}"
                    )
                    ProgressStatCard(modifier = Modifier.weight(1f), label = "Sessions", value = "${progressData.size}")
                }
            }

            // Estimated 1RM card
            item {
                est1RM?.let { oneRM ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Estimated 1 Rep Max",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${oneRM.toInt()} lbs",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "From your best working set",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                listOf(3, 5, 8, 10).forEach { reps ->
                                    val weight = OneRepMaxCalculator.weightForReps(oneRM, reps)
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("${weight.toInt()}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                                        Text("${reps}RM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Chart with metric toggle
            item {
                Column {
                    Text(
                        "Progress",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ChartMetric.values().forEach { metric ->
                            FilterChip(
                                selected = selectedMetric == metric,
                                onClick = { selectedMetric = metric },
                                label = { Text(metric.label, style = MaterialTheme.typography.labelMedium) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    MetricChart(
                        metric = selectedMetric,
                        progressData = progressData,
                        sessions = sessions,
                        modifier = Modifier.fillMaxWidth().height(220.dp)
                    )
                }
            }

            // Session history header
            item {
                Text(
                    "History (${sessions.size} session${if (sessions.size == 1) "" else "s"})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Detailed per-session rows, newest first
            items(sessions.asReversed()) { session ->
                SessionRow(session)
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
            barColor = MaterialTheme.colorScheme.tertiary,
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
                lineColor = MaterialTheme.colorScheme.secondary,
                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SessionRow(session: SessionDetail) {
    var expanded by remember(session.startTime) { mutableStateOf(false) }
    val dateFmt = remember { SimpleDateFormat("EEE, MMM d, yyyy", Locale.US) }
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

    Card(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        dateFmt.format(Date(session.startTime)),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (expanded) Int.MAX_VALUE else 2
                    )
                }
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    if (warmups.isNotEmpty()) {
                        Text(
                            "Warmup",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        warmups.forEach { SessionSetLine(it, isWarmup = true) }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Working",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
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

    Card(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
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

    Card(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
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
                    size = androidx.compose.ui.geometry.Size(barWidth, barH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
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
