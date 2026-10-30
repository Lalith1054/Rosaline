package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ProductEntry
import com.example.data.ProductRepository
import com.example.data.SettingsPreferences
import com.example.data.UserSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class ProductFilter {
    ALL,
    ACTIVE,
    FINISHED
}

data class ProductTrackerUiState(
    val allProducts: List<ProductEntry> = emptyList(),
    val filteredProducts: List<ProductEntry> = emptyList(),
    val selectedFilter: ProductFilter = ProductFilter.ALL,
    val nowMillis: Long = System.currentTimeMillis(),
    val activeCount: Int = 0,
    val finishedCount: Int = 0,
    val averageFinishedDays: Int = 0,
    val isAddDialogOpen: Boolean = false,
    val isSettingsOpen: Boolean = false,
    val userSettings: UserSettings = UserSettings()
)

class ProductTrackerViewModel(
    private val repository: ProductRepository,
    private val settingsPreferences: SettingsPreferences? = null
) : ViewModel() {

    private val selectedFilterFlow = MutableStateFlow(ProductFilter.ALL)
    private val nowMillisFlow = MutableStateFlow(System.currentTimeMillis())
    private val isAddDialogOpenFlow = MutableStateFlow(false)
    private val isSettingsOpenFlow = MutableStateFlow(false)
    private val fallbackSettingsFlow = MutableStateFlow(UserSettings())

    private val activeSettingsFlow: StateFlow<UserSettings> =
        settingsPreferences?.settingsFlow ?: fallbackSettingsFlow

    init {
        viewModelScope.launch {
            while (isActive) {
                nowMillisFlow.value = System.currentTimeMillis()
                delay(1_000L)
            }
        }
    }

    val uiState: StateFlow<ProductTrackerUiState> = combine(
        repository.allProducts,
        selectedFilterFlow,
        nowMillisFlow,
        isAddDialogOpenFlow,
        combine(isSettingsOpenFlow, activeSettingsFlow) { isSettings, settings ->
            isSettings to settings
        }
    ) { products, filter, now, isDialogOpen, (isSettings, settings) ->
        val filtered = when (filter) {
            ProductFilter.ALL -> products
            ProductFilter.ACTIVE -> products.filter { it.isActive }
            ProductFilter.FINISHED -> products.filter { !it.isActive }
        }
        val activeItems = products.filter { it.isActive }
        val finishedItems = products.filter { !it.isActive }

        val avgDays = if (finishedItems.isNotEmpty()) {
            finishedItems.map {
                it.elapsedDaysCount(now, settings.countStartDayAsDayOne).toDouble()
            }.average().roundToInt()
        } else {
            0
        }

        ProductTrackerUiState(
            allProducts = products,
            filteredProducts = filtered,
            selectedFilter = filter,
            nowMillis = now,
            activeCount = activeItems.size,
            finishedCount = finishedItems.size,
            averageFinishedDays = avgDays,
            isAddDialogOpen = isDialogOpen,
            isSettingsOpen = isSettings,
            userSettings = settings
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProductTrackerUiState()
    )

    fun refreshNow() {
        nowMillisFlow.value = System.currentTimeMillis()
    }

    fun setFilter(filter: ProductFilter) {
        selectedFilterFlow.value = filter
    }

    fun openAddDialog() {
        isAddDialogOpenFlow.value = true
    }

    fun closeAddDialog() {
        isAddDialogOpenFlow.value = false
    }

    fun openSettings() {
        isSettingsOpenFlow.value = true
    }

    fun closeSettings() {
        isSettingsOpenFlow.value = false
    }

    fun setCountStartDayAsDayOne(enabled: Boolean) {
        if (settingsPreferences != null) {
            settingsPreferences.setCountStartDayAsDayOne(enabled)
        } else {
            fallbackSettingsFlow.value = fallbackSettingsFlow.value.copy(countStartDayAsDayOne = enabled)
        }
    }

    fun setShowLiveSubDayTimer(enabled: Boolean) {
        if (settingsPreferences != null) {
            settingsPreferences.setShowLiveSubDayTimer(enabled)
        } else {
            fallbackSettingsFlow.value = fallbackSettingsFlow.value.copy(showLiveSubDayTimer = enabled)
        }
    }

    fun setSelectedTheme(theme: com.example.data.AppThemeOption) {
        if (settingsPreferences != null) {
            settingsPreferences.setSelectedTheme(theme)
        } else {
            fallbackSettingsFlow.value = fallbackSettingsFlow.value.copy(selectedTheme = theme)
        }
    }

    fun addProduct(name: String, onAdded: (String) -> Unit = {}) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            nowMillisFlow.value = now
            repository.startNewProduct(trimmed, now)
            isAddDialogOpenFlow.value = false
            if (selectedFilterFlow.value == ProductFilter.FINISHED) {
                selectedFilterFlow.value = ProductFilter.ALL
            }
            onAdded(trimmed)
        }
    }

    fun stopProduct(product: ProductEntry, onStopped: (ProductEntry, Long) -> Unit = { _, _ -> }) {
        if (!product.isActive) return
        viewModelScope.launch {
            val stopTime = System.currentTimeMillis()
            nowMillisFlow.value = stopTime
            repository.stopProduct(product, stopTime)
            val countDayOne = activeSettingsFlow.value.countStartDayAsDayOne
            val daysUsed = product.copy(endTimeMillis = stopTime, isActive = false)
                .elapsedDaysCount(stopTime, countDayOne)
            onStopped(product, daysUsed)
        }
    }

    fun restartProduct(product: ProductEntry, onRestarted: (String) -> Unit = {}) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            nowMillisFlow.value = now
            repository.startNewProduct(product.name, now)
            onRestarted(product.name)
        }
    }

    fun deleteProduct(product: ProductEntry, onDeleted: (ProductEntry) -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteProduct(product.id)
            onDeleted(product)
        }
    }

    fun undoDelete(product: ProductEntry) {
        viewModelScope.launch {
            repository.restoreProduct(product)
        }
    }

    fun clearFinishedProducts(onCleared: () -> Unit = {}) {
        viewModelScope.launch {
            repository.clearFinishedProducts()
            onCleared()
        }
    }

    fun clearAllProducts(onCleared: () -> Unit = {}) {
        viewModelScope.launch {
            repository.clearAllProducts()
            onCleared()
        }
    }

    companion object {
        fun provideFactory(
            repository: ProductRepository,
            settingsPreferences: SettingsPreferences? = null
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ProductTrackerViewModel(repository, settingsPreferences) as T
                }
            }
    }
}
