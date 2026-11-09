package com.lilt.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.lilt.mobile.data.SettingsManager

private val DarkColorScheme = darkColorScheme(
    primary = primaryDark,
    primaryContainer = primaryContainerDark,
    secondary = secondaryDark,
    tertiary = tertiaryDark,
    background = backgroundDark,
    surface = surfaceDark,
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC),
    outline = Color(0x36FFFFFF)
)

private val LightColorScheme = lightColorScheme(
    primary = primaryLight,
    primaryContainer = primaryContainerLight,
    secondary = secondaryLight,
    tertiary = tertiaryLight,
    background = backgroundLight,
    surface = surfaceLight,
    onBackground = Color(0xFF0F1117),
    onSurface = Color(0xFF0F1117),
    outline = Color(0x220F172A)
)

@Composable
fun LiltTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val settings = SettingsManager.getInstance(context)
    val systemDark = isSystemInDarkTheme()

    val savedMode = settings.themeMode
    val currentMode = LiltThemeState.mode.ifBlank { savedMode }
    val resolvedDark = when (currentMode) {
        "light" -> false
        "system" -> systemDark
        else -> true
    }

    SideEffect {
        LiltThemeState.isDark = resolvedDark
    }
    LiltThemeState.isDark = resolvedDark

    MaterialTheme(
        colorScheme = if (resolvedDark) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
