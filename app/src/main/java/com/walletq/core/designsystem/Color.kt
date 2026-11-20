package com.walletq.core.designsystem

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Fintech Palette: Blue, Teal, and Neutral

// Primary Blue
val Blue90 = Color(0xFFDDE7FD)
val Blue80 = Color(0xFF93C5FD)
val Blue60 = Color(0xFF3B82F6)
val Blue40 = Color(0xFF0A58CA)
val Blue30 = Color(0xFF07439C)
val Blue20 = Color(0xFF002B66)

// Teal Accent
val Teal90 = Color(0xFFCCFBF1)
val Teal80 = Color(0xFF5EEAD4)
val Teal50 = Color(0xFF14B8A6)
val Teal40 = Color(0xFF0D9488)
val Teal30 = Color(0xFF0F766E)
val Teal20 = Color(0xFF003731)

// Neutral Slate Palette
val Slate50 = Color(0xFFF8FAFC)
val Slate100 = Color(0xFFF1F5F9)
val Slate200 = Color(0xFFE2E8F0)
val Slate300 = Color(0xFFCBD5E1)
val Slate400 = Color(0xFF94A3B8)
val Slate500 = Color(0xFF64748B)
val Slate600 = Color(0xFF475569)
val Slate700 = Color(0xFF334155)
val Slate800 = Color(0xFF1E293B)
val Slate900 = Color(0xFF0F172A)
val Slate950 = Color(0xFF090D16)

// Semantic Financial Colors
val IncomeGreen = Color(0xFF10B981)
val ExpenseRed = Color(0xFFEF4444)
val WarningAmber = Color(0xFFF59E0B)

val LightColorScheme = lightColorScheme(
    primary = Blue40,
    onPrimary = Color.White,
    primaryContainer = Blue90,
    onPrimaryContainer = Blue20,
    secondary = Teal40,
    onSecondary = Color.White,
    secondaryContainer = Teal90,
    onSecondaryContainer = Teal20,
    tertiary = Color(0xFF0284C7),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE0F2FE),
    onTertiaryContainer = Color(0xFF082F49),
    background = Slate50,
    onBackground = Slate900,
    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate600,
    outline = Slate300,
    outlineVariant = Slate200
)

val DarkColorScheme = darkColorScheme(
    primary = Blue80,
    onPrimary = Blue20,
    primaryContainer = Blue30,
    onPrimaryContainer = Blue90,
    secondary = Teal80,
    onSecondary = Teal20,
    secondaryContainer = Teal30,
    onSecondaryContainer = Teal90,
    tertiary = Color(0xFF7DD3FC),
    onTertiary = Color(0xFF075985),
    tertiaryContainer = Color(0xFF0284C7),
    onTertiaryContainer = Color(0xFFF0F9FF),
    background = Slate950,
    onBackground = Slate100,
    surface = Slate900,
    onSurface = Slate100,
    surfaceVariant = Slate800,
    onSurfaceVariant = Slate400,
    outline = Slate700,
    outlineVariant = Slate800
)
