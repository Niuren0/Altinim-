package com.altinim.app.data

// API'den gelen ve kullanıcının Türkçe alışkanlığıyla girebileceği
// sayılar nokta binlik ayıraç, virgül ondalık ayıraç kullanır
// (örn. "6.630" = altı bin altı yüz otuz). Önce binlik noktaları
// temizleyip virgülü ondalık noktaya çeviriyoruz.
fun parseTurkishNumber(text: String): Double? {
    val cleaned = text.trim().replace(".", "").replace(",", ".")
    return cleaned.toDoubleOrNull()
}