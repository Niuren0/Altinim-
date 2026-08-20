package com.altinim.app.data.repository

import com.altinim.app.data.remote.GoldProduct
import com.altinim.app.data.remote.KurpanoApi

class PriceRepository(
    private val api: KurpanoApi
) {
    suspend fun fetchPrices(): List<GoldProduct> {
        val response = api.getPrices()
        return response.Value
    }
}
