package com.walletq.core.model

import com.walletq.core.designsystem.DarkColorScheme
import com.walletq.core.designsystem.DarkExtendedColors
import com.walletq.core.designsystem.LightColorScheme
import com.walletq.core.designsystem.LightExtendedColors
import com.walletq.core.designsystem.MidnightBackground
import com.walletq.core.designsystem.MidnightElevatedSurface
import com.walletq.core.designsystem.MidnightError
import com.walletq.core.designsystem.MidnightPrimary
import com.walletq.core.designsystem.MidnightSecondary
import com.walletq.core.designsystem.MidnightSuccess
import com.walletq.core.designsystem.MidnightSurface
import com.walletq.core.designsystem.MidnightTextPrimary
import com.walletq.core.designsystem.MidnightTextSecondary
import com.walletq.core.designsystem.WalletQMotion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeTest {

    @Test
    fun themeModes_containSystemLightDark() {
        val modes = ThemeMode.entries.map { it.name }
        assertTrue(modes.contains("SYSTEM"))
        assertTrue(modes.contains("LIGHT"))
        assertTrue(modes.contains("DARK"))
    }

    @Test
    fun userPreferences_defaultValues_areCorrect() {
        val defaults = UserPreferences()
        assertEquals(ThemeMode.SYSTEM, defaults.themeMode)
        assertTrue(defaults.dynamicColor)
        assertEquals("INR", defaults.defaultCurrencyCode)
        assertEquals(null, defaults.activeProfileId)
    }

    @Test
    fun midnightLuxePalette_matchesDesignSpecification() {
        assertEquals(MidnightBackground, DarkColorScheme.background)
        assertEquals(MidnightSurface, DarkColorScheme.surface)
        assertEquals(MidnightElevatedSurface, DarkExtendedColors.elevatedSurface)
        assertEquals(MidnightPrimary, DarkColorScheme.primary)
        assertEquals(MidnightSecondary, DarkColorScheme.secondary)
        assertEquals(MidnightSuccess, DarkExtendedColors.success)
        assertEquals(MidnightError, DarkExtendedColors.error)
        assertEquals(MidnightTextPrimary, DarkExtendedColors.primaryText)
        assertEquals(MidnightTextSecondary, DarkExtendedColors.secondaryText)

        assertNotEquals(LightColorScheme.background, DarkColorScheme.background)
        assertNotEquals(LightExtendedColors.surface, DarkExtendedColors.surface)
    }

    @Test
    fun motionSpecs_haveFastNonDecorativeDurations() {
        assertTrue(WalletQMotion.FAST_DURATION_MS in 100..200)
        assertTrue(WalletQMotion.STANDARD_DURATION_MS in 180..280)
    }
}
