package com.pennywise.app.presentation.viewmodel

import android.content.Context
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pennywise.app.presentation.auth.DeviceAuthService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    private val deviceAuthService: DeviceAuthService,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DeviceAuthPromptUiState())
    val uiState: StateFlow<DeviceAuthPromptUiState> = _uiState.asStateFlow()
    
    fun authenticate(activity: FragmentActivity? = null) {
        println("🔍 DeviceAuthPromptViewModel: authenticate() called with activity = ${activity != null}")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )
            
            // Show the actual device authentication prompt
            activity?.let { fragmentActivity ->
                println("🔍 DeviceAuthPromptViewModel: Showing device auth prompt")
                deviceAuthService.showDeviceAuthPrompt(
                    activity = fragmentActivity,
                    onSuccess = {
                        println("🔍 DeviceAuthPromptViewModel: Authentication success")
                        onAuthenticationSuccess()
                    },
                    onError = { error ->
                        println("🔍 DeviceAuthPromptViewModel: Authentication error: $error")
                        onAuthenticationError(error)
                    },
                    onCancel = {
                        println("🔍 DeviceAuthPromptViewModel: Authentication cancelled")
                        onAuthenticationCancel()
                    }
                )
            } ?: run {
                // If no activity provided, show an error
                println("❌ DeviceAuthPromptViewModel: No activity provided, cannot perform device authentication")
                onAuthenticationError("Device authentication is not available. Please try again.")
            }
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
