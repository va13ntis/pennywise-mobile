package com.pennywise.app.domain.repository

import com.pennywise.app.domain.model.PaymentMethod
import com.pennywise.app.domain.model.PaymentMethodConfig
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for PaymentMethodConfig operations
 */
interface PaymentMethodConfigRepository {
    
    /**
     * Get all payment method configurations
     */
    fun getPaymentMethodConfigs(): Flow<List<PaymentMethodConfig>>
    
    /**
     * Get payment method configuration by ID
     */
    suspend fun getPaymentMethodConfigById(id: Long): PaymentMethodConfig?
    
    /**
     * Get default payment method configuration
     */
    suspend fun getDefaultPaymentMethodConfig(): PaymentMethodConfig?
    
    /**
     * Get payment method configurations by payment method type
     */
    fun getPaymentMethodConfigsByType(paymentMethod: PaymentMethod): Flow<List<PaymentMethodConfig>>
    
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
    suspend fun setDefaultPaymentMethodConfig(configId: Long)
    
    /**
     * Check if there are any payment method configurations
     */
    suspend fun getPaymentMethodConfigCount(): Int
    
    /**
     * Get credit card configurations that need withdraw day configuration
     */
    fun getCreditCardConfigs(): Flow<List<PaymentMethodConfig>>
}
