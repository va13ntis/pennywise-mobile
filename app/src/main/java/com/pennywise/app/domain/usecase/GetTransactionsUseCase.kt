package com.pennywise.app.domain.usecase

import com.pennywise.app.domain.model.Transaction
import com.pennywise.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for retrieving transactions
 */
class GetTransactionsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(): Flow<List<Transaction>> {
        return transactionRepository.getAllTransactions()
    }
}

