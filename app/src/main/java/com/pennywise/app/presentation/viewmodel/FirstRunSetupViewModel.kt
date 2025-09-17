package com.pennywise.app.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pennywise.app.domain.repository.UserRepository
import com.pennywise.app.presentation.auth.DeviceAuthService
import com.pennywise.app.presentation.util.LocaleManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
                
                // Create new user with device settings
                _uiState.value = _uiState.value.copy(
                    loadingMessage = "Detecting your preferences..."
                )
                
                val detectedLocale = localeManager.detectDeviceLocale(context)
                val detectedCurrency = getCurrencyForLocale(detectedLocale)
                
                val result = userRepository.createUser(
                    defaultCurrency = detectedCurrency,
                    locale = detectedLocale
                )
                
                result.fold(
                    onSuccess = { userId ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            canUseBiometric = deviceAuthService.canUseBiometric(),
                            canUseDeviceCredentials = deviceAuthService.canUseDeviceCredentials(),
                            isSetupComplete = false
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to create user account"
                        )
                    }
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
                
                // Enable device authentication if not NONE
                val enableAuth = selectedMethod != AuthMethod.NONE
                println("🔍 FirstRunSetupViewModel: Setting device auth enabled = $enableAuth for method = $selectedMethod")
                deviceAuthService.setDeviceAuthEnabled(enableAuth)
                
                // Get the user and update device auth setting
                val user = userRepository.getSingleUser()
                if (user != null) {
                    userRepository.updateDeviceAuthEnabled(user.id, enableAuth)
                }
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSetupComplete = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to setup authentication"
                )
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
