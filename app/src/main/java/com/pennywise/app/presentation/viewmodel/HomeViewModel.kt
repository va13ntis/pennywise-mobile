package com.pennywise.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pennywise.app.domain.model.Transaction
import com.pennywise.app.domain.model.TransactionType
import com.pennywise.app.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.async
import java.time.YearMonth
import java.time.temporal.WeekFields
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

/**
 * ViewModel for the Home screen that manages monthly expense data
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {
    
    private val _userId = MutableStateFlow<Long?>(null)
    private val _currentMonth = MutableStateFlow(YearMonth.now())
    
    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()
    
    private val _recurringTransactions = MutableStateFlow<List<Transaction>>(emptyList())
    val recurringTransactions: StateFlow<List<Transaction>> = _recurringTransactions.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    val currentMonth: StateFlow<YearMonth> = _currentMonth.asStateFlow()
    
    /**
     * Set the current user ID and load their data
     */
    fun setUserId(userId: Long) {
        println("🔄 HomeViewModel: Setting user ID to $userId")
        _userId.value = userId
        loadTransactions()
    }
    
    /**
     * Navigate to a different month (positive for next, negative for previous)
     */
    fun changeMonth(offset: Int) {
        _currentMonth.value = _currentMonth.value.plusMonths(offset.toLong())
        loadTransactions()
    }
    
    /**
     * Load transactions for the current month and user
     */
    private fun loadTransactions() {
        val userId = _userId.value ?: return
        
        println("🔄 HomeViewModel: Loading transactions for user $userId")
        _isLoading.value = true
        _error.value = null
        
        viewModelScope.launch {
            try {
                // Use async to run both operations concurrently
                val monthlyDeferred = async {
                    println("🔄 HomeViewModel: Loading monthly transactions...")
                    val transactions = transactionRepository.getTransactionsByMonth(userId, _currentMonth.value).first()
                    println("✅ HomeViewModel: Loaded ${transactions.size} monthly transactions")
                    transactions
                }
                
                val recurringDeferred = async {
                    println("🔄 HomeViewModel: Loading recurring transactions...")
                    val transactions = transactionRepository.getRecurringTransactionsByUser(userId).first()
                    println("✅ HomeViewModel: Loaded ${transactions.size} recurring transactions")
                    transactions
                }
                
                // Wait for both operations to complete
                val monthlyTransactions = monthlyDeferred.await()
                val recurringTransactions = recurringDeferred.await()
                
                // Update the state
                _transactions.value = monthlyTransactions
                _recurringTransactions.value = recurringTransactions
                
                println("✅ HomeViewModel: All transactions loading completed")
            } catch (e: Exception) {
                println("❌ HomeViewModel: Failed to load transactions: ${e.message}")
                e.printStackTrace()
                _error.value = "Failed to load transactions: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Get transactions grouped by week number
     */
    fun getTransactionsGroupedByWeek(): Map<Int, List<Transaction>> {
        return _transactions.value
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { transaction ->
                val calendar = Calendar.getInstance().apply {
                    time = transaction.date
                }
                calendar.get(Calendar.WEEK_OF_MONTH)
            }
            .toSortedMap()
    }
    
    /**
     * Get total expenses for the current month
     */
    fun getTotalExpenses(): Double {
        return _transactions.value
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }
    }
    
    /**
     * Get total income for the current month
     */
    fun getTotalIncome(): Double {
        return _transactions.value
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amount }
    }
    
    /**
     * Get net balance (income - expenses) for the current month
     */
    fun getNetBalance(): Double {
        return getTotalIncome() - getTotalExpenses()
    }
    
    /**
     * Clear any error messages
     */
    fun clearError() {
        _error.value = null
    }
}
