package com.pennywise.app.domain.repository

import com.pennywise.app.domain.model.CurrencyUsage
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for currency usage operations
 */
interface CurrencyUsageRepository {
    suspend fun insertCurrencyUsage(currencyUsage: CurrencyUsage): Long
    suspend fun updateCurrencyUsage(currencyUsage: CurrencyUsage)
    suspend fun deleteCurrencyUsage(currencyUsage: CurrencyUsage)
    suspend fun getCurrencyUsageById(id: Long): CurrencyUsage?
    suspend fun getCurrencyUsageByUserAndCurrency(userId: Long, currency: String): CurrencyUsage?
    fun getCurrencyUsageByUser(userId: Long): Flow<List<CurrencyUsage>>
    fun getTopCurrenciesByUser(userId: Long, limit: Int = 10): Flow<List<CurrencyUsage>>
    fun getUserCurrenciesSortedByUsage(userId: Long): Flow<List<CurrencyUsage>>
    suspend fun incrementCurrencyUsage(userId: Long, currency: String)
    suspend fun deleteAllCurrencyUsageForUser(userId: Long)
    suspend fun getCurrencyUsageCountForUser(userId: Long): Int
}
