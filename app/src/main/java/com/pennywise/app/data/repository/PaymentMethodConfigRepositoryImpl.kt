package com.pennywise.app.data.repository

import com.pennywise.app.data.local.dao.PaymentMethodConfigDao
import com.pennywise.app.data.local.entity.PaymentMethodConfigEntity
import com.pennywise.app.domain.model.PaymentMethod
import com.pennywise.app.domain.model.PaymentMethodConfig
import com.pennywise.app.domain.repository.PaymentMethodConfigRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Implementation of PaymentMethodConfigRepository using Room database
 */
class PaymentMethodConfigRepositoryImpl @Inject constructor(
    private val paymentMethodConfigDao: PaymentMethodConfigDao
) : PaymentMethodConfigRepository {
    
    override fun getPaymentMethodConfigsByUser(userId: Long): Flow<List<PaymentMethodConfig>> {
        return paymentMethodConfigDao.getPaymentMethodConfigsByUser(userId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    override suspend fun getPaymentMethodConfigById(id: Long): PaymentMethodConfig? {
        return paymentMethodConfigDao.getPaymentMethodConfigById(id)?.toDomainModel()
    }
    
    override suspend fun getDefaultPaymentMethodConfig(userId: Long): PaymentMethodConfig? {
        return paymentMethodConfigDao.getDefaultPaymentMethodConfig(userId)?.toDomainModel()
    }
    
    override fun getPaymentMethodConfigsByType(userId: Long, paymentMethod: PaymentMethod): Flow<List<PaymentMethodConfig>> {
        return paymentMethodConfigDao.getPaymentMethodConfigsByType(userId, paymentMethod).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    override suspend fun insertPaymentMethodConfig(config: PaymentMethodConfig): Long {
        return paymentMethodConfigDao.insertPaymentMethodConfig(PaymentMethodConfigEntity.fromDomainModel(config))
    }
    
    override suspend fun updatePaymentMethodConfig(config: PaymentMethodConfig) {
        paymentMethodConfigDao.updatePaymentMethodConfig(PaymentMethodConfigEntity.fromDomainModel(config))
    }
    
    override suspend fun deletePaymentMethodConfig(id: Long) {
        paymentMethodConfigDao.deletePaymentMethodConfig(id)
    }
    
    override suspend fun setDefaultPaymentMethodConfig(userId: Long, configId: Long) {
        paymentMethodConfigDao.setDefaultPaymentMethodConfig(userId, configId)
    }
    
    override suspend fun getPaymentMethodConfigCount(userId: Long): Int {
        return paymentMethodConfigDao.getPaymentMethodConfigCount(userId)
    }
    
    override fun getCreditCardConfigs(userId: Long): Flow<List<PaymentMethodConfig>> {
        return paymentMethodConfigDao.getCreditCardConfigs(userId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
}
