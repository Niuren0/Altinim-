package com.altinim.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.altinim.app.AltinimApplication
import com.altinim.app.data.local.AppSettings
import com.altinim.app.data.remote.GoldProduct
import com.altinim.app.data.repository.PriceUiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as AltinimApplication).container
    private val repository = container.settingsRepository
    private val priceStore = container.priceStore

    val settings: StateFlow<AppSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val availableProducts: StateFlow<List<GoldProduct>> = priceStore.uiState
        .map { state -> (state as? PriceUiState.Success)?.products ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    fun setAppLockPin(pin: String) {
        viewModelScope.launch {
            repository.setAppLockPin(pin)
        }
    }

    fun disableAppLock() {
        viewModelScope.launch {
            repository.disableAppLock()
        }
    }
}