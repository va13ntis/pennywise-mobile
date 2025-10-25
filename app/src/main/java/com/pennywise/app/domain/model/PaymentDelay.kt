package com.pennywise.app.domain.model

import android.content.Context
import com.pennywise.app.R

/**
 * Enum representing billing delay options for credit card payments
 * Supports localized labels for Hebrew, English, and Russian
 */
enum class PaymentDelay(val days: Int, val stringResId: Int) {
    NONE(0, R.string.payment_delay_none),
    PLUS_30(30, R.string.payment_delay_plus_30),
    PLUS_60(60, R.string.payment_delay_plus_60),
    PLUS_90(90, R.string.payment_delay_plus_90);

    /**
     * Get localized label for the payment delay option
     */
    fun localizedLabel(context: Context): String =
        context.getString(stringResId)
}

