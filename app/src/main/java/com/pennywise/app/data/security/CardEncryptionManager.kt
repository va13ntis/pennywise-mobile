package com.pennywise.app.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.util.Base64
import java.security.SecureRandom

/**
 * Manages encryption and decryption of sensitive bank card information using Android KeyStore
 */
class CardEncryptionManager(private val context: Context) {
    
    private val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore")
    private val keyAlias = "PennyWiseBankCardKey"
    private val transformation = "AES/GCM/NoPadding"
    private val gcmIvLength = 12
    
    init {
        keyStore.load(null)
        generateKeyIfNeeded()
    }
    
    /**
     * Generate a new encryption key if it doesn't exist
     */
    private fun generateKeyIfNeeded() {
        if (!keyStore.containsAlias(keyAlias)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(false) // Set to true if you want biometric authentication
                .setRandomizedEncryptionRequired(true)
                .build()
            
            keyGenerator.init(keyGenParameterSpec)
            keyGenerator.generateKey()
        }
    }
    
    /**
     * Encrypt sensitive card information
     */
    fun encryptCardData(cardData: String): String {
        try {
            val secretKey = keyStore.getKey(keyAlias, null) as SecretKey
            val cipher = Cipher.getInstance(transformation)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            
            val iv = cipher.iv
            val encryptedData = cipher.doFinal(cardData.toByteArray())
            
            // Combine IV and encrypted data
            val combined = iv + encryptedData
            return Base64.encodeToString(combined, Base64.DEFAULT)
        } catch (e: Exception) {
            throw CardEncryptionException("Failed to encrypt card data", e)
        }
    }
    
    /**
     * Decrypt sensitive card information
     */
    fun decryptCardData(encryptedData: String): String {
        try {
            val secretKey = keyStore.getKey(keyAlias, null) as SecretKey
            val cipher = Cipher.getInstance(transformation)
            
            val combined = Base64.decode(encryptedData, Base64.DEFAULT)
            val iv = combined.sliceArray(0 until gcmIvLength)
            val encrypted = combined.sliceArray(gcmIvLength until combined.size)
            
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            
            val decryptedData = cipher.doFinal(encrypted)
            return String(decryptedData)
        } catch (e: Exception) {
            throw CardEncryptionException("Failed to decrypt card data", e)
        }
    }
    
    /**
     * Encrypt last four digits (for additional security)
     */
    fun encryptLastFourDigits(lastFourDigits: String): String {
        return encryptCardData(lastFourDigits)
    }
    
    /**
     * Decrypt last four digits
     */
    fun decryptLastFourDigits(encryptedLastFourDigits: String): String {
        return decryptCardData(encryptedLastFourDigits)
    }
    
    /**
     * Check if encryption is available
     */
    fun isEncryptionAvailable(): Boolean {
        return try {
            keyStore.containsAlias(keyAlias)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Clear the encryption key (for testing or security purposes)
     */
    fun clearEncryptionKey() {
        try {
            if (keyStore.containsAlias(keyAlias)) {
                keyStore.deleteEntry(keyAlias)
            }
        } catch (e: Exception) {
            // Handle error silently
        }
    }
}

/**
 * Exception thrown when card encryption/decryption fails
 */
class CardEncryptionException(message: String, cause: Throwable? = null) : Exception(message, cause)
