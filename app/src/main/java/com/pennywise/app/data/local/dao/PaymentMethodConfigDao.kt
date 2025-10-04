package com.pennywise.app.data.local.dao

import androidx.room.*
import com.pennywise.app.data.local.entity.PaymentMethodConfigEntity
import com.pennywise.app.domain.model.PaymentMethod
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for PaymentMethodConfig operations
 */
@Dao
interface PaymentMethodConfigDao {
    
    /**
     * Get all payment method configurations
     */
    @Query("SELECT * FROM payment_method_configs WHERE isActive = 1 ORDER BY isDefault DESC, alias ASC")
    fun getAllPaymentMethodConfigs(): Flow<List<PaymentMethodConfigEntity>>
    
    /**
     * Get payment method configuration by ID
     */
    @Query("SELECT * FROM payment_method_configs WHERE id = :id")
    suspend fun getPaymentMethodConfigById(id: Long): PaymentMethodConfigEntity?
    
    /**
     * Get default payment method configuration
     */
    @Query("SELECT * FROM payment_method_configs WHERE isDefault = 1 AND isActive = 1 LIMIT 1")
    suspend fun getDefaultPaymentMethodConfig(): PaymentMethodConfigEntity?
    
    /**
     * Get payment method configurations by payment method type
     */
    @Query("SELECT * FROM payment_method_configs WHERE paymentMethod = :paymentMethod AND isActive = 1 ORDER BY alias ASC")
    fun getPaymentMethodConfigsByType(paymentMethod: PaymentMethod): Flow<List<PaymentMethodConfigEntity>>
    
    /**
     * Insert a new payment method configuration
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaymentMethodConfig(config: PaymentMethodConfigEntity): Long
    
    /**
     * Update an existing payment method configuration
     */
    @Update
    suspend fun updatePaymentMethodConfig(config: PaymentMethodConfigEntity)
    
    /**
     * Delete a payment method configuration (soft delete by setting isActive = false)
     */
    @Query("UPDATE payment_method_configs SET isActive = 0, updatedAt = :timestamp WHERE id = :id")
    suspend fun deletePaymentMethodConfig(id: Long, timestamp: Long = System.currentTimeMillis())
    
    /**
     * Set a payment method configuration as default
     * This will unset all other default configurations
     */
    @Transaction
    suspend fun setDefaultPaymentMethodConfig(configId: Long) {
        // First, unset all defaults
        unsetAllDefaults()
        
        // Then set the new default
        setDefault(configId)
    }
    
    @Query("UPDATE payment_method_configs SET isDefault = 0, updatedAt = :timestamp")
    suspend fun unsetAllDefaults(timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE payment_method_configs SET isDefault = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun setDefault(id: Long, timestamp: Long = System.currentTimeMillis())
    
    /**
     * Check if there are any payment method configurations
     */
    @Query("SELECT COUNT(*) FROM payment_method_configs WHERE isActive = 1")
    suspend fun getPaymentMethodConfigCount(): Int
    
    /**
     * Get payment method configurations that need withdraw day configuration (credit cards)
     */
    @Query("SELECT * FROM payment_method_configs WHERE paymentMethod = 'CREDIT_CARD' AND withdrawDay IS NOT NULL AND isActive = 1")
    fun getCreditCardConfigs(): Flow<List<PaymentMethodConfigEntity>>
}