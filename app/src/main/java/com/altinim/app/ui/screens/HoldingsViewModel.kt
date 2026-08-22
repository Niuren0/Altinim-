package com.altinim.app.ui.screens

import android.app.Application
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.altinim.app.AltinimApplication
import com.altinim.app.data.local.AppSettings
import com.altinim.app.data.local.GoldEntry
import com.altinim.app.data.remote.GoldProduct
import com.altinim.app.data.repository.PriceUiState
import com.altinim.app.widget.HoldingsWidget
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HoldingsViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as AltinimApplication).container
    private val entryRepository = container.goldEntryRepository
    private val priceStore = container.priceStore
    private val settingsRepository = container.settingsRepository

    val entries: StateFlow<List<GoldEntry>> = entryRepository.getAllEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            entries.drop(1).collect { notifyWidget() }
        }
    }

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val currentProducts: StateFlow<List<GoldProduct>> = priceStore.uiState
        .map { state -> (state as? PriceUiState.Success)?.products ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pricesStale: StateFlow<Boolean> = priceStore.stale

    fun addEntry(productName: String, amount: Double, unit: String, pricePerUnit: Double, dateMillis: Long) {
        viewModelScope.launch {
            entryRepository.addEntry(
                GoldEntry(
                    productName = productName,
                    amount = amount,
                    unit = unit,
                    pricePerUnit = pricePerUnit,
                    dateMillis = dateMillis
                )
            )
        }
    }

    fun updateEntry(id: Long, productName: String, amount: Double, unit: String, pricePerUnit: Double, dateMillis: Long) {
        viewModelScope.launch {
            entryRepository.updateEntry(
                GoldEntry(
                    id = id,
                    productName = productName,
                    amount = amount,
                    unit = unit,
                    pricePerUnit = pricePerUnit,
                    dateMillis = dateMillis
                )
            )
        }
    }

    fun deleteEntry(entry: GoldEntry) {
        viewModelScope.launch {
            entryRepository.deleteEntry(entry)
        }
    }

    private suspend fun notifyWidget() {
        HoldingsWidget().updateAll(getApplication())
    }
}