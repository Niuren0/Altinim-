package com.altinim.app.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface KurpanoApi {

    // customUrl her kuyumcunun kendi slug'ı. Bizimki için sabit "aliaga",
    // ama ileride başka bir kuyumcuya geçilirse buradan değiştirilir.
    @GET("CustomHome/GetCurrentCompanyProductPrice")
    suspend fun getPrices(
        @Query("customUrl") customUrl: String = "aliaga"
    ): PriceApiResponse
}
