package com.pennywise.app.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pennywise.app.R
import com.pennywise.app.domain.model.Transaction
import com.pennywise.app.domain.model.SplitPaymentInstallment
import com.pennywise.app.presentation.theme.expense_red
import com.pennywise.app.presentation.theme.income_green
import com.pennywise.app.presentation.util.CategoryMapper

/**
 * Specialized section for recurring expenses that appears at the top of the home screen
 * Aligned with weekly summary card design for consistency
 */
@Composable
fun RecurringExpensesSection(
    transactions: List<Transaction>,
    splitPaymentInstallments: List<SplitPaymentInstallment> = emptyList(),
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    if (transactions.isEmpty() && splitPaymentInstallments.isEmpty()) {
        return
    }
    
    var isExpanded by remember { mutableStateOf(false) }
    
    // Calculate total
    val totalAmount = transactions.sumOf { it.amount } + splitPaymentInstallments.sumOf { it.amount }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header - matches weekly card style
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Title
                Text(
                    text = stringResource(R.string.recurring_expenses),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // Right: Amount and expand/collapse arrow
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Amount
                    Text(
                        text = "$currencySymbol${String.format("%.2f", totalAmount)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = expense_red
                    )
                    
                    // Expand/collapse triangle
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = stringResource(if (isExpanded) R.string.collapse else R.string.expand),
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Expanded content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(
                    animationSpec = tween(300)
                ) + fadeIn(
                    animationSpec = tween(300)
                ),
                exit = shrinkVertically(
                    animationSpec = tween(300)
                ) + fadeOut(
                    animationSpec = tween(300)
                )
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Display recurring transactions
                    transactions.sortedByDescending { it.date }.forEach { transaction ->
                        RecurringTransactionItem(
                            transaction = transaction,
                            currencySymbol = currencySymbol
                        )
                        
                        if (transaction != transactions.last() || splitPaymentInstallments.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    
                    // Display split payment installments
                    splitPaymentInstallments.sortedBy { it.dueDate }.forEach { installment ->
                        SplitPaymentInstallmentItem(
                            installment = installment,
                            currencySymbol = currencySymbol
                        )
                        
                        if (installment != splitPaymentInstallments.last()) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}


/**
 * Specialized transaction item for recurring expenses
 * Aligned with regular transaction item style
 */
@Composable
private fun RecurringTransactionItem(
    transaction: Transaction,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Category emoji and description
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Category icon background with recurring indicator
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🔄",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Description and category
            Column {
                Text(
                    text = transaction.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (transaction.category.isNotEmpty()) {
                    Text(
                        text = CategoryMapper.getLocalizedCategory(transaction.category),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        // Right: Amount
        Text(
            text = "$currencySymbol${String.format("%.2f", transaction.amount)}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = expense_red
        )
    }
}

/**
 * Specialized item for split payment installments
 * Aligned with regular transaction item style
 */
@Composable
private fun SplitPaymentInstallmentItem(
    installment: SplitPaymentInstallment,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Icon and details
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Split payment icon background
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "💳",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            // Description and status
            Column {
                Text(
                    text = installment.getFormattedDescription(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (installment.category.isNotEmpty()) {
                        Text(
                            text = CategoryMapper.getLocalizedCategory(installment.category),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = if (installment.isPaid) "✓ Paid" else "Pending",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (installment.isPaid) income_green else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (installment.isPaid) FontWeight.Medium else FontWeight.Normal
                    )
                }
            }
        }
        
        // Right: Amount
        Text(
            text = "$currencySymbol${String.format("%.2f", installment.amount)}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = if (installment.isPaid) MaterialTheme.colorScheme.onSurfaceVariant else expense_red
        )
    }
}
