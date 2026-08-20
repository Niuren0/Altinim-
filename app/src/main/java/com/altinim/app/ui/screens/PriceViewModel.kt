package com.altinim.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.altinim.app.data.local.AppSettings
import com.altinim.app.data.local.SettingsRepository
import com.altinim.app.data.parseTurkishNumber
import com.altinim.app.data.remote.GoldProduct
import com.altinim.app.data.remote.NetworkModule
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

enum class PriceDirection { UP, DOWN, SAME }

data class ProductPriceChange(
    val purchaseDirection: PriceDirection,
    val salesDirection: PriceDirection
)

sealed interface PriceUiState {
    data object Loading : PriceUiState
    data class Success(
        val products: List<GoldProduct>,
        val changes: Map<Int, ProductPriceChange> = emptyMap()
    ) : PriceUiState
    data class Error(val message: String) : PriceUiState
}

class PriceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PriceRepository(NetworkModule.kurpanoApi)
    private val settingsRepository = SettingsRepository(application)

    private val _uiState = MutableStateFlow<PriceUiState>(PriceUiState.Loading)
    val uiState: StateFlow<PriceUiState> = _uiState.asStateFlow()

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    private var refreshJob: Job? = null
    private var lastProducts: List<GoldProduct>? = null

    init {
        viewModelScope.launch {
            settings.map { it.refreshIntervalSeconds }
                .distinctUntilChanged()
                .collect { intervalSeconds ->
                    startPeriodicRefresh(intervalSeconds)
                }
        }
    }

    fun loadPrices() {
        startPeriodicRefresh(settings.value.refreshIntervalSeconds)
    }

    private fun startPeriodicRefresh(intervalSeconds: Int) {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            var isFirstFetch = true
            while (true) {
                fetchOnce(isFirstFetch)
                isFirstFetch = false
                delay(intervalSeconds * 1000L)
            }
        }
    }

    private suspend fun fetchOnce(isFirstFetch: Boolean) {
        if (isFirstFetch) {
            _uiState.value = PriceUiState.Loading
        }
        try {
            val newProducts = repository.fetchPrices()
            val changes = computeChanges(lastProducts, newProducts)
            lastProducts = newProducts
            _uiState.value = PriceUiState.Success(newProducts, changes)
        } catch (e: Exception) {
            if (isFirstFetch || _uiState.value !is PriceUiState.Success) {
                _uiState.value = PriceUiState.Error(
                    e.message ?: "Fiyatlar alınamadı, internet bağlantını kontrol et."
                )
            }
        }
    }

    private fun computeChanges(
        old: List<GoldProduct>?,
        new: List<GoldProduct>
    ): Map<Int, ProductPriceChange> {
        if (old == null) return emptyMap()
        val oldById = old.associateBy { it.Id }
        return new.mapNotNull { newProduct ->
            val oldProduct = oldById[newProduct.Id] ?: return@mapNotNull null
            val purchaseDirection = directionOf(
                parseTurkishNumber(oldProduct.RoundPurchasePrice),
                parseTurkishNumber(newProduct.RoundPurchasePrice)
            )
            val salesDirection = directionOf(
                parseTurkishNumber(oldProduct.RoundSalesPrice),
                parseTurkishNumber(newProduct.RoundSalesPrice)
            )
            newProduct.Id to ProductPriceChange(purchaseDirection, salesDirection)
        }.toMap()
    }

    private fun directionOf(old: Double?, new: Double?): PriceDirection {
        if (old == null || new == null) return PriceDirection.SAME
        return when {
            new > old -> PriceDirection.UP
            new < old -> PriceDirection.DOWN
            else -> PriceDirection.SAME
        }
    }

    override fun onCleared() {
        super.onCleared()
        refreshJob?.cancel()
    }
}