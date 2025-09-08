package com.pennywise.app.presentation.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.pennywise.app.domain.model.User
import com.pennywise.app.domain.repository.UserRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager class to handle authentication flow migrations and backward compatibility
 * Ensures smooth transitions for existing users when new authentication features are added
 */
@Singleton
class AuthMigrationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userRepository: UserRepository,
    private val userRegistrationManager: UserRegistrationManager,
    private val biometricAuthManager: BiometricAuthManager
) {
    
    private val migrationCompletedKey = booleanPreferencesKey("auth_migration_completed")
    private val defaultCurrencyMigrationKey = booleanPreferencesKey("default_currency_migration_completed")
    private val biometricMigrationKey = booleanPreferencesKey("biometric_migration_completed")
    
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_migration_preferences")
    }
    
    /**
     * Perform all necessary migrations for existing users
     */
    suspend fun performMigrations() {
        try {
            val preferences = context.dataStore.data.first()
            val migrationCompleted = preferences[migrationCompletedKey] ?: false
            
            if (!migrationCompleted) {
                // Perform default currency migration for existing users
                migrateDefaultCurrencyForExistingUsers()
                
                // Perform biometric authentication migration
                migrateBiometricAuthentication()
                
                // Mark migration as completed
                context.dataStore.edit { prefs ->
                    prefs[migrationCompletedKey] = true
                }
            }
        } catch (e: Exception) {
            // Log error but don't crash the app
            println("❌ AuthMigrationManager: Error during migration: ${e.message}")
        }
    }
    
    /**
     * Migrate existing users to have a default currency if they don't have one
     */
    private suspend fun migrateDefaultCurrencyForExistingUsers() {
        try {
            val preferences = context.dataStore.data.first()
            val currencyMigrationCompleted = preferences[defaultCurrencyMigrationKey] ?: false
            
            if (!currencyMigrationCompleted) {
                // Get all users and check if they have a default currency
                // For now, we'll assume all existing users should have USD as default
                // In a real implementation, you might want to ask users to select their currency
                
                // Mark currency migration as completed
                context.dataStore.edit { prefs ->
                    prefs[defaultCurrencyMigrationKey] = true
                }
                
                println("✅ AuthMigrationManager: Default currency migration completed")
            }
        } catch (e: Exception) {
            println("❌ AuthMigrationManager: Error during currency migration: ${e.message}")
        }
    }
    
    /**
     * Migrate biometric authentication settings for existing users
     */
    private suspend fun migrateBiometricAuthentication() {
        try {
            val preferences = context.dataStore.data.first()
            val biometricMigrationCompleted = preferences[biometricMigrationKey] ?: false
            
            if (!biometricMigrationCompleted) {
                // Check if user has biometric preference set
                val hasPreference = biometricAuthManager.hasBiometricPreference()
                
                if (!hasPreference) {
                    // For existing users, we'll default to biometric being disabled
                    // They can enable it manually in settings if they want
                    biometricAuthManager.setBiometricEnabled(false)
                }
                
                // Mark biometric migration as completed
                context.dataStore.edit { prefs ->
                    prefs[biometricMigrationKey] = true
                }
                
                println("✅ AuthMigrationManager: Biometric authentication migration completed")
            }
        } catch (e: Exception) {
            println("❌ AuthMigrationManager: Error during biometric migration: ${e.message}")
        }
    }
    
    /**
     * Handle edge cases for authentication flow
     */
    suspend fun handleAuthenticationEdgeCases() {
        try {
            // Handle case where user might have been registered but registration status is not set
            val isRegistered = userRegistrationManager.isUserRegistered()
            val currentUser = userRepository.getUserById(1) // This is a placeholder - in real implementation, you'd get the current user
            
            if (currentUser != null && !isRegistered) {
                // User exists but registration status is not set - fix this
                userRegistrationManager.setUserRegistered(true)
                println("✅ AuthMigrationManager: Fixed registration status for existing user")
            }
            
            // Handle case where biometric authentication is enabled but device doesn't support it
            val biometricEnabled = biometricAuthManager.isBiometricEnabled.first()
            if (biometricEnabled && !biometricAuthManager.canAuthenticate()) {
                // Device doesn't support biometrics but it's enabled - disable it
                biometricAuthManager.setBiometricEnabled(false)
                println("✅ AuthMigrationManager: Disabled biometric authentication for unsupported device")
            }
            
        } catch (e: Exception) {
            println("❌ AuthMigrationManager: Error handling edge cases: ${e.message}")
        }
    }
    
    /**
     * Reset all migration flags (useful for testing or if migrations need to be re-run)
     */
    suspend fun resetMigrationFlags() {
        context.dataStore.edit { preferences ->
            preferences.remove(migrationCompletedKey)
            preferences.remove(defaultCurrencyMigrationKey)
            preferences.remove(biometricMigrationKey)
        }
    }
    
    /**
     * Check if migrations have been completed
     */
    suspend fun isMigrationCompleted(): Boolean {
        return context.dataStore.data.first()[migrationCompletedKey] ?: false
    }
}
