package com.pennywise.app.domain.model

/**
 * Enum representing different payment methods for expenses
 */
enum class PaymentMethod(val displayName: String) {
    CASH("Cash"),
    BANK_CARD("Bank Card"),
    CHEQUE("Cheque");
    
    companion object {
        /**
         * Get PaymentMethod by display name
         */
        fun fromDisplayName(displayName: String): PaymentMethod? {
            return values().find { it.displayName == displayName }
        }
        
        /**
         * Get all payment methods as a list of display names
         */
        fun getAllDisplayNames(): List<String> {
            return values().map { it.displayName }
        }
    }
}
