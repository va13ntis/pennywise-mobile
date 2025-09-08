package com.pennywise.app.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.pennywise.app.R
import com.pennywise.app.domain.model.Currency

/**
 * Currency selection dropdown component for settings screen
 */
@Composable
fun CurrencySelectionDropdown(
    currentCurrency: String,
    onCurrencySelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val currencies = remember { Currency.getSortedByPopularity() }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Dropdown trigger
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val currentCurrencyObj = Currency.fromCode(currentCurrency)
                val displayText = currentCurrencyObj?.let { 
                    "${it.code} - ${it.symbol} - ${it.displayName}" 
                } ?: currentCurrency
                
                Text(
                    text = displayText,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = stringResource(R.string.select_currency),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Currency selection dialog
        if (expanded) {
            AlertDialog(
                onDismissRequest = { expanded = false },
                title = {
                    Text(
                        text = stringResource(R.string.select_currency),
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        currencies.forEach { currency ->
                            val isSelected = currency.code == currentCurrency
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onCurrencySelected(currency.code)
                                        expanded = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        onCurrencySelected(currency.code)
                                        expanded = false
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                Column(modifier = Modifier.weight(1f)) {
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
                            }
                            
                            if (currency != currencies.last()) {
                                Divider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { expanded = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                },
                properties = DialogProperties(
                    dismissOnBackPress = true,
                    dismissOnClickOutside = true
                )
            )
        }
    }
}
