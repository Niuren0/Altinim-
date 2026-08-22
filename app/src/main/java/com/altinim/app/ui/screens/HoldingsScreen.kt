package com.altinim.app.ui.screens

import android.app.DatePickerDialog
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.altinim.app.data.local.GoldEntry
import com.altinim.app.data.remote.GoldProduct
import com.altinim.app.data.computePortfolioSummary
import com.altinim.app.data.currentPriceFor
import com.altinim.app.data.hasLivePriceFor
import com.altinim.app.data.parseTurkishNumber
import com.altinim.app.data.sortAndFilterProductNames
import com.altinim.app.ui.theme.AntiqueBrass
import com.altinim.app.ui.theme.BottleInk
import com.altinim.app.ui.theme.HairlineRule
import com.altinim.app.ui.theme.InkCharcoal
import com.altinim.app.ui.theme.InkFaded
import com.altinim.app.ui.theme.ParchmentLight
import com.altinim.app.ui.theme.WaxSeal
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// Ekle/Düzenle formları ve kayıt satırı aynı tarih formatını kullanıyor —
// üç ayrı yerde remember { SimpleDateFormat(...) } oluşturmak yerine tek
// paylaşılan bir biçimlendirici.
private val entryDateFormat = SimpleDateFormat("dd.MM.yyyy", Locale("tr"))

// AddEntryForm ve EditEntryDialog'un ikisi de aynı "0'dan büyük olmalı"
// kurallarını uyguluyordu — ortak doğrulama burada.
private fun validateEntry(productName: String, amount: Double?, price: Double?): String? = when {
    productName.isBlank() -> "Önce bir ürün seç."
    amount == null || amount <= 0 -> "Geçerli bir miktar gir (0'dan büyük)."
    price == null || price <= 0 -> "Geçerli bir fiyat gir (0'dan büyük)."
    else -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HoldingsScreen(
    viewModel: HoldingsViewModel = viewModel()
) {
    val entries by viewModel.entries.collectAsState()
    val products by viewModel.currentProducts.collectAsState()
    val pricesStale by viewModel.pricesStale.collectAsState()
    val settings by viewModel.settings.collectAsState()

    var entryPendingDelete by remember { mutableStateOf<GoldEntry?>(null) }
    var entryPendingEdit by remember { mutableStateOf<GoldEntry?>(null) }

    val productNamesForEdit = sortAndFilterProductNames(
        products.map { it.ProductName },
        settings.productOrder,
        settings.hiddenProducts
    )
    val onProductSelected: (String) -> String? = { name ->
        products.find { it.ProductName == name }?.RoundSalesPrice
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        AddEntryForm(
            productNames = productNamesForEdit,
            onProductSelected = onProductSelected,
            onSave = { productName, amount, unit, pricePerUnit, dateMillis ->
                viewModel.addEntry(productName, amount, unit, pricePerUnit, dateMillis)
            }
        )
        HorizontalDivider(color = HairlineRule, thickness = 2.dp)
        if (entries.isNotEmpty()) {
            HoldingsSummary(entries = entries, currentProducts = products, pricesStale = pricesStale)
            HorizontalDivider(color = HairlineRule, thickness = 1.dp)
        }
        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Henüz kayıt yok. Yukarıdan ilk altınını ekle.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkFaded
                )
            }
        } else {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                entries.forEach { entry ->
                    EntryRow(
                        entry = entry,
                        currentProducts = products,
                        onEdit = { entryPendingEdit = entry },
                        onDelete = { entryPendingDelete = entry }
                    )
                    HorizontalDivider(color = HairlineRule, thickness = 1.dp)
                }
            }
        }
    }

    entryPendingDelete?.let { entry ->
        DeleteConfirmationDialog(
            entry = entry,
            onConfirm = {
                viewModel.deleteEntry(entry)
                entryPendingDelete = null
            },
            onDismiss = { entryPendingDelete = null }
        )
    }

    entryPendingEdit?.let { entry ->
        EditEntryDialog(
            entry = entry,
            productNames = productNamesForEdit,
            onProductSelected = onProductSelected,
            onSave = { productName, amount, unit, pricePerUnit, dateMillis ->
                viewModel.updateEntry(entry.id, productName, amount, unit, pricePerUnit, dateMillis)
                entryPendingEdit = null
            },
            onDismiss = { entryPendingEdit = null }
        )
    }
}

