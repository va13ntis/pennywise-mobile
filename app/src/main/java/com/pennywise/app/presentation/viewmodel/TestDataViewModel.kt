package com.pennywise.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pennywise.app.data.util.DataSeeder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing test data operations
 */
@HiltViewModel
class TestDataViewModel @Inject constructor(
    private val dataSeeder: DataSeeder
) : ViewModel() {
    
    private val _isSeeding = MutableStateFlow(false)
    val isSeeding: StateFlow<Boolean> = _isSeeding.asStateFlow()
    
    private val _isClearing = MutableStateFlow(false)
    val isClearing: StateFlow<Boolean> = _isClearing.asStateFlow()
    
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    
    /**
     * Seed the database with test data
     */
    fun seedTestData() {
        viewModelScope.launch {
            _isSeeding.value = true
            _message.value = null
            
            try {
                dataSeeder.seedTestData()
                _message.value = "✅ Test data seeded successfully! You can now log in with:\nUsername: testuser\nPassword: test123"
            } catch (e: Exception) {
                _message.value = "❌ Failed to seed test data: ${e.message}"
            } finally {
                _isSeeding.value = false
            }
        }
    }
    
    /**
     * Clear all test data
     */
    fun clearTestData() {
        viewModelScope.launch {
            _isClearing.value = true
            _message.value = null
            
            try {
                dataSeeder.clearTestData()
                _message.value = "✅ All test data cleared successfully!"
            } catch (e: Exception) {
                _message.value = "❌ Failed to clear test data: ${e.message}"
            } finally {
                _isClearing.value = false
            }
        }
    }
    
    /**
     * Clear the message
     */
    fun clearMessage() {
        _message.value = null
    }
}
