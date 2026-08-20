package com.altinim.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.altinim.app.ui.screens.HoldingsScreen
import com.altinim.app.ui.screens.PriceScreen
import com.altinim.app.ui.screens.SettingsScreen
import com.altinim.app.ui.theme.AntiqueBrass
import com.altinim.app.ui.theme.LedgerCover
import com.altinim.app.ui.theme.ParchmentLight

private enum class AltinimTab { Prices, Holdings, Settings }

@Composable
fun AltinimApp() {
    var currentTab by rememberSaveable { mutableStateOf(AltinimTab.Prices) }
    val updateViewModel: com.altinim.app.ui.screens.UpdateViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val updateState by updateViewModel.uiState.collectAsState()
    var showUpdateDialog by remember { mutableStateOf(false) }

    // Hangi sekmede olursa olsun, uygulama açılır açılmaz bir kere kontrol et.
    LaunchedEffect(Unit) {
        updateViewModel.checkForUpdate()
    }

    LaunchedEffect(updateState) {
        if (updateState is com.altinim.app.data.repository.UpdateCheckResult.UpdateAvailable) {
            showUpdateDialog = true
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            when (currentTab) {
                AltinimTab.Prices -> PriceScreen()
                AltinimTab.Holdings -> HoldingsScreen()
                AltinimTab.Settings -> SettingsScreen()
            }
        }
        LedgerTabBar(
            selected = currentTab,
            onSelect = { currentTab = it }
        )
    }

    val availableUpdate = updateState as? com.altinim.app.data.repository.UpdateCheckResult.UpdateAvailable
    if (showUpdateDialog && availableUpdate != null) {
        UpdateAvailableDialog(
            version = availableUpdate.version,
            onDownload = {
                updateViewModel.downloadAndInstall(availableUpdate.downloadUrl, availableUpdate.version)
                showUpdateDialog = false
            },
            onDismiss = { showUpdateDialog = false }
        )
    }
}

@Composable
private fun UpdateAvailableDialog(
    version: String,
    onDownload: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = com.altinim.app.ui.theme.ParchmentLight,
        titleContentColor = com.altinim.app.ui.theme.InkCharcoal,
        textContentColor = com.altinim.app.ui.theme.InkFaded,
        title = {
            Text(text = "Yeni sürüm var", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Text(text = "$version yayınlandı. Şimdi indirip kurmak ister misin?")
        },
        confirmButton = {
            Text(
                text = "İNDİR",
                color = AntiqueBrass,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .clickable(onClick = onDownload)
                    .padding(12.dp)
            )
        },
        dismissButton = {
            Text(
                text = "DAHA SONRA",
                color = com.altinim.app.ui.theme.InkFaded,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .clickable(onClick = onDismiss)
                    .padding(12.dp)
            )
        }
    )
}

@Composable
private fun LedgerTabBar(selected: AltinimTab, onSelect: (AltinimTab) -> Unit) {
    Surface(color = LedgerCover) {
        Row(modifier = Modifier.fillMaxWidth()) {
            TabItem(
                label = "FİYATLAR",
                selected = selected == AltinimTab.Prices,
                modifier = Modifier.weight(1f)
            ) { onSelect(AltinimTab.Prices) }
            TabItem(
                label = "BİRİKİMİM",
                selected = selected == AltinimTab.Holdings,
                modifier = Modifier.weight(1f)
            ) { onSelect(AltinimTab.Holdings) }
            // Ayarlar eşit genişlikte bir sekme değil, sağ kenarda küçük
            // bir simge — iki ana bölümü kalabalıklaştırmasın diye.
            SettingsIconButton(
                selected = selected == AltinimTab.Settings,
                onClick = { onSelect(AltinimTab.Settings) }
            )
        }
    }
}

@Composable
private fun RowScope.TabItem(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.height(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) AntiqueBrass else ParchmentLight.copy(alpha = 0.5f)
            )
        }
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .height(2.dp)
                .width(28.dp)
                .background(if (selected) AntiqueBrass else Color.Transparent)
        )
    }
}

@Composable
private fun SettingsIconButton(selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .padding(end = 8.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.height(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "⚙",
                fontSize = 16.sp,
                color = if (selected) AntiqueBrass else ParchmentLight.copy(alpha = 0.6f)
            )
        }
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .height(2.dp)
                .width(20.dp)
                .background(if (selected) AntiqueBrass else Color.Transparent)
        )
    }
}