package com.pennywise.app.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pennywise.app.R
import com.pennywise.app.domain.model.Transaction
import com.pennywise.app.presentation.theme.expense_red
import com.pennywise.app.presentation.theme.income_green
import com.pennywise.app.presentation.util.CurrencyFormatter
import com.pennywise.app.presentation.util.LocaleFormatter
import com.pennywise.app.presentation.viewmodel.HomeViewModel
import kotlin.math.abs

/**
 * Specialized section for recurring expenses that appears at the top of the home screen
 */
@Composable
fun RecurringExpensesSection(
    transactions: List<Transaction>,
    currency: String = "",
    currencyConversionEnabled: Boolean = false,
    originalCurrency: String = "",
    conversionState: HomeViewModel.ConversionState = HomeViewModel.ConversionState.Idle,
    onConvertAmount: (Double) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (transactions.isEmpty()) {
        return
    }
    
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    // Calculate total only when transactions change - prevents unnecessary recomposition
    val totalAmount by remember(transactions) {
        derivedStateOf<Double> { transactions.sumOf { it.amount } }
    }
    
    // Format amount only when total or currency changes - prevents unnecessary recomposition
    val formattedTotal by remember(totalAmount, currency) {
        derivedStateOf<String> {
            CurrencyFormatter.formatAmount(totalAmount, currency, context)
        }
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            // Header with special styling for recurring expenses
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Title and total amount
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.recurring_expenses),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = stringResource(R.string.recurring_expenses),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formattedTotal,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = expense_red,
                        textAlign = TextAlign.Start
                    )
                    
                    // Currency conversion display
                    if (currencyConversionEnabled && originalCurrency.isNotEmpty() && originalCurrency != currency) {
                        Spacer(modifier = Modifier.height(2.dp))
                        
                        // Trigger conversion for total amount
                        LaunchedEffect(totalAmount, originalCurrency, currency) {
                            if (totalAmount > 0) {
                                onConvertAmount(totalAmount)
                            }
                        }
                        
                        when (conversionState) {
                            is HomeViewModel.ConversionState.Loading -> {
                                Text(
                                    text = stringResource(R.string.loading),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            is HomeViewModel.ConversionState.Success -> {
                                val originalFormatted = CurrencyFormatter.formatAmount(
                                    conversionState.originalAmount, 
                                    originalCurrency,
                                    context
                                )
                                val convertedFormatted = CurrencyFormatter.formatAmount(
                                    conversionState.convertedAmount, 
                                    currency,
                                    context
                                )
                                
                                Text(
                                    text = "$originalFormatted → $convertedFormatted",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            is HomeViewModel.ConversionState.Error -> {
                                Text(
                                    text = stringResource(R.string.conversion_unavailable),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            else -> { /* Idle state, do nothing */ }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Expand/collapse icon
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) 
                        stringResource(R.string.collapse) 
                    else 
                        stringResource(R.string.expand),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            // Animated content for transaction list
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(
                    animationSpec = tween(300)
                ),
                exit = shrinkVertically(
                    animationSpec = tween(300)
                )
            ) {
                Column {
                    Divider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    
                    transactions.forEachIndexed { index, transaction ->
                        RecurringTransactionItem(
                            transaction = transaction,
                            currency = currency
                        )
                        
                        // Add divider between items (except for the last one)
                        if (index < transactions.size - 1) {
                            Divider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Specialized transaction item for recurring expenses with enhanced styling
 */
@Composable
fun RecurringTransactionItem(
    transaction: Transaction,
    currency: String = "",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Format amount only when transaction amount or currency changes - prevents unnecessary recomposition
    val formattedAmount by remember(transaction.amount, currency) {
        derivedStateOf<String> {
            CurrencyFormatter.formatAmount(transaction.amount, currency, context)
        }
    }
    
    // Format date only when transaction date changes - prevents unnecessary recomposition
    val formattedDate by remember(transaction.date) {
        derivedStateOf<String> {
            LocaleFormatter.formatTransactionDate(transaction.date, context)
        }
    }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        // Transaction details
        Column(
            modifier = Modifier.weight(1f)
        ) {
            // Title row with description and recurring badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Text(
                    text = transaction.description,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Recurring badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "🔄",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            // Date
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            
            // Category and frequency row
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (transaction.category.isNotEmpty()) {
                    Text(
                        text = transaction.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // Show recurring period if available
                transaction.recurringPeriod?.let { period ->
                    if (transaction.category.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = formatRecurringPeriod(period),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Amount
        Text(
            text = formattedAmount,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = expense_red,
            textAlign = TextAlign.End
        )
    }
}

// Note: Currency formatting is now handled by CurrencyFormatter utility class

/**
 * Format recurring period for display using localized strings
 */
@Composable
private fun formatRecurringPeriod(period: com.pennywise.app.domain.model.RecurringPeriod): String {
    return when (period) {
        com.pennywise.app.domain.model.RecurringPeriod.DAILY -> stringResource(R.string.recurring_daily)
        com.pennywise.app.domain.model.RecurringPeriod.WEEKLY -> stringResource(R.string.recurring_weekly)
        com.pennywise.app.domain.model.RecurringPeriod.MONTHLY -> stringResource(R.string.recurring_monthly)
        com.pennywise.app.domain.model.RecurringPeriod.YEARLY -> stringResource(R.string.recurring_yearly)
    }
}
