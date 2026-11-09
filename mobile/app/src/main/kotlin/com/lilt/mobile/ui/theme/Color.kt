package com.lilt.mobile.ui.theme

import androidx.compose.ui.graphics.Color

// Lilt Luxury Monochrome Design Palette (Aligned with Desktop VocaWin)
val LiltBlack = Color(0xFF000000)
val LiltDark = Color(0xFF070708)
val LiltSurface = Color(0xFF0E0E11)
val LiltSurfaceElevated = Color(0xFF16161A)
val LiltSurfaceHover = Color(0xFF1F1F24)
val LiltInput = Color(0xFF141418)

// Hairline Borders & Dividers
val LiltBorder = Color(0xFF222226)
val LiltBorderSubtle = Color(0xFF18181C)
val LiltBorderBright = Color(0xFF383840)
val LiltBorderFocus = Color(0xFF6E6E78)

// Monochrome Typography Hierarchy
val LiltTextPrimary = Color(0xFFFFFFFF)
val LiltTextSecondary = Color(0xFFA1A1AA)
val LiltTextMuted = Color(0xFF71717A)

// Subtle Accents (Zero eye strain, pure monochrome)
val LiltAccentWhite = Color(0xFFFFFFFF)
val LiltAccentOffWhite = Color(0xFFE4E4E7)
val LiltAccentPillBg = Color(0xFF18181C)
val LiltStatusActive = Color(0xFFFFFFFF)

// Legacy alias compatibility for smooth build
val NothingBlack = LiltBlack
val NothingOledDark = LiltDark
val NothingCard = LiltSurface
val NothingCardElevated = LiltSurfaceElevated
val NothingBorder = LiltBorder
val NothingBorderSubtle = LiltBorderSubtle
val NothingRed = Color(0xFFFFFFFF) // Replaced red with clean white
val NothingRedGlow = Color(0x1AFFFFFF)
val NothingRedBright = Color(0xFFFFFFFF)
val NothingTextPrimary = LiltTextPrimary
val NothingTextSecondary = LiltTextSecondary
val NothingTextMuted = LiltTextMuted
val NothingGreen = Color(0xFFFFFFFF) // Replaced bright green with subtle white
val NothingGreenGlow = Color(0x1AFFFFFF)
val NothingPillBg = LiltAccentPillBg

// Material 3 Dark Colors
val primaryDark = LiltAccentWhite
val primaryContainerDark = LiltSurfaceElevated
val secondaryDark = LiltTextSecondary
val tertiaryDark = LiltTextMuted
val backgroundDark = LiltBlack
val surfaceDark = LiltSurface

// Material 3 Light Colors (Dark theme default)
val primaryLight = LiltAccentWhite
val primaryContainerLight = LiltSurfaceElevated
val secondaryLight = LiltTextSecondary
val tertiaryLight = LiltTextMuted
val backgroundLight = LiltBlack
val surfaceLight = LiltSurface
