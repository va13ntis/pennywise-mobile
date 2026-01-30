package com.pennywise.app.data.service

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.pennywise.app.data.api.CurrencyApi
import com.pennywise.app.data.model.CachedExchangeRate
import com.pennywise.app.data.model.ExchangeRateResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for converting currency with caching and API fallback.
 */
@Singleton
class CurrencyConversionService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val currencyApi: CurrencyApi
) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    suspend fun convertCurrency(
        amount: Double,
        fromCurrency: String,
        toCurrency: String
    ): Double? = withContext(Dispatchers.IO) {
        if (fromCurrency.equals(toCurrency, ignoreCase = true)) {
            return@withContext amount
        }
        if (amount == 0.0) {
            return@withContext 0.0
        }

        val cachedRate = getCachedRate(fromCurrency, toCurrency)
        if (cachedRate != null && !cachedRate.isExpired()) {
            return@withContext amount * cachedRate.conversionRate
        }

        val apiRate = fetchRate(fromCurrency, toCurrency)
        if (apiRate != null) {
            cacheRate(fromCurrency, toCurrency, apiRate)
            return@withContext amount * apiRate
        }

        if (cachedRate != null) {
            return@withContext amount * cachedRate.conversionRate
        }

        null
    }

    suspend fun isConversionAvailable(fromCurrency: String, toCurrency: String): Boolean {
        if (fromCurrency.equals(toCurrency, ignoreCase = true)) {
            return true
        }

        val cachedRate = getCachedRate(fromCurrency, toCurrency)
        if (cachedRate != null && !cachedRate.isExpired()) {
            return true
        }

        return fetchRate(fromCurrency, toCurrency) != null
    }

    fun getCacheStats(): Map<String, Any> {
        val keys = sharedPreferences.all.keys.filter { it.startsWith(EXCHANGE_RATE_PREFIX) }
        var validCount = 0
        var expiredCount = 0

        keys.forEach { key ->
            val json = sharedPreferences.getString(key, null)
            val cachedRate = json?.let { parseCachedRate(it) }
            if (cachedRate != null && !cachedRate.isExpired()) {
                validCount++
            } else {
                expiredCount++
            }
        }

        return mapOf(
            "total_cached" to keys.size,
            "valid_cached" to validCount,
            "expired_cached" to expiredCount
        )
    }

    fun clearCache() {
        val editor = sharedPreferences.edit()
        sharedPreferences.all.keys.forEach { key ->
            if (key.startsWith(EXCHANGE_RATE_PREFIX) || key.startsWith(TIMESTAMP_PREFIX)) {
                editor.remove(key)
            }
        }
        editor.apply()
    }

    private suspend fun fetchRate(fromCurrency: String, toCurrency: String): Double? {
        return try {
            val response = currencyApi.getExchangeRate(fromCurrency, toCurrency)
            extractRate(response, toCurrency)
        } catch (_: Exception) {
            null
        }
    }

    private fun extractRate(response: ExchangeRateResponse, targetCurrency: String): Double? {
        val isSuccess = response.success == true || response.result == "success"
        if (!isSuccess) {
            return null
        }

        response.conversionRate?.let { return it }

        val rates = response.rates ?: return null
        return rates[targetCurrency.uppercase()]
    }

    private fun getCachedRate(fromCurrency: String, toCurrency: String): CachedExchangeRate? {
        val key = buildRateKey(fromCurrency, toCurrency)
        val json = sharedPreferences.getString(key, null) ?: return null
        return parseCachedRate(json)
    }

    private fun cacheRate(fromCurrency: String, toCurrency: String, rate: Double) {
        val cachedRate = CachedExchangeRate(
            baseCode = fromCurrency.uppercase(),
            targetCode = toCurrency.uppercase(),
            conversionRate = rate,
            lastUpdateTime = System.currentTimeMillis()
        )
        val json = gson.toJson(cachedRate)
        sharedPreferences.edit().putString(buildRateKey(fromCurrency, toCurrency), json).apply()
    }

    private fun parseCachedRate(json: String): CachedExchangeRate? {
        return try {
            gson.fromJson(json, CachedExchangeRate::class.java)
        } catch (_: Exception) {
            null
        }
    }

    private fun buildRateKey(fromCurrency: String, toCurrency: String): String {
        return "$EXCHANGE_RATE_PREFIX${fromCurrency.uppercase()}_${toCurrency.uppercase()}"
    }

    private companion object {
        const val PREFS_NAME = "currency_exchange_rates"
        const val EXCHANGE_RATE_PREFIX = "exchange_rate_"
        const val TIMESTAMP_PREFIX = "timestamp_"
    }
}
