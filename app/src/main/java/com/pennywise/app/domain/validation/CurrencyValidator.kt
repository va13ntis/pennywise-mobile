package com.pennywise.app.domain.validation

import com.pennywise.app.domain.model.Currency
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for validating currency codes and providing fallback mechanisms
 */
@Singleton
class CurrencyValidator @Inject constructor() {
    
    companion object {
        private const val DEFAULT_CURRENCY = "USD"
        private const val CURRENCY_CODE_LENGTH = 3
    }
    
    /**
     * Validates a currency code and returns a validation result
     */
    fun validateCurrencyCode(code: String): ValidationResult {
        return when {
            code.isBlank() -> {
                Timber.w("Currency validation failed: code is blank")
                ValidationResult.Error("Currency code cannot be empty")
            }
            code.length != CURRENCY_CODE_LENGTH -> {
                Timber.w("Currency validation failed: code length is ${code.length}, expected $CURRENCY_CODE_LENGTH")
                ValidationResult.Error("Currency code must be exactly $CURRENCY_CODE_LENGTH characters")
            }
            !Currency.isValidCode(code) -> {
                Timber.w("Currency validation failed: unsupported currency code: $code")
                ValidationResult.Error("Unsupported currency code: $code")
            }
            else -> {
                Timber.d("Currency validation successful: $code")
                ValidationResult.Success
            }
        }
    }
    
    /**
     * Gets a valid currency code or falls back to default (USD)
     */
    fun getValidCurrencyCodeOrFallback(code: String): String {
        return if (Currency.isValidCode(code)) {
            Timber.d("Using valid currency code: $code")
            code
        } else {
            Timber.w("Invalid currency code '$code', falling back to $DEFAULT_CURRENCY")
            DEFAULT_CURRENCY
        }
    }
    
    /**
     * Gets a valid Currency enum or falls back to default (USD)
     */
    fun getValidCurrencyOrFallback(code: String): Currency {
        return Currency.fromCode(code) ?: run {
            Timber.w("Invalid currency code '$code', falling back to $DEFAULT_CURRENCY")
            Currency.USD
        }
    }
    
    /**
     * Validates and formats an amount for a specific currency
     */
    fun validateAmountForCurrency(amount: Double, currency: Currency): ValidationResult {
        return when {
            amount.isNaN() || amount.isInfinite() -> {
                Timber.w("Amount validation failed: invalid amount $amount for currency ${currency.code}")
                ValidationResult.Error("Invalid amount")
            }
            amount < 0 -> {
                Timber.w("Amount validation failed: negative amount $amount for currency ${currency.code}")
                ValidationResult.Error("Amount cannot be negative")
            }
            currency.decimalPlaces == 0 && amount != amount.toInt().toDouble() -> {
                Timber.w("Amount validation failed: decimal places not allowed for ${currency.code}")
                ValidationResult.Error("${currency.displayName} does not support decimal places")
            }
            else -> {
                Timber.d("Amount validation successful: $amount for ${currency.code}")
                ValidationResult.Success
            }
        }
    }
    
    /**
     * Formats an amount for a specific currency with validation
     */
    fun formatAmountWithValidation(amount: Double, currencyCode: String): String {
        val currency = getValidCurrencyOrFallback(currencyCode)
        val validationResult = validateAmountForCurrency(amount, currency)
        
        return when (validationResult) {
            is ValidationResult.Success -> Currency.formatAmount(amount, currency)
            is ValidationResult.Error -> {
                Timber.e("Amount formatting failed: ${validationResult.message}")
                // Return a safe fallback format
                "${currency.symbol}0"
            }
        }
    }
    
    /**
     * Validates a currency code and provides detailed error information
     */
    fun validateCurrencyCodeWithDetails(code: String): DetailedValidationResult {
        return when {
            code.isBlank() -> {
                DetailedValidationResult.Error(
                    message = "Currency code cannot be empty",
                    errorType = CurrencyErrorType.EMPTY_CODE,
                    suggestedFix = "Please enter a valid 3-letter currency code (e.g., USD, EUR, GBP)"
                )
            }
            code.length != CURRENCY_CODE_LENGTH -> {
                DetailedValidationResult.Error(
                    message = "Currency code must be exactly $CURRENCY_CODE_LENGTH characters",
                    errorType = CurrencyErrorType.INVALID_LENGTH,
                    suggestedFix = "Please enter a 3-letter currency code (e.g., USD, EUR, GBP)"
                )
            }
            !Currency.isValidCode(code) -> {
                val suggestions = getSimilarCurrencyCodes(code)
                DetailedValidationResult.Error(
                    message = "Unsupported currency code: $code",
                    errorType = CurrencyErrorType.UNSUPPORTED_CODE,
                    suggestedFix = if (suggestions.isNotEmpty()) {
                        "Did you mean: ${suggestions.joinToString(", ")}?"
                    } else {
                        "Please select from the supported currencies"
                    }
                )
            }
            else -> {
                DetailedValidationResult.Success(Currency.fromCode(code)!!)
            }
        }
    }
    
    /**
     * Gets similar currency codes for suggestion purposes
     */
    private fun getSimilarCurrencyCodes(code: String): List<String> {
        val upperCode = code.uppercase()
        return Currency.values()
            .filter { currency ->
                currency.code.startsWith(upperCode) || 
                currency.code.contains(upperCode) ||
                currency.displayName.uppercase().contains(upperCode)
            }
            .take(3)
            .map { it.code }
    }
}

/**
 * Sealed class representing validation results
 */
sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}

/**
 * Detailed validation result with additional information
 */
sealed class DetailedValidationResult {
    data class Success(val currency: Currency) : DetailedValidationResult()
    data class Error(
        val message: String,
        val errorType: CurrencyErrorType,
        val suggestedFix: String
    ) : DetailedValidationResult()
}

/**
 * Enum representing different types of currency validation errors
 */
enum class CurrencyErrorType {
    EMPTY_CODE,
    INVALID_LENGTH,
    UNSUPPORTED_CODE,
    INVALID_AMOUNT
}
