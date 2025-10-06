package com.pennywise.app.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pennywise.app.domain.model.Currency

/**
 * Adapter class for currency dropdown items that provides consistent display and selection handling
 * across different screens (registration, expense form, settings, etc.)
 */
class CurrencyAdapter {
    
    /**
     * Creates a dropdown menu item for a currency with consistent styling and selection handling
     * 
     * @param currency The currency to display
     * @param isSelected Whether this currency is currently selected
     * @param onCurrencySelected Callback when this currency is selected
     * @param modifier Modifier for the dropdown menu item
     */
    @Composable
    fun CurrencyDropdownItem(
        currency: Currency,
        isSelected: Boolean,
        onCurrencySelected: (Currency) -> Unit,
        modifier: Modifier = Modifier
    ) {
        DropdownMenuItem(
            text = { 
                Column {
                    Text(
                        text = "${currency.code} - ${currency.symbol}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = currency.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            leadingIcon = {
                if (isSelected) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            },
            onClick = {
                onCurrencySelected(currency)
            },
            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 16.dp),
            modifier = modifier
        )
    }
    
    /**
     * Creates a list of currency dropdown items for an ExposedDropdownMenu
     * 
     * @param currencies List of currencies to display
     * @param selectedCurrency Currently selected currency
     * @param onCurrencySelected Callback when a currency is selected
     * @param onDismissRequest Callback when the dropdown should be dismissed
     */
    @Composable
    fun CurrencyDropdownMenu(
        currencies: List<Currency>,
        selectedCurrency: Currency?,
        onCurrencySelected: (Currency) -> Unit,
        onDismissRequest: () -> Unit
    ) {
        currencies.forEach { currency ->
            val isSelected = currency.code == selectedCurrency?.code
            
            CurrencyDropdownItem(
                currency = currency,
                isSelected = isSelected,
                onCurrencySelected = { selectedCurrency ->
                    onCurrencySelected(selectedCurrency)
                    onDismissRequest()
                }
            )
            
            // Add divider between items (except for the last item)
            if (currency != currencies.last()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
    
    /**
     * Creates a display text for the selected currency in the dropdown field
     * 
     * @param selectedCurrency The currently selected currency
     * @return Formatted display text
     */
    fun getDisplayText(selectedCurrency: Currency?): String {
        return selectedCurrency?.let { 
            "${it.code} - ${it.symbol} - ${it.displayName}" 
        } ?: "USD - $ - US Dollar"
    }
    
    /**
     * Creates a compact display text for the selected currency (code and symbol only)
     * 
     * @param selectedCurrency The currently selected currency
     * @return Compact display text
     */
    fun getCompactDisplayText(selectedCurrency: Currency?): String {
        return selectedCurrency?.let { 
            "${it.code} - ${it.symbol}" 
        } ?: "USD - $"
    }
    
    /**
     * Gets the list of currencies sorted by popularity
     * 
     * @return List of currencies sorted by popularity
     */
    fun getSortedCurrencies(): List<Currency> {
        return Currency.getSortedByPopularity()
    }
    
    /**
     * Gets the default currency
     * 
     * @return Default currency (USD)
     */
    fun getDefaultCurrency(): Currency {
        return Currency.getDefault()
    }
    
    /**
     * Finds a currency by its code
     * 
     * @param code Currency code (e.g., "USD", "EUR")
     * @return Currency object or null if not found
     */
    fun getCurrencyByCode(code: String): Currency? {
        return Currency.fromCode(code)
    }
}
