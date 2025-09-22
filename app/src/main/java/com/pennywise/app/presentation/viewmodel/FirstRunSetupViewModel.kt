package com.pennywise.app.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pennywise.app.R
import com.pennywise.app.domain.repository.UserRepository
import com.pennywise.app.domain.model.PaymentMethod
import com.pennywise.app.domain.repository.PaymentMethodConfigRepository
import com.pennywise.app.presentation.auth.AuthManager
import com.pennywise.app.presentation.auth.DeviceAuthService
import com.pennywise.app.presentation.util.LocaleManager
import com.pennywise.app.data.util.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for first run setup screen
 */
@HiltViewModel
class FirstRunSetupViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val deviceAuthService: DeviceAuthService,
    private val localeManager: LocaleManager,
    private val authManager: AuthManager,
    private val paymentMethodConfigRepository: PaymentMethodConfigRepository,
    private val settingsManager: com.pennywise.app.presentation.util.SettingsManager,
    private val settingsDataStore: SettingsDataStore,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(FirstRunSetupUiState())
    val uiState: StateFlow<FirstRunSetupUiState> = _uiState.asStateFlow()
    
    init {
        initializeSetup()
    }
    
    private fun initializeSetup() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    loadingMessage = "Setting up your account..."
                )
                
                // Check if user already exists
                val existingUser = userRepository.getSingleUser()
                if (existingUser != null) {
                    // User already exists, complete setup
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSetupComplete = true
                    )
                    return@launch
                }
                
                // Don't create user yet - just detect preferences and start setup
                _uiState.value = _uiState.value.copy(
                    loadingMessage = "Detecting your preferences..."
                )
                
                val detectedLocale = localeManager.detectDeviceLocale(context)
                val detectedCurrency = getCurrencyForLocale(detectedLocale)
                
                // Check if we're returning from a language change during setup
                val savedLanguage = settingsDataStore.getLanguage()
                val isReturningFromLanguageChange = savedLanguage.isNotEmpty() && savedLanguage != detectedLocale
                
                // Initialize setup state without creating user
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    canUseBiometric = deviceAuthService.canUseBiometric(),
                    canUseDeviceCredentials = deviceAuthService.canUseDeviceCredentials(),
                    isSetupComplete = false,
                    step = if (isReturningFromLanguageChange) FirstRunStep.AUTH else FirstRunStep.LANGUAGE,
                    selectedLanguage = savedLanguage.ifEmpty { detectedLocale },
                    selectedCurrency = detectedCurrency
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Setup failed"
                )
            }
        }
    }
    
    fun selectAuthMethod(authMethod: AuthMethod) {
        _uiState.value = _uiState.value.copy(selectedAuthMethod = authMethod)
    }
    
    fun setupSelectedAuth() {
        viewModelScope.launch {
            try {
                val selectedMethod = _uiState.value.selectedAuthMethod
                if (selectedMethod == null) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Please select an authentication method"
                    )
                    return@launch
                }
                
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    loadingMessage = when (selectedMethod) {
                        AuthMethod.BIOMETRIC -> "Setting up biometric authentication..."
                        AuthMethod.DEVICE_CREDENTIALS -> "Setting up device credentials..."
                        AuthMethod.NONE -> "Completing setup..."
                    }
                )
                
                // Store the selected auth method for later use in finishSetup()
                // We'll apply device auth settings when the user is actually created
                println("🔍 FirstRunSetupViewModel: Selected auth method = $selectedMethod (will be applied when user is created)")
                
                // Move to next step (currency)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    step = FirstRunStep.CURRENCY
                )
                // Don't initialize auth state yet - we're still in the setup process
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to setup authentication"
                )
            }
        }
    }
    
    fun setLanguage(languageCode: String) {
        _uiState.value = _uiState.value.copy(selectedLanguage = languageCode)
    }
    
    fun continueFromLanguage() {
        if (_uiState.value.selectedLanguage == null) return
        // Save language to DataStore - this will trigger activity restart
        viewModelScope.launch {
            settingsDataStore.setLanguage(_uiState.value.selectedLanguage!!)
        }
        // Move to auth step
        _uiState.value = _uiState.value.copy(step = FirstRunStep.AUTH)
    }
    
    fun setCurrency(currencyCode: String) {
        _uiState.value = _uiState.value.copy(selectedCurrency = currencyCode)
    }
    
    fun continueFromCurrency() {
        if (_uiState.value.selectedCurrency == null) return
        _uiState.value = _uiState.value.copy(step = FirstRunStep.PAYMENT_METHOD)
    }
    
    fun setPaymentMethod(method: PaymentMethod) {
        _uiState.value = _uiState.value.copy(selectedPaymentMethod = method)
    }
    
    fun continueFromPaymentMethod() {
        if (_uiState.value.selectedPaymentMethod == null) return
        _uiState.value = _uiState.value.copy(step = FirstRunStep.SUMMARY)
    }
    
    fun finishSetup() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, loadingMessage = context.getString(
                    R.string.finalizing_setup
                ))
                
                // Get current selections from UI state
                val selectedCurrency = _uiState.value.selectedCurrency ?: "USD"
                val selectedLanguage = _uiState.value.selectedLanguage ?: "en"
                val selectedPaymentMethod = _uiState.value.selectedPaymentMethod
                val selectedAuthMethod = _uiState.value.selectedAuthMethod
                
                // Create user with final settings
                val result = userRepository.createUser(
                    defaultCurrency = selectedCurrency,
                    locale = selectedLanguage
                )
                
                result.fold(
                    onSuccess = { userId ->
                        println("✅ FirstRunSetupViewModel: User created with ID: $userId")
                        
                        // Apply device authentication settings
                        selectedAuthMethod?.let { authMethod ->
                            val enableAuth = authMethod != AuthMethod.NONE
                            println("🔍 FirstRunSetupViewModel: Setting device auth enabled = $enableAuth for method = $authMethod")
                            deviceAuthService.setDeviceAuthEnabled(enableAuth)
                            userRepository.updateDeviceAuthEnabled(userId, enableAuth)
                        }
                        
                        // Save preferences to settings manager
                        println("🔧 FirstRunSetupViewModel: Saving currency to SettingsManager: $selectedCurrency")
                        settingsManager.saveCurrencyPreference(selectedCurrency)
                        
                        println("🔧 FirstRunSetupViewModel: Language already saved to DataStore: $selectedLanguage")

                        // Apply payment method if selected
                        selectedPaymentMethod?.let { method ->
                            println("🔧 FirstRunSetupViewModel: Applying payment method ${method.displayName}")
                            val configId = paymentMethodConfigRepository.insertPaymentMethodConfig(
                                com.pennywise.app.domain.model.PaymentMethodConfig.createDefault(userId, method)
                            )
                            val paymentMethodResult = paymentMethodConfigRepository.setDefaultPaymentMethodConfig(userId, configId)
                            println("🔧 FirstRunSetupViewModel: Payment method set result: $paymentMethodResult")
                        }
                        
                        // Verify user was created correctly
                        val createdUser = userRepository.getSingleUser()
                        println("✅ FirstRunSetupViewModel: VERIFIED user settings: currency=${createdUser?.defaultCurrency}, locale=${createdUser?.locale}")
                        
                        // Add a delay to ensure database updates are complete
                        kotlinx.coroutines.delay(1000)
                        
                        // Now it's safe to initialize the auth state
                        authManager.initializeAuthState()
                        
                        _uiState.value = _uiState.value.copy(isLoading = false, isSetupComplete = true)
                    },
                    onFailure = { error ->
                        println("❌ FirstRunSetupViewModel: Failed to create user: ${error.message}")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false, 
                            errorMessage = error.message ?: "Failed to create user account"
                        )
                    }
                )
            } catch (e: Exception) {
                println("❌ FirstRunSetupViewModel: Error in finishSetup: ${e.message}")
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message ?: "Failed to finish setup")
            }
        }
    }
    
    fun skipDeviceAuth() {
        selectAuthMethod(AuthMethod.NONE)
        setupSelectedAuth()
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    fun goBack() {
        val currentStep = _uiState.value.step
        val previousStep = when (currentStep) {
            FirstRunStep.AUTH -> FirstRunStep.LANGUAGE
            FirstRunStep.CURRENCY -> FirstRunStep.AUTH
            FirstRunStep.PAYMENT_METHOD -> FirstRunStep.CURRENCY
            FirstRunStep.SUMMARY -> FirstRunStep.PAYMENT_METHOD
            FirstRunStep.LANGUAGE -> return // Can't go back from first step
        }
        _uiState.value = _uiState.value.copy(step = previousStep)
    }
    
    /**
     * Get the default currency for a given locale
     */
    private fun getCurrencyForLocale(locale: String): String {
        return when (locale) {
            "iw" -> "ILS" // Israeli Shekel for Hebrew
            "ru" -> "RUB" // Russian Ruble for Russian
            "en" -> "USD" // US Dollar for English
            else -> "USD" // Default to USD
        }
    }
}

/**
 * UI state for first run setup screen
 */
data class FirstRunSetupUiState(
    val isLoading: Boolean = false,
    val loadingMessage: String = "",
    val isSetupComplete: Boolean = false,
    val canUseBiometric: Boolean = false,
    val canUseDeviceCredentials: Boolean = false,
    val selectedAuthMethod: AuthMethod? = null,
    val selectedLanguage: String? = null,
    val selectedCurrency: String? = null,
    val selectedPaymentMethod: PaymentMethod? = null,
    val step: FirstRunStep = FirstRunStep.LANGUAGE,
    val errorMessage: String? = null
)

/**
 * Available authentication methods
 */
enum class AuthMethod {
    BIOMETRIC,
    DEVICE_CREDENTIALS,
    NONE
}

/** Steps for first run setup */
enum class FirstRunStep {
    LANGUAGE,
    AUTH,
    CURRENCY,
    PAYMENT_METHOD,
    SUMMARY
}
