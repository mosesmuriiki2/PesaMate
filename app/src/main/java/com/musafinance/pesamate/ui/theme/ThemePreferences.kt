package com.musafinance.pesamate.ui.theme

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class ThemePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
    private val NOTIFICATIONS_KEY = booleanPreferencesKey("notifications_enabled")
    private val BIOMETRIC_KEY = booleanPreferencesKey("biometric_enabled")
    private val DAILY_LIMIT_KEY = doublePreferencesKey("daily_limit")
    private val LAST_NOTIFIED_THRESHOLD = intPreferencesKey("last_notified_threshold")
    private val LAST_NOTIFIED_DATE = stringPreferencesKey("last_notified_date")
    private val HAS_SEEN_ONBOARDING_KEY = booleanPreferencesKey("has_seen_onboarding")
    private val CLOUD_SYNC_KEY = booleanPreferencesKey("cloud_sync_enabled")
    
    val isDarkMode: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[DARK_MODE_KEY] ?: false
        }
    
    val isNotificationsEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[NOTIFICATIONS_KEY] ?: true
        }
    
    val isBiometricEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[BIOMETRIC_KEY] ?: false
        }
    
    val dailyLimit: Flow<Double> = context.dataStore.data
        .map { preferences ->
            preferences[DAILY_LIMIT_KEY] ?: 0.0
        }

    val lastNotifiedThreshold: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[LAST_NOTIFIED_THRESHOLD] ?: 0
        }

    val lastNotifiedDate: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[LAST_NOTIFIED_DATE] ?: ""
        }

    val hasSeenOnboarding: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[HAS_SEEN_ONBOARDING_KEY] ?: false
        }

    val isCloudSyncEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[CLOUD_SYNC_KEY] ?: false
        }
    
    suspend fun toggleDarkMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DARK_MODE_KEY] = enabled
        }
    }
    
    suspend fun toggleNotifications(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATIONS_KEY] = enabled
        }
    }
    
    suspend fun toggleBiometric(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BIOMETRIC_KEY] = enabled
        }
    }
    
    suspend fun setDailyLimit(limit: Double) {
        context.dataStore.edit { preferences ->
            preferences[DAILY_LIMIT_KEY] = limit
        }
    }

    suspend fun setLastNotified(threshold: Int, date: String) {
        context.dataStore.edit { preferences ->
            preferences[LAST_NOTIFIED_THRESHOLD] = threshold
            preferences[LAST_NOTIFIED_DATE] = date
        }
    }

    suspend fun setHasSeenOnboarding(seen: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HAS_SEEN_ONBOARDING_KEY] = seen
        }
    }

    suspend fun toggleCloudSync(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[CLOUD_SYNC_KEY] = enabled
        }
    }
}
