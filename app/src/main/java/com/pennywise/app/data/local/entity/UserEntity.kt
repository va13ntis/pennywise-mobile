package com.pennywise.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.pennywise.app.data.local.converter.DateConverter
import com.pennywise.app.data.local.converter.PaymentMethodConverter
import com.pennywise.app.data.local.converter.UserRoleConverter
import com.pennywise.app.data.local.converter.UserStatusConverter
import com.pennywise.app.domain.model.PaymentMethod
import com.pennywise.app.domain.model.User
import com.pennywise.app.domain.model.UserRole
import com.pennywise.app.domain.model.UserStatus
import java.util.Date

/**
 * Room entity for user data
 */
@Entity(tableName = "users")
@TypeConverters(DateConverter::class, UserRoleConverter::class, UserStatusConverter::class, PaymentMethodConverter::class)
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val defaultCurrency: String = "USD",
    val defaultPaymentMethod: PaymentMethod = PaymentMethod.CASH,
    val locale: String = "en",
    val deviceAuthEnabled: Boolean = false,
    val role: UserRole = UserRole.USER,
    val status: UserStatus = UserStatus.ACTIVE,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
) {
    fun toDomainModel(): User {
        return User(
            id = id,
            defaultCurrency = defaultCurrency,
            defaultPaymentMethod = defaultPaymentMethod,
            locale = locale,
            deviceAuthEnabled = deviceAuthEnabled,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
    
    companion object {
        fun fromDomainModel(user: User): UserEntity {
            return UserEntity(
                id = user.id,
                defaultCurrency = user.defaultCurrency,
                defaultPaymentMethod = user.defaultPaymentMethod,
                locale = user.locale,
                deviceAuthEnabled = user.deviceAuthEnabled,
                createdAt = user.createdAt,
                updatedAt = user.updatedAt
            )
        }
    }
}
