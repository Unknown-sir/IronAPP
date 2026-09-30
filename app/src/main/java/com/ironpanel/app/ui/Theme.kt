package com.ironpanel.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// IronAPP 2026 identity: pitch black + signal red.
val BloodRed = Color(0xFFFF2E44)
val DeepRed = Color(0xFF8E0E1E)
val EmberOrange = Color(0xFFFF7A45)
val CoalBlack = Color(0xFF060608)
val CoalPanel = Color(0xFF101014)
val CoalRaised = Color(0xFF1A1A20)
val GoodGreen = Color(0xFF34D399)
val WarnAmber = Color(0xFFFBBF24)
val BadRed = Color(0xFFF87171)

private val IronDark = darkColorScheme(
    primary = BloodRed,
    onPrimary = Color.White,
    secondary = DeepRed,
    tertiary = EmberOrange,
    background = CoalBlack,
    surface = CoalPanel,
    surfaceVariant = CoalRaised,
    onBackground = Color(0xFFF5F5F5),
    onSurface = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFFA1A1AA),
    error = BadRed,
)
private val IronLight = lightColorScheme(
    primary = Color(0xFFD60F26),
    onPrimary = Color.White,
    secondary = DeepRed,
    tertiary = EmberOrange,
    background = Color(0xFFFAFAFA),
    surface = Color.White,
    surfaceVariant = Color(0xFFF1F1F4),
    error = Color(0xFFDC2626),
)

/** themeMode: "system" | "light" | "dark" — responsive on phones + tablets. */
@Composable
fun IronTheme(themeMode: String, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    }
    MaterialTheme(colorScheme = if (dark) IronDark else IronLight, content = content)
}

@Composable
fun bloodBrush(): Brush = Brush.linearGradient(listOf(BloodRed, DeepRed))

@Composable
fun emberBrush(): Brush = Brush.linearGradient(
    listOf(BloodRed, EmberOrange, DeepRed)
)

@Composable
fun heroBrush(dark: Boolean): Brush = if (dark) {
    Brush.verticalGradient(listOf(Color(0xFF14060A), CoalBlack))
} else {
    Brush.verticalGradient(listOf(Color(0xFFFFE9EC), Color(0xFFFAFAFA)))
}
