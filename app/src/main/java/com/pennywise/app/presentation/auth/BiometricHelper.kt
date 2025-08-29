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
 * Helper class for managing biometric authentication functionality
 */
@Singleton
class BiometricHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val biometricEnabledKey = booleanPreferencesKey("biometric_enabled")
    
    val isBiometricEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[biometricEnabledKey] ?: false
    }
    
    /**
     * Set whether biometric authentication is enabled
     */
    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[biometricEnabledKey] = enabled
        }
    }
    
    /**
     * Check if biometric authentication is available on this device
     */
    fun canAuthenticate(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == 
            BiometricManager.BIOMETRIC_SUCCESS
    }
    
    /**
     * Show biometric authentication prompt
     */
    fun showBiometricPrompt(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(context)
        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }
                
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onError(errString.toString())
                }
                
                override fun onAuthenticationFailed() {
                    onError("Authentication failed. Please try again.")
                }
            })
            
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(context.getString(R.string.biometric_login_title))
            .setSubtitle(context.getString(R.string.biometric_login_subtitle))
            .setNegativeButtonText(context.getString(R.string.biometric_login_use_password))
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()
            
        biometricPrompt.authenticate(promptInfo)
    }
    
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "biometric_preferences")
    }
}
