package com.pennywise.app.data.service

import android.content.Context
import android.content.SharedPreferences
import com.pennywise.app.data.api.CurrencyApi
import com.pennywise.app.data.model.CachedExchangeRate
import com.pennywise.app.data.model.ExchangeRateResponse
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for handling currency conversion operations
 * Includes API integration, caching, and offline fallback
 */
@Singleton
class CurrencyConversionService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        "currency_conversion_cache",
        Context.MODE_PRIVATE
    )
    
    private val gson = Gson()
    
    // Using a free API key for demonstration - in production, use a proper API key
    private val apiKey = "YOUR_API_KEY" // Replace with actual API key
    private val baseUrl = "https://v6.exchangerate-api.com/"
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
    
    private val currencyApi: CurrencyApi = retrofit.create(CurrencyApi::class.java)
    
    companion object {
        private const val CACHE_KEY_PREFIX = "exchange_rate_"
        private const val CACHE_TIMESTAMP_PREFIX = "timestamp_"
        private const val CACHE_DURATION_HOURS = 24L
    }
    
    /**
     * Convert amount from one currency to another
     * @param amount The amount to convert
     * @param fromCurrency The source currency code
     * @param toCurrency The target currency code
     * @return The converted amount, or null if conversion fails
     */
    suspend fun convertCurrency(
        amount: Double,
        fromCurrency: String,
        toCurrency: String
    ): Double? = withContext(Dispatchers.IO) {
        try {
            // If currencies are the same, no conversion needed
            if (fromCurrency == toCurrency) {
                return@withContext amount
            }
            
            // Try to get cached rate first
            val cachedRate = getCachedExchangeRate(fromCurrency, toCurrency)
            if (cachedRate != null && !cachedRate.isExpired()) {
                return@withContext amount * cachedRate.conversionRate
            }
            
            // If no valid cache, fetch from API
            val exchangeRate = fetchExchangeRate(fromCurrency, toCurrency)
            if (exchangeRate != null) {
                // Cache the new rate
                cacheExchangeRate(fromCurrency, toCurrency, exchangeRate)
                return@withContext amount * exchangeRate.conversionRate
            }
            
            // If API fails and we have expired cache, use it as fallback
            if (cachedRate != null) {
                return@withContext amount * cachedRate.conversionRate
            }
            
            null
        } catch (e: Exception) {
            // Log error and return null
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Fetch exchange rate from API
     */
    private suspend fun fetchExchangeRate(
        fromCurrency: String,
        toCurrency: String
    ): CachedExchangeRate? {
        return try {
            val response = currencyApi.getExchangeRate(fromCurrency, toCurrency)
            CachedExchangeRate(
                baseCode = response.baseCode,
                targetCode = response.targetCode,
                conversionRate = response.conversionRate,
                lastUpdateTime = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Get cached exchange rate
     */
    private fun getCachedExchangeRate(
        fromCurrency: String,
        toCurrency: String
    ): CachedExchangeRate? {
        val cacheKey = "$CACHE_KEY_PREFIX${fromCurrency}_$toCurrency"
        val json = sharedPreferences.getString(cacheKey, null)
        
        return if (json != null) {
            try {
                gson.fromJson(json, CachedExchangeRate::class.java)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }
    
    /**
     * Cache exchange rate
     */
    private fun cacheExchangeRate(
        fromCurrency: String,
        toCurrency: String,
        rate: CachedExchangeRate
    ) {
        val cacheKey = "$CACHE_KEY_PREFIX${fromCurrency}_$toCurrency"
        val json = gson.toJson(rate)
        sharedPreferences.edit().putString(cacheKey, json).apply()
    }
    
    /**
     * Clear all cached exchange rates
     */
    fun clearCache() {
        val editor = sharedPreferences.edit()
        val allKeys = sharedPreferences.all.keys
        allKeys.filter { it.startsWith(CACHE_KEY_PREFIX) }
            .forEach { editor.remove(it) }
        editor.apply()
    }
    
    /**
     * Get cache statistics
     */
    fun getCacheStats(): Map<String, Any> {
        val allKeys = sharedPreferences.all.keys
        val cachedRates = allKeys.filter { it.startsWith(CACHE_KEY_PREFIX) }
        
        val validRates = cachedRates.count { key ->
            val json = sharedPreferences.getString(key, null)
            if (json != null) {
                try {
                    val rate = gson.fromJson(json, CachedExchangeRate::class.java)
                    !rate.isExpired()
                } catch (e: Exception) {
                    false
                }
            } else {
                false
            }
        }
        
        return mapOf(
            "total_cached" to cachedRates.size,
            "valid_cached" to validRates,
            "expired_cached" to (cachedRates.size - validRates)
        )
    }
    
    /**
     * Check if conversion is available (either cached or API accessible)
     */
    suspend fun isConversionAvailable(
        fromCurrency: String,
        toCurrency: String
    ): Boolean {
        if (fromCurrency == toCurrency) return true
        
        val cachedRate = getCachedExchangeRate(fromCurrency, toCurrency)
        if (cachedRate != null && !cachedRate.isExpired()) {
            return true
        }
        
        // Try to fetch from API to check availability
        return try {
            val rate = fetchExchangeRate(fromCurrency, toCurrency)
            rate != null
        } catch (e: Exception) {
            false
        }
    }
}
