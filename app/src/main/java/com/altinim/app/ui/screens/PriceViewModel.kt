package com.altinim.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.altinim.app.AltinimApplication
import com.altinim.app.data.local.AppSettings
import com.altinim.app.data.repository.PriceUiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class PriceViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as AltinimApplication).container
    private val priceStore = container.priceStore
    private val settingsRepository = container.settingsRepository

    val uiState: StateFlow<PriceUiState> = priceStore.uiState

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    fun loadPrices() {
        priceStore.refreshNow()
    }
}