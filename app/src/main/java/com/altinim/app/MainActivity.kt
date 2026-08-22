package com.altinim.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.altinim.app.data.local.AppSettings
import com.altinim.app.data.local.SettingsRepository
import com.altinim.app.data.local.verifyAppLockPin
import com.altinim.app.ui.AltinimApp
import com.altinim.app.ui.theme.AltinimTheme
import com.altinim.app.ui.theme.AntiqueBrass
import com.altinim.app.ui.theme.HairlineRule
import com.altinim.app.ui.theme.InkCharcoal
import com.altinim.app.ui.theme.InkFaded
import com.altinim.app.ui.theme.WaxSeal

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AltinimTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppLockGate()
                }
            }
        }
    }
}

@Composable
private fun AppLockGate() {
    val context = LocalContext.current
    val settingsRepository = remember { SettingsRepository(context) }

    var settings by remember { mutableStateOf<AppSettings?>(null) }
    var authenticated by remember { mutableStateOf(false) }
    var biometricAttempted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        settingsRepository.settings.collect { newSettings ->
            settings = newSettings
            if (!newSettings.appLockEnabled) {
                authenticated = true
            }
        }
    }

    val currentSettings = settings
    val pinHash = currentSettings?.appLockPinHash
    val pinSalt = currentSettings?.appLockPinSalt
    // Kilit, sadece appLockEnabled=true VE geçerli bir PIN kurulmuşsa aktiftir.
    // Böylece hiçbir durumda (biyometri yok/kayıtlı değil, telefon kilidi yok)
    // sessiz bir bypass olmaz: PIN her zaman devrede bir alternatif olarak durur.
    val lockActive = currentSettings != null && currentSettings.appLockEnabled &&
            pinHash != null && pinSalt != null

    // Uygulama her açıldığında, PIN ekranına düşmeden önce bir kez parmak izini
    // dener (cihazda kayıtlıysa). Telefonun genel kilit ekranı ayarına bakılmaz;
    // sadece BIOMETRIC_WEAK sorgulanır, DEVICE_CREDENTIAL kullanılmaz.
    LaunchedEffect(lockActive, authenticated, biometricAttempted) {
        val activity = context as? FragmentActivity
        if (lockActive && !authenticated && !biometricAttempted && activity != null) {
            biometricAttempted = true
            val biometricManager = BiometricManager.from(context)
            val canAuthenticate = biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_WEAK
            )
            if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
                return@LaunchedEffect
            }
            val executor = ContextCompat.getMainExecutor(context)
            val prompt = BiometricPrompt(
                activity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(
                        result: BiometricPrompt.AuthenticationResult
                    ) {
                        authenticated = true
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        // İptal edildi ya da hata oluştu: kullanıcı PIN ekranında kalır,
                        // "Parmak izini tekrar dene" ile prompt yeniden tetiklenebilir.
                    }
                }
            )
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Altınım")
                .setSubtitle("Parmak izinle aç veya PIN gir")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                .setNegativeButtonText("PIN kullan")
                .build()
            prompt.authenticate(promptInfo)
        }
    }

    when {
        settings == null -> Box(Modifier.fillMaxSize())
        authenticated -> AltinimApp()
        lockActive -> PinEntryScreen(
            expectedHash = pinHash!!,
            expectedSalt = pinSalt!!,
            onUnlock = { authenticated = true },
            onRetryBiometric = { biometricAttempted = false }
        )
        // appLockEnabled=true ama PIN kurulmamış: tutarsız/beklenmedik bir durum
        // (Settings ekranı PIN'siz kilit açılmasına izin vermiyor). Kullanıcıyı
        // kilitli tutup PIN'i olmayan bir ekranda sıkıştırmak yerine içeri alıyoruz.
        else -> AltinimApp()
    }
}

@Composable
private fun PinEntryScreen(
    expectedHash: String,
    expectedSalt: String,
    onUnlock: () -> Unit,
    onRetryBiometric: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    fun tryUnlock() {
        if (verifyAppLockPin(pin, expectedHash, expectedSalt)) {
            onUnlock()
        } else {
            error = "Yanlış PIN."
            pin = ""
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Altınım kilitli", color = InkCharcoal)
            Spacer(modifier = Modifier.height(20.dp))
            OutlinedTextField(
                value = pin,
                onValueChange = {
                    if (it.length <= 6 && it.all(Char::isDigit)) {
                        pin = it
                        error = null
                    }
                },
                label = { Text("PIN") },
                singleLine = true,
                isError = error != null,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.width(200.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AntiqueBrass,
                    unfocusedBorderColor = HairlineRule
                )
            )
            if (error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = error ?: "", color = WaxSeal)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "AÇ",
                color = AntiqueBrass,
                modifier = Modifier.clickable(enabled = pin.length >= 4) { tryUnlock() }
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Parmak izini tekrar dene",
                color = InkFaded,
                modifier = Modifier.clickable { onRetryBiometric() }
            )
        }
    }
}