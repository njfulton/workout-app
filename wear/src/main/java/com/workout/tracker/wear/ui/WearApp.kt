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

    MaterialTheme {
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
            modifier = Modifier.fillMaxSize().background(Color.Black),
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
                    color = Color.White
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Start a workout\non your phone",
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    color = Color.Gray
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
                        color = Color.DarkGray,
                        maxLines = 1
                    )
                    Text(
                        "${ago}s ago",
                        textAlign = TextAlign.Center,
                        fontSize = 9.sp,
                        color = Color.DarkGray
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

    Scaffold(timeText = { TimeText() }) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                state.workoutName.ifBlank { "Workout" },
                fontSize = 12.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Spacer(Modifier.height(2.dp))
            Text(
                state.exerciseName.ifBlank { "—" },
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
            Spacer(Modifier.height(4.dp))
            val setsText = if (state.totalSets > 0)
                "${state.setsDone} / ${state.totalSets} sets"
            else "${state.setsDone} sets"
            Text(
                setsText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colors.primary
            )
            if (state.lastSet.isNotBlank()) {
                Text(
                    "Last: ${state.lastSet}",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
            Spacer(Modifier.height(8.dp))
            CompactChip(
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
                label = { Text(if (sending) "…" else "Log Set") },
                colors = ChipDefaults.primaryChipColors()
            )
        }
    }
}

@Composable
private fun RestTimerScreen(state: WatchWorkoutState) {
    // Local countdown: compute remaining from restEndTimeMillis and tick every ~200ms
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

    Scaffold(timeText = { TimeText() }) {
        Column(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("REST", fontSize = 14.sp, color = Color.Gray)
            Spacer(Modifier.height(4.dp))
            Text(
                "${remainingSec / 60}:${(remainingSec % 60).toString().padStart(2, '0')}",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.primary
            )
            if (state.exerciseName.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    state.exerciseName,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
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
