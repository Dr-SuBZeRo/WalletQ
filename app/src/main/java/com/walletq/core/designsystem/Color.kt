package com.walletq.core.designsystem

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Midnight Luxe — Dark Theme Palette
val MidnightBackground = Color(0xFF101522)
val MidnightSurface = Color(0xFF171F2E)
val MidnightElevatedSurface = Color(0xFF202B3D)
val MidnightPrimary = Color(0xFFA8C4FF)
val MidnightOnPrimary = Color(0xFF0A1E42)
val MidnightPrimaryContainer = Color(0xFF1E3258)
val MidnightOnPrimaryContainer = Color(0xFFDCE7FF)
val MidnightSecondary = Color(0xFF8FA7D8)
val MidnightOnSecondary = Color(0xFF11203D)
val MidnightSecondaryContainer = Color(0xFF22304B)
val MidnightOnSecondaryContainer = Color(0xFFD6E2FA)
val MidnightSuccess = Color(0xFF62D6A5)
val MidnightError = Color(0xFFF28E9B)
val MidnightWarning = Color(0xFFF4BF6E)
val MidnightTextPrimary = Color(0xFFF4F6FC)
val MidnightTextSecondary = Color(0xFF9BA8BD)
val MidnightOutline = Color(0xFF2E3B52)
val MidnightOutlineSubtle = Color(0xFF232E42)

// Coordinated Light Theme Palette (Modern Financial Minimalism)
val LuxeLightBackground = Color(0xFFF6F8FC)
val LuxeLightSurface = Color(0xFFFFFFFF)
val LuxeLightElevatedSurface = Color(0xFFEEF2F9)
val LuxeLightPrimary = Color(0xFF2452B3)
val LuxeLightOnPrimary = Color(0xFFFFFFFF)
val LuxeLightPrimaryContainer = Color(0xFFDCE6FA)
val LuxeLightOnPrimaryContainer = Color(0xFF0F2961)
val LuxeLightSecondary = Color(0xFF476296)
val LuxeLightOnSecondary = Color(0xFFFFFFFF)
val LuxeLightSecondaryContainer = Color(0xFFE4EAF6)
val LuxeLightOnSecondaryContainer = Color(0xFF1A2D52)
val LuxeLightSuccess = Color(0xFF118A5B)
val LuxeLightError = Color(0xFFC43B50)
val LuxeLightWarning = Color(0xFFB87414)
val LuxeLightTextPrimary = Color(0xFF111827)
val LuxeLightTextSecondary = Color(0xFF5A677D)
val LuxeLightOutline = Color(0xFFD0D8E8)
val LuxeLightOutlineSubtle = Color(0xFFE4E9F2)

// Legacy / Semantic Aliases preserved for incremental screen migration
val Blue40 = LuxeLightPrimary
val Teal40 = Color(0xFF0D9488)
val Slate600 = LuxeLightTextSecondary
val IncomeGreen = LuxeLightSuccess
val ExpenseRed = LuxeLightError
val WarningAmber = LuxeLightWarning

val DarkColorScheme = darkColorScheme(
    primary = MidnightPrimary,
    onPrimary = MidnightOnPrimary,
    primaryContainer = MidnightPrimaryContainer,
    onPrimaryContainer = MidnightOnPrimaryContainer,
    secondary = MidnightSecondary,
    onSecondary = MidnightOnSecondary,
    secondaryContainer = MidnightSecondaryContainer,
    onSecondaryContainer = MidnightOnSecondaryContainer,
    tertiary = MidnightSuccess,
    onTertiary = Color(0xFF052E1E),
    tertiaryContainer = Color(0xFF153E30),
    onTertiaryContainer = Color(0xFFB7F4D8),
    background = MidnightBackground,
    onBackground = MidnightTextPrimary,
    surface = MidnightSurface,
    onSurface = MidnightTextPrimary,
    surfaceVariant = MidnightElevatedSurface,
    onSurfaceVariant = MidnightTextSecondary,
    surfaceContainerLowest = MidnightBackground,
    surfaceContainerLow = MidnightSurface,
    surfaceContainer = MidnightSurface,
    surfaceContainerHigh = MidnightElevatedSurface,
    surfaceContainerHighest = Color(0xFF28354B),
    error = MidnightError,
    onError = Color(0xFF4B0C17),
    errorContainer = Color(0xFF4C222B),
    onErrorContainer = Color(0xFFFFDADE),
    outline = MidnightOutline,
    outlineVariant = MidnightOutlineSubtle
)

