package com.pennywise.app.domain.usecase

import com.pennywise.app.domain.model.Transaction
import com.pennywise.app.domain.repository.TransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Use case for adding a new transaction
 */
class AddTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val currencyUsageTracker: CurrencyUsageTracker
) {
    suspend operator fun invoke(transaction: Transaction): Long {
        // Insert the transaction
        val transactionId = transactionRepository.insertTransaction(transaction)
        
        // Track currency usage asynchronously (don't block the main flow)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                currencyUsageTracker.trackCurrencyUsage(transaction.userId, transaction.currency)
            } catch (e: Exception) {
                // Log error but don't fail the transaction creation
                println("Error tracking currency usage for transaction $transactionId: ${e.message}")
            }
        }
        
        return transactionId
    }
}

