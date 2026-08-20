package com.altinim.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.altinim.app.data.local.AppSettings
import com.altinim.app.data.local.SettingsRepository
import com.altinim.app.data.remote.GoldProduct
import com.altinim.app.data.remote.NetworkModule
import com.altinim.app.data.repository.PriceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository(application)
    private val priceRepository = PriceRepository(NetworkModule.kurpanoApi)

    val settings: StateFlow<AppSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    // Sıralama/gizleme listesini gösterebilmek için ürün adlarına
    // ihtiyacımız var, bu yüzden burada da bir kere fiyat çekiyoruz.
    private val _availableProducts = MutableStateFlow<List<GoldProduct>>(emptyList())
    val availableProducts: StateFlow<List<GoldProduct>> = _availableProducts.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                _availableProducts.value = priceRepository.fetchPrices()
            } catch (e: Exception) {
                android.util.Log.e("Settings", "Ürün listesi çekilemedi", e)
            }
        }
    }

    fun moveProduct(orderedNames: List<String>, fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            if (toIndex !in orderedNames.indices) return@launch
            val mutable = orderedNames.toMutableList()
            val item = mutable.removeAt(fromIndex)
            mutable.add(toIndex, item)
            repository.updateProductOrder(mutable)
        }
    }

    fun toggleHidden(name: String) {
        viewModelScope.launch {
            val current = settings.value.hiddenProducts.toMutableSet()
            if (name in current) current.remove(name) else current.add(name)
            repository.updateHiddenProducts(current)
        }
    }

    fun updateRefreshInterval(seconds: Int) {
        viewModelScope.launch {
            repository.updateRefreshInterval(seconds)
        }
    }

    fun updateAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateAppLockEnabled(enabled)
        }
    }
}