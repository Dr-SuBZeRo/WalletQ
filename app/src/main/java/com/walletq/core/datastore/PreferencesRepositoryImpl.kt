package com.walletq.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.walletq.core.model.Currency
import com.walletq.core.model.ThemeMode
import com.walletq.core.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : PreferencesRepository {

    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val DEFAULT_CURRENCY = stringPreferencesKey("default_currency")
        val ACTIVE_PROFILE_ID = stringPreferencesKey("active_profile_id")
    }

    override val userPreferencesFlow: Flow<UserPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val themeModeString = preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name
            val themeMode = try {
                ThemeMode.valueOf(themeModeString)
            } catch (e: Exception) {
                ThemeMode.SYSTEM
            }

            val dynamicColor = preferences[PreferencesKeys.DYNAMIC_COLOR] ?: true
            val defaultCurrency = preferences[PreferencesKeys.DEFAULT_CURRENCY] ?: Currency.DEFAULT_REPORTING_CURRENCY.code
            val activeProfileId = preferences[PreferencesKeys.ACTIVE_PROFILE_ID]

            UserPreferences(
                themeMode = themeMode,
                dynamicColor = dynamicColor,
                defaultCurrencyCode = defaultCurrency,
                activeProfileId = activeProfileId
            )
        }

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = themeMode.name
        }
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DYNAMIC_COLOR] = enabled
        }
    }

    override suspend fun setDefaultCurrency(currencyCode: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_CURRENCY] = currencyCode.uppercase()
        }
    }

    override suspend fun setActiveProfileId(profileId: String?) {
        dataStore.edit { preferences ->
            if (profileId != null) {
                preferences[PreferencesKeys.ACTIVE_PROFILE_ID] = profileId
            } else {
                preferences.remove(PreferencesKeys.ACTIVE_PROFILE_ID)
            }
        }
    }
}
