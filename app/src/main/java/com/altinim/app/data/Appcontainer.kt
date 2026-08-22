package com.altinim.app.data

import android.content.Context
import com.altinim.app.data.local.AppDatabase
import com.altinim.app.data.local.SettingsRepository
import com.altinim.app.data.remote.NetworkModule
import com.altinim.app.data.repository.GoldEntryRepository
import com.altinim.app.data.repository.PriceRepository
import com.altinim.app.data.repository.PriceStore
import com.altinim.app.data.repository.UpdateRepository

class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(appContext)
    }

    val goldEntryRepository: GoldEntryRepository by lazy {
        GoldEntryRepository(AppDatabase.getInstance(appContext).goldEntryDao())
    }

    val priceStore: PriceStore by lazy {
        PriceStore(PriceRepository(NetworkModule.kurpanoApi), settingsRepository)
    }

    val updateRepository: UpdateRepository by lazy {
        UpdateRepository()
    }
}