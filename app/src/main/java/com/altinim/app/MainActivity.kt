package com.altinim.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
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
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.altinim.app.data.local.AppSettings
import com.altinim.app.data.local.SettingsRepository
import com.altinim.app.ui.AltinimApp
import com.altinim.app.ui.theme.AltinimTheme
import com.altinim.app.ui.theme.InkFaded

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
    var promptShown by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        settingsRepository.settings.collect { newSettings ->
            settings = newSettings
            if (!newSettings.appLockEnabled) {
                authenticated = true
            }
        }
    }

    LaunchedEffect(settings, authenticated) {
        val currentSettings = settings
        val activity = context as? FragmentActivity
        if (currentSettings != null && currentSettings.appLockEnabled && !authenticated && !promptShown && activity != null) {
            val biometricManager = BiometricManager.from(context)
            val canAuthenticate = biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
                authenticated = true
                return@LaunchedEffect
            }

            promptShown = true
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
                        promptShown = false
                    }
                }
            )
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Altınım")
                .setSubtitle("Devam etmek için kimliğini doğrula")
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_WEAK or
                            BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
                .build()
            prompt.authenticate(promptInfo)
        }
    }

    when {
        settings == null -> Box(Modifier.fillMaxSize())
        authenticated -> AltinimApp()
        else -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Kilit açılması bekleniyor…", color = InkFaded)
        }
    }
}