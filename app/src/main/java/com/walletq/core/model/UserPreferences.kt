package com.walletq.core.model

/**
 * UI theme mode setting.
 */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

/**
 * Lightweight user preferences persisted in DataStore.
 */
data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val defaultCurrencyCode: String = Currency.DEFAULT_REPORTING_CURRENCY.code,
    val activeProfileId: String? = null
)
