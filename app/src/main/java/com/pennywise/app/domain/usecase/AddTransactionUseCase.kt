package com.pennywise.app.domain.usecase

import com.pennywise.app.domain.model.Transaction
import com.pennywise.app.domain.repository.TransactionRepository
import javax.inject.Inject

/**
 * Use case for adding a new transaction
 */
class AddTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(transaction: Transaction): Long {
        return transactionRepository.insertTransaction(transaction)
    }
}

