package com.pennywise.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.pennywise.app.data.local.converter.DateConverter
import com.pennywise.app.data.local.converter.TransactionTypeConverter
import com.pennywise.app.data.local.converter.RecurringPeriodConverter
import com.pennywise.app.data.local.converter.PaymentMethodConverter
import com.pennywise.app.domain.model.Transaction
import com.pennywise.app.domain.model.TransactionType
import com.pennywise.app.domain.model.RecurringPeriod
import com.pennywise.app.domain.model.PaymentMethod
import java.util.Date

/**
 * Room entity for transactions
 */
@Entity(tableName = "transactions")
@TypeConverters(DateConverter::class, TransactionTypeConverter::class, RecurringPeriodConverter::class, PaymentMethodConverter::class)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val currency: String = "USD",
    val description: String,
    val category: String,
    val type: TransactionType,
    val date: Date,
    val isRecurring: Boolean = false,
    val recurringPeriod: RecurringPeriod? = null,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val paymentMethodConfigId: Long? = null, // Link to specific PaymentMethodConfig (e.g., which credit card)
    val installments: Int? = null, // Only used for split payments
    val installmentAmount: Double? = null, // Calculated monthly payment amount
    val billingDelayDays: Int = 0, // Billing delay in days for credit card payments
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
) {
    fun toDomainModel(): Transaction {
        return Transaction(
            id = id,
            amount = amount,
            currency = currency,
            description = description,
            category = category,
            type = type,
            date = date,
            isRecurring = isRecurring,
            recurringPeriod = recurringPeriod,
            paymentMethod = paymentMethod,
            paymentMethodConfigId = paymentMethodConfigId,
            installments = installments,
            installmentAmount = installmentAmount,
            billingDelayDays = billingDelayDays,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
    
    companion object {
        fun fromDomainModel(transaction: Transaction): TransactionEntity {
            return TransactionEntity(
                id = transaction.id,
                amount = transaction.amount,
                currency = transaction.currency,
                description = transaction.description,
                category = transaction.category,
                type = transaction.type,
                date = transaction.date,
                isRecurring = transaction.isRecurring,
                recurringPeriod = transaction.recurringPeriod,
                paymentMethod = transaction.paymentMethod,
                paymentMethodConfigId = transaction.paymentMethodConfigId,
                installments = transaction.installments,
                installmentAmount = transaction.installmentAmount,
                billingDelayDays = transaction.billingDelayDays,
                createdAt = transaction.createdAt,
                updatedAt = transaction.updatedAt
            )
        }
    }
}

