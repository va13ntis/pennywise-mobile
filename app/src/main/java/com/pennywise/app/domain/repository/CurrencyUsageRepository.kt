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
    suspend fun getCurrencyUsageByCurrency(currency: String): CurrencyUsage?
    fun getCurrencyUsage(): Flow<List<CurrencyUsage>>
    fun getTopCurrencies(limit: Int = 10): Flow<List<CurrencyUsage>>
    fun getCurrenciesSortedByUsage(): Flow<List<CurrencyUsage>>
    suspend fun incrementCurrencyUsage(currency: String)
    suspend fun deleteAllCurrencyUsage()
    suspend fun getCurrencyUsageCount(): Int
}
