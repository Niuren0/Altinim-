package com.altinim.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.viewmodel.compose.viewModel
import com.altinim.app.BuildConfig
import com.altinim.app.data.local.AppSettings.Companion.MIN_REFRESH_INTERVAL_SECONDS
import com.altinim.app.data.orderProductNames
import com.altinim.app.data.repository.UpdateCheckResult
import com.altinim.app.ui.theme.AntiqueBrass
import com.altinim.app.ui.theme.BottleInk
import com.altinim.app.ui.theme.HairlineRule
import com.altinim.app.ui.theme.InkCharcoal
import com.altinim.app.ui.theme.InkFaded
import com.altinim.app.ui.theme.ParchmentLight
import com.altinim.app.ui.theme.WaxSeal

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    updateViewModel: UpdateViewModel = viewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val availableProducts by viewModel.availableProducts.collectAsState()
    var intervalText by remember(settings.refreshIntervalSeconds) {
        mutableStateOf(settings.refreshIntervalSeconds.toString())
    }
    var intervalError by remember { mutableStateOf<String?>(null) }
    var showPinDialog by remember { mutableStateOf(false) }

    val orderedNames = remember(availableProducts, settings.productOrder) {
        orderProductNames(availableProducts.map { it.ProductName }, settings.productOrder)
    }

    // Ekran her açıldığında elle butona basmadan otomatik kontrol et.
    LaunchedEffect(Unit) {
        updateViewModel.checkForUpdate()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            text = "Ayarlar",
            style = MaterialTheme.typography.headlineMedium,
            color = InkCharcoal
        )
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Ürün Sıralaması",
            style = MaterialTheme.typography.titleLarge,
            color = InkCharcoal
        )
        Text(
            text = "Sık takip ettiğini yukarı taşı, ilgilenmediğini gizle.",
            style = MaterialTheme.typography.bodyMedium,
            color = InkFaded
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (orderedNames.isEmpty()) {
            Text(
                text = "Ürün listesi yükleniyor…",
                style = MaterialTheme.typography.bodyMedium,
                color = InkFaded,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            orderedNames.forEachIndexed { index, name ->
                val isHidden = name in settings.hiddenProducts
                ProductOrderRow(
                    name = name,
                    isHidden = isHidden,
                    canMoveUp = index > 0,
                    canMoveDown = index < orderedNames.lastIndex,
                    onMoveUp = { viewModel.moveProduct(orderedNames, index, index - 1) },
                    onMoveDown = { viewModel.moveProduct(orderedNames, index, index + 1) },
                    onToggleHidden = { viewModel.toggleHidden(name) }
                )
                HorizontalDivider(color = HairlineRule, thickness = 1.dp)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Yenileme Aralığı",
            style = MaterialTheme.typography.titleLarge,
            color = InkCharcoal
        )
        Text(
            text = "Fiyatların kaç saniyede bir otomatik yenileneceği (en az $MIN_REFRESH_INTERVAL_SECONDS sn).",
            style = MaterialTheme.typography.bodyMedium,
            color = InkFaded
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = intervalText,
            onValueChange = {
                intervalText = it
                intervalError = null
            },
            label = { Text("Saniye") },
            isError = intervalError != null,
            supportingText = intervalError?.let { message ->
                { Text(text = message, color = WaxSeal) }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AntiqueBrass,
                unfocusedBorderColor = HairlineRule
            )
        )
        Spacer(modifier = Modifier.height(12.dp))

        ActionButton(
            text = "KAYDET",
            onClick = {
                val seconds = intervalText.toIntOrNull()
                when {
                    seconds == null -> intervalError = "Geçerli bir sayı gir."
                    seconds < MIN_REFRESH_INTERVAL_SECONDS ->
                        intervalError = "En az $MIN_REFRESH_INTERVAL_SECONDS saniye olmalı."
                    else -> {
                        intervalError = null
                        viewModel.updateRefreshInterval(seconds)
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Güvenlik",
            style = MaterialTheme.typography.titleLarge,
            color = InkCharcoal
        )
        Text(
            text = "Açıksa uygulama her açılışta PIN isteyecek. Cihazında parmak izi " +
                    "kayıtlıysa, PIN yerine kısayol olarak parmak izini de kullanabilirsin.",
            style = MaterialTheme.typography.bodyMedium,
            color = InkFaded
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, HairlineRule))
                .clickable {
                    if (settings.appLockEnabled) {
                        viewModel.disableAppLock()
                    } else {
                        showPinDialog = true
                    }
                }
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Uygulama Kilidi",
                style = MaterialTheme.typography.bodyLarge,
                color = InkCharcoal
            )
            Text(
                text = if (settings.appLockEnabled) "AÇIK" else "KAPALI",
                style = MaterialTheme.typography.labelSmall,
                color = if (settings.appLockEnabled) BottleInk else InkFaded,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (settings.appLockEnabled) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "PIN'i değiştir",
                style = MaterialTheme.typography.labelSmall,
                color = AntiqueBrass,
                modifier = Modifier
                    .clickable { showPinDialog = true }
                    .padding(vertical = 4.dp)
            )
        }

        if (showPinDialog) {
            AppLockPinDialog(
                onDismiss = { showPinDialog = false },
                onConfirm = { pin ->
                    viewModel.setAppLockPin(pin)
                    showPinDialog = false
                }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Güncellemeler",
            style = MaterialTheme.typography.titleLarge,
            color = InkCharcoal
        )
        Text(
            text = "Mevcut sürüm: ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.bodyMedium,
            color = InkFaded
        )
        Text(
            text = "Geliştirici: Softepen",
            style = MaterialTheme.typography.bodyMedium,
            color = InkFaded
        )
        Spacer(modifier = Modifier.height(12.dp))

        UpdateSection(updateViewModel = updateViewModel)
    }
}

@Composable
private fun UpdateSection(updateViewModel: UpdateViewModel) {
    val updateState by updateViewModel.uiState.collectAsState()
    val isDownloading by updateViewModel.isDownloading.collectAsState()

    when (val state = updateState) {
        null -> {
            ActionButton(
                text = "GÜNCELLEMELERİ KONTROL ET",
                filled = false,
                onClick = { updateViewModel.checkForUpdate() }
            )
        }
        is UpdateCheckResult.Loading -> {
            Text(
                text = "Kontrol ediliyor…",
                style = MaterialTheme.typography.bodyMedium,
                color = InkFaded
            )
        }
        is UpdateCheckResult.UpToDate -> {
            Text(
                text = "En güncel sürümü kullanıyorsun.",
                style = MaterialTheme.typography.bodyMedium,
                color = BottleInk
            )
        }
        is UpdateCheckResult.Error -> {
            Column {
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = WaxSeal
                )
                Spacer(modifier = Modifier.height(10.dp))
                ActionButton(
                    text = "TEKRAR DENE",
                    filled = false,
                    onClick = { updateViewModel.checkForUpdate() }
                )
            }
        }
        is UpdateCheckResult.UpdateAvailable -> {
            Column {
                Text(
                    text = "Yeni sürüm var: ${state.version}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AntiqueBrass
                )
                Spacer(modifier = Modifier.height(10.dp))
                ActionButton(
                    text = if (isDownloading) "İNDİRİLİYOR…" else "İNDİR VE KUR",
                    filled = true,
                    enabled = !isDownloading,
                    onClick = { updateViewModel.downloadAndInstall(state.downloadUrl, state.version) }
                )
            }
        }
    }
}

/**
 * Uygulama kilidi için PIN belirleme/değiştirme diyaloğu.
 * PIN 4-6 haneli rakamlardan oluşmalı ve tekrar alanıyla eşleşmeli.
 * Doğrulanan PIN, çağıran tarafa (onConfirm) düz metin olarak sadece bu
 * diyalogdan çıkışta bir kez iletilir; kalıcı olarak asla düz metin
 * saklanmaz (bkz. SettingsRepository.setAppLockPin).
 */
@Composable
private fun AppLockPinDialog(
    onDismiss: () -> Unit,
    onConfirm: (pin: String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    fun onlyDigits(input: String, maxLength: Int = 6): String =
        input.filter(Char::isDigit).take(maxLength)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Uygulama PIN'i") },
        text = {
            Column {
                Text(
                    text = "4-6 haneli bir PIN belirle.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkFaded
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        pin = onlyDigits(it)
                        error = null
                    },
                    label = { Text("PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AntiqueBrass,
                        unfocusedBorderColor = HairlineRule
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = {
                        confirmPin = onlyDigits(it)
                        error = null
                    },
                    label = { Text("PIN (tekrar)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AntiqueBrass,
                        unfocusedBorderColor = HairlineRule
                    )
                )
                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = error!!, color = WaxSeal, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when {
                    pin.length < 4 -> error = "PIN en az 4 haneli olmalı."
                    pin != confirmPin -> error = "Girdiğin PIN'ler eşleşmiyor."
                    else -> onConfirm(pin)
                }
            }) {
                Text("KAYDET", color = AntiqueBrass)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İPTAL", color = InkFaded)
            }
        }
    )
}

/**
 * Ortak eylem butonu: dolu (filled) ya da çerçeveli (outlined) stilde,
 * tam genişlikte, ortalanmış etiketli tıklanabilir kutu.
 * KAYDET / KONTROL ET / TEKRAR DENE / İNDİR VE KUR butonlarındaki
 * tekrar eden Box+clickable+Text bloğunu tek yerde toplar.
 */
@Composable
private fun ActionButton(
    text: String,
    modifier: Modifier = Modifier,
    filled: Boolean = true,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val contentColor = when {
        !enabled -> InkFaded
        filled -> ParchmentLight
        else -> InkCharcoal
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (filled) {
                    Modifier.background(if (enabled) AntiqueBrass else HairlineRule)
                } else {
                    Modifier.border(BorderStroke(1.dp, if (enabled) AntiqueBrass else HairlineRule))
                }
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ProductOrderRow(
    name: String,
    isHidden: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onToggleHidden: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isHidden) InkFaded else InkCharcoal,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = if (isHidden) "GÖSTER" else "GİZLE",
            style = MaterialTheme.typography.titleMedium,
            color = if (isHidden) HairlineRule else AntiqueBrass,
            modifier = Modifier
                .clickable(onClick = onToggleHidden)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        )
        Text(
            text = "▲",
            color = if (canMoveUp) AntiqueBrass else HairlineRule,
            modifier = Modifier
                .clickable(enabled = canMoveUp, onClick = onMoveUp)
                .padding(8.dp)
        )
        Text(
            text = "▼",
            color = if (canMoveDown) AntiqueBrass else HairlineRule,
            modifier = Modifier
                .clickable(enabled = canMoveDown, onClick = onMoveDown)
                .padding(8.dp)
        )
    }
}