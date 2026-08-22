package com.altinim.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.altinim.app.data.remote.GoldProduct
import com.altinim.app.data.repository.PriceDirection
import com.altinim.app.data.repository.PriceUiState
import com.altinim.app.data.repository.ProductPriceChange
import com.altinim.app.data.sortAndFilterGoldProducts
import com.altinim.app.ui.theme.AntiqueBrass
import com.altinim.app.ui.theme.BottleInk
import com.altinim.app.ui.theme.HairlineRule
import com.altinim.app.ui.theme.InkCharcoal
import com.altinim.app.ui.theme.InkFaded
import com.altinim.app.ui.theme.LedgerCover
import com.altinim.app.ui.theme.Parchment
import com.altinim.app.ui.theme.ParchmentLight
import com.altinim.app.ui.theme.WaxSeal

@Composable
fun PriceScreen(
    viewModel: PriceViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        containerColor = Parchment,
        topBar = { LedgerHeader() }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is PriceUiState.Loading -> LoadingContent()
                is PriceUiState.Error -> ErrorContent(
                    message = state.message,
                    onRetry = { viewModel.loadPrices() }
                )
                is PriceUiState.Success -> {
                    val visibleProducts = sortAndFilterGoldProducts(
                        state.products, settings.productOrder, settings.hiddenProducts
                    )
                    if (visibleProducts.isEmpty()) {
                        EmptyPricesContent()
                    } else {
                        PriceLedger(products = visibleProducts, changes = state.changes)
                    }
                }
            }
        }
    }
}

@Composable
private fun LedgerHeader() {
    Surface(color = LedgerCover) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ALTINIM",
                        style = MaterialTheme.typography.headlineMedium,
                        color = ParchmentLight
                    )
                    Text(
                        text = "Aliağa kuyumcu fiyatları",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ParchmentLight.copy(alpha = 0.65f)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(AntiqueBrass)
            )
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Defter açılıyor…",
            style = MaterialTheme.typography.bodyLarge,
            color = InkFaded
        )
    }
}

@Composable
private fun EmptyPricesContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Tüm ürünler gizlenmiş. Ayarlar sekmesinden en az birini göster.",
            style = MaterialTheme.typography.bodyLarge,
            color = InkFaded,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = WaxSeal,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        Box(
            modifier = Modifier
                .border(BorderStroke(1.dp, AntiqueBrass))
                .clickable(onClick = onRetry)
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(
                text = "TEKRAR DENE",
                style = MaterialTheme.typography.labelSmall,
                color = InkCharcoal
            )
        }
    }
}

@Composable
private fun PriceLedger(products: List<GoldProduct>, changes: Map<Int, ProductPriceChange>) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(products, key = { it.id }) { product ->
            LedgerRow(product = product, change = changes[product.id])
            HorizontalDivider(color = HairlineRule, thickness = 1.dp)
        }
    }
}

@Composable
private fun LedgerRow(product: GoldProduct, change: ProductPriceChange?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HallmarkBadge(label = stampLabel(product.productName))
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = product.productName,
            style = MaterialTheme.typography.titleMedium,
            color = InkCharcoal,
            modifier = Modifier.weight(1f)
        )
        Column(horizontalAlignment = Alignment.End) {
            PriceLine(
                label = "ALIŞ",
                value = product.roundPurchasePrice,
                color = BottleInk,
                direction = change?.purchaseDirection
            )
            Spacer(modifier = Modifier.height(2.dp))
            PriceLine(
                label = "SATIŞ",
                value = product.roundSalesPrice,
                color = WaxSeal,
                direction = change?.salesDirection
            )
        }
    }
}

@Composable
private fun PriceLine(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    direction: PriceDirection?
) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = "$label ",
            style = MaterialTheme.typography.labelSmall,
            color = InkFaded
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = color,
            fontWeight = FontWeight.Medium
        )
        ChangeArrow(direction)
    }
}

@Composable
private fun ChangeArrow(direction: PriceDirection?) {
    when (direction) {
        PriceDirection.UP -> Text(
            text = " ▲",
            style = MaterialTheme.typography.labelSmall,
            color = BottleInk
        )
        PriceDirection.DOWN -> Text(
            text = " ▼",
            style = MaterialTheme.typography.labelSmall,
            color = WaxSeal
        )
        else -> Unit
    }
}

private fun stampLabel(productName: String): String {
    val leadingDigits = Regex("^(\\d+)").find(productName.trim())?.value
    if (leadingDigits != null) return leadingDigits

    val words = productName.trim().split(" ").filter { it.isNotBlank() }
    return when {
        words.size >= 2 -> "${words[0].first()}${words[1].first()}".uppercase()
        words.isNotEmpty() -> words[0].take(2).uppercase()
        else -> "AU"
    }
}

@Composable
private fun HallmarkBadge(label: String) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .border(BorderStroke(1.5.dp, AntiqueBrass), CircleShape)
            .background(ParchmentLight, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = AntiqueBrass,
            fontWeight = FontWeight.SemiBold
        )
    }
}