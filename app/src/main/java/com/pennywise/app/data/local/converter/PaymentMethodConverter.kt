package com.pennywise.app.data.local.converter

import androidx.room.TypeConverter
import com.pennywise.app.domain.model.PaymentMethod

/**
 * Type converter for PaymentMethod enum to work with Room database
 */
class PaymentMethodConverter {
    
    @TypeConverter
    fun fromPaymentMethod(paymentMethod: PaymentMethod): String {
        return paymentMethod.name
    }
    
    @TypeConverter
    fun toPaymentMethod(paymentMethodString: String): PaymentMethod {
        return try {
            PaymentMethod.valueOf(paymentMethodString)
        } catch (e: IllegalArgumentException) {
            PaymentMethod.CASH // Default fallback
        }
    }
}
