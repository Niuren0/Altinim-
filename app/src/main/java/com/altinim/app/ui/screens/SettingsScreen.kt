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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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

    val orderedNames = remember(availableProducts, settings.productOrder) {
        orderProductNames(availableProducts.map { it.ProductName }, settings.productOrder)
    }

    // Ekran her açıldığında elle butona basmadan otomatik kontrol et.
    androidx.compose.runtime.LaunchedEffect(Unit) {
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
            text = "Fiyatların kaç saniyede bir otomatik yenileneceği.",
            style = MaterialTheme.typography.bodyMedium,
            color = InkFaded
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = intervalText,
            onValueChange = { intervalText = it },
            label = { Text("Saniye") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AntiqueBrass,
                unfocusedBorderColor = HairlineRule
            )
        )
        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(AntiqueBrass)
                .clickable {
                    val seconds = intervalText.toIntOrNull()
                    if (seconds != null && seconds >= 5) {
                        viewModel.updateRefreshInterval(seconds)
                    }
                }
                .padding(vertical = 14.dp)
        ) {
            Text(
                text = "KAYDET",
                style = MaterialTheme.typography.labelSmall,
                color = ParchmentLight,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Güvenlik",
            style = MaterialTheme.typography.titleLarge,
            color = InkCharcoal
        )
        Text(
            text = "Açıksa uygulama her açılışta parmak izi/yüz tanıma isteyecek.",
            style = MaterialTheme.typography.bodyMedium,
            color = InkFaded
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, HairlineRule))
                .clickable { viewModel.updateAppLockEnabled(!settings.appLockEnabled) }
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

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Güncellemeler",
            style = MaterialTheme.typography.titleLarge,
            color = InkCharcoal
        )
        Text(
            text = "Mevcut sürüm: ${com.altinim.app.BuildConfig.VERSION_NAME}",
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

    when (val state = updateState) {
        null -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, AntiqueBrass))
                    .clickable { updateViewModel.checkForUpdate() }
                    .padding(vertical = 14.dp)
            ) {
                Text(
                    text = "GÜNCELLEMELERİ KONTROL ET",
                    style = MaterialTheme.typography.labelSmall,
                    color = InkCharcoal,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(BorderStroke(1.dp, AntiqueBrass))
                        .clickable { updateViewModel.checkForUpdate() }
                        .padding(vertical = 14.dp)
                ) {
                    Text(
                        text = "TEKRAR DENE",
                        style = MaterialTheme.typography.labelSmall,
                        color = InkCharcoal,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AntiqueBrass)
                        .clickable {
                            updateViewModel.downloadAndInstall(state.downloadUrl, state.version)
                        }
                        .padding(vertical = 14.dp)
                ) {
                    Text(
                        text = "İNDİR VE KUR",
                        style = MaterialTheme.typography.labelSmall,
                        color = ParchmentLight,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
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