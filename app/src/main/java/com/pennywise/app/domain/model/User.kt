package com.pennywise.app.domain.model

import java.util.Date

/**
 * Domain model representing a user in the PennyWise app
 * Simplified for single-user per app with device authentication
 */
data class User(
    val id: Long = 0,
    val defaultCurrency: String = "USD",
    val locale: String = "en",
    val deviceAuthEnabled: Boolean = false,
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
