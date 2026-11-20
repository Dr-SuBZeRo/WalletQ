package com.walletq.core.model

import org.junit.Assert.assertEquals
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
}
