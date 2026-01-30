package com.pennywise.app.data.model

import java.util.concurrent.TimeUnit

/**
 * Cached exchange rate stored in SharedPreferences.
 */
data class CachedExchangeRate(
    val baseCode: String,
    val targetCode: String,
    val conversionRate: Double,
    val lastUpdateTime: Long
) {
    fun isExpired(now: Long = System.currentTimeMillis()): Boolean {
        return now - lastUpdateTime >= TimeUnit.HOURS.toMillis(24)
    }
}
