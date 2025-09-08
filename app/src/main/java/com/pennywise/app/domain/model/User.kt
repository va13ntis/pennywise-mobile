package com.pennywise.app.domain.model

import java.util.Date

/**
 * Domain model representing a user in the PennyWise app
 */
data class User(
    val id: Long = 0,
    val username: String,
    val passwordHash: String,
    val email: String? = null,
    val defaultCurrency: String = "USD",
    val locale: String = "en",
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)

/**
 * Enum representing user roles (for future expansion)
 */
enum class UserRole {
    USER,
    ADMIN
}

/**
 * Enum representing user status
 */
enum class UserStatus {
    ACTIVE,
    INACTIVE,
    SUSPENDED
}
