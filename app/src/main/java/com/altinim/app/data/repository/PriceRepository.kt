package com.altinim.app.data.repository

import com.altinim.app.data.remote.GoldProduct
import com.altinim.app.data.remote.KurpanoApi
import java.io.IOException

class PriceRepository(
    private val api: KurpanoApi
) {
    suspend fun fetchPrices(): List<GoldProduct> {
        val response = api.getPrices()
        if (!response.status) {
            throw IOException("Fiyat servisi geçerli veri döndürmedi (Status=false).")
        }
        return response.value
    }
}