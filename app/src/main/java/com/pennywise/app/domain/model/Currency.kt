package com.pennywise.app.domain.model

/**
 * Enum representing supported currencies with their symbols, names, and popularity rankings
 */
enum class Currency(
    val code: String,
    val symbol: String,
    val displayName: String,
    val popularity: Int,
    val decimalPlaces: Int = 2
) {
    USD("USD", "$", "US Dollar", 1),
    EUR("EUR", "€", "Euro", 2),
    GBP("GBP", "£", "British Pound", 3),
    JPY("JPY", "¥", "Japanese Yen", 4, 0), // JPY has 0 decimal places
    CAD("CAD", "C$", "Canadian Dollar", 5),
    AUD("AUD", "A$", "Australian Dollar", 6),
    CHF("CHF", "CHF", "Swiss Franc", 7),
    CNY("CNY", "¥", "Chinese Yuan", 8),
    SEK("SEK", "kr", "Swedish Krona", 9),
    NOK("NOK", "kr", "Norwegian Krone", 10),
    DKK("DKK", "kr", "Danish Krone", 11),
    PLN("PLN", "zł", "Polish Złoty", 12),
    CZK("CZK", "Kč", "Czech Koruna", 13),
    HUF("HUF", "Ft", "Hungarian Forint", 14),
    RUB("RUB", "₽", "Russian Ruble", 15),
    TRY("TRY", "₺", "Turkish Lira", 16),
    BRL("BRL", "R$", "Brazilian Real", 17),
    INR("INR", "₹", "Indian Rupee", 18),
    KRW("KRW", "₩", "South Korean Won", 19, 0), // KRW has 0 decimal places
    SGD("SGD", "S$", "Singapore Dollar", 20),
    HKD("HKD", "HK$", "Hong Kong Dollar", 21),
    ILS("ILS", "₪", "Israeli Shekel", 22),
    AED("AED", "د.إ", "UAE Dirham", 23),
    SAR("SAR", "ر.س", "Saudi Riyal", 24),
    ZAR("ZAR", "R", "South African Rand", 25);

    companion object {
        /**
         * Get currency by code
         */
        fun fromCode(code: String): Currency? {
            return values().find { it.code == code.uppercase() }
        }

        /**
         * Get default currency (USD)
         */
        fun getDefault(): Currency = USD

        /**
         * Get currencies sorted by popularity (most popular first)
         */
        fun getSortedByPopularity(): List<Currency> {
            return values().sortedBy { it.popularity }
        }

        /**
         * Get most popular currencies (top 10)
         */
        fun getMostPopular(): List<Currency> {
            return getSortedByPopularity().take(10)
        }

        /**
         * Format amount with currency symbol
         */
        fun formatAmount(amount: Double, currency: Currency): String {
            val formattedAmount = when (currency.decimalPlaces) {
                0 -> amount.toInt().toString()
                else -> String.format("%.${currency.decimalPlaces}f", amount)
            }
            return "${currency.symbol}$formattedAmount"
        }

        /**
         * Get display text for currency selection (code - symbol - name)
         */
        fun getDisplayText(currency: Currency): String {
            return "${currency.code} - ${currency.symbol} - ${currency.displayName}"
        }

        /**
         * Validate currency code
         */
        fun isValidCode(code: String): Boolean {
            return fromCode(code) != null
        }
    }
}
