package com.pennywise.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import androidx.room.ForeignKey
import androidx.room.Index
import com.pennywise.app.data.local.converter.DateConverter
import com.pennywise.app.data.local.converter.TransactionTypeConverter
import com.pennywise.app.data.local.converter.RecurringPeriodConverter
import com.pennywise.app.domain.model.Transaction
import com.pennywise.app.domain.model.TransactionType
import com.pennywise.app.domain.model.RecurringPeriod
import java.util.Date

/**
 * Room entity for transactions
 */
@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId")]
)
@TypeConverters(DateConverter::class, TransactionTypeConverter::class, RecurringPeriodConverter::class)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long, // Foreign key to users table
    val amount: Double,
    val currency: String = "USD",
    val description: String,
    val category: String,
    val type: TransactionType,
    val date: Date,
    val isRecurring: Boolean = false,
    val recurringPeriod: RecurringPeriod? = null,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
) {
    fun toDomainModel(): Transaction {
        return Transaction(
            id = id,
            userId = userId,
            amount = amount,
            currency = currency,
            description = description,
            category = category,
            type = type,
            date = date,
            isRecurring = isRecurring,
            recurringPeriod = recurringPeriod,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
    
    companion object {
        fun fromDomainModel(transaction: Transaction): TransactionEntity {
            return TransactionEntity(
                id = transaction.id,
                userId = transaction.userId,
                amount = transaction.amount,
                currency = transaction.currency,
                description = transaction.description,
                category = transaction.category,
                type = transaction.type,
                date = transaction.date,
                isRecurring = transaction.isRecurring,
                recurringPeriod = transaction.recurringPeriod,
                createdAt = transaction.createdAt,
                updatedAt = transaction.updatedAt
            )
        }
    }
}

