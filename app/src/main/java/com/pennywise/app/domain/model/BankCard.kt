package com.pennywise.app.domain.model

import java.util.Date

/**
 * Domain model representing a bank card
 */
data class BankCard(
    val id: Long = 0,
    val alias: String, // User-friendly name for the card (e.g., "My Visa Card")
    val lastFourDigits: String, // Last 4 digits of the card (e.g., "1234")
    val paymentDay: Int, // Day of the month when payment is due (1-31)
    val isActive: Boolean = true,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)

/**
 * Enum representing different card types
 */
enum class CardType(val displayName: String) {
    VISA("Visa"),
    MASTERCARD("Mastercard"),
    AMERICAN_EXPRESS("American Express"),
    DISCOVER("Discover"),
    OTHER("Other");
    
    companion object {
        /**
         * Get CardType by display name
         */
        fun fromDisplayName(displayName: String): CardType? {
            return values().find { it.displayName == displayName }
        }
        
        /**
         * Get all card types as a list of display names
         */
        fun getAllDisplayNames(): List<String> {
            return values().map { it.displayName }
        }
    }
}
