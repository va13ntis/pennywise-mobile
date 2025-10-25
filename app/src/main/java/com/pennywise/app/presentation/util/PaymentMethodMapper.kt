package com.pennywise.app.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.pennywise.app.R
import com.pennywise.app.domain.model.PaymentMethod

/**
 * Utility class for mapping payment methods to icons and localized names
 * Provides consistent visual representation across the app
 */
object PaymentMethodMapper {
    
    /**
     * Get emoji icon for payment method
     */
    fun getPaymentMethodIcon(paymentMethod: PaymentMethod): String {
        return when (paymentMethod) {
            PaymentMethod.CASH -> "💵"
            PaymentMethod.CREDIT_CARD -> "💳"
            PaymentMethod.BANK_TRANSFER -> "🏦"
            PaymentMethod.MOBILE_PAYMENT -> "📱"
            PaymentMethod.CHEQUE -> "📝"
        }
    }
    
    /**
     * Get localized name for payment method
     */
    @Composable
    fun getLocalizedPaymentMethod(paymentMethod: PaymentMethod): String {
        return when (paymentMethod) {
            PaymentMethod.CASH -> stringResource(R.string.payment_method_cash)
            PaymentMethod.CREDIT_CARD -> stringResource(R.string.payment_method_credit_card)
            PaymentMethod.BANK_TRANSFER -> stringResource(R.string.payment_method_bank_transfer)
            PaymentMethod.MOBILE_PAYMENT -> stringResource(R.string.payment_method_mobile_payment)
            PaymentMethod.CHEQUE -> stringResource(R.string.payment_method_cheque)
        }
    }
}

