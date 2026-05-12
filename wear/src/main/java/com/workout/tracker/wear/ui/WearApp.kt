package com.workout.tracker.wear.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import com.workout.tracker.wear.WatchMessageSender
import com.workout.tracker.wear.WatchState
import com.workout.tracker.wear.WatchWorkoutState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun WearApp() {
    val state by WatchState.state.collectAsState()
    val restFinishedTick by WatchState.restFinishedTick.collectAsState()
    val context = LocalContext.current

    // Buzz the watch when rest hits zero
    LaunchedEffect(restFinishedTick) {
        if (restFinishedTick > 0L) vibrate(context)
    }

    // Sweat theme: lime accent on near-black
    val sweatColors = MaterialTheme.colors.copy(
        primary = Color(0xFFD4FF3D),
        primaryVariant = Color(0xFFD4FF3D),
        secondary = Color(0xFFD4FF3D),
        background = Color(0xFF0A0A0B),
        surface = Color(0xFF141416),
        onPrimary = Color(0xFF0A0A0B),
        onBackground = Color(0xFFFAFAFA),
        onSurface = Color(0xFFFAFAFA)
    )

    MaterialTheme(colors = sweatColors) {
        when {
            state.restRunning -> RestTimerScreen(state)
            state.isActive -> ActiveWorkoutScreen(state, context)
            else -> IdleScreen(state)
        }
    }
}

@Composable
private fun IdleScreen(state: WatchWorkoutState) {
    Scaffold(timeText = { TimeText() }) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0B)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text(
                    "Workout\nTracker",
                    textAlign = TextAlign.Center,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFAFAFA)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Start a workout\non your phone",
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    color = Color(0xFF9A9AA2)
                )
                // Diagnostic: show last-received message so user can verify
                // the phone→watch link without ADB.
                if (state.lastReceivedTs > 0L) {
                    Spacer(Modifier.height(12.dp))
                    val ago = ((System.currentTimeMillis() - state.lastReceivedTs) / 1000).coerceAtLeast(0)
                    Text(
                        "last msg: ${state.lastReceivedPath}",
                        textAlign = TextAlign.Center,
                        fontSize = 9.sp,
                        color = Color(0xFF5C5C66),
                        maxLines = 1
                    )
                    Text(
                        "${ago}s ago",
                        textAlign = TextAlign.Center,
                        fontSize = 9.sp,
                        color = Color(0xFF5C5C66)
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveWorkoutScreen(state: WatchWorkoutState, context: Context) {
    val scope = rememberCoroutineScope()
    var sending by remember { mutableStateOf(false) }

    val lime = Color(0xFFD4FF3D)
    val limeText = Color(0xFF0A0A0B)
    val total = if (state.totalSets > 0) state.totalSets else 4
    val done = state.setsDone
    val pct = if (total > 0) done.toFloat() / total else 0f

    Scaffold(timeText = { TimeText() }) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0B)),
            contentAlignment = Alignment.Center
        ) {
            // Sets-complete arc
            androidx.compose.foundation.Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val arcR = size.minDimension * 0.40f
                val strokeW = 10.dp.toPx()
                val topLeft = androidx.compose.ui.geometry.Offset(
                    (size.width - arcR * 2) / 2f,
                    (size.height - arcR * 2) / 2f
                )
                val arcSize = androidx.compose.ui.geometry.Size(arcR * 2, arcR * 2)

                // Track
                drawArc(
                    color = Color(0x1AFFFFFF),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = strokeW,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                )

                // Filled progress
                if (pct > 0f) {
                    drawArc(
                        color = lime,
                        startAngle = -90f,
                        sweepAngle = pct * 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = strokeW,
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    )
                }
            }

            // Center content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 40.dp)
            ) {
                // Exercise name
                Text(
                    state.exerciseName.ifBlank { "—" }.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF5C5C66),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    letterSpacing = 1.sp
                )

                // Big reps × weight
                val r = state.targetReps
                val w = state.targetWeight
                if (r != null && r > 0 && w != null && w > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "$r",
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFAFAFA)
                        )
                        Text(
                            " × ",
                            fontSize = 20.sp,
                            color = Color(0xFF5C5C66)
                        )
                        Text(
                            "${w.toInt()}",
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFAFAFA)
                        )
                    }
                    Text(
                        "lb",
                        fontSize = 11.sp,
                        color = Color(0xFF9A9AA2)
                    )
                } else if (r != null && r > 0) {
                    Text(
                        "$r reps",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFAFAFA)
                    )
                }

                Spacer(Modifier.height(6.dp))

                // Set counter pill
                Text(
                    "SET ${done + 1}/$total",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = lime,
                    letterSpacing = 0.5.sp
                )

                Spacer(Modifier.height(10.dp))

                // Big LOG SET button
                Chip(
                    onClick = {
                        if (!sending) {
                            sending = true
                            scope.launch {
                                WatchMessageSender.requestLogSet(context)
                                delay(500)
                                sending = false
                            }
                        }
                    },
                    label = {
                        Text(
                            if (sending) "…" else "LOG SET",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    colors = ChipDefaults.chipColors(
                        backgroundColor = lime,
                        contentColor = limeText
                    ),
                    modifier = Modifier.fillMaxWidth(0.7f).height(44.dp)
                )
            }
        }
    }
}

@Composable
private fun RestTimerScreen(state: WatchWorkoutState) {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(state.restEndTimeMillis) {
        while (state.restRunning) {
            now = System.currentTimeMillis()
            if (now >= state.restEndTimeMillis) {
                WatchState.markRestFinishedLocally()
                break
            }
            delay(200)
        }
    }
    val remainingMs = (state.restEndTimeMillis - now).coerceAtLeast(0L)
    val remainingSec = (remainingMs / 1000).toInt()

    val lime = Color(0xFFD4FF3D)
    val totalRestSec = state.restInitialSeconds.coerceAtLeast(1)
    val pct = remainingSec.toFloat() / totalRestSec

    Scaffold(timeText = { TimeText() }) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0B)),
            contentAlignment = Alignment.Center
        ) {
            // Countdown ring
            androidx.compose.foundation.Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val arcR = size.minDimension * 0.40f
                val strokeW = 10.dp.toPx()
                val topLeft = androidx.compose.ui.geometry.Offset(
                    (size.width - arcR * 2) / 2f,
                    (size.height - arcR * 2) / 2f
                )
                val arcSize = androidx.compose.ui.geometry.Size(arcR * 2, arcR * 2)

                drawArc(
                    color = Color(0x1AFFFFFF),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = strokeW,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                )

                if (pct > 0f) {
                    drawArc(
                        color = lime,
                        startAngle = -90f,
                        sweepAngle = pct * 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = strokeW,
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "REST",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF9A9AA2),
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "${remainingSec / 60}:${(remainingSec % 60).toString().padStart(2, '0')}",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = lime
                )
                if (state.exerciseName.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        state.exerciseName,
                        fontSize = 11.sp,
                        color = Color(0xFF9A9AA2),
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

private fun vibrate(context: Context) {
    try {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0, 200, 100, 200, 100, 400),
                    -1
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 200, 100, 200, 100, 400), -1)
        }
    } catch (_: Exception) { }
}
