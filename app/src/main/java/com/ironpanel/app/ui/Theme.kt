package com.ironpanel.app.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val IronDark = darkColorScheme(
    primary = Color(0xFF00C8D7),
    secondary = Color(0xFF16A34A),
    tertiary = Color(0xFF7C3AED),
)
private val IronLight = lightColorScheme(
    primary = Color(0xFF0891B2),
    secondary = Color(0xFF16A34A),
    tertiary = Color(0xFF7C3AED),
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
