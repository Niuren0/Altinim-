package com.altinim.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.altinim.app.data.local.AppSettings
import com.altinim.app.data.local.SettingsRepository
import com.altinim.app.data.repository.PriceStore
import com.altinim.app.data.repository.PriceUiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class PriceViewModel(application: Application) : AndroidViewModel(application) {

    private val priceStore = PriceStore.getInstance(application)
    private val settingsRepository = SettingsRepository(application)

    val uiState: StateFlow<PriceUiState> = priceStore.uiState

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    fun loadPrices() {
        priceStore.refreshNow()
    }
}