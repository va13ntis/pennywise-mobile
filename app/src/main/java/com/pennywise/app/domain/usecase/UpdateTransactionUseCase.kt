package com.pennywise.app.domain.usecase

import com.pennywise.app.domain.model.Transaction
import com.pennywise.app.domain.repository.TransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Use case for updating an existing transaction
 */
class UpdateTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val currencyUsageTracker: CurrencyUsageTracker
) {
    suspend operator fun invoke(updatedTransaction: Transaction) {
        // Get the original transaction to compare currency
        val originalTransaction = transactionRepository.getTransactionById(updatedTransaction.id)
        
        // Update the transaction
        transactionRepository.updateTransaction(updatedTransaction)
        
        // Check if currency has changed and track usage if needed
        originalTransaction?.let { original ->
            if (original.currency != updatedTransaction.currency) {
                // Currency has changed, track the new currency usage
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        currencyUsageTracker.trackCurrencyUsage(updatedTransaction.userId, updatedTransaction.currency)
                    } catch (e: Exception) {
                        // Log error but don't fail the transaction update
                        println("Error tracking currency usage for updated transaction ${updatedTransaction.id}: ${e.message}")
                    }
                }
            }
        }
    }
}
