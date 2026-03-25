package com.workout.tracker.wear.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController

@Composable
fun WearApp() {
    val navController = rememberSwipeDismissableNavController()

    MaterialTheme {
        SwipeDismissableNavHost(
            navController = navController,
            startDestination = "home"
        ) {
            composable("home") {
                WearHomeScreen(
                    onStartWorkout = { navController.navigate("workout") },
                    onQuickLog = { navController.navigate("quick_log") }
                )
            }
            composable("workout") {
                WearActiveWorkoutScreen(
                    onFinish = { navController.popBackStack() }
                )
            }
            composable("quick_log") {
                WearQuickLogScreen(
                    onDone = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun WearHomeScreen(
    onStartWorkout: () -> Unit,
    onQuickLog: () -> Unit
) {
    val listState = rememberScalingLazyListState()

    Scaffold(
        timeText = { TimeText() },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
    ) {
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            item {
                Text(
                    "Workout\nTracker",
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            item {
                Spacer(Modifier.height(8.dp))
            }

            item {
                Chip(
                    onClick = onStartWorkout,
                    label = { Text("Start Workout") },
                    modifier = Modifier.fillMaxWidth(0.9f),
                    colors = ChipDefaults.primaryChipColors()
                )
            }

            item {
                Chip(
                    onClick = onQuickLog,
                    label = { Text("Quick Log") },
                    modifier = Modifier.fillMaxWidth(0.9f),
                    colors = ChipDefaults.secondaryChipColors()
                )
            }
        }
    }
}

@Composable
fun WearActiveWorkoutScreen(onFinish: () -> Unit) {
    var elapsedSeconds by remember { mutableStateOf(0L) }
    var setCount by remember { mutableStateOf(0) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var restSeconds by remember { mutableStateOf(0) }

    // Elapsed timer
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            elapsedSeconds++
            if (isTimerRunning && restSeconds > 0) {
                restSeconds--
                if (restSeconds == 0) isTimerRunning = false
            }
        }
    }

    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60

    Scaffold(
        timeText = { TimeText() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isTimerRunning) {
                // Rest timer display
                Text("REST", fontSize = 14.sp, color = Color.Gray)
                Text(
                    "${restSeconds / 60}:${(restSeconds % 60).toString().padStart(2, '0')}",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.primary
                )
                Spacer(Modifier.height(8.dp))
                CompactChip(
                    onClick = { isTimerRunning = false; restSeconds = 0 },
                    label = { Text("Skip") }
                )
            } else {
                // Workout display
                Text(
                    "$minutes:${seconds.toString().padStart(2, '0')}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text("$setCount sets", fontSize = 14.sp, color = Color.Gray)
                Spacer(Modifier.height(8.dp))

                CompactChip(
                    onClick = {
                        setCount++
                        isTimerRunning = true
                        restSeconds = 90
                    },
                    label = { Text("Log Set") },
                    colors = ChipDefaults.primaryChipColors()
                )

                Spacer(Modifier.height(4.dp))

                CompactChip(
                    onClick = onFinish,
                    label = { Text("Finish") },
                    colors = ChipDefaults.secondaryChipColors()
                )
            }
        }
    }
}

@Composable
fun WearQuickLogScreen(onDone: () -> Unit) {
    var reps by remember { mutableStateOf(10) }
    var logged by remember { mutableStateOf(false) }

    Scaffold(
        timeText = { TimeText() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (logged) {
                Text("Logged!", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colors.primary)
                Spacer(Modifier.height(8.dp))
                CompactChip(
                    onClick = onDone,
                    label = { Text("Done") }
                )
            } else {
                Text("Reps", fontSize = 14.sp, color = Color.Gray)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CompactChip(
                        onClick = { if (reps > 1) reps-- },
                        label = { Text("-") }
                    )
                    Text("$reps", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    CompactChip(
                        onClick = { reps++ },
                        label = { Text("+") }
                    )
                }

                Spacer(Modifier.height(8.dp))

                CompactChip(
                    onClick = { logged = true },
                    label = { Text("Log") },
                    colors = ChipDefaults.primaryChipColors()
                )
            }
        }
    }
}
