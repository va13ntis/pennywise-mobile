package com.pennywise.app.data.repository

import com.pennywise.app.data.local.dao.CurrencyUsageDao
import com.pennywise.app.data.local.entity.CurrencyUsageEntity
import com.pennywise.app.domain.model.CurrencyUsage
import com.pennywise.app.domain.repository.CurrencyUsageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Date
import javax.inject.Inject

/**
 * Implementation of CurrencyUsageRepository
 */
class CurrencyUsageRepositoryImpl @Inject constructor(
    private val currencyUsageDao: CurrencyUsageDao
) : CurrencyUsageRepository {
    
    override suspend fun insertCurrencyUsage(currencyUsage: CurrencyUsage): Long {
        return currencyUsageDao.insertCurrencyUsage(CurrencyUsageEntity.fromDomainModel(currencyUsage))
    }
    
    override suspend fun updateCurrencyUsage(currencyUsage: CurrencyUsage) {
        currencyUsageDao.updateCurrencyUsage(CurrencyUsageEntity.fromDomainModel(currencyUsage))
    }
    
    override suspend fun deleteCurrencyUsage(currencyUsage: CurrencyUsage) {
        currencyUsageDao.deleteCurrencyUsage(CurrencyUsageEntity.fromDomainModel(currencyUsage))
    }
    
    override suspend fun getCurrencyUsageById(id: Long): CurrencyUsage? {
        return currencyUsageDao.getCurrencyUsageById(id)?.toDomainModel()
    }
    
    override suspend fun getCurrencyUsageByUserAndCurrency(userId: Long, currency: String): CurrencyUsage? {
        return currencyUsageDao.getCurrencyUsageByUserAndCurrency(userId, currency)?.toDomainModel()
    }
    
    override fun getCurrencyUsageByUser(userId: Long): Flow<List<CurrencyUsage>> {
        return currencyUsageDao.getCurrencyUsageByUser(userId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    override fun getTopCurrenciesByUser(userId: Long, limit: Int): Flow<List<CurrencyUsage>> {
        return currencyUsageDao.getTopCurrenciesByUser(userId, limit).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    override fun getUserCurrenciesSortedByUsage(userId: Long): Flow<List<CurrencyUsage>> {
        return currencyUsageDao.getUserCurrenciesSortedByUsage(userId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    override suspend fun incrementCurrencyUsage(userId: Long, currency: String) {
        val now = Date()
        currencyUsageDao.insertOrIncrementCurrencyUsage(userId, currency, now, now, now)
    }
    
    override suspend fun deleteAllCurrencyUsageForUser(userId: Long) {
        currencyUsageDao.deleteAllCurrencyUsageForUser(userId)
    }
    
    override suspend fun getCurrencyUsageCountForUser(userId: Long): Int {
        return currencyUsageDao.getCurrencyUsageCountForUser(userId)
    }
}
