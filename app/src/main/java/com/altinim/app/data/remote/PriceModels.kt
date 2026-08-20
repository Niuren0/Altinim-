package com.altinim.app.data.remote

// kurpano.com/CustomHome/GetCurrentCompanyProductPrice cevabının şekli.
// Alan adları API'nin döndüğü isimlerle birebir aynı (Gson eşlemesi için).

data class GoldProduct(
    val Id: Int,
    val ProductName: String,
    val RoundPurchasePrice: String,
    val RoundSalesPrice: String,
    val TableId: Int,
    val TableSort: Int
)

data class PriceApiResponse(
    val Value: List<GoldProduct>,
    val Status: Boolean
)
