package com.pennywise.app.domain.model

import java.util.Date

/**
 * Domain model representing currency usage tracking for a user
 */
data class CurrencyUsage(
    val id: Long = 0,
    val userId: Long,
    val currency: String,
    val usageCount: Int = 0,
    val lastUsed: Date = Date(),
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)
