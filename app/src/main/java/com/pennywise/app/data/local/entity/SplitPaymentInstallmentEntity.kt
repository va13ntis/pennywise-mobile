package com.pennywise.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import androidx.room.ForeignKey
import androidx.room.Index
import com.pennywise.app.data.local.converter.DateConverter
import com.pennywise.app.data.local.converter.TransactionTypeConverter
import com.pennywise.app.domain.model.SplitPaymentInstallment
import com.pennywise.app.domain.model.TransactionType
import java.util.Date

/**
 * Room entity for split payment installments
 */
@Entity(
    tableName = "split_payment_installments",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentTransactionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("userId"),
        Index("parentTransactionId"),
        Index("dueDate"),
        Index("isPaid")
    ]
)
@TypeConverters(DateConverter::class, TransactionTypeConverter::class)
data class SplitPaymentInstallmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val parentTransactionId: Long, // Foreign key to the original transaction
    val userId: Long, // Foreign key to users table
    val amount: Double,
    val currency: String = "USD",
    val description: String,
    val category: String,
    val type: TransactionType,
    val dueDate: Date,
    val installmentNumber: Int,
    val totalInstallments: Int,
    val isPaid: Boolean = false,
    val paidDate: Date? = null,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
) {
    fun toDomainModel(): SplitPaymentInstallment {
        return SplitPaymentInstallment(
            id = id,
            parentTransactionId = parentTransactionId,
            userId = userId,
            amount = amount,
            currency = currency,
            description = description,
            category = category,
            type = type,
            dueDate = dueDate,
            installmentNumber = installmentNumber,
            totalInstallments = totalInstallments,
            isPaid = isPaid,
            paidDate = paidDate,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
    
    companion object {
        fun fromDomainModel(installment: SplitPaymentInstallment): SplitPaymentInstallmentEntity {
            return SplitPaymentInstallmentEntity(
                id = installment.id,
                parentTransactionId = installment.parentTransactionId,
                userId = installment.userId,
                amount = installment.amount,
                currency = installment.currency,
                description = installment.description,
                category = installment.category,
                type = installment.type,
                dueDate = installment.dueDate,
                installmentNumber = installment.installmentNumber,
                totalInstallments = installment.totalInstallments,
                isPaid = installment.isPaid,
                paidDate = installment.paidDate,
                createdAt = installment.createdAt,
                updatedAt = installment.updatedAt
            )
        }
    }
}
