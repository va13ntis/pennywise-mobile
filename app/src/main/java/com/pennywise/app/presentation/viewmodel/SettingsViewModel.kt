package com.pennywise.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pennywise.app.data.util.SettingsDataStore
import com.pennywise.app.domain.model.Currency
import com.pennywise.app.domain.model.User
import com.pennywise.app.domain.repository.UserRepository
import com.pennywise.app.domain.usecase.CurrencySortingService
import com.pennywise.app.presentation.auth.AuthManager
import com.pennywise.app.presentation.utils.CurrencyViewModelExtensions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing app settings including theme preferences, language selection, and default currency
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val userRepository: UserRepository,
    private val authManager: AuthManager,
    private val currencySortingService: CurrencySortingService
) : ViewModel() {

    // Theme mode state
    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode

    // Language state
    private val _language = MutableStateFlow("")
    val language: StateFlow<String> = _language

    // Currency conversion state
    private val _currencyConversionEnabled = MutableStateFlow(false)
    val currencyConversionEnabled: StateFlow<Boolean> = _currencyConversionEnabled

    // Original currency state
    private val _originalCurrency = MutableStateFlow("")
    val originalCurrency: StateFlow<String> = _originalCurrency

    // Default currency state
    private val _defaultCurrencyState = MutableStateFlow<DefaultCurrencyState>(DefaultCurrencyState.Loading)
    val defaultCurrencyState: StateFlow<DefaultCurrencyState> = _defaultCurrencyState

    // Currency update state
    private val _currencyUpdateState = MutableStateFlow<CurrencyUpdateState>(CurrencyUpdateState.Idle)
    val currencyUpdateState: StateFlow<CurrencyUpdateState> = _currencyUpdateState
    
    // Sorted currencies for selection
    private val _sortedCurrencies = MutableStateFlow<List<Currency>>(emptyList())
    val sortedCurrencies: StateFlow<List<Currency>> = _sortedCurrencies
    
    // Top currencies (most used)
    private val _topCurrencies = MutableStateFlow<List<Currency>>(emptyList())
    val topCurrencies: StateFlow<List<Currency>> = _topCurrencies

    init {
        loadSettings()
        loadDefaultCurrency()
        loadSortedCurrencies()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            // Load theme mode
            val themeMode = settingsDataStore.getThemeMode()
            _themeMode.value = when (themeMode) {
                "light" -> ThemeMode.LIGHT
                "dark" -> ThemeMode.DARK
                else -> ThemeMode.SYSTEM
            }
        }

        viewModelScope.launch {
            // Load language
            val language = settingsDataStore.getLanguage()
            _language.value = language
        }

        viewModelScope.launch {
            // Load currency conversion settings
            val enabled = settingsDataStore.getCurrencyConversionEnabled()
            _currencyConversionEnabled.value = enabled
        }

        viewModelScope.launch {
            // Load original currency
            val currency = settingsDataStore.getOriginalCurrency()
            _originalCurrency.value = currency
        }
    }

    private fun loadDefaultCurrency() {
        viewModelScope.launch {
            try {
                _defaultCurrencyState.value = DefaultCurrencyState.Loading
                
                authManager.currentUser.collect { currentUser ->
                    if (currentUser != null) {
                        val defaultCurrency = currentUser.defaultCurrency ?: "USD"
                        _defaultCurrencyState.value = DefaultCurrencyState.Success(defaultCurrency)
                    } else {
                        _defaultCurrencyState.value = DefaultCurrencyState.Error("User not authenticated")
                    }
                }
            } catch (e: Exception) {
                _defaultCurrencyState.value = DefaultCurrencyState.Error("Failed to load default currency: ${e.message}")
            }
        }
    }
    
    private fun loadSortedCurrencies() {
        viewModelScope.launch {
            authManager.currentUser.collect { user ->
                if (user != null) {
                    // Load sorted currencies
                    currencySortingService.getSortedCurrencies(user.id).collect { sortedCurrencies ->
                        _sortedCurrencies.value = sortedCurrencies
                    }
                }
            }
        }
        
        viewModelScope.launch {
            authManager.currentUser.collect { user ->
                if (user != null) {
                    // Load top currencies
                    currencySortingService.getTopCurrencies(user.id, 10).collect { topCurrencies ->
                        _topCurrencies.value = topCurrencies
                    }
                }
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            val themeString = when (mode) {
                ThemeMode.LIGHT -> "light"
                ThemeMode.DARK -> "dark"
                ThemeMode.SYSTEM -> "system"
            }
            settingsDataStore.setThemeMode(themeString)
            _themeMode.value = mode
        }
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch {
            settingsDataStore.setLanguage(lang)
            _language.value = lang
        }
    }

    fun setCurrencyConversionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setCurrencyConversionEnabled(enabled)
            _currencyConversionEnabled.value = enabled
        }
    }

    fun setOriginalCurrency(currency: String) {
        viewModelScope.launch {
            settingsDataStore.setOriginalCurrency(currency)
            _originalCurrency.value = currency
        }
    }

    fun updateDefaultCurrency(newCurrency: String) {
        viewModelScope.launch {
            try {
                _currencyUpdateState.value = CurrencyUpdateState.Loading
                
                val currentUser = authManager.currentUser.value
                if (currentUser != null) {
                    // Update the user's default currency
                    userRepository.updateDefaultCurrency(currentUser.id, newCurrency)
                    
                    // Update the current user in auth manager
                    val updatedUser = currentUser.copy(defaultCurrency = newCurrency)
                    authManager.updateCurrentUser(updatedUser)
                    
                    // Track currency usage for sorting
                    currencySortingService.trackCurrencyUsage(currentUser.id, newCurrency)
                    
                    // Update the default currency state
                    _defaultCurrencyState.value = DefaultCurrencyState.Success(newCurrency)
                    _currencyUpdateState.value = CurrencyUpdateState.Success
                } else {
                    _currencyUpdateState.value = CurrencyUpdateState.Error("User not authenticated")
                }
            } catch (e: Exception) {
                _currencyUpdateState.value = CurrencyUpdateState.Error("Failed to update default currency: ${e.message}")
            }
        }
    }

    fun resetCurrencyUpdateState() {
        _currencyUpdateState.value = CurrencyUpdateState.Idle
    }

    enum class ThemeMode {
        LIGHT, DARK, SYSTEM
    }

    sealed class DefaultCurrencyState {
        object Loading : DefaultCurrencyState()
        data class Success(val currencyCode: String) : DefaultCurrencyState()
        data class Error(val message: String) : DefaultCurrencyState()
    }

    sealed class CurrencyUpdateState {
        object Idle : CurrencyUpdateState()
        object Loading : CurrencyUpdateState()
        object Success : CurrencyUpdateState()
        data class Error(val message: String) : CurrencyUpdateState()
    }
}
