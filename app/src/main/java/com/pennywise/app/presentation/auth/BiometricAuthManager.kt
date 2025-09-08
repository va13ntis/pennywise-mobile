package com.pennywise.app.presentation.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
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
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager class for handling biometric authentication with secure storage
 * Provides comprehensive biometric authentication functionality with proper security measures
 */
@Singleton
class BiometricAuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val biometricEnabledKey = booleanPreferencesKey("biometric_auth_enabled")
    private val biometricPreferenceKey = booleanPreferencesKey("biometric_preference_set")
    
    companion object {
        private const val KEY_NAME = "PennyWiseBiometricKey"
        private const val KEYSTORE_NAME = "AndroidKeyStore"
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "biometric_auth_preferences")
    }
    
    /**
     * Flow to observe biometric authentication preference
     */
    val isBiometricEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[biometricEnabledKey] ?: false
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
     * Set whether biometric authentication is enabled
     */
    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[biometricEnabledKey] = enabled
            preferences[biometricPreferenceKey] = true
        }
    }
    
    /**
     * Check if user has set biometric preference
     */
    suspend fun hasBiometricPreference(): Boolean {
        return context.dataStore.data.map { preferences ->
            preferences[biometricPreferenceKey] ?: false
        }.let { flow ->
            var result = false
            flow.collect { result = it; return@collect }
            result
        }
    }
    
    /**
     * Generate or retrieve the biometric authentication key
     */
    private fun generateSecretKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_NAME)
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEY_NAME,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)
            .build()
        
        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }
    
    /**
     * Get the cipher for encryption/decryption
     */
    private fun getCipher(): Cipher {
        return Cipher.getInstance(
            KeyProperties.KEY_ALGORITHM_AES + "/" +
            KeyProperties.BLOCK_MODE_CBC + "/" +
            KeyProperties.ENCRYPTION_PADDING_PKCS7
        )
    }
    
    /**
     * Initialize the cipher for encryption
     */
    private fun initEncryptCipher(): Cipher {
        val cipher = getCipher()
        val secretKey = generateSecretKey()
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        return cipher
    }
    
    /**
     * Initialize the cipher for decryption
     */
    private fun initDecryptCipher(): Cipher {
        val cipher = getCipher()
        val keyStore = KeyStore.getInstance(KEYSTORE_NAME)
        keyStore.load(null)
        val secretKey = keyStore.getKey(KEY_NAME, null) as SecretKey
        cipher.init(Cipher.DECRYPT_MODE, secretKey)
        return cipher
    }
    
    /**
     * Show biometric authentication prompt with secure storage
     */
    fun authenticateWithBiometrics(
        activity: FragmentActivity,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
        onFallback: () -> Unit
    ) {
        if (!canAuthenticate()) {
            onError("Biometric authentication is not available on this device")
            return
        }
        
        val executor = ContextCompat.getMainExecutor(context)
        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    try {
                        // Generate a secure token using the cipher
                        val cipher = initEncryptCipher()
                        val token = "biometric_auth_token_${System.currentTimeMillis()}"
                        val encryptedToken = cipher.doFinal(token.toByteArray())
                        val tokenString = android.util.Base64.encodeToString(encryptedToken, android.util.Base64.DEFAULT)
                        onSuccess(tokenString)
                    } catch (e: Exception) {
                        onError("Failed to generate authentication token: ${e.message}")
                    }
                }
                
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    when (errorCode) {
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON -> {
                            // User tapped "Use password instead"
                            onFallback()
                        }
                        BiometricPrompt.ERROR_USER_CANCELED -> {
                            // User canceled authentication
                            onError("Authentication canceled")
                        }
                        BiometricPrompt.ERROR_LOCKOUT -> {
                            onError("Too many failed attempts. Please try again later.")
                        }
                        BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> {
                            onError("Biometric authentication is permanently locked. Please use password.")
                        }
                        else -> {
                            onError("Authentication error: $errString")
                        }
                    }
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
    
    /**
     * Validate a biometric authentication token
     */
    fun validateBiometricToken(token: String): Boolean {
        return try {
            val encryptedToken = android.util.Base64.decode(token, android.util.Base64.DEFAULT)
            val cipher = initDecryptCipher()
            val decryptedToken = String(cipher.doFinal(encryptedToken))
            decryptedToken.startsWith("biometric_auth_token_")
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Clear biometric authentication data
     */
    suspend fun clearBiometricData() {
        context.dataStore.edit { preferences ->
            preferences.remove(biometricEnabledKey)
            preferences.remove(biometricPreferenceKey)
        }
        
        // Clear the biometric key from keystore
        try {
            val keyStore = KeyStore.getInstance(KEYSTORE_NAME)
            keyStore.load(null)
            keyStore.deleteEntry(KEY_NAME)
        } catch (e: Exception) {
            // Key might not exist, which is fine
        }
    }
}
