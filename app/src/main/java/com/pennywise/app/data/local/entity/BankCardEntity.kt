package com.pennywise.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.pennywise.app.data.local.converter.DateConverter
import com.pennywise.app.domain.model.BankCard
import java.util.Date

/**
 * Room entity for bank cards
 */
@Entity(tableName = "bank_cards")
@TypeConverters(DateConverter::class)
data class BankCardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val alias: String, // User-friendly name for the card
    val lastFourDigits: String, // Last 4 digits of the card
    val paymentDay: Int, // Day of the month when payment is due (1-31)
    val isActive: Boolean = true,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
) {
    fun toDomainModel(): BankCard {
        return BankCard(
            id = id,
            alias = alias,
            lastFourDigits = lastFourDigits,
            paymentDay = paymentDay,
            isActive = isActive,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
    
    companion object {
        fun fromDomainModel(bankCard: BankCard): BankCardEntity {
            return BankCardEntity(
                id = bankCard.id,
                alias = bankCard.alias,
                lastFourDigits = bankCard.lastFourDigits,
                paymentDay = bankCard.paymentDay,
                isActive = bankCard.isActive,
                createdAt = bankCard.createdAt,
                updatedAt = bankCard.updatedAt
            )
        }
    }
}
