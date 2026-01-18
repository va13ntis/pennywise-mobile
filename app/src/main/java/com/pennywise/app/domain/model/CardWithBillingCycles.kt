package com.pennywise.app.domain.model

/**
 * Data class representing a payment method configuration (card) with its billing cycles
 * This is used to return a card along with its calculated billing cycles in a single object
 */
data class CardWithBillingCycles(
    val paymentMethodConfig: PaymentMethodConfig,
    val billingCycles: List<BillingCycle>
) {
    /**
     * Convenience property to get the card ID
     */
    val cardId: Long
        get() = paymentMethodConfig.id
    
    /**
     * Convenience property to get the card name/display name
     */
    val cardName: String
        get() = paymentMethodConfig.getDisplayName()
}



