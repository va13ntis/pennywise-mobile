package com.pennywise.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import androidx.room.ForeignKey
import androidx.room.Index
import com.pennywise.app.data.local.converter.DateConverter
import com.pennywise.app.domain.model.CurrencyUsage
import java.util.Date

/**
 * Room entity for currency usage tracking
 */
@Entity(
    tableName = "currency_usage",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId"), Index("userId", "currency", unique = true)]
)
@TypeConverters(DateConverter::class)
data class CurrencyUsageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long, // Foreign key to users table
    val currency: String,
    val usageCount: Int = 0,
    val lastUsed: Date = Date(),
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
) {
    fun toDomainModel(): CurrencyUsage {
        return CurrencyUsage(
            id = id,
            userId = userId,
            currency = currency,
            usageCount = usageCount,
            lastUsed = lastUsed,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
    
    companion object {
        fun fromDomainModel(currencyUsage: CurrencyUsage): CurrencyUsageEntity {
            return CurrencyUsageEntity(
                id = currencyUsage.id,
                userId = currencyUsage.userId,
                currency = currencyUsage.currency,
                usageCount = currencyUsage.usageCount,
                lastUsed = currencyUsage.lastUsed,
                createdAt = currencyUsage.createdAt,
                updatedAt = currencyUsage.updatedAt
            )
        }
    }
}
