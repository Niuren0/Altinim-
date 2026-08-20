package com.altinim.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

// Kullanıcının kaydettiği tek bir altın alım kaydı.
// amount: gram ya da adet miktarı, unit: "gram" | "adet".
// pricePerUnit: alım anındaki birim fiyat (TL) — o günkü kurpano fiyatından
// otomatik dolduruluyor ama kullanıcı isterse elle değiştirebiliyor.
@Entity(tableName = "gold_entries")
data class GoldEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productName: String,
    val amount: Double,
    val unit: String,
    val pricePerUnit: Double,
    val dateMillis: Long
)