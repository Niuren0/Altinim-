# Altınım

Kişisel altın birikim takip uygulaması. Fiyatlar kurpano.com/aliaga'nın kendi API'sinden çekiliyor.

## Şu an neler var (v0.1)
- Canlı fiyat listesi ekranı (has altın, çeyrek, yarım, ziynet, gümüş vb. — API ne dönerse)
- Alış/satış fiyatları renkli gösterim

## Henüz yok (sırada)
- Birikim kayıtları (Room database)
- "Altın ekle" ekranı
- Toplam birikim özeti (gram, TL karşılığı, kâr/zarar)
- Arka planda periyodik fiyat güncelleme (WorkManager)

## Nasıl çalıştırılır

1. **Android Studio kur** (ücretsiz): https://developer.android.com/studio
2. Android Studio'yu aç → **Open** → bu klasörü (Altinim) seç
3. Android Studio ilk açılışta Gradle senkronizasyonu yapacak, birkaç dakika sürebilir (internet gerekiyor, bağımlılıkları indiriyor)
4. Senkron bitince üstteki yeşil ▶ (Run) butonuna bas
5. Ya bir Android emulator seç, ya da telefonunu USB ile bağlayıp "Geliştirici seçenekleri > USB hata ayıklama"yı açarak gerçek cihazda çalıştır

## Proje yapısı

```
app/src/main/java/com/altinim/app/
├── MainActivity.kt              → giriş noktası
├── data/remote/                 → kurpano API bağlantısı (Retrofit)
│   ├── KurpanoApi.kt
│   ├── PriceModels.kt
│   └── NetworkModule.kt
├── data/repository/
│   └── PriceRepository.kt       → API çağrısını UI'dan soyutlayan katman
└── ui/
    ├── theme/                   → renkler, Material3 tema
    └── screens/
        ├── PriceScreen.kt       → fiyat listesi ekranı (Compose)
        └── PriceViewModel.kt    → ekranın durum yönetimi (yükleniyor/hata/başarı)
```

## Not
kurpano.com'un fiyat endpoint'i resmi/dokümante bir API değil, sitenin kendi
iç kullanımı. İleride path veya format değişebilir — böyle bir durumda
`NetworkModule.kt` ve `KurpanoApi.kt` güncellenir, geri kalan kod etkilenmez.
