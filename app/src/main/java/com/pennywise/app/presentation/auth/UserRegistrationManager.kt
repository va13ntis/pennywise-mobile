package com.pennywise.app.presentation.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager class to handle user registration status tracking
 * Tracks whether a user has been registered on this device
 */
@Singleton
class UserRegistrationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val isUserRegisteredKey = booleanPreferencesKey("is_user_registered")
    
    /**
     * Flow to observe registration status
     */
    val isUserRegistered: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[isUserRegisteredKey] ?: false
    }
    
    /**
     * Check if user is registered on this device
     */
    suspend fun isUserRegistered(): Boolean {
        return context.dataStore.data.map { preferences ->
            preferences[isUserRegisteredKey] ?: false
        }.map { it }.let { flow ->
            // Get the first value from the flow
            var result = false
            flow.collect { result = it; return@collect }
            result
        }
    }
    
    /**
     * Mark user as registered on this device
     */
    suspend fun setUserRegistered(registered: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[isUserRegisteredKey] = registered
        }
    }
    
    /**
     * Clear registration status (used when user removes account)
     */
    suspend fun clearRegistrationStatus() {
        setUserRegistered(false)
    }
    
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_registration_preferences")
    }
}
