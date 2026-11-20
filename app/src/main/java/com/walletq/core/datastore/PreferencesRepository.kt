package com.walletq.core.datastore

import com.walletq.core.model.ThemeMode
import com.walletq.core.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    val userPreferencesFlow: Flow<UserPreferences>

    suspend fun setThemeMode(themeMode: ThemeMode)
    suspend fun setDynamicColor(enabled: Boolean)
    suspend fun setDefaultCurrency(currencyCode: String)
    suspend fun setActiveProfileId(profileId: String?)
}
