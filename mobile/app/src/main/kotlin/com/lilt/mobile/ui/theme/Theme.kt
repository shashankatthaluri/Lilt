package com.lilt.mobile.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.lilt.mobile.data.SettingsManager

data class LiltGlassPalette(
    val isDark: Boolean,
    val background: Color,
    val card: Color,
    val cardElevated: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val cardBrush: Brush,
    val specularBorderBrush: Brush
)

val LocalLiltGlassPalette = staticCompositionLocalOf {
    LiltGlassPalette(
        isDark = true,
        background = Color(0xFF07080C),
        card = Color(0xFF121318),
        cardElevated = Color(0xFF1A1B22),
        border = Color(0xFF282A36),
        textPrimary = Color(0xFFF6F6F9),
        textSecondary = Color(0xFFA1A1AA),
        textMuted = Color(0xFF71717A),
        cardBrush = LiltGlassCardBrush,
        specularBorderBrush = LiltGlassSpecularBorderBrush
    )
}

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFF3B1F),
    secondary = Color(0xFFF6F6F9),
    tertiary = Color(0xFFA1A1AA),
    background = Color(0xFF07080C),
    surface = Color(0xFF121318),
    surfaceVariant = Color(0xFF1A1B22),
    onPrimary = Color.White,
    onSecondary = Color(0xFF07080C),
    onBackground = Color(0xFFF6F6F9),
    onSurface = Color(0xFFF6F6F9),
    onSurfaceVariant = Color(0xFFA1A1AA),
    outline = Color(0xFF282A36),
    error = Color(0xFFFF3B1F)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFFF3B1F),
    secondary = Color(0xFF111216),
    tertiary = Color(0xFF52525B),
    background = Color(0xFFF4F3EF),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEBEAE4),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF111216),
    onSurface = Color(0xFF111216),
    onSurfaceVariant = Color(0xFF52525B),
    outline = Color(0xFFDCDAD2),
    error = Color(0xFFD71921)
)

@Composable
fun LiltTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val settings = SettingsManager.getInstance(context)

    if (LiltThemeState.mode != settings.themeMode && LiltThemeState.mode == "dark" && settings.themeMode != "dark") {
        LiltThemeState.mode = settings.themeMode
    }

    val useDark = when (LiltThemeState.mode) {
        "light" -> false
        "system" -> darkTheme
        else -> true
    }
    LiltThemeState.isDark = useDark

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !useDark
                controller.isAppearanceLightNavigationBars = !useDark
            }
        }
    }

    val palette = LiltGlassPalette(
        isDark = useDark,
        background = NothingBlack,
        card = NothingCard,
        cardElevated = NothingCardElevated,
        border = NothingBorder,
        textPrimary = NothingTextPrimary,
        textSecondary = NothingTextSecondary,
        textMuted = NothingTextMuted,
        cardBrush = LiltGlassCardBrush,
        specularBorderBrush = LiltGlassSpecularBorderBrush
    )

    CompositionLocalProvider(LocalLiltGlassPalette provides palette) {
        MaterialTheme(
            colorScheme = if (useDark) DarkColorScheme else LightColorScheme,
            typography = Typography,
            content = content
        )
    }
}
