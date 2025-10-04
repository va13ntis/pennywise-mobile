package com.pennywise.app.domain.validation

import android.content.Context
import com.pennywise.app.R
import com.pennywise.app.domain.model.Currency
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for handling currency-related errors and providing user-friendly messages
 */
@Singleton
class CurrencyErrorHandler @Inject constructor() {
    
    /**
     * Gets a user-friendly error message for currency validation errors
     */
    fun getUserFriendlyErrorMessage(
        errorType: CurrencyErrorType,
        invalidValue: String? = null,
        context: Context? = null
    ): String {
        return when (errorType) {
            CurrencyErrorType.EMPTY_CODE -> {
                context?.getString(R.string.currency_error_empty_code) 
                    ?: "Please select a currency"
            }
            CurrencyErrorType.INVALID_LENGTH -> {
                context?.getString(R.string.currency_error_invalid_length, invalidValue?.length ?: 0)
                    ?: "Currency code must be exactly 3 characters"
            }
            CurrencyErrorType.UNSUPPORTED_CODE -> {
                val code = invalidValue ?: "unknown"
                context?.getString(R.string.currency_error_unsupported_code, code)
                    ?: "Currency '$code' is not supported"
            }
            CurrencyErrorType.INVALID_AMOUNT -> {
                context?.getString(R.string.currency_error_invalid_amount)
                    ?: "Please enter a valid amount"
            }
        }
    }
    
    /**
     * Gets a user-friendly error message with suggestions for currency validation errors
     */
    fun getUserFriendlyErrorMessageWithSuggestions(
        errorType: CurrencyErrorType,
        invalidValue: String? = null,
        suggestions: List<String> = emptyList(),
        context: Context? = null
    ): String {
        val baseMessage = getUserFriendlyErrorMessage(errorType, invalidValue, context)
        
        return when (errorType) {
            CurrencyErrorType.UNSUPPORTED_CODE -> {
                if (suggestions.isNotEmpty()) {
                    val suggestionText = suggestions.joinToString(", ")
                    context?.getString(R.string.currency_error_with_suggestions, baseMessage, suggestionText)
                        ?: "$baseMessage. Did you mean: $suggestionText?"
                } else {
                    baseMessage
                }
            }
            else -> baseMessage
        }
    }
    
    /**
     * Handles currency validation error and logs it appropriately
     */
    fun handleCurrencyValidationError(
        errorType: CurrencyErrorType,
        invalidValue: String? = null,
        context: String = "Unknown"
    ) {
        val errorMessage = when (errorType) {
            CurrencyErrorType.EMPTY_CODE -> "Currency code is empty"
            CurrencyErrorType.INVALID_LENGTH -> "Currency code has invalid length: ${invalidValue?.length ?: 0}"
            CurrencyErrorType.UNSUPPORTED_CODE -> "Unsupported currency code: $invalidValue"
            CurrencyErrorType.INVALID_AMOUNT -> "Invalid amount for currency"
        }
        
        Timber.w("Currency validation error in $context: $errorMessage")
    }
    
    /**
     * Handles currency fallback and logs the action
     */
    fun handleCurrencyFallback(
        originalCode: String,
        fallbackCode: String = "USD",
        context: String = "Unknown"
    ) {
        Timber.w("Currency fallback in $context: '$originalCode' -> '$fallbackCode'")
    }
    
    /**
     * Handles currency formatting error and logs it
     */
    fun handleCurrencyFormattingError(
        amount: Double,
        currencyCode: String,
        error: String,
        context: String = "Unknown"
    ) {
        Timber.e("Currency formatting error in $context: amount=$amount, currency=$currencyCode, error=$error")
    }
    
    /**
     * Gets a recovery suggestion for currency errors
     */
    fun getRecoverySuggestion(errorType: CurrencyErrorType, context: Context? = null): String {
        return when (errorType) {
            CurrencyErrorType.EMPTY_CODE -> {
                context?.getString(R.string.currency_recovery_empty_code)
                    ?: "Please select a currency from the dropdown"
            }
            CurrencyErrorType.INVALID_LENGTH -> {
                context?.getString(R.string.currency_recovery_invalid_length)
                    ?: "Please enter a 3-letter currency code"
            }
            CurrencyErrorType.UNSUPPORTED_CODE -> {
                context?.getString(R.string.currency_recovery_unsupported_code)
                    ?: "Please select from the supported currencies"
            }
            CurrencyErrorType.INVALID_AMOUNT -> {
                context?.getString(R.string.currency_recovery_invalid_amount)
                    ?: "Please enter a valid positive number"
            }
        }
    }
    
    /**
     * Creates a comprehensive error report for debugging
     */
    fun createErrorReport(
        errorType: CurrencyErrorType,
        invalidValue: String? = null,
        context: String = "Unknown",
        additionalInfo: Map<String, Any> = emptyMap()
    ): String {
        val report = StringBuilder().apply {
            appendLine("Currency Error Report")
            appendLine("==================")
            appendLine("Error Type: $errorType")
            appendLine("Context: $context")
            if (invalidValue != null) {
                appendLine("Invalid Value: '$invalidValue'")
            }
            if (additionalInfo.isNotEmpty()) {
                appendLine("Additional Info:")
                additionalInfo.forEach { (key, value) ->
                    appendLine("  $key: $value")
                }
            }
            appendLine("Timestamp: ${System.currentTimeMillis()}")
        }
        
        Timber.e("Currency error report:\n${report}")
        return report.toString()
    }
    
    /**
     * Validates if a currency code change is safe (no data loss)
     */
    fun isCurrencyChangeSafe(
        @Suppress("UNUSED_PARAMETER") oldCurrency: Currency,
        @Suppress("UNUSED_PARAMETER") newCurrency: Currency
    ): Boolean {
        // Currency changes are generally safe, but we can add specific logic here
        // For example, checking if there are existing transactions with the old currency
        return true
    }
    
    /**
     * Gets a warning message for potentially unsafe currency changes
     */
    fun getCurrencyChangeWarning(
        oldCurrency: Currency,
        newCurrency: Currency,
        context: Context? = null
    ): String? {
        return if (!isCurrencyChangeSafe(oldCurrency, newCurrency)) {
            context?.getString(
                R.string.currency_change_warning,
                oldCurrency.displayName,
                newCurrency.displayName
            ) ?: "Changing from ${oldCurrency.displayName} to ${newCurrency.displayName} may affect existing data"
        } else {
            null
        }
    }
}
