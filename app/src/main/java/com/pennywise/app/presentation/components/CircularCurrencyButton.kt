package com.pennywise.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pennywise.app.domain.model.Currency

/**
 * A circular button component for currency selection that displays the currency code
 * and symbol in a compact, visually appealing format.
 */
@Composable
fun CircularCurrencyButton(
    selectedCurrency: Currency?,
    onCurrencyClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val currencyCode = selectedCurrency?.code ?: "USD"
    val currencySymbol = selectedCurrency?.symbol ?: "$"
    
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(
                color = if (enabled) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
            .border(
                width = 1.dp,
                color = if (enabled) {
                    MaterialTheme.colorScheme.outline
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                shape = CircleShape
            )
            .clickable(enabled = enabled) { onCurrencyClick() }
            .semantics {
                contentDescription = "Select currency. Currently selected: $currencyCode"
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Currency symbol
            Text(
                text = currencySymbol,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            // Currency code
            Text(
                text = currencyCode,
                style = MaterialTheme.typography.labelSmall,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        // Dropdown indicator
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = if (enabled) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier
                .size(16.dp)
                .align(Alignment.BottomEnd)
                .padding(4.dp)
        )
    }
}

/**
 * A variant of the circular currency button with a more compact design
 * suitable for inline placement with form fields.
 */
@Composable
fun CompactCircularCurrencyButton(
    selectedCurrency: Currency?,
    onCurrencyClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val currencyCode = selectedCurrency?.code ?: "USD"
    val currencySymbol = selectedCurrency?.symbol ?: "$"
    
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(
                color = if (enabled) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
            .border(
                width = 1.dp,
                color = if (enabled) {
                    MaterialTheme.colorScheme.outline
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                shape = CircleShape
            )
            .clickable(enabled = enabled) { onCurrencyClick() }
            .semantics {
                contentDescription = "Select currency. Currently selected: $currencyCode"
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Currency symbol
            Text(
                text = currencySymbol,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            // Currency code
            Text(
                text = currencyCode,
                style = MaterialTheme.typography.labelSmall,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        // Dropdown indicator
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = if (enabled) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier
                .size(12.dp)
                .align(Alignment.BottomEnd)
                .padding(2.dp)
        )
    }
}
