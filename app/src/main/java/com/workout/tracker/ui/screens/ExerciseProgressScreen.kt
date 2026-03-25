package com.workout.tracker.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.workout.tracker.data.dao.ExerciseProgressEntry
import com.workout.tracker.ui.viewmodel.WorkoutViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseProgressScreen(
    navController: NavController,
    workoutViewModel: WorkoutViewModel,
    exerciseId: Long
) {
    val progressData by workoutViewModel.exerciseProgress.collectAsStateWithLifecycle()
    val exerciseName by workoutViewModel.progressExerciseName.collectAsStateWithLifecycle()

    LaunchedEffect(exerciseId) {
        // Name is already set before navigation
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
        } else {
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
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ProgressStatCard(
                            modifier = Modifier.weight(1f),
                            label = "Best",
                            value = "${maxWeight.toInt()} lbs"
                        )
                        ProgressStatCard(
                            modifier = Modifier.weight(1f),
                            label = "Current",
                            value = "${latestWeight.toInt()} lbs"
                        )
                        ProgressStatCard(
                            modifier = Modifier.weight(1f),
                            label = "Change",
                            value = "${if (weightChange >= 0) "+" else ""}${weightChange.toInt()} lbs"
                        )
                    }
                }

                // Estimated 1RM card
                item {
                    val est1RM by workoutViewModel.estimated1RM.collectAsStateWithLifecycle()
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
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "${oneRM.toInt()} lbs",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Based on your best working sets (Epley/Brzycki avg)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                                )
                                Spacer(Modifier.height(8.dp))
                                // Rep max estimates
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    listOf(3, 5, 8, 10).forEach { reps ->
                                        val weight = com.workout.tracker.util.OneRepMaxCalculator.weightForReps(oneRM, reps)
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

                // Weight chart
                item {
                    Text("Weight Over Time", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    WeightChart(
                        data = progressData,
                        modifier = Modifier.fillMaxWidth().height(220.dp),
                        lineColor = MaterialTheme.colorScheme.primary,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Volume chart
                item {
                    Text("Volume Over Time", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    VolumeChart(
                        data = progressData,
                        modifier = Modifier.fillMaxWidth().height(220.dp),
                        barColor = MaterialTheme.colorScheme.tertiary,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Session count
                item {
                    Text(
                        "${progressData.size} sessions tracked",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
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
