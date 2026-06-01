package com.workout.tracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// "Sweat" design system — high-contrast athletic + soft modern
// Lime accent (#D4FF3D) replaces the old blue for active states, PRs, CTAs.
// Near-black layered surfaces for gym-lighting readability.

private val Lime = Color(0xFFD4FF3D)
private val LimeText = Color(0xFF0A0A0B)
private val LimeDim = Color(0x24D4FF3D)  // 14% alpha lime

private val Ok = Color(0xFF22C55E)
private val Bad = Color(0xFFFF3D6E)
private val Warn = Color(0xFFFFB020)

private val DarkColorScheme = darkColorScheme(
    primary = Lime,
    onPrimary = LimeText,
    primaryContainer = LimeDim,
    onPrimaryContainer = Lime,
    secondary = Color(0xFF9A9AA2),          // textDim — for secondary text
    onSecondary = Color(0xFFFAFAFA),
    secondaryContainer = Color(0xFF1C1C20), // surface
    onSecondaryContainer = Color(0xFFFAFAFA),
    tertiary = Ok,
    onTertiary = LimeText,
    tertiaryContainer = Color(0x1F22C55E),  // ok @ 12%
    onTertiaryContainer = Ok,
    error = Bad,
    onError = Color(0xFFFAFAFA),
    errorContainer = Color(0x1FFF3D6E),     // bad @ 12%
    onErrorContainer = Bad,
    background = Color(0xFF0A0A0B),         // near-black
    onBackground = Color(0xFFFAFAFA),
    surface = Color(0xFF141416),            // bgRaised
    onSurface = Color(0xFFFAFAFA),
    surfaceVariant = Color(0xFF1C1C20),     // surface
    onSurfaceVariant = Color(0xFF9A9AA2),   // textDim
    outline = Color(0x1AFFFFFF),            // hairline (6% white)
    outlineVariant = Color(0x1AFFFFFF)
)

// Light mode: inverted for outdoor use, lime accent stays
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF4A7A00),            // dark lime for contrast on white
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8FFB0),
    onPrimaryContainer = Color(0xFF1A2E00),
    secondary = Color(0xFF5C5C66),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8E8EC),
    onSecondaryContainer = Color(0xFF1A1A1E),
    tertiary = Color(0xFF1B8A3D),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD4F5DC),
    onTertiaryContainer = Color(0xFF002E0E),
    error = Color(0xFFD42050),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF5F5F7),
    onBackground = Color(0xFF0A0A0B),
    surface = Color.White,
    onSurface = Color(0xFF0A0A0B),
    surfaceVariant = Color(0xFFECECF0),
    onSurfaceVariant = Color(0xFF5C5C66),
    outline = Color(0xFFD0D0D8),
    outlineVariant = Color(0xFFE0E0E4)
)

private val WorkoutTypography = Typography(
    displayLarge = TextStyle(fontSize = 57.sp, lineHeight = 64.sp, fontWeight = FontWeight.Bold),
    displayMedium = TextStyle(fontSize = 45.sp, lineHeight = 52.sp, fontWeight = FontWeight.Bold),
    displaySmall = TextStyle(fontSize = 36.sp, lineHeight = 44.sp, fontWeight = FontWeight.Bold),
    headlineLarge = TextStyle(fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 18.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 18.sp, lineHeight = 26.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Normal),
    bodySmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold),
    labelSmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold)
)

// Card shape with 24dp radius matching the Sweat design spec
val SweatCardShape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)

@Composable
fun WorkoutTrackerTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = WorkoutTypography,
        shapes = Shapes(
            small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            medium = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
            large = SweatCardShape,
            extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(32.dp)
        ),
        content = content
    )
}
