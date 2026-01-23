package com.pennywise.app.data.api

import com.pennywise.app.data.model.ExchangeRateResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Currency exchange API.
 * Uses base + symbols parameters for single or multi-currency lookups.
 */
interface CurrencyApi {
    @GET("latest/{baseCode}")
    suspend fun getExchangeRate(
        @Path("baseCode") baseCode: String,
        @Query("symbols") targetCode: String
    ): ExchangeRateResponse

    @GET("latest/{baseCode}")
    suspend fun getExchangeRates(
        @Path("baseCode") baseCode: String,
        @Query("symbols") targetCodes: String
    ): ExchangeRateResponse
}
