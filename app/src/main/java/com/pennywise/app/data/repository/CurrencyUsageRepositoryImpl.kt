package com.pennywise.app.data.repository

import com.pennywise.app.data.local.dao.CurrencyUsageDao
import com.pennywise.app.data.local.entity.CurrencyUsageEntity
import com.pennywise.app.domain.model.CurrencyUsage
import com.pennywise.app.domain.repository.CurrencyUsageRepository
import com.pennywise.app.domain.validation.AuthenticationValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.util.Date
import javax.inject.Inject

/**
 * Implementation of CurrencyUsageRepository with authentication validation.
 * All database operations require user authentication.
 */
class CurrencyUsageRepositoryImpl @Inject constructor(
    private val currencyUsageDao: CurrencyUsageDao,
    authValidator: AuthenticationValidator
) : CurrencyUsageRepository, BaseAuthenticatedRepository(authValidator) {
    
    override suspend fun insertCurrencyUsage(currencyUsage: CurrencyUsage): Long = withAuthentication {
        currencyUsageDao.insertCurrencyUsage(CurrencyUsageEntity.fromDomainModel(currencyUsage))
    }
    
    override suspend fun updateCurrencyUsage(currencyUsage: CurrencyUsage) = withAuthentication {
        currencyUsageDao.updateCurrencyUsage(CurrencyUsageEntity.fromDomainModel(currencyUsage))
    }
    
    override suspend fun deleteCurrencyUsage(currencyUsage: CurrencyUsage) = withAuthentication {
        currencyUsageDao.deleteCurrencyUsage(CurrencyUsageEntity.fromDomainModel(currencyUsage))
    }
    
    override suspend fun getCurrencyUsageById(id: Long): CurrencyUsage? = withAuthentication {
        currencyUsageDao.getCurrencyUsageById(id)?.toDomainModel()
    }
    
    override suspend fun getCurrencyUsageByCurrency(currency: String): CurrencyUsage? = withAuthentication {
        currencyUsageDao.getCurrencyUsageByCurrency(currency)?.toDomainModel()
    }
    
    override fun getCurrencyUsage(): Flow<List<CurrencyUsage>> = flow {
        withAuthentication {
            currencyUsageDao.getAllCurrencyUsage().collect { entities ->
                emit(entities.map { it.toDomainModel() })
            }
        }
    }
    
    override fun getTopCurrencies(limit: Int): Flow<List<CurrencyUsage>> = flow {
        withAuthentication {
            currencyUsageDao.getTopCurrencies(limit).collect { entities ->
                emit(entities.map { it.toDomainModel() })
            }
        }
    }
    
    override fun getCurrenciesSortedByUsage(): Flow<List<CurrencyUsage>> = flow {
        withAuthentication {
            currencyUsageDao.getCurrencyUsageSortedByUsage().collect { entities ->
                emit(entities.map { it.toDomainModel() })
            }
        }
    }
    
    override suspend fun incrementCurrencyUsage(currency: String) = withAuthentication {
        val now = Date()
        currencyUsageDao.insertOrIncrementCurrencyUsage(currency, now, now, now)
    }
    
    override suspend fun deleteAllCurrencyUsage() = withAuthentication {
        currencyUsageDao.deleteAllCurrencyUsage()
    }
    
    override suspend fun getCurrencyUsageCount(): Int = withAuthentication {
        currencyUsageDao.getCurrencyUsageCount()
    }
}