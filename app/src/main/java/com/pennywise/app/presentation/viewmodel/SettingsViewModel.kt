package com.pennywise.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pennywise.app.data.util.SettingsDataStore
import com.pennywise.app.domain.model.Currency
import com.pennywise.app.domain.model.User
import com.pennywise.app.domain.model.PaymentMethod
import com.pennywise.app.domain.model.PaymentMethodConfig
import com.pennywise.app.domain.repository.UserRepository
import com.pennywise.app.domain.repository.PaymentMethodConfigRepository
import com.pennywise.app.domain.usecase.CurrencySortingService
import com.pennywise.app.presentation.auth.AuthManager
import com.pennywise.app.presentation.auth.DeviceAuthService
import com.pennywise.app.presentation.utils.CurrencyViewModelExtensions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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
    private val deviceAuthService: DeviceAuthService,
    private val currencySortingService: CurrencySortingService,
    private val paymentMethodConfigRepository: PaymentMethodConfigRepository
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
    
    // Payment method configurations state
    private val _paymentMethodConfigs = MutableStateFlow<List<PaymentMethodConfig>>(emptyList())
    val paymentMethodConfigs: StateFlow<List<PaymentMethodConfig>> = _paymentMethodConfigs
    
    // Default payment method state
    private val _defaultPaymentMethodState = MutableStateFlow<DefaultPaymentMethodState>(DefaultPaymentMethodState.Loading)
    val defaultPaymentMethodState: StateFlow<DefaultPaymentMethodState> = _defaultPaymentMethodState
    
    // Payment method update state
    private val _paymentMethodUpdateState = MutableStateFlow<PaymentMethodUpdateState>(PaymentMethodUpdateState.Idle)
    val paymentMethodUpdateState: StateFlow<PaymentMethodUpdateState> = _paymentMethodUpdateState
    
    // Developer options state
    private val _developerOptionsEnabled = MutableStateFlow(false)
    val developerOptionsEnabled: StateFlow<Boolean> = _developerOptionsEnabled
    
    // Authentication method state
    private val _currentAuthMethod = MutableStateFlow<AuthMethod?>(null)
    val currentAuthMethod: StateFlow<AuthMethod?> = _currentAuthMethod
    
    private val _authMethodUpdateState = MutableStateFlow<AuthMethodUpdateState>(AuthMethodUpdateState.Idle)
    val authMethodUpdateState: StateFlow<AuthMethodUpdateState> = _authMethodUpdateState
    
    private val _needsAuthentication = MutableStateFlow(false)
    val needsAuthentication: StateFlow<Boolean> = _needsAuthentication
    
    // Device authentication capabilities
    val canUseBiometric: Boolean get() = deviceAuthService.canUseBiometric()
    val canUseDeviceCredentials: Boolean get() = deviceAuthService.canUseDeviceCredentials()
    val isDeviceAuthEnabled: Flow<Boolean> = deviceAuthService.isDeviceAuthEnabled

    init {
        loadSettings()
        loadDefaultCurrency()
        loadSortedCurrencies()
        loadPaymentMethodConfigs()
        loadDefaultPaymentMethod()
        loadCurrentAuthMethod()
    }

    private fun loadCurrentAuthMethod() {
        viewModelScope.launch {
            try {
                val isDeviceAuthEnabled = deviceAuthService.isDeviceAuthEnabled.first()
                _currentAuthMethod.value = when {
                    isDeviceAuthEnabled && canUseBiometric -> AuthMethod.BIOMETRIC
                    isDeviceAuthEnabled && canUseDeviceCredentials -> AuthMethod.DEVICE_CREDENTIALS
                    else -> AuthMethod.NONE
                }
            } catch (e: Exception) {
                _currentAuthMethod.value = AuthMethod.NONE
            }
        }
    }
    
    fun updateAuthMethod(authMethod: AuthMethod) {
        viewModelScope.launch {
            try {
                _authMethodUpdateState.value = AuthMethodUpdateState.Loading
                
                val enabled = authMethod != AuthMethod.NONE
                deviceAuthService.setDeviceAuthEnabled(enabled)
                
                _currentAuthMethod.value = authMethod
                _authMethodUpdateState.value = AuthMethodUpdateState.Success
                
                // Reset success state after a delay
                kotlinx.coroutines.delay(2000)
                _authMethodUpdateState.value = AuthMethodUpdateState.Idle
                
            } catch (e: Exception) {
                _authMethodUpdateState.value = AuthMethodUpdateState.Error(
                    e.message ?: "Failed to update authentication method"
                )
            }
        }
    }
    
    fun resetAuthMethodUpdateState() {
        _authMethodUpdateState.value = AuthMethodUpdateState.Idle
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
                        val defaultCurrency = currentUser.defaultCurrency
                        _defaultCurrencyState.value = DefaultCurrencyState.Success(defaultCurrency)
                        _needsAuthentication.value = false
                    } else {
                        _defaultCurrencyState.value = DefaultCurrencyState.Error("User not authenticated")
                        _needsAuthentication.value = true
                    }
                }
            } catch (e: SecurityException) {
                _defaultCurrencyState.value = DefaultCurrencyState.Error("Authentication required")
                _needsAuthentication.value = true
            } catch (e: Exception) {
                _defaultCurrencyState.value = DefaultCurrencyState.Error("Failed to load default currency: ${e.message}")
                _needsAuthentication.value = false
            }
        }
    }
    
    private fun loadSortedCurrencies() {
        viewModelScope.launch {
            try {
                authManager.currentUser.collect { user ->
                    if (user != null) {
                        // Load sorted currencies
                        currencySortingService.getSortedCurrencies().collect { sortedCurrencies ->
                            _sortedCurrencies.value = sortedCurrencies
                            _needsAuthentication.value = false
                        }
                    }
                }
            } catch (e: SecurityException) {
                _needsAuthentication.value = true
            } catch (e: Exception) {
                _needsAuthentication.value = false
            }
        }
        
        viewModelScope.launch {
            try {
                authManager.currentUser.collect { user ->
                    if (user != null) {
                        // Load top currencies
                        currencySortingService.getTopCurrencies(10).collect { topCurrencies ->
                            _topCurrencies.value = topCurrencies
                            _needsAuthentication.value = false
                        }
                    }
                }
            } catch (e: SecurityException) {
                _needsAuthentication.value = true
            } catch (e: Exception) {
                _needsAuthentication.value = false
            }
        }
    }
    
    private fun loadPaymentMethodConfigs() {
        viewModelScope.launch {
            try {
                authManager.currentUser.collect { user ->
                    if (user != null) {
                        paymentMethodConfigRepository.getPaymentMethodConfigs().collect { configs ->
                            _paymentMethodConfigs.value = configs
                            _needsAuthentication.value = false
                        }
                    }
                }
            } catch (e: SecurityException) {
                _needsAuthentication.value = true
            } catch (e: Exception) {
                _needsAuthentication.value = false
            }
        }
    }
    
    private fun loadDefaultPaymentMethod() {
        viewModelScope.launch {
            try {
                _defaultPaymentMethodState.value = DefaultPaymentMethodState.Loading
                
                authManager.currentUser.collect { currentUser ->
                    if (currentUser != null) {
                        val defaultConfig = paymentMethodConfigRepository.getDefaultPaymentMethodConfig()
                        _defaultPaymentMethodState.value = DefaultPaymentMethodState.Success(defaultConfig)
                        _needsAuthentication.value = false
                    } else {
                        _defaultPaymentMethodState.value = DefaultPaymentMethodState.Error("User not authenticated")
                        _needsAuthentication.value = true
                    }
                }
            } catch (e: SecurityException) {
                _defaultPaymentMethodState.value = DefaultPaymentMethodState.Error("Authentication required")
                _needsAuthentication.value = true
            } catch (e: Exception) {
                _defaultPaymentMethodState.value = DefaultPaymentMethodState.Error("Failed to load default payment method: ${e.message}")
                _needsAuthentication.value = false
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
                    userRepository.updateDefaultCurrency(newCurrency)
                    
                    // Update the current user in auth manager
                    val updatedUser = currentUser.copy(defaultCurrency = newCurrency)
                    authManager.updateCurrentUser(updatedUser)
                    
                    // Track currency usage for sorting
                    currencySortingService.trackCurrencyUsage(newCurrency)
                    
                    // Update the default currency state
                    _defaultCurrencyState.value = DefaultCurrencyState.Success(newCurrency)
                    _currencyUpdateState.value = CurrencyUpdateState.Success
                    _needsAuthentication.value = false
                } else {
                    _currencyUpdateState.value = CurrencyUpdateState.Error("User not authenticated")
                    _needsAuthentication.value = true
                }
            } catch (e: SecurityException) {
                _currencyUpdateState.value = CurrencyUpdateState.Error("Authentication required")
                _needsAuthentication.value = true
            } catch (e: Exception) {
                _currencyUpdateState.value = CurrencyUpdateState.Error("Failed to update default currency: ${e.message}")
                _needsAuthentication.value = false
            }
        }
    }

    fun resetCurrencyUpdateState() {
        _currencyUpdateState.value = CurrencyUpdateState.Idle
    }
    
    // Payment Method Management Functions
    
    fun addPaymentMethodConfig(paymentMethod: PaymentMethod, alias: String, withdrawDay: Int? = null) {
        viewModelScope.launch {
            try {
                _paymentMethodUpdateState.value = PaymentMethodUpdateState.Loading
                
                val currentUser = authManager.currentUser.value
                if (currentUser != null) {
                    val config = PaymentMethodConfig.createDefault(
                        paymentMethod = paymentMethod,
                        alias = alias
                    ).copy(withdrawDay = withdrawDay)
                    
                    val configId = paymentMethodConfigRepository.insertPaymentMethodConfig(config)
                    
                    // If this is the first payment method, set it as default
                    val configCount = paymentMethodConfigRepository.getPaymentMethodConfigCount()
                    if (configCount == 1) {
                        paymentMethodConfigRepository.setDefaultPaymentMethodConfig(configId)
                    }
                    
                    _paymentMethodUpdateState.value = PaymentMethodUpdateState.Success("Payment method added successfully")
                    _needsAuthentication.value = false
                } else {
                    _paymentMethodUpdateState.value = PaymentMethodUpdateState.Error("User not authenticated")
                    _needsAuthentication.value = true
                }
            } catch (e: SecurityException) {
                _paymentMethodUpdateState.value = PaymentMethodUpdateState.Error("Authentication required")
                _needsAuthentication.value = true
            } catch (e: Exception) {
                _paymentMethodUpdateState.value = PaymentMethodUpdateState.Error("Failed to add payment method: ${e.message}")
                _needsAuthentication.value = false
            }
        }
    }
    
    fun updatePaymentMethodConfig(config: PaymentMethodConfig) {
        viewModelScope.launch {
            try {
                _paymentMethodUpdateState.value = PaymentMethodUpdateState.Loading
                
                val updatedConfig = config.copy(updatedAt = System.currentTimeMillis())
                paymentMethodConfigRepository.updatePaymentMethodConfig(updatedConfig)
                
                _paymentMethodUpdateState.value = PaymentMethodUpdateState.Success("Payment method updated successfully")
                _needsAuthentication.value = false
            } catch (e: SecurityException) {
                _paymentMethodUpdateState.value = PaymentMethodUpdateState.Error("Authentication required")
                _needsAuthentication.value = true
            } catch (e: Exception) {
                _paymentMethodUpdateState.value = PaymentMethodUpdateState.Error("Failed to update payment method: ${e.message}")
                _needsAuthentication.value = false
            }
        }
    }
    
    fun deletePaymentMethodConfig(configId: Long) {
        viewModelScope.launch {
            try {
                _paymentMethodUpdateState.value = PaymentMethodUpdateState.Loading
                
                val currentUser = authManager.currentUser.value
                if (currentUser != null) {
                    paymentMethodConfigRepository.deletePaymentMethodConfig(configId)
                    
                    // If we deleted the default payment method, set a new default
                    @Suppress("UNUSED_VARIABLE")
                    val remainingConfigs = paymentMethodConfigRepository.getPaymentMethodConfigs()
                    // Note: This would need to be handled in the UI layer by calling setDefaultPaymentMethodConfig
                    
                    _paymentMethodUpdateState.value = PaymentMethodUpdateState.Success("Payment method deleted successfully")
                    _needsAuthentication.value = false
                } else {
                    _paymentMethodUpdateState.value = PaymentMethodUpdateState.Error("User not authenticated")
                    _needsAuthentication.value = true
                }
            } catch (e: SecurityException) {
                _paymentMethodUpdateState.value = PaymentMethodUpdateState.Error("Authentication required")
                _needsAuthentication.value = true
            } catch (e: Exception) {
                _paymentMethodUpdateState.value = PaymentMethodUpdateState.Error("Failed to delete payment method: ${e.message}")
                _needsAuthentication.value = false
            }
        }
    }
    
    fun setDefaultPaymentMethodConfig(configId: Long) {
        viewModelScope.launch {
            try {
                _paymentMethodUpdateState.value = PaymentMethodUpdateState.Loading
                
                val currentUser = authManager.currentUser.value
                if (currentUser != null) {
                    paymentMethodConfigRepository.setDefaultPaymentMethodConfig(configId)
                    
                    _paymentMethodUpdateState.value = PaymentMethodUpdateState.Success("Default payment method updated successfully")
                    _needsAuthentication.value = false
                } else {
                    _paymentMethodUpdateState.value = PaymentMethodUpdateState.Error("User not authenticated")
                    _needsAuthentication.value = true
                }
            } catch (e: SecurityException) {
                _paymentMethodUpdateState.value = PaymentMethodUpdateState.Error("Authentication required")
                _needsAuthentication.value = true
            } catch (e: Exception) {
                _paymentMethodUpdateState.value = PaymentMethodUpdateState.Error("Failed to set default payment method: ${e.message}")
                _needsAuthentication.value = false
            }
        }
    }
    
    fun resetPaymentMethodUpdateState() {
        _paymentMethodUpdateState.value = PaymentMethodUpdateState.Idle
        _needsAuthentication.value = false
    }
    
    // Developer Options Management
    
    fun toggleDeveloperOptions() {
        _developerOptionsEnabled.value = !_developerOptionsEnabled.value
    }
    
    fun setDeveloperOptionsEnabled(enabled: Boolean) {
        _developerOptionsEnabled.value = enabled
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
    
    sealed class DefaultPaymentMethodState {
        object Loading : DefaultPaymentMethodState()
        data class Success(val config: PaymentMethodConfig?) : DefaultPaymentMethodState()
        data class Error(val message: String) : DefaultPaymentMethodState()
    }
    
    sealed class PaymentMethodUpdateState {
        object Idle : PaymentMethodUpdateState()
        object Loading : PaymentMethodUpdateState()
        data class Success(val message: String) : PaymentMethodUpdateState()
        data class Error(val message: String) : PaymentMethodUpdateState()
    }
    
    enum class AuthMethod {
        BIOMETRIC,
        DEVICE_CREDENTIALS,
        NONE
    }
    
    sealed class AuthMethodUpdateState {
        object Idle : AuthMethodUpdateState()
        object Loading : AuthMethodUpdateState()
        object Success : AuthMethodUpdateState()
        data class Error(val message: String) : AuthMethodUpdateState()
    }
}
