package com.pennywise.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pennywise.app.domain.model.RecurringPeriod
import com.pennywise.app.domain.model.Transaction
import com.pennywise.app.domain.model.TransactionType
import com.pennywise.app.domain.repository.TransactionRepository
import com.pennywise.app.presentation.screens.ExpenseFormData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

/**
 * ViewModel for the Add Expense screen
 */
@HiltViewModel
class AddExpenseViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<AddExpenseUiState>(AddExpenseUiState.Idle)
    val uiState: StateFlow<AddExpenseUiState> = _uiState
    
    /**
     * Save expense to the repository
     */
    fun saveExpense(expenseData: ExpenseFormData, userId: Long) {
        viewModelScope.launch {
            _uiState.value = AddExpenseUiState.Loading
            
            try {
                val transaction = Transaction(
                    userId = userId,
                    amount = expenseData.amount,
                    description = expenseData.merchant,
                    category = expenseData.category,
                    type = TransactionType.EXPENSE,
                    date = expenseData.date,
                    isRecurring = expenseData.isRecurring,
                    recurringPeriod = if (expenseData.isRecurring) RecurringPeriod.MONTHLY else null,
                    createdAt = Date(),
                    updatedAt = Date()
                )
                
                val transactionId = transactionRepository.insertTransaction(transaction)
                _uiState.value = AddExpenseUiState.Success(transactionId)
            } catch (e: Exception) {
                _uiState.value = AddExpenseUiState.Error(e.message ?: "Failed to save expense")
            }
        }
    }
    
    /**
     * Reset UI state to idle
     */
    fun resetState() {
        _uiState.value = AddExpenseUiState.Idle
    }
}

/**
 * UI state for the Add Expense screen
 */
sealed class AddExpenseUiState {
    object Idle : AddExpenseUiState()
    object Loading : AddExpenseUiState()
    data class Success(val transactionId: Long) : AddExpenseUiState()
    data class Error(val message: String) : AddExpenseUiState()
}