val LightColorScheme = lightColorScheme(
    primary = LuxeLightPrimary,
    onPrimary = LuxeLightOnPrimary,
    primaryContainer = LuxeLightPrimaryContainer,
    onPrimaryContainer = LuxeLightOnPrimaryContainer,
    secondary = LuxeLightSecondary,
    onSecondary = LuxeLightOnSecondary,
    secondaryContainer = LuxeLightSecondaryContainer,
    onSecondaryContainer = LuxeLightOnSecondaryContainer,
    tertiary = LuxeLightSuccess,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD4F5E6),
    onTertiaryContainer = Color(0xFF073B25),
    background = LuxeLightBackground,
    onBackground = LuxeLightTextPrimary,
    surface = LuxeLightSurface,
    onSurface = LuxeLightTextPrimary,
    surfaceVariant = LuxeLightElevatedSurface,
    onSurfaceVariant = LuxeLightTextSecondary,
    surfaceContainerLowest = LuxeLightSurface,
    surfaceContainerLow = LuxeLightBackground,
    surfaceContainer = LuxeLightSurface,
    surfaceContainerHigh = LuxeLightElevatedSurface,
    surfaceContainerHighest = Color(0xFFE3E9F4),
    error = LuxeLightError,
    onError = Color.White,
    errorContainer = Color(0xFFFDE4E8),
    onErrorContainer = Color(0xFF5F111E),
    outline = LuxeLightOutline,
    outlineVariant = LuxeLightOutlineSubtle
)

@Immutable
data class WalletQExtendedColors(
    val background: Color,
    val surface: Color,
    val elevatedSurface: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val success: Color,
    val successContainer: Color,
    val error: Color,
    val errorContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val subtleBorder: Color,
    val navBarBackground: Color,
    val navIndicator: Color
)

val DarkExtendedColors = WalletQExtendedColors(
    background = MidnightBackground,
    surface = MidnightSurface,
    elevatedSurface = MidnightElevatedSurface,
    primaryText = MidnightTextPrimary,
    secondaryText = MidnightTextSecondary,
    success = MidnightSuccess,
    successContainer = Color(0xFF15362C),
    error = MidnightError,
    errorContainer = Color(0xFF3E2029),
    warning = MidnightWarning,
    warningContainer = Color(0xFF3A2D1B),
    subtleBorder = MidnightOutlineSubtle,
    navBarBackground = Color(0xFF131A29),
    navIndicator = MidnightPrimaryContainer
)

val LightExtendedColors = WalletQExtendedColors(
    background = LuxeLightBackground,
    surface = LuxeLightSurface,
    elevatedSurface = LuxeLightElevatedSurface,
    primaryText = LuxeLightTextPrimary,
    secondaryText = LuxeLightTextSecondary,
    success = LuxeLightSuccess,
    successContainer = Color(0xFFE1F6EC),
    error = LuxeLightError,
    errorContainer = Color(0xFFFCE8EB),
    warning = LuxeLightWarning,
    warningContainer = Color(0xFFFEF3DF),
    subtleBorder = LuxeLightOutlineSubtle,
    navBarBackground = LuxeLightSurface,
    navIndicator = LuxeLightPrimaryContainer
)

val LocalWalletQColors = staticCompositionLocalOf { DarkExtendedColors }
