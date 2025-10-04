package com.pennywise.app.data.security

import com.pennywise.app.domain.model.BankCard
import java.util.Date

/**
 * Secure wrapper for bank card data that handles encryption/decryption
 */
data class SecureBankCard(
    val id: Long = 0,
    val alias: String,
    val encryptedLastFourDigits: String, // Encrypted last four digits
    val paymentDay: Int,
    val isActive: Boolean = true,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
) {
    
    /**
     * Convert to domain model with decrypted data
     */
    fun toDomainModel(encryptionManager: CardEncryptionManager): BankCard {
        return BankCard(
            id = id,
            alias = alias,
            lastFourDigits = encryptionManager.decryptLastFourDigits(encryptedLastFourDigits),
            paymentDay = paymentDay,
            isActive = isActive,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
    
    companion object {
        /**
         * Create from domain model with encrypted data
         */
        fun fromDomainModel(bankCard: BankCard, encryptionManager: CardEncryptionManager): SecureBankCard {
            return SecureBankCard(
                id = bankCard.id,
                alias = bankCard.alias,
                encryptedLastFourDigits = encryptionManager.encryptLastFourDigits(bankCard.lastFourDigits),
                paymentDay = bankCard.paymentDay,
                isActive = bankCard.isActive,
                createdAt = bankCard.createdAt,
                updatedAt = bankCard.updatedAt
            )
        }
    }
}