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
                            canUseDeviceAuth = deviceAuthService.canUseDeviceAuth(),
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
    
    fun setupDeviceAuth() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    loadingMessage = "Setting up device authentication..."
                )
                
                // Enable device authentication
                deviceAuthService.setDeviceAuthEnabled(true)
                
                // Get the user and update device auth setting
                val user = userRepository.getSingleUser()
                if (user != null) {
                    userRepository.updateDeviceAuthEnabled(user.id, true)
                }
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSetupComplete = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to setup device authentication"
                )
            }
        }
    }
    
    fun skipDeviceAuth() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    loadingMessage = "Completing setup..."
                )
                
                // Keep device auth disabled (default)
                deviceAuthService.setDeviceAuthEnabled(false)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSetupComplete = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to complete setup"
                )
            }
        }
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
    val canUseDeviceAuth: Boolean = false,
    val errorMessage: String? = null
)
