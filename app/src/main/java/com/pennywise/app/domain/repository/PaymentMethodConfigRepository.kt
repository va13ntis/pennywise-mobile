package com.pennywise.app.domain.repository

import com.pennywise.app.domain.model.PaymentMethod
import com.pennywise.app.domain.model.PaymentMethodConfig
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for PaymentMethodConfig operations
 */
interface PaymentMethodConfigRepository {
    
    /**
     * Get all payment method configurations for a user
     */
    fun getPaymentMethodConfigsByUser(userId: Long): Flow<List<PaymentMethodConfig>>
    
    /**
     * Get payment method configuration by ID
     */
    suspend fun getPaymentMethodConfigById(id: Long): PaymentMethodConfig?
    
    /**
     * Get default payment method configuration for a user
     */
    suspend fun getDefaultPaymentMethodConfig(userId: Long): PaymentMethodConfig?
    
    /**
     * Get payment method configurations by payment method type
     */
    fun getPaymentMethodConfigsByType(userId: Long, paymentMethod: PaymentMethod): Flow<List<PaymentMethodConfig>>
    
    /**
     * Insert a new payment method configuration
     */
    suspend fun insertPaymentMethodConfig(config: PaymentMethodConfig): Long
    
    /**
     * Update an existing payment method configuration
     */
    suspend fun updatePaymentMethodConfig(config: PaymentMethodConfig)
    
    /**
     * Delete a payment method configuration
     */
    suspend fun deletePaymentMethodConfig(id: Long)
    
    /**
     * Set a payment method configuration as default
     */
    suspend fun setDefaultPaymentMethodConfig(userId: Long, configId: Long)
    
    /**
     * Check if a user has any payment method configurations
     */
    suspend fun getPaymentMethodConfigCount(userId: Long): Int
    
    /**
     * Get credit card configurations that need withdraw day configuration
     */
    fun getCreditCardConfigs(userId: Long): Flow<List<PaymentMethodConfig>>
}
