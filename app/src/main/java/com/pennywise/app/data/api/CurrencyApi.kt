package com.pennywise.app.data.api

import com.pennywise.app.data.model.ExchangeRateResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * API interface for currency conversion using ExchangeRate-API
 * Documentation: https://www.exchangerate-api.com/docs/overview
 */
interface CurrencyApi {
    
    /**
     * Get exchange rate between two currencies
     * @param baseCode The base currency code (e.g., "USD")
     * @param targetCode The target currency code (e.g., "EUR")
     * @return ExchangeRateResponse containing the conversion rate
     */
    @GET("v6/latest")
    suspend fun getExchangeRate(
        @Query("base") baseCode: String,
        @Query("symbols") targetCode: String
    ): ExchangeRateResponse
    
    /**
     * Get exchange rates for multiple currencies from a base currency
     * @param baseCode The base currency code (e.g., "USD")
     * @param symbols Comma-separated list of target currency codes (e.g., "EUR,GBP,ILS")
     * @return ExchangeRateResponse containing multiple conversion rates
     */
    @GET("v6/latest")
    suspend fun getExchangeRates(
        @Query("base") baseCode: String,
        @Query("symbols") symbols: String
    ): ExchangeRateResponse
}
