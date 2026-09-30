package com.ironpanel.app.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// 2026 VPN-app palette: deep space background, cyan→violet aurora accents.
val AuroraCyan = Color(0xFF00E5FF)
val AuroraViolet = Color(0xFF7C3AED)
val AuroraPink = Color(0xFFF0ABFC)
val SpaceBlack = Color(0xFF05070F)
val SpacePanel = Color(0xFF0C1120)
val GoodGreen = Color(0xFF34D399)
val WarnAmber = Color(0xFFFBBF24)
val BadRed = Color(0xFFF87171)

private val IronDark = darkColorScheme(
    primary = AuroraCyan,
    onPrimary = Color(0xFF00363D),
    secondary = AuroraViolet,
    tertiary = AuroraPink,
    background = SpaceBlack,
    surface = SpacePanel,
    surfaceVariant = Color(0xFF141B30),
    error = BadRed,
)
private val IronLight = lightColorScheme(
    primary = Color(0xFF0891B2),
    secondary = AuroraViolet,
    tertiary = Color(0xFFA21CAF),
    background = Color(0xFFF4F7FB),
    surface = Color.White,
    surfaceVariant = Color(0xFFE8EEF6),
    error = Color(0xFFDC2626),
)

/** themeMode: "system" | "light" | "dark" — responsive on phones + tablets. */
@Composable
fun IronTheme(themeMode: String, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    val context = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= 31 -> if (dark) dynamicDarkColorScheme(context)
        else dynamicLightColorScheme(context)
        dark -> IronDark
        else -> IronLight
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

@Composable
fun auroraBrush(): Brush = Brush.linearGradient(
    listOf(AuroraCyan, AuroraViolet, AuroraPink)
)

@Composable
fun heroBrush(dark: Boolean): Brush = if (dark) {
    Brush.verticalGradient(listOf(Color(0xFF0B1030), SpaceBlack))
} else {
    Brush.verticalGradient(listOf(Color(0xFFE0F7FF), Color(0xFFF4F7FB)))
}
