package com.pennywise.app.data.repository

import com.pennywise.app.data.local.dao.PaymentMethodConfigDao
import com.pennywise.app.data.local.entity.PaymentMethodConfigEntity
import com.pennywise.app.domain.model.PaymentMethod
import com.pennywise.app.domain.model.PaymentMethodConfig
import com.pennywise.app.domain.repository.PaymentMethodConfigRepository
import com.pennywise.app.domain.validation.AuthenticationValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Implementation of PaymentMethodConfigRepository using Room database with authentication validation.
 * All database operations require user authentication.
 */
class PaymentMethodConfigRepositoryImpl @Inject constructor(
    private val paymentMethodConfigDao: PaymentMethodConfigDao,
    authValidator: AuthenticationValidator
) : PaymentMethodConfigRepository, BaseAuthenticatedRepository(authValidator) {
    
    override fun getPaymentMethodConfigs(): Flow<List<PaymentMethodConfig>> = flow {
        withAuthentication {
            paymentMethodConfigDao.getAllPaymentMethodConfigs().collect { entities ->
                emit(entities.map { it.toDomainModel() })
            }
        }
    }
    
    override suspend fun getPaymentMethodConfigById(id: Long): PaymentMethodConfig? = withAuthentication {
        paymentMethodConfigDao.getPaymentMethodConfigById(id)?.toDomainModel()
    }
    
    override suspend fun getDefaultPaymentMethodConfig(): PaymentMethodConfig? = withAuthentication {
        paymentMethodConfigDao.getDefaultPaymentMethodConfig()?.toDomainModel()
    }
    
    override fun getPaymentMethodConfigsByType(paymentMethod: PaymentMethod): Flow<List<PaymentMethodConfig>> = flow {
        withAuthentication {
            paymentMethodConfigDao.getPaymentMethodConfigsByType(paymentMethod).collect { entities ->
                emit(entities.map { it.toDomainModel() })
            }
        }
    }
    
    override suspend fun insertPaymentMethodConfig(config: PaymentMethodConfig): Long = withAuthentication {
        paymentMethodConfigDao.insertPaymentMethodConfig(PaymentMethodConfigEntity.fromDomainModel(config))
    }
    
    override suspend fun updatePaymentMethodConfig(config: PaymentMethodConfig) = withAuthentication {
        paymentMethodConfigDao.updatePaymentMethodConfig(PaymentMethodConfigEntity.fromDomainModel(config))
    }
    
    override suspend fun deletePaymentMethodConfig(id: Long) = withAuthentication {
        paymentMethodConfigDao.deletePaymentMethodConfig(id)
    }
    
    override suspend fun setDefaultPaymentMethodConfig(configId: Long) = withAuthentication {
        paymentMethodConfigDao.setDefaultPaymentMethodConfig(configId)
    }
    
    override suspend fun getPaymentMethodConfigCount(): Int = withAuthentication {
        paymentMethodConfigDao.getPaymentMethodConfigCount()
    }
    
    override fun getCreditCardConfigs(): Flow<List<PaymentMethodConfig>> = flow {
        withAuthentication {
            paymentMethodConfigDao.getCreditCardConfigs().collect { entities ->
                emit(entities.map { it.toDomainModel() })
            }
        }
    }
}
