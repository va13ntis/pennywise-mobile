package com.pennywise.app.presentation.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import com.pennywise.app.R
import com.pennywise.app.domain.model.Currency

/**
 * Confirmation dialog for currency changes
 */
@Composable
fun CurrencyChangeConfirmationDialog(
    @Suppress("UNUSED_PARAMETER") currentCurrency: String,
    newCurrency: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val newCurrencyObj = Currency.fromCode(newCurrency)
    
    val newDisplay = newCurrencyObj?.let { 
        "${it.code} - ${it.symbol} - ${it.displayName}" 
    } ?: newCurrency
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.change_default_currency),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = stringResource(
                    R.string.change_default_currency_confirmation,
                    newDisplay
                ),
                style = MaterialTheme.typography.bodyLarge
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(stringResource(R.string.change))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    )
}

