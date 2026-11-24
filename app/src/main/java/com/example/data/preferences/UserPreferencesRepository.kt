package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.EmployeeRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "rajeev_digital_prefs")

data class LocalPreferences(
    val themeMode: String = "DARK_NAVY",
    val dateFormat: String = "dd MMM yyyy",
    val notificationsEnabled: Boolean = true,
    val activeRole: EmployeeRole = EmployeeRole.ADMIN
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DATE_FORMAT = stringPreferencesKey("date_format")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val ACTIVE_ROLE = stringPreferencesKey("active_role")
    }

    val preferencesFlow: Flow<LocalPreferences> = context.dataStore.data.map { prefs ->
        LocalPreferences(
            themeMode = prefs[Keys.THEME_MODE] ?: "DARK_NAVY",
            dateFormat = prefs[Keys.DATE_FORMAT] ?: "dd MMM yyyy",
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            activeRole = EmployeeRole.fromName(prefs[Keys.ACTIVE_ROLE] ?: EmployeeRole.ADMIN.name)
        )
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode }
    }

    suspend fun setDateFormat(format: String) {
        context.dataStore.edit { it[Keys.DATE_FORMAT] = format }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setActiveRole(role: EmployeeRole) {
        context.dataStore.edit { it[Keys.ACTIVE_ROLE] = role.name }
    }
}
