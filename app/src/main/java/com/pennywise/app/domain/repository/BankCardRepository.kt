package com.pennywise.app.domain.repository

import com.pennywise.app.domain.model.BankCard
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for bank card data operations.
 * All operations implicitly use the currently authenticated user.
 */
interface BankCardRepository {
    suspend fun insertBankCard(bankCard: BankCard): Result<Long>
    suspend fun updateBankCard(bankCard: BankCard): Result<Unit>
    suspend fun deleteBankCard(bankCard: BankCard): Result<Unit>
    suspend fun getBankCardById(cardId: Long): BankCard?
    fun getBankCards(): Flow<List<BankCard>>
    fun getActiveBankCards(): Flow<List<BankCard>>
    suspend fun getBankCardByAlias(alias: String): BankCard?
    suspend fun updateBankCardStatus(cardId: Long, isActive: Boolean): Result<Unit>
    suspend fun updateBankCardAlias(cardId: Long, alias: String): Result<Unit>
    suspend fun updateBankCardPaymentDay(cardId: Long, paymentDay: Int): Result<Unit>
    suspend fun isAliasTaken(alias: String, excludeId: Long = 0): Boolean
    suspend fun deleteAllBankCards(): Result<Unit>
}
