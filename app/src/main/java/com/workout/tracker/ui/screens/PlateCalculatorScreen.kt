package com.workout.tracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.workout.tracker.util.PlateCalculator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlateCalculatorScreen(navController: NavController) {
    var targetWeightText by remember { mutableStateOf("135") }
    var barWeightText by remember { mutableStateOf("45") }

    val targetWeight = targetWeightText.toDoubleOrNull() ?: 0.0
    val barWeight = barWeightText.toDoubleOrNull() ?: 45.0
    val loadout = remember(targetWeight, barWeight) {
        if (targetWeight > 0) PlateCalculator.calculate(targetWeight, barWeight) else null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Plate Calculator", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Input fields
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = targetWeightText,
                    onValueChange = { targetWeightText = it },
                    label = { Text("Target Weight (lbs)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = barWeightText,
                    onValueChange = { barWeightText = it },
                    label = { Text("Bar Weight") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            // Quick-select buttons
            Spacer(Modifier.height(12.dp))
            Text("Quick Select", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(135, 185, 225, 275, 315).forEach { weight ->
                    FilterChip(
                        selected = targetWeightText == weight.toString(),
                        onClick = { targetWeightText = weight.toString() },
                        label = { Text("$weight") }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Result
            loadout?.let { result ->
                if (targetWeight <= barWeight) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Just the bar!", style = MaterialTheme.typography.titleMedium)
                            Text("No plates needed", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    // Visual barbell representation
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Each Side",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(16.dp))

                            // Plate visualization
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Plates (largest to smallest, left to right)
                                result.platesPerSide.forEach { (plate, count) ->
                                    repeat(count) {
                                        PlateView(plate)
                                        Spacer(Modifier.width(2.dp))
                                    }
                                }
                                // Bar end
                                Box(
                                    modifier = Modifier
                                        .width(60.dp)
                                        .height(12.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                )
                            }

                            Spacer(Modifier.height(20.dp))

                            // Plate breakdown list
                            result.platesPerSide.forEach { (plate, count) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        PlateView(plate, compact = true)
                                        Spacer(Modifier.width(12.dp))
                                        Text(
                                            "${PlateCalculator.formatPlate(plate)} lb plate",
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                    Text(
                                        "x$count each side",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            if (!result.isExact) {
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    "Closest achievable: ${result.achievedWeight.toInt()} lbs",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Summary card
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "${result.totalPlates}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text("Total Plates", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val perSideWeight = (result.achievedWeight - result.barWeight) / 2
                                Text(
                                    "${perSideWeight.toInt()} lbs",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text("Per Side", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Reference table
            Text("Common Loads", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    val commonWeights = listOf(95, 135, 185, 225, 275, 315, 365, 405)
                    commonWeights.forEach { weight ->
                        val calc = PlateCalculator.calculate(weight.toDouble(), barWeight)
                        val plateStr = calc.platesPerSide.joinToString(" + ") { "${PlateCalculator.formatPlate(it.first)}x${it.second}" }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "$weight lbs",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.width(80.dp)
                            )
                            Text(
                                plateStr.ifEmpty { "Bar only" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PlateView(weight: Double, compact: Boolean = false) {
    val height = if (compact) 28.dp else when {
        weight >= 45 -> 80.dp
        weight >= 35 -> 70.dp
        weight >= 25 -> 60.dp
        weight >= 10 -> 45.dp
        weight >= 5 -> 35.dp
        else -> 28.dp
    }
    val width = if (compact) 8.dp else 14.dp
    val color = when {
        weight >= 45 -> Color(0xFF1565C0) // Blue
        weight >= 35 -> Color(0xFFFFB300) // Yellow
        weight >= 25 -> Color(0xFF2E7D32) // Green
        weight >= 10 -> Color(0xFF424242) // Dark gray
        weight >= 5 -> Color(0xFF757575) // Gray
        else -> Color(0xFFBDBDBD) // Light gray
    }

    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(2.dp))
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        if (!compact && height >= 45.dp) {
            Text(
                PlateCalculator.formatPlate(weight),
                fontSize = 8.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
