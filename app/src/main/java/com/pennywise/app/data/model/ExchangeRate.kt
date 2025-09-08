package com.pennywise.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * Data model for exchange rate information
 */
data class ExchangeRate(
    @SerializedName("base_code")
    val baseCode: String,
    
    @SerializedName("target_code")
    val targetCode: String,
    
    @SerializedName("conversion_rate")
    val conversionRate: Double,
    
    @SerializedName("time_last_update_utc")
    val lastUpdateTime: String,
    
    @SerializedName("time_next_update_utc")
    val nextUpdateTime: String
)

/**
 * Response wrapper for exchange rate API
 */
data class ExchangeRateResponse(
    @SerializedName("result")
    val result: String,
    
    @SerializedName("base_code")
    val baseCode: String,
    
    @SerializedName("target_code")
    val targetCode: String,
    
    @SerializedName("conversion_rate")
    val conversionRate: Double,
    
    @SerializedName("time_last_update_utc")
    val lastUpdateTime: String,
    
    @SerializedName("time_next_update_utc")
    val nextUpdateTime: String
)

/**
 * Cached exchange rate data for offline use
 */
data class CachedExchangeRate(
    val baseCode: String,
    val targetCode: String,
    val conversionRate: Double,
    val lastUpdateTime: Long
) {
    companion object {
        const val CACHE_DURATION_HOURS = 24L // Cache for 24 hours
    }
    
    fun isExpired(): Boolean {
        val currentTime = System.currentTimeMillis()
        val cacheExpiryTime = lastUpdateTime + (CACHE_DURATION_HOURS * 60 * 60 * 1000)
        return currentTime > cacheExpiryTime
    }
}
