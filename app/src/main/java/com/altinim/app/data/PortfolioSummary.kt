package com.altinim.app.data

import com.altinim.app.data.local.GoldEntry
import com.altinim.app.data.remote.GoldProduct

data class PortfolioSummary(
    val totalInvested: Double,
    val currentValue: Double,
    val hasLivePrices: Boolean
) {
    val profit: Double get() = currentValue - totalInvested
    val profitPercent: Double get() = if (totalInvested > 0) (profit / totalInvested) * 100 else 0.0
}

fun computePortfolioSummary(
    entries: List<GoldEntry>,
    currentProducts: List<GoldProduct>
): PortfolioSummary {
    val hasLivePrices = currentProducts.isNotEmpty()
    val totalInvested = entries.sumOf { it.amount * it.pricePerUnit }
    val currentValue = entries.sumOf { entry ->
        entry.amount * currentPriceFor(entry, currentProducts)
    }
    return PortfolioSummary(totalInvested, currentValue, hasLivePrices)
}

fun hasLivePriceFor(entry: GoldEntry, currentProducts: List<GoldProduct>): Boolean =
    currentProducts.any { it.productName == entry.productName }

// Bir kaydın "şimdi satsan ne alırsın" değeri için kullanılan güncel birim
// fiyatı. Kuyumcunun geri ALIŞ fiyatı (RoundPurchasePrice) esas alınıyor;
// ürün artık API'de yoksa (isim değişmiş/kaldırılmış) veya fiyat
// parse edilemiyorsa, kaydın alındığı andaki fiyata (pricePerUnit) düşülüyor.
// Hem portföy özeti hem tekil kayıt satırları bu fonksiyonu kullanmalı ki
// fiyatlama mantığı tek yerden değişsin.
fun currentPriceFor(entry: GoldEntry, currentProducts: List<GoldProduct>): Double =
    currentProducts
        .find { it.productName == entry.productName }
        ?.roundPurchasePrice
        ?.let { parseTurkishNumber(it) }
        ?: entry.pricePerUnit