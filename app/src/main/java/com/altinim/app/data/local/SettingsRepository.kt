package com.altinim.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.glance.appwidget.updateAll
import com.altinim.app.widget.AltinimWidget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "altinim_settings")

data class AppSettings(
    val productOrder: List<String> = emptyList(),
    val hiddenProducts: Set<String> = emptySet(),
    val refreshIntervalSeconds: Int = MIN_REFRESH_INTERVAL_SECONDS,
    val appLockEnabled: Boolean = false
) {
    companion object {
        const val MIN_REFRESH_INTERVAL_SECONDS = 30
    }
}

class SettingsRepository(private val context: Context) {

    private val orderKey = stringPreferencesKey("product_order")
    private val hiddenKey = stringPreferencesKey("hidden_products")
    private val intervalKey = intPreferencesKey("refresh_interval_seconds")
    private val lockKey = booleanPreferencesKey("app_lock_enabled")

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        val order = prefs[orderKey]?.split("|")?.filter { it.isNotBlank() } ?: emptyList()
        val hidden = prefs[hiddenKey]?.split("|")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
        val interval = prefs[intervalKey] ?: AppSettings.MIN_REFRESH_INTERVAL_SECONDS
        val lockEnabled = prefs[lockKey] ?: false
        AppSettings(
            productOrder = order,
            hiddenProducts = hidden,
            refreshIntervalSeconds = interval,
            appLockEnabled = lockEnabled
        )
    }

    suspend fun updateProductOrder(order: List<String>) {
        context.settingsDataStore.edit { prefs ->
            prefs[orderKey] = order.joinToString("|")
        }
        AltinimWidget().updateAll(context)
    }

    suspend fun updateHiddenProducts(hidden: Set<String>) {
        context.settingsDataStore.edit { prefs ->
            prefs[hiddenKey] = hidden.joinToString("|")
        }
        AltinimWidget().updateAll(context)
    }

    suspend fun updateRefreshInterval(seconds: Int) {
        context.settingsDataStore.edit { prefs ->
            prefs[intervalKey] = seconds
        }
    }

    suspend fun updateAppLockEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[lockKey] = enabled
        }
    }
}