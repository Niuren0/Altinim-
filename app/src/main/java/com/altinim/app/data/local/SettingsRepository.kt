package com.altinim.app.data.local

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.glance.appwidget.updateAll
import com.altinim.app.widget.AltinimWidget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.security.SecureRandom

private val Context.settingsDataStore by preferencesDataStore(name = "altinim_settings")

data class AppSettings(
    val productOrder: List<String> = emptyList(),
    val hiddenProducts: Set<String> = emptySet(),
    val refreshIntervalSeconds: Int = MIN_REFRESH_INTERVAL_SECONDS,
    val appLockEnabled: Boolean = false,
    val appLockPinHash: String? = null,
    val appLockPinSalt: String? = null
) {
    companion object {
        const val MIN_REFRESH_INTERVAL_SECONDS = 30
    }
}

/**
 * Verilen PIN'in, verilen tuz ile üretilmiş özet (hash) ile eşleşip
 * eşleşmediğini kontrol eder. Context gerektirmez; AppLockGate gibi
 * repository örneğine ihtiyaç duymayan yerlerden de çağrılabilir.
 */
fun verifyAppLockPin(pin: String, expectedHash: String, saltBase64: String): Boolean {
    return hashPin(pin, saltBase64) == expectedHash
}

private fun generateSaltBase64(): String {
    val bytes = ByteArray(16)
    SecureRandom().nextBytes(bytes)
    return Base64.encodeToString(bytes, Base64.NO_WRAP)
}

private fun hashPin(pin: String, saltBase64: String): String {
    val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
    val digest = MessageDigest.getInstance("SHA-256")
    digest.update(salt)
    val hashed = digest.digest(pin.toByteArray(Charsets.UTF_8))
    return Base64.encodeToString(hashed, Base64.NO_WRAP)
}

class SettingsRepository(private val context: Context) {

    private val orderKey = stringPreferencesKey("product_order")
    private val hiddenKey = stringPreferencesKey("hidden_products")
    private val intervalKey = intPreferencesKey("refresh_interval_seconds")
    private val lockKey = booleanPreferencesKey("app_lock_enabled")
    private val pinHashKey = stringPreferencesKey("app_lock_pin_hash")
    private val pinSaltKey = stringPreferencesKey("app_lock_pin_salt")

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        val order = prefs[orderKey]?.split("|")?.filter { it.isNotBlank() } ?: emptyList()
        val hidden = prefs[hiddenKey]?.split("|")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
        val interval = prefs[intervalKey] ?: AppSettings.MIN_REFRESH_INTERVAL_SECONDS
        val lockEnabled = prefs[lockKey] ?: false
        AppSettings(
            productOrder = order,
            hiddenProducts = hidden,
            refreshIntervalSeconds = interval,
            appLockEnabled = lockEnabled,
            appLockPinHash = prefs[pinHashKey],
            appLockPinSalt = prefs[pinSaltKey]
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

    /** Yeni bir uygulama PIN'i belirler (veya mevcudunu değiştirir) ve kilidi açar. */
    suspend fun setAppLockPin(pin: String) {
        val salt = generateSaltBase64()
        val hash = hashPin(pin, salt)
        context.settingsDataStore.edit { prefs ->
            prefs[lockKey] = true
            prefs[pinHashKey] = hash
            prefs[pinSaltKey] = salt
        }
    }

    /** Uygulama kilidini tamamen kapatır; saklanan hash/tuz de temizlenir. */
    suspend fun disableAppLock() {
        context.settingsDataStore.edit { prefs ->
            prefs[lockKey] = false
            prefs.remove(pinHashKey)
            prefs.remove(pinSaltKey)
        }
    }
}