@Composable
private fun DeleteConfirmationDialog(
    entry: GoldEntry,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ParchmentLight,
        titleContentColor = InkCharcoal,
        textContentColor = InkFaded,
        title = {
            Text(text = "Kaydı sil", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Text(
                text = "\"${entry.productName}\" — ${formatAmount(entry.amount)} ${entry.unit} kaydı kalıcı olarak silinecek. Emin misin?"
            )
        },
        confirmButton = {
            Text(
                text = "SİL",
                color = WaxSeal,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .clickable(onClick = onConfirm)
                    .padding(12.dp)
            )
        },
        dismissButton = {
            Text(
                text = "VAZGEÇ",
                color = InkFaded,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .clickable(onClick = onDismiss)
                    .padding(12.dp)
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditEntryDialog(
    entry: GoldEntry,
    productNames: List<String>,
    onProductSelected: (String) -> String?,
    onSave: (productName: String, amount: Double, unit: String, pricePerUnit: Double, dateMillis: Long) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    var productExpanded by remember { mutableStateOf(false) }
    var selectedProduct by remember { mutableStateOf(entry.productName) }
    var amountText by remember { mutableStateOf(formatAmount(entry.amount)) }
    var priceText by remember { mutableStateOf(formatAmount(entry.pricePerUnit)) }
    var unit by remember { mutableStateOf(entry.unit) }
    var dateMillis by remember { mutableStateOf(entry.dateMillis) }
    var validationError by remember { mutableStateOf<String?>(null) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ParchmentLight,
        titleContentColor = InkCharcoal,
        textContentColor = InkFaded,
        title = {
            Text(text = "Kaydı düzenle", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                ExposedDropdownMenuBox(
                    expanded = productExpanded,
                    onExpandedChange = { productExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedProduct,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Ürün") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = productExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AntiqueBrass,
                            unfocusedBorderColor = HairlineRule
                        )
                    )
                    DropdownMenu(
                        expanded = productExpanded,
                        onDismissRequest = { productExpanded = false }
                    ) {
                        productNames.forEach { name ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    selectedProduct = name
                                    productExpanded = false
                                    unit = detectUnit(name)
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text(if (unit == "gram") "Gram" else "Adet") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AntiqueBrass,
                        unfocusedBorderColor = HairlineRule
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("Birim alış fiyatı (TL)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AntiqueBrass,
                        unfocusedBorderColor = HairlineRule
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    val picked = Calendar.getInstance()
                                    picked.set(year, month, day)
                                    dateMillis = picked.timeInMillis
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                        .border(BorderStroke(1.dp, HairlineRule))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Tarih: ", style = MaterialTheme.typography.bodyMedium, color = InkFaded)
                    Text(
                        text = entryDateFormat.format(dateMillis),
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkCharcoal,
                        fontWeight = FontWeight.Medium
                    )
                }

                validationError?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = WaxSeal,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Text(
                text = "KAYDET",
                color = AntiqueBrass,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .clickable {
                        val amount = parseTurkishNumber(amountText)
                        val price = parseTurkishNumber(priceText)
                        validationError = validateEntry(selectedProduct, amount, price)
                        if (validationError == null && amount != null && price != null) {
                            onSave(selectedProduct, amount, unit, price, dateMillis)
                        }
                    }
                    .padding(12.dp)
            )
        },
        dismissButton = {
            Text(
                text = "VAZGEÇ",
                color = InkFaded,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .clickable(onClick = onDismiss)
                    .padding(12.dp)
            )
        }
    )
}

@Composable
private fun HoldingsSummary(
    entries: List<GoldEntry>,
    currentProducts: List<GoldProduct>,
    pricesStale: Boolean
) {
    val summary = computePortfolioSummary(entries, currentProducts)
    val profitColor = if (summary.profit >= 0) BottleInk else WaxSeal
    val sign = if (summary.profit >= 0) "+" else ""

    // Aynı üründen birden fazla kayıt varsa (örn. 2 ayrı tarihte HAS ALTIN
    // alınmışsa) miktarları tek satırda topluyoruz.
    val distribution = entries
        .groupBy { it.productName to it.unit }
        .map { (key, group) -> Triple(key.first, key.second, group.sumOf { it.amount }) }
        .sortedByDescending { it.third }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = "Özet",
            style = MaterialTheme.typography.titleLarge,
            color = InkCharcoal
        )

        if (!summary.hasLivePrices) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Güncel fiyatlar alınamadı — aşağıdaki değerler alış fiyatlarına dayanıyor.",
                style = MaterialTheme.typography.bodySmall,
                color = WaxSeal
            )
        } else if (pricesStale) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Fiyatlar güncellenemedi, son bilinen değerler gösteriliyor.",
                style = MaterialTheme.typography.bodySmall,
                color = InkFaded
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        SummaryRow(label = "Toplam yatırılan", value = "${summary.totalInvested.toInt()} TL")
        SummaryRow(label = "Güncel değer", value = "${summary.currentValue.toInt()} TL")
        SummaryRow(
            label = "Kâr / zarar",
            value = "$sign${summary.profit.toInt()} TL (${String.format(Locale("tr"), "%.1f", summary.profitPercent)}%)",
            valueColor = profitColor
        )

        if (distribution.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Varlık Dağılımı",
                style = MaterialTheme.typography.titleMedium,
                color = InkCharcoal
            )
            Spacer(modifier = Modifier.height(8.dp))
            distribution.forEach { (productName, unit, amount) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = productName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkFaded
                    )
                    Text(
                        text = "${formatAmount(amount)} $unit",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkCharcoal,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// Tam sayıysa "3 adet" gibi virgülsüz, küsuratlıysa "2.5 gram" gibi göster.
private fun formatAmount(amount: Double): String {
    return if (amount == amount.toLong().toDouble()) {
        amount.toLong().toString()
    } else {
        String.format(Locale("tr"), "%.1f", amount)
    }
}

@Composable
private fun SummaryRow(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color = InkCharcoal) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = InkFaded)
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = valueColor,
            fontWeight = FontWeight.Medium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEntryForm(
    productNames: List<String>,
    onProductSelected: (String) -> String?,
    onSave: (productName: String, amount: Double, unit: String, pricePerUnit: Double, dateMillis: Long) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var productExpanded by remember { mutableStateOf(false) }
    var selectedProduct by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("adet") }
    var dateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var validationError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Text(
            text = "Altın Ekle",
            style = MaterialTheme.typography.titleLarge,
            color = InkCharcoal
        )
        Spacer(modifier = Modifier.height(14.dp))

        ExposedDropdownMenuBox(
            expanded = productExpanded,
            onExpandedChange = { productExpanded = it }
        ) {
            OutlinedTextField(
                value = selectedProduct,
                onValueChange = {},
                readOnly = true,
                label = { Text(if (productNames.isEmpty()) "Ürün (yükleniyor…)" else "Ürün") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = productExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AntiqueBrass,
                    unfocusedBorderColor = HairlineRule
                )
            )
            DropdownMenu(
                expanded = productExpanded,
                onDismissRequest = { productExpanded = false }
            ) {
                productNames.forEach { name ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = {
                            selectedProduct = name
                            productExpanded = false
                            unit = detectUnit(name)
                            onProductSelected(name)?.let { autoPrice ->
                                priceText = autoPrice
                            }
                        }
                    )
                }
            }
        }


        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = amountText,
            onValueChange = { amountText = it; productExpanded = false },
            label = { Text(if (unit == "gram") "Gram" else "Adet") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AntiqueBrass,
                unfocusedBorderColor = HairlineRule
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = priceText,
            onValueChange = { priceText = it; productExpanded = false },
            label = { Text("Birim alış fiyatı (TL)") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AntiqueBrass,
                unfocusedBorderColor = HairlineRule
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    productExpanded = false
                    val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            val picked = Calendar.getInstance()
                            picked.set(year, month, day)
                            dateMillis = picked.timeInMillis
                        },
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH),
                        cal.get(Calendar.DAY_OF_MONTH)
                    ).show()
                }
                .border(BorderStroke(1.dp, HairlineRule))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Tarih: ", style = MaterialTheme.typography.bodyMedium, color = InkFaded)
            Text(
                text = entryDateFormat.format(dateMillis),
                style = MaterialTheme.typography.bodyMedium,
                color = InkCharcoal,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(AntiqueBrass)
                .clickable {
                    val amount = parseTurkishNumber(amountText)
                    val price = parseTurkishNumber(priceText)
                    validationError = validateEntry(selectedProduct, amount, price)
                    if (validationError == null && amount != null && price != null) {
                        onSave(selectedProduct, amount, unit, price, dateMillis)
                        selectedProduct = ""
                        amountText = ""
                        priceText = ""
                        dateMillis = System.currentTimeMillis()
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    }
                }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "KAYDET",
                style = MaterialTheme.typography.labelSmall,
                color = ParchmentLight,
                fontWeight = FontWeight.SemiBold
            )
        }

        validationError?.let { error ->
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = WaxSeal,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

private fun detectUnit(productName: String): String {
    val upper = productName.uppercase(Locale("tr"))
    return if (upper.contains("GR") || upper.contains("KG") ||
        upper.contains("BİLEZİK") || upper.contains("HAS ALTIN")
    ) {
        "gram"
    } else {
        "adet"
    }
}

@Composable
private fun EntryRow(
    entry: GoldEntry,
    currentProducts: List<GoldProduct>,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy", Locale("tr")) }
    val total = entry.amount * entry.pricePerUnit

    val isLive = hasLivePriceFor(entry, currentProducts)
    val currentPrice = currentPriceFor(entry, currentProducts)
    val currentValue = entry.amount * currentPrice
    val profit = currentValue - total
    val profitPercent = if (total > 0) (profit / total) * 100 else 0.0
    val profitColor = if (profit >= 0) BottleInk else WaxSeal
    val sign = if (profit >= 0) "+" else ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.productName,
                style = MaterialTheme.typography.titleMedium,
                color = InkCharcoal
            )
            Text(
                text = "${entry.amount} ${entry.unit} · ${dateFormat.format(entry.dateMillis)}",
                style = MaterialTheme.typography.bodyMedium,
                color = InkFaded
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${entry.pricePerUnit.toInt()} TL / ${entry.unit}",
                style = MaterialTheme.typography.bodyMedium,
                color = InkFaded
            )
            Text(
                text = "${total.toInt()} TL",
                style = MaterialTheme.typography.labelLarge,
                color = BottleInk,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "$sign${profit.toInt()} TL (${String.format(Locale("tr"), "%.1f", profitPercent)}%)",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isLive) profitColor else InkFaded
            )
            if (!isLive) {
                Text(
                    text = "fiyat güncel değil",
                    style = MaterialTheme.typography.labelSmall,
                    color = InkFaded
                )
            }

            Row(modifier = Modifier.padding(top = 4.dp)) {
                Text(
                    text = "DÜZENLE",
                    style = MaterialTheme.typography.labelSmall,
                    color = AntiqueBrass,
                    modifier = Modifier.clickable(onClick = onEdit)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "SİL",
                    style = MaterialTheme.typography.labelSmall,
                    color = WaxSeal,
                    modifier = Modifier.clickable(onClick = onDelete)
                )
            }
        }
    }
}