package com.altinim.app.ui.screens

import android.app.Application
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.altinim.app.data.local.AppDatabase
import com.altinim.app.data.local.AppSettings
import com.altinim.app.data.local.GoldEntry
import com.altinim.app.data.local.SettingsRepository
import com.altinim.app.data.remote.GoldProduct
import com.altinim.app.data.remote.NetworkModule
import com.altinim.app.data.repository.GoldEntryRepository
import com.altinim.app.data.repository.PriceRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class HoldingsViewModel(application: Application) : AndroidViewModel(application) {

    private val entryRepository = GoldEntryRepository(
        AppDatabase.getInstance(application).goldEntryDao()
    )
    private val priceRepository = PriceRepository(NetworkModule.kurpanoApi)
    private val settingsRepository = SettingsRepository(application)

    val entries: StateFlow<List<GoldEntry>> = entryRepository.getAllEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    private val _currentProducts = MutableStateFlow<List<GoldProduct>>(emptyList())
    val currentProducts: StateFlow<List<GoldProduct>> = _currentProducts.asStateFlow()

    private var priceRefreshJob: Job? = null

    init {
        viewModelScope.launch {
            settings.map { it.refreshIntervalSeconds }
                .distinctUntilChanged()
                .collect { intervalSeconds ->
                    startPeriodicPriceRefresh(intervalSeconds)
                }
        }
    }

    fun loadCurrentPrices() {
        viewModelScope.launch { fetchPricesOnce() }
    }

    private fun startPeriodicPriceRefresh(intervalSeconds: Int) {
        priceRefreshJob?.cancel()
        priceRefreshJob = viewModelScope.launch {
            while (true) {
                fetchPricesOnce()
                delay((intervalSeconds * 1000L).milliseconds)
            }
        }
    }

    private suspend fun fetchPricesOnce() {
        try {
            _currentProducts.value = priceRepository.fetchPrices()
        } catch (e: Exception) {
            android.util.Log.e("Holdings", "Fiyatlar çekilemedi", e)
        }
    }

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
            com.altinim.app.widget.HoldingsWidget().updateAll(getApplication())
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
            com.altinim.app.widget.HoldingsWidget().updateAll(getApplication())
        }
    }

    fun deleteEntry(entry: GoldEntry) {
        viewModelScope.launch {
            entryRepository.deleteEntry(entry)
            com.altinim.app.widget.HoldingsWidget().updateAll(getApplication())
        }
    }

    override fun onCleared() {
        super.onCleared()
        priceRefreshJob?.cancel()
    }
}