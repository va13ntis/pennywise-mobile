package com.pennywise.app.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pennywise.app.R
import com.pennywise.app.presentation.theme.expense_red
import com.pennywise.app.presentation.theme.income_green
import com.pennywise.app.presentation.theme.neutral_gray
import com.pennywise.app.presentation.util.CurrencyFormatter
import com.pennywise.app.presentation.viewmodel.HomeViewModel
import kotlin.math.abs

/**
 * Monthly summary card component that displays income, expenses, and net balance
 */
@Composable
fun MonthlySummaryCard(
    totalIncome: Double,
    totalExpenses: Double,
    currency: String = "",
    currencyConversionEnabled: Boolean = false,
    originalCurrency: String = "",
    conversionState: HomeViewModel.ConversionState = HomeViewModel.ConversionState.Idle,
    onConvertAmount: (Double) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val netBalance = totalIncome - totalExpenses
    val isPositive = netBalance >= 0
    val context = LocalContext.current
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Net Balance Section
            AnimatedContent(
                targetState = netBalance,
                transitionSpec = {
                    ContentTransform(
                        targetContentEnter = slideInVertically(
                            animationSpec = tween(300)
                        ) { height -> height } + fadeIn(
                            animationSpec = tween(300)
                        ),
                        initialContentExit = slideOutVertically(
                            animationSpec = tween(300)
                        ) { height -> -height } + fadeOut(
                            animationSpec = tween(300)
                        )
                    )
                }
            ) { balance ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.net_balance),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = CurrencyFormatter.formatAmount(balance, currency, context),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isPositive) income_green else expense_red,
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            // Currency conversion display
            if (currencyConversionEnabled && originalCurrency.isNotEmpty() && originalCurrency != currency) {
                Spacer(modifier = Modifier.height(8.dp))
                
                // Trigger conversion for total expenses
                LaunchedEffect(totalExpenses, originalCurrency, currency) {
                    if (totalExpenses > 0) {
                        onConvertAmount(totalExpenses)
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
                        
                        if (conversionState.isUsingCachedRate) {
                            Text(
                                text = stringResource(R.string.using_cached_rate),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Income and Expenses Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Income Column
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.income),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    AnimatedContent(
                        targetState = totalIncome,
                        transitionSpec = {
                            ContentTransform(
                                targetContentEnter = slideInVertically(
                                    animationSpec = tween(300)
                                ) { height -> height } + fadeIn(
                                    animationSpec = tween(300)
                                ),
                                initialContentExit = slideOutVertically(
                                    animationSpec = tween(300)
                                ) { height -> -height } + fadeOut(
                                    animationSpec = tween(300)
                                )
                            )
                        }
                    ) { income ->
                        Text(
                            text = CurrencyFormatter.formatAmount(income, currency, context),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Medium,
                            color = income_green,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                
                // Divider
                Text(
                    text = "•",
                    style = MaterialTheme.typography.titleLarge,
                    color = neutral_gray
                )
                
                // Expenses Column
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.expenses),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    AnimatedContent(
                        targetState = totalExpenses,
                        transitionSpec = {
                            ContentTransform(
                                targetContentEnter = slideInVertically(
                                    animationSpec = tween(300)
                                ) { height -> height } + fadeIn(
                                    animationSpec = tween(300)
                                ),
                                initialContentExit = slideOutVertically(
                                    animationSpec = tween(300)
                                ) { height -> -height } + fadeOut(
                                    animationSpec = tween(300)
                                )
                            )
                        }
                    ) { expenses ->
                        Text(
                            text = CurrencyFormatter.formatAmount(expenses, currency, context),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Medium,
                            color = expense_red,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// Note: Currency formatting is now handled by CurrencyFormatter utility class
