package com.pennywise.app.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.pennywise.app.R
import com.pennywise.app.domain.model.PaymentMethod
import com.pennywise.app.presentation.theme.expense_red
import com.pennywise.app.presentation.theme.income_green
import com.pennywise.app.presentation.theme.neutral_gray
import com.pennywise.app.presentation.util.CurrencyFormatter
import com.pennywise.app.presentation.utils.DateFormatter
import com.pennywise.app.presentation.viewmodel.HomeViewModel
import kotlin.math.abs
import java.time.YearMonth

/**
 * Get localized payment method name
 */
@Composable
private fun getLocalizedPaymentMethodName(paymentMethod: PaymentMethod, context: android.content.Context): String {
    return when (paymentMethod) {
        PaymentMethod.CASH -> context.getString(R.string.payment_method_cash)
        PaymentMethod.CHEQUE -> context.getString(R.string.payment_method_cheque)
        PaymentMethod.CREDIT_CARD -> "Credit Card"
    }
}

/**
 * Get icon for payment method
 */
@Composable
private fun getPaymentMethodIcon(paymentMethod: PaymentMethod): androidx.compose.ui.graphics.vector.ImageVector {
    return when (paymentMethod) {
        PaymentMethod.CASH -> Icons.Default.AccountBalanceWallet
        PaymentMethod.CHEQUE -> Icons.Default.Receipt
        PaymentMethod.CREDIT_CARD -> Icons.Default.CreditCard
    }
}

/**
 * Pill button for payment method selection with icon and tooltip
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentMethodPillButton(
    paymentMethod: PaymentMethod?,
    isSelected: Boolean,
    onClick: () -> Unit,
    context: android.content.Context,
    modifier: Modifier = Modifier
) {
    val tooltipState = rememberTooltipState()
    
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            Text(
                text = paymentMethod?.let { getLocalizedPaymentMethodName(it, context) } 
                    ?: context.getString(R.string.all_payment_methods)
            )
        },
        state = tooltipState,
        modifier = modifier
    ) {
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isSelected) 
                    MaterialTheme.colorScheme.primary 
                else 
                    Color.Transparent
            ),
            border = if (!isSelected) 
                BorderStroke(
                    1.dp, 
                    MaterialTheme.colorScheme.outline
                ) 
            else 
                null,
            modifier = Modifier.size(48.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
        ) {
            if (paymentMethod != null) {
                Icon(
                    imageVector = getPaymentMethodIcon(paymentMethod),
                    contentDescription = getLocalizedPaymentMethodName(paymentMethod, context),
                    modifier = Modifier.size(20.dp),
                    tint = if (isSelected) 
                        MaterialTheme.colorScheme.onPrimary 
                    else 
                        MaterialTheme.colorScheme.onSurface
                )
            } else {
                Text(
                    text = context.getString(R.string.all),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) 
                        MaterialTheme.colorScheme.onPrimary 
                    else 
                        MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * Monthly summary card component that displays total expenses with payment method filtering
 */
@Composable
fun MonthlySummaryCard(
    totalExpenses: Double,
    currentMonth: YearMonth,
    currency: String = "",
    currencyConversionEnabled: Boolean = false,
    originalCurrency: String = "",
    conversionState: HomeViewModel.ConversionState = HomeViewModel.ConversionState.Idle,
    onConvertAmount: (Double) -> Unit = {},
    onPaymentMethodFilterChanged: (PaymentMethod?) -> Unit = {},
    onPreviousMonth: () -> Unit = {},
    onNextMonth: () -> Unit = {},
    onCurrentMonth: () -> Unit = {},
    onBeginningOfYear: () -> Unit = {},
    onEndOfYear: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedPaymentMethod by remember { mutableStateOf<PaymentMethod?>(null) }
    
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
            // Music Player Style Navigation Controls (at top) - RTL aware
            val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Beginning of Year Button (⏮ - double left arrow)
                Button(
                    onClick = onBeginningOfYear,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent
                    ),
                    border = BorderStroke(
                        1.dp, 
                        MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.size(40.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = if (isRtl) Icons.Default.KeyboardDoubleArrowRight else Icons.Default.KeyboardDoubleArrowLeft,
                        contentDescription = stringResource(R.string.beginning_of_year),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                // Previous Month Button (◀ - single left arrow)
                Button(
                    onClick = onPreviousMonth,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent
                    ),
                    border = BorderStroke(
                        1.dp, 
                        MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.size(40.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = if (isRtl) Icons.Default.KeyboardArrowRight else Icons.Default.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.previous_month),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                // Current Month Button (disabled when already in current month)
                val isCurrentMonth = currentMonth == YearMonth.now()
                Button(
                    onClick = onCurrentMonth,
                    enabled = !isCurrentMonth,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent
                    ),
                    border = BorderStroke(
                        1.dp, 
                        if (isCurrentMonth) MaterialTheme.colorScheme.outline.copy(alpha = 0.3f) 
                        else MaterialTheme.colorScheme.outline
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Today,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (isCurrentMonth) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.current_month),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isCurrentMonth) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                
                // Next Month Button (▶ - single right arrow)
                Button(
                    onClick = onNextMonth,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent
                    ),
                    border = BorderStroke(
                        1.dp, 
                        MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.size(40.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = if (isRtl) Icons.Default.KeyboardArrowLeft else Icons.Default.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.next_month),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                // End of Year Button (⏭ - double right arrow)
                Button(
                    onClick = onEndOfYear,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent
                    ),
                    border = BorderStroke(
                        1.dp, 
                        MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.size(40.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = if (isRtl) Icons.Default.KeyboardDoubleArrowLeft else Icons.Default.KeyboardDoubleArrowRight,
                        contentDescription = stringResource(R.string.end_of_year),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Month/Year and Total Expense Amount (aligned to sides)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Month/Year on the left
                Text(
                    text = DateFormatter.formatMonthYear(context, currentMonth),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // Total Expenses Display on the right
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
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = expense_red,
                        textAlign = TextAlign.End
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
            
            // Payment Method Filter (at bottom)
            Spacer(modifier = Modifier.height(16.dp))
            
            // Payment method filter pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // All payment methods pill
                PaymentMethodPillButton(
                    paymentMethod = null,
                    isSelected = selectedPaymentMethod == null,
                    onClick = {
                        selectedPaymentMethod = null
                        onPaymentMethodFilterChanged(null)
                    },
                    context = context
                )
                
                // Individual payment method pills
                PaymentMethod.values().forEach { paymentMethod ->
                    PaymentMethodPillButton(
                        paymentMethod = paymentMethod,
                        isSelected = selectedPaymentMethod == paymentMethod,
                        onClick = {
                            selectedPaymentMethod = paymentMethod
                            onPaymentMethodFilterChanged(paymentMethod)
                        },
                        context = context
                    )
                }
            }
        }
    }
}

// Note: Currency formatting is now handled by CurrencyFormatter utility class
