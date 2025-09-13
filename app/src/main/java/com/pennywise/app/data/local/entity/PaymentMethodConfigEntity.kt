package com.pennywise.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pennywise.app.domain.model.PaymentMethod
import com.pennywise.app.domain.model.PaymentMethodConfig

/**
 * Room entity for PaymentMethodConfig
 */
@Entity(tableName = "payment_method_configs")
data class PaymentMethodConfigEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val paymentMethod: PaymentMethod,
    val alias: String,
    val isDefault: Boolean = false,
    val withdrawDay: Int? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    
    /**
     * Convert entity to domain model
     */
    fun toDomainModel(): PaymentMethodConfig {
        return PaymentMethodConfig(
            id = id,
            userId = userId,
            paymentMethod = paymentMethod,
            alias = alias,
            isDefault = isDefault,
            withdrawDay = withdrawDay,
            isActive = isActive,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
    
    companion object {
        /**
         * Convert domain model to entity
         */
        fun fromDomainModel(config: PaymentMethodConfig): PaymentMethodConfigEntity {
            return PaymentMethodConfigEntity(
                id = config.id,
                userId = config.userId,
                paymentMethod = config.paymentMethod,
                alias = config.alias,
                isDefault = config.isDefault,
                withdrawDay = config.withdrawDay,
                isActive = config.isActive,
                createdAt = config.createdAt,
                updatedAt = config.updatedAt
            )
        }
    }
}
