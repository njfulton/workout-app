package com.workout.tracker.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Blue-focused color palette
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF82B1FF),           // Light blue
    onPrimary = Color(0xFF002F6C),
    primaryContainer = Color(0xFF0D47A1),  // Deep blue container
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = Color(0xFF64B5F6),         // Medium blue
    onSecondary = Color(0xFF003258),
    secondaryContainer = Color(0xFF1565C0),
    onSecondaryContainer = Color(0xFFD1E4FF),
    tertiary = Color(0xFF4FC3F7),          // Sky blue
    onTertiary = Color(0xFF003547),
    tertiaryContainer = Color(0xFF004D67),
    onTertiaryContainer = Color(0xFFBDE9FF),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0D1117),        // Dark navy
    onBackground = Color(0xFFE3E8F0),
    surface = Color(0xFF161B22),           // Slightly lighter navy
    onSurface = Color(0xFFE3E8F0),
    surfaceVariant = Color(0xFF1C2333),    // Card/chip backgrounds
    onSurfaceVariant = Color(0xFFC0CAD8),
    outline = Color(0xFF3D4F66),
    outlineVariant = Color(0xFF2D3A4E)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1565C0),           // Strong blue
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1E4FF),  // Light blue container
    onPrimaryContainer = Color(0xFF001D36),
    secondary = Color(0xFF1976D2),         // Medium blue
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFBBDEFB),
    onSecondaryContainer = Color(0xFF001D36),
    tertiary = Color(0xFF0288D1),          // Sky blue
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFB3E5FC),
    onTertiaryContainer = Color(0xFF001F2A),
    error = Color(0xFFD32F2F),
    onError = Color.White,
    errorContainer = Color(0xFFFFCDD2),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF5F7FA),        // Cool grey background
    onBackground = Color(0xFF1A1C1E),
    surface = Color.White,
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFE8EDF4),    // Light blue-grey
    onSurfaceVariant = Color(0xFF44474F),
    outline = Color(0xFF74777F),
    outlineVariant = Color(0xFFC4C7D0)
)

@Composable
fun WorkoutTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
