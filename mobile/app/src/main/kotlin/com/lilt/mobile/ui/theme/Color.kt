package com.lilt.mobile.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Reactive Universal Liquid Glass Theme State.
 * Supports:
 * - "dark"   -> Obsidian Liquid Glass (Deep dark translucent glass over warm aurora mesh)
 * - "light"  -> Alabaster Pearl Glass (Frosted daylight quartz over soft pearl mesh)
 * - "system" -> Follows device Dark / Light setting automatically
 */
object LiltThemeState {
    var mode by mutableStateOf("dark")
    var isDark by mutableStateOf(true)
}

// ============================================================================
// CORE REACTIVE LIQUID GLASS PALETTE
// ============================================================================

val LiltBlack: Color
    get() = if (LiltThemeState.isDark) Color(0xFF07080D) else Color(0xFFF3F2EE)

val LiltDark: Color
    get() = if (LiltThemeState.isDark) Color(0xB80B0D14) else Color(0xD9EAE8E1)

val LiltSurface: Color
    get() = if (LiltThemeState.isDark) Color(0xBF12141E) else Color(0xE0FFFFFF)

val LiltSurfaceElevated: Color
    get() = if (LiltThemeState.isDark) Color(0xD4191C28) else Color(0xF2FFFFFF)

val LiltSurfaceHover: Color
    get() = if (LiltThemeState.isDark) Color(0xE0222636) else Color(0xFFFFFFFF)

val LiltInput: Color
    get() = if (LiltThemeState.isDark) Color(0xCC11131C) else Color(0xE6F7F6F2)

// Specular Hairline Borders & Dividers
val LiltBorder: Color
    get() = if (LiltThemeState.isDark) Color(0x36FFFFFF) else Color(0x220F172A)

val LiltBorderSubtle: Color
    get() = if (LiltThemeState.isDark) Color(0x1FFFFFFF) else Color(0x140F172A)

val LiltBorderBright: Color
    get() = if (LiltThemeState.isDark) Color(0x59FFFFFF) else Color(0x380F172A)

val LiltBorderFocus: Color
    get() = if (LiltThemeState.isDark) Color(0x99FFFFFF) else Color(0xFF0F172A)

// Adaptive Typography Hierarchy
val LiltTextPrimary: Color
    get() = if (LiltThemeState.isDark) Color(0xFFF8FAFC) else Color(0xFF0F1117)

val LiltTextSecondary: Color
    get() = if (LiltThemeState.isDark) Color(0xFFA8ACB8) else Color(0xFF475569)

val LiltTextMuted: Color
    get() = if (LiltThemeState.isDark) Color(0xFF767B8A) else Color(0xFF64748B)

// Adaptive Accents
val LiltAccentWhite: Color
    get() = if (LiltThemeState.isDark) Color(0xFFFFFFFF) else Color(0xFF0F1117)

val LiltOnAccentWhite: Color
    get() = if (LiltThemeState.isDark) Color(0xFF090A0F) else Color(0xFFFFFFFF)

val LiltAccentOffWhite: Color
    get() = if (LiltThemeState.isDark) Color(0xFFE4E4E7) else Color(0xFF1E293B)

val LiltAccentPillBg: Color
    get() = if (LiltThemeState.isDark) Color(0xCC181B26) else Color(0xE8FFFFFF)

val LiltStatusActive: Color
    get() = if (LiltThemeState.isDark) Color(0xFFFFFFFF) else Color(0xFF0F1117)

// ============================================================================
// LIQUID GLASS OPTICAL BRUSHES (4-Layer Refractive Aesthetic)
// ============================================================================

val LiltGlassCardBrush: Brush
    get() = if (LiltThemeState.isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xD41B1E2B),
                Color(0xA610121A),
                Color(0xBF151824)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0xF5FFFFFF),
                Color(0xD9F8F7F4),
                Color(0xEBFFFFFF)
            )
        )
    }

val LiltGlassSpecularBorderBrush: Brush
    get() = if (LiltThemeState.isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color(0x66FFFFFF), // Bright top-left specular catch
                Color(0x14FFFFFF), // Translucent mid-rim
                Color(0x33FFFFFF)  // Subtle bottom-right caustic reflection
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFFFFFFF), // Pure quartz top-left reflection
                Color(0x1F0F172A), // Subtle structural edge
                Color(0x330F172A)  // Soft bottom-right definition
            )
        )
    }

val LiltNavGlassColor: Color
    get() = if (LiltThemeState.isDark) Color(0xE010121C) else Color(0xEBFFFFFF)

val LiltNavBorderBrush: Brush
    get() = if (LiltThemeState.isDark) {
        Brush.verticalGradient(listOf(Color(0x59FFFFFF), Color(0x18FFFFFF)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0x280F172A)))
    }

val LiltNavSelectedBrush: Brush
    get() = if (LiltThemeState.isDark) {
        Brush.verticalGradient(listOf(Color(0x40FFFFFF), Color(0x1AFFFFFF)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFF181A24), Color(0xFF0F1117)))
    }

val LiltNavSelectedTint: Color
    get() = Color.White

val LiltNavUnselectedTint: Color
    get() = if (LiltThemeState.isDark) Color(0xFF7C8292) else Color(0xFF64748B)

// ============================================================================
// SEMANTIC ALIASES (Used across all tabs in MainScreen.kt)
// ============================================================================

val NothingBlack: Color get() = LiltBlack
val NothingOledDark: Color get() = LiltDark
val NothingCard: Color get() = LiltSurface
val NothingCardElevated: Color get() = LiltSurfaceElevated
val NothingBorder: Color get() = LiltBorder
val NothingBorderSubtle: Color get() = LiltBorderSubtle
val NothingRed: Color get() = LiltAccentWhite
val NothingRedGlow: Color get() = if (LiltThemeState.isDark) Color(0x1AFFFFFF) else Color(0x140F172A)
val NothingRedBright: Color get() = LiltAccentWhite
val NothingTextPrimary: Color get() = LiltTextPrimary
val NothingTextSecondary: Color get() = LiltTextSecondary
val NothingTextMuted: Color get() = LiltTextMuted
val NothingGreen: Color get() = Color(0xFF10B981)
val NothingGreenGlow: Color get() = Color(0x2210B981)
val NothingPillBg: Color get() = LiltAccentPillBg

// Material 3 Dark Colors
val primaryDark = Color(0xFFFFFFFF)
val primaryContainerDark = Color(0xFF191C28)
val secondaryDark = Color(0xFFA8ACB8)
val tertiaryDark = Color(0xFF767B8A)
val backgroundDark = Color(0xFF07080D)
val surfaceDark = Color(0xFF12141E)

// Material 3 Light Colors
val primaryLight = Color(0xFF0F1117)
val primaryContainerLight = Color(0xFFFFFFFF)
val secondaryLight = Color(0xFF475569)
val tertiaryLight = Color(0xFF64748B)
val backgroundLight = Color(0xFFF3F2EE)
val surfaceLight = Color(0xFFFFFFFF)
