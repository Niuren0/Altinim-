package com.altinim.app.data.repository

import android.content.Context
import com.altinim.app.data.local.SettingsRepository
import com.altinim.app.data.parseTurkishNumber
import com.altinim.app.data.remote.GoldProduct
import com.altinim.app.data.remote.NetworkModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

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

// Uygulama genelinde tek fiyat kaynağı. Önceden Fiyatlar, Birikimim ve
// Ayarlar ekranları kurpano.com'a birbirinden bağımsız istek atıyordu
// (Fiyatlar + Birikimim sürekli polling, Ayarlar tek seferlik) — artık
// tek bir polling döngüsü var, hepsi bu döngünün sonucuna abone oluyor.
class PriceStore private constructor(context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val repository = PriceRepository(NetworkModule.kurpanoApi)
    private val settingsRepository = SettingsRepository(context)

    private val _uiState = MutableStateFlow<PriceUiState>(PriceUiState.Loading)
    val uiState: StateFlow<PriceUiState> = _uiState.asStateFlow()

    // Son başarılı fetch'ten sonra en az bir deneme başarısız oldu mu —
    // Birikimim ekranındaki "fiyat güncel değil" göstergesi için.
    private val _stale = MutableStateFlow(false)
    val stale: StateFlow<Boolean> = _stale.asStateFlow()

    private var refreshJob: Job? = null
    private var lastProducts: List<GoldProduct>? = null

    init {
        scope.launch {
            settingsRepository.settings
                .map { it.refreshIntervalSeconds }
                .distinctUntilChanged()
                .collect { intervalSeconds -> startPeriodicRefresh(intervalSeconds) }
        }
    }

    // "TEKRAR DENE" butonu için anında fetch — periyodik döngüyü resetlemez.
    fun refreshNow() {
        scope.launch { fetchOnce(forceLoading = true) }
    }

    private fun startPeriodicRefresh(intervalSeconds: Int) {
        refreshJob?.cancel()
        refreshJob = scope.launch {
            var isFirstFetch = lastProducts == null
            while (true) {
                fetchOnce(forceLoading = isFirstFetch)
                isFirstFetch = false
                delay((intervalSeconds * 1000L).milliseconds)
            }
        }
    }

    private suspend fun fetchOnce(forceLoading: Boolean) {
        if (forceLoading) {
            _uiState.value = PriceUiState.Loading
        }
        try {
            val newProducts = repository.fetchPrices()
            val changes = computeChanges(lastProducts, newProducts)
            lastProducts = newProducts
            _uiState.value = PriceUiState.Success(newProducts, changes)
            _stale.value = false
        } catch (e: Exception) {
            _stale.value = lastProducts != null
            if (forceLoading || _uiState.value !is PriceUiState.Success) {
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

    companion object {
        @Volatile private var INSTANCE: PriceStore? = null

        fun getInstance(context: Context): PriceStore {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PriceStore(context.applicationContext).also { INSTANCE = it }
            }
        }

        suspend fun getPricesForWidget(context: Context): List<GoldProduct> {
            val cached = (INSTANCE?.uiState?.value as? PriceUiState.Success)?.products
            if (cached != null) return cached
            return try {
                PriceRepository(NetworkModule.kurpanoApi).fetchPrices()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}