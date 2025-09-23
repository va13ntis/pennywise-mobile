package com.pennywise.app.presentation.auth

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.fragment.app.FragmentActivity
import com.pennywise.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for handling device authentication including biometric and device credentials
 */
@Singleton
class DeviceAuthService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val deviceAuthEnabledKey = booleanPreferencesKey("device_auth_enabled")
    
    val isDeviceAuthEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        val enabled = preferences[deviceAuthEnabledKey] ?: false
        println("🔍 DeviceAuthService: isDeviceAuthEnabled = $enabled")
        enabled
    }
    
    /**
     * Set whether device authentication is enabled
     */
    suspend fun setDeviceAuthEnabled(enabled: Boolean) {
        println("🔍 DeviceAuthService: Setting device auth enabled = $enabled")
        context.dataStore.edit { preferences ->
            preferences[deviceAuthEnabledKey] = enabled
        }
        println("🔍 DeviceAuthService: Device auth enabled set to $enabled")
    }
    
    /**
     * Check if biometric authentication is available on this device
     */
    fun canUseBiometric(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == 
            BiometricManager.BIOMETRIC_SUCCESS
    }
    
    /**
     * Check if device credentials (PIN/pattern/password) are available
     */
    fun canUseDeviceCredentials(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.DEVICE_CREDENTIAL) == 
            BiometricManager.BIOMETRIC_SUCCESS
    }
    
    /**
     * Check if any form of device authentication is available
     */
    fun canUseDeviceAuth(): Boolean {
        return canUseBiometric() || canUseDeviceCredentials()
    }
    
    /**
     * Show device authentication prompt with biometric and device credentials
     */
    fun showDeviceAuthPrompt(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onCancel: () -> Unit = {}
    ) {
        val executor = ContextCompat.getMainExecutor(context)
        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }
                
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    when (errorCode) {
                        BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON -> onCancel()
                        else -> onError(errString.toString())
                    }
                }
                
                override fun onAuthenticationFailed() {
                    onError("Authentication failed. Please try again.")
                }
            })
            
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(context.getString(R.string.device_auth_title))
            .setSubtitle(context.getString(R.string.device_auth_subtitle))
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or 
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()
            
        biometricPrompt.authenticate(promptInfo)
    }
    
    /**
     * Show biometric-only authentication prompt
     */
    fun showBiometricPrompt(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onCancel: () -> Unit = {}
    ) {
        val executor = ContextCompat.getMainExecutor(context)
        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }
                
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    when (errorCode) {
                        BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON -> onCancel()
                        else -> onError(errString.toString())
                    }
                }
                
                override fun onAuthenticationFailed() {
                    onError("Biometric authentication failed. Please try again.")
                }
            })
            
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(context.getString(R.string.biometric_auth_title))
            .setSubtitle(context.getString(R.string.biometric_auth_subtitle))
            .setNegativeButtonText(context.getString(R.string.biometric_auth_cancel))
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()
            
        biometricPrompt.authenticate(promptInfo)
    }
    
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "device_auth_preferences")
    }
}
