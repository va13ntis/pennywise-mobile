package com.pennywise.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.pennywise.app.data.local.converter.DateConverter
import com.pennywise.app.data.local.converter.UserRoleConverter
import com.pennywise.app.data.local.converter.UserStatusConverter
import com.pennywise.app.domain.model.User
import com.pennywise.app.domain.model.UserRole
import com.pennywise.app.domain.model.UserStatus
import java.util.Date

/**
 * Room entity for users
 */
@Entity(tableName = "users")
@TypeConverters(DateConverter::class, UserRoleConverter::class, UserStatusConverter::class)
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val username: String,
    val passwordHash: String,
    val email: String? = null,
    val role: UserRole = UserRole.USER,
    val status: UserStatus = UserStatus.ACTIVE,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
) {
    fun toDomainModel(): User {
        return User(
            id = id,
            username = username,
            passwordHash = passwordHash,
            email = email,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
    
    companion object {
        fun fromDomainModel(user: User): UserEntity {
            return UserEntity(
                id = user.id,
                username = user.username,
                passwordHash = user.passwordHash,
                email = user.email,
                createdAt = user.createdAt,
                updatedAt = user.updatedAt
            )
        }
    }
}
