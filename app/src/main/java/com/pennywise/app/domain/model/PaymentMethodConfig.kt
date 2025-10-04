package com.pennywise.app.domain.model

/**
 * Data class representing a payment method configuration
 */
data class PaymentMethodConfig(
    val id: Long = 0,
    val paymentMethod: PaymentMethod,
    val alias: String, // e.g., "Personal Visa", "Corporate Card", "Main Credit Card"
    val isDefault: Boolean = false,
    val withdrawDay: Int? = null, // Day of month for credit card payment (1-31, null for non-credit cards)
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    
    /**
     * Get display name combining payment method and alias
     */
    fun getDisplayName(): String {
        return if (alias.isBlank()) {
            paymentMethod.displayName
        } else {
            "$alias (${paymentMethod.displayName})"
        }
    }
    
    /**
     * Check if this is a credit card that needs withdraw day configuration
     */
    fun isCreditCard(): Boolean {
        return paymentMethod.isCreditCard && withdrawDay != null
    }
    
    /**
     * Validate withdraw day for credit cards
     */
    fun isValidWithdrawDay(): Boolean {
        return withdrawDay == null || (withdrawDay in 1..31)
    }
    
    companion object {
        /**
         * Create a default payment method config
         */
        fun createDefault(paymentMethod: PaymentMethod, alias: String = ""): PaymentMethodConfig {
            return PaymentMethodConfig(
                paymentMethod = paymentMethod,
                alias = alias,
                isDefault = false,
                withdrawDay = if (paymentMethod.isCreditCard) 15 else null // Default to 15th for credit cards
            )
        }
    }
}
