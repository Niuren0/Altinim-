package com.altinim.app.data.remote

import com.google.gson.annotations.SerializedName

data class GoldProduct(
    @SerializedName("Id") val id: Int,
    @SerializedName("ProductName") val productName: String,
    @SerializedName("RoundPurchasePrice") val roundPurchasePrice: String,
    @SerializedName("RoundSalesPrice") val roundSalesPrice: String
)

data class PriceApiResponse(
    @SerializedName("Value") val value: List<GoldProduct>,
    @SerializedName("Status") val status: Boolean
)