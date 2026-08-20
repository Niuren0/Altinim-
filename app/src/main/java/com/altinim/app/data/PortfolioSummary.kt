package com.altinim.app.data

import com.altinim.app.data.local.GoldEntry
import com.altinim.app.data.remote.GoldProduct

data class PortfolioSummary(
    val totalInvested: Double,
    val currentValue: Double
) {
    val profit: Double get() = currentValue - totalInvested
    val profitPercent: Double get() = if (totalInvested > 0) (profit / totalInvested) * 100 else 0.0
}

fun computePortfolioSummary(
    entries: List<GoldEntry>,
    currentProducts: List<GoldProduct>
): PortfolioSummary {
    val totalInvested = entries.sumOf { it.amount * it.pricePerUnit }
    val currentValue = entries.sumOf { entry ->
        val currentPrice = currentProducts
            .find { it.ProductName == entry.productName }
            ?.RoundPurchasePrice
            ?.let { parseTurkishNumber(it) }
            ?: entry.pricePerUnit
        entry.amount * currentPrice
    }
    return PortfolioSummary(totalInvested, currentValue)
}