package com.pennywise.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pennywise.app.presentation.auth.DeviceAuthService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for device authentication prompt screen
 */
@HiltViewModel
class DeviceAuthPromptViewModel @Inject constructor(
    private val deviceAuthService: DeviceAuthService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DeviceAuthPromptUiState())
    val uiState: StateFlow<DeviceAuthPromptUiState> = _uiState.asStateFlow()
    
    fun authenticate() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )
            
            // Note: The actual authentication prompt will be shown by the screen
            // This ViewModel just manages the state
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isAuthenticated = true
            )
        }
    }
    
    fun onAuthenticationSuccess() {
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            isAuthenticated = true,
            errorMessage = null
        )
    }
    
    fun onAuthenticationError(error: String) {
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            isAuthenticated = false,
            errorMessage = error
        )
    }
    
    fun onAuthenticationCancel() {
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            isAuthenticated = false,
            errorMessage = null
        )
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

/**
 * UI state for device authentication prompt screen
 */
data class DeviceAuthPromptUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val errorMessage: String? = null
)
