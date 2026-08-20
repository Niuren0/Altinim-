package com.altinim.app.data

import com.altinim.app.data.remote.GoldProduct
import java.util.Locale

// İlk kurulumda kullanıcı henüz elle sıralama yapmadıysa bu anahtar
// kelimeler öncelikli ürünleri öne çıkarır. Kullanıcı Ayarlar'dan elle
// sıralama yaptıkça bu sadece ikincil (yedek) bir kıstas olarak kalır.
val DEFAULT_PRODUCT_PRIORITY = listOf("ATA", "REŞAT", "YENİ YARIM", "YENİ ÇEYREK", "HAS")

private fun keywordPriorityIndex(name: String): Int {
    val upper = name.uppercase(Locale("tr"))
    val index = DEFAULT_PRODUCT_PRIORITY.indexOfFirst { keyword -> upper.contains(keyword) }
    return if (index == -1) Int.MAX_VALUE else index
}

// Kullanıcının Ayarlar'dan belirlediği tam sırayı uygular; sırada
// olmayan (henüz elle taşınmamış) ürünler için DEFAULT_PRODUCT_PRIORITY'ye,
// o da eşleşmezse orijinal API sırasına düşer.
fun orderProductNames(names: List<String>, customOrder: List<String>): List<String> {
    return names.sortedWith(
        compareBy(
            { name -> customOrder.indexOf(name).let { if (it == -1) Int.MAX_VALUE else it } },
            { name -> keywordPriorityIndex(name) }
        )
    )
}

fun sortAndFilterProductNames(
    names: List<String>,
    customOrder: List<String>,
    hidden: Set<String>
): List<String> = orderProductNames(names, customOrder).filterNot { it in hidden }

fun sortAndFilterGoldProducts(
    products: List<GoldProduct>,
    customOrder: List<String>,
    hidden: Set<String>
): List<GoldProduct> {
    val orderedNames = orderProductNames(products.map { it.ProductName }, customOrder)
    val byName = products.associateBy { it.ProductName }
    return orderedNames.mapNotNull { byName[it] }.filterNot { it.ProductName in hidden }
}