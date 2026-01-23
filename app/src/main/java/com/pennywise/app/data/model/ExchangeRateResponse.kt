package com.pennywise.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * API response for exchange rate lookup.
 * Supports multiple providers via alternate field names.
 */
data class ExchangeRateResponse(
    @SerializedName("result")
    val result: String? = null,
    @SerializedName("success")
    val success: Boolean? = null,
    @SerializedName(value = "base_code", alternate = ["base"])
    val baseCode: String? = null,
    @SerializedName(value = "target_code", alternate = ["target"])
    val targetCode: String? = null,
    @SerializedName(value = "conversion_rate", alternate = ["rate"])
    val conversionRate: Double? = null,
    @SerializedName(value = "time_last_update_utc", alternate = ["time_last_update", "date"])
    val lastUpdateTime: String? = null,
    @SerializedName(value = "time_next_update_utc", alternate = ["time_next_update"])
    val nextUpdateTime: String? = null,
    @SerializedName("rates")
    val rates: Map<String, Double>? = null
)
