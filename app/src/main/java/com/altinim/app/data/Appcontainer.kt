package com.altinim.app.data

import android.content.Context
import com.altinim.app.data.local.AppDatabase
import com.altinim.app.data.local.SettingsRepository
import com.altinim.app.data.repository.GoldEntryRepository
import com.altinim.app.data.repository.PriceStore

class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(appContext)
    }

    val goldEntryRepository: GoldEntryRepository by lazy {
        GoldEntryRepository(AppDatabase.getInstance(appContext).goldEntryDao())
    }

    val priceStore: PriceStore by lazy {
        PriceStore.getInstance(appContext)
    }
}