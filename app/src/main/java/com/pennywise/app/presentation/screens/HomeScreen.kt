package com.pennywise.app.presentation.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pennywise.app.R
import com.pennywise.app.domain.model.Transaction
import com.pennywise.app.presentation.components.RecurringExpensesSection
import com.pennywise.app.presentation.theme.expense_red
import com.pennywise.app.presentation.util.CurrencyFormatter
import com.pennywise.app.presentation.util.CategoryMapper
import com.pennywise.app.presentation.util.LocaleFormatter
import com.pennywise.app.presentation.viewmodel.HomeViewModel
import com.pennywise.app.presentation.viewmodel.SettingsViewModel
import com.pennywise.app.domain.model.PaymentMethodConfig
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.HorizontalDivider
import java.time.YearMonth

/**
 * Data class for payment method summary with billing cycle info
 */
data class PaymentMethodSummary(
    val paymentMethod: com.pennywise.app.domain.model.PaymentMethod,
    val paymentMethodConfigId: Long? = null, // For credit cards, identifies which specific card
    val displayName: String,
    val totalAmount: Double,
    val billingCycleText: String? = null,
    val accentColor: Color
)

/**
 * Modern home screen with Material 3 design
 * Features monthly summary, weekly breakdown, and quick actions
 * Uses real transaction data from HomeViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddExpense: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    // State collection from real ViewModel
    val currentMonth by viewModel.currentMonth.collectAsState()
    val totalExpenses by viewModel.totalExpenses.collectAsState()
    val transactionsByWeek by viewModel.transactionsByWeek.collectAsState()
    val recurringTransactions by viewModel.recurringTransactions.collectAsState()
    val splitPaymentInstallments by viewModel.splitPaymentInstallments.collectAsState()
    val currencyCode by viewModel.currency.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    // Payment method configs and filtering state
    val paymentMethodConfigs by settingsViewModel.paymentMethodConfigs.collectAsState(initial = emptyList())
    // Note: selectedPaymentMethodFilter is managed internally by the ViewModel
    // Store the full summary to support filtering by specific credit cards
    var selectedPaymentMethodFilter by remember { mutableStateOf<PaymentMethodSummary?>(null) }
    
    // Refresh data when the screen becomes visible (e.g., returning from AddExpense)
    LaunchedEffect(Unit) {
        println("🔄 HomeScreen: Screen became visible, refreshing data")
        viewModel.refreshData()
    }
    
    // Convert currency code to symbol
    val currencySymbol = remember(currencyCode) {
        CurrencyFormatter.getCurrencySymbol(currencyCode)
    }
    
    // Get context for date formatting
    val context = LocalContext.current
    
    // Compute payment method summaries with billing cycle info
    val paymentMethodSummaries = remember(transactionsByWeek, recurringTransactions, currentMonth, paymentMethodConfigs, context) {
        computePaymentMethodSummaries(
            transactions = transactionsByWeek.values.flatten(),
            recurringTransactions = recurringTransactions,
            paymentMethodConfigs = paymentMethodConfigs,
            currentMonth = currentMonth,
            context = context
        )
    }
    
    // Local state for week expansion (not in ViewModel)
    var expandedWeeks by remember { mutableStateOf<Set<Int>>(emptySet()) }
    
    // UI state
    val lazyListState = rememberLazyListState()
    
    // Filter transactions by selected payment method (and specific credit card if applicable)
    val filteredWeeks: Map<Int, List<Transaction>> = if (selectedPaymentMethodFilter == null) {
        transactionsByWeek
    } else {
        transactionsByWeek.mapValues { entry: Map.Entry<Int, List<Transaction>> ->
            entry.value.filter { transaction: Transaction -> 
                val matchesPaymentMethod = transaction.paymentMethod == selectedPaymentMethodFilter?.paymentMethod
                // For credit cards, also check the specific card config ID
                val matchesConfigId = if (transaction.paymentMethod == com.pennywise.app.domain.model.PaymentMethod.CREDIT_CARD) {
                    transaction.paymentMethodConfigId == selectedPaymentMethodFilter?.paymentMethodConfigId
                } else {
                    true // For non-credit card methods, config ID doesn't matter
                }
                matchesPaymentMethod && matchesConfigId
            }
        }
    }
    
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }
    
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Main Add Expense button
                FloatingActionButton(
                    onClick = onAddExpense,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape,
                    modifier = Modifier.size(72.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.add_expense),
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                // Smaller Settings button underneath
                FloatingActionButton(
                    onClick = onNavigateToSettings,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.nav_settings),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            // Billing-Aware Summary Card
            item {
                BillingAwareSummaryCard(
                    currentMonth = currentMonth,
                    totalAmount = totalExpenses,
                    currencySymbol = currencySymbol,
                    paymentMethodSummaries = paymentMethodSummaries,
                    selectedPaymentMethod = selectedPaymentMethodFilter,
                    onPreviousMonth = { viewModel.changeMonth(-1) },
                    onNextMonth = { viewModel.changeMonth(1) },
                    onPaymentMethodClick = { summary ->
                        selectedPaymentMethodFilter = if (selectedPaymentMethodFilter == summary) null else summary
                    }
                )
            }
            
            // Recurring Expenses Section (appears after top summary)
            if (recurringTransactions.isNotEmpty() || splitPaymentInstallments.isNotEmpty()) {
                item {
                    RecurringExpensesSection(
                        transactions = recurringTransactions,
                        splitPaymentInstallments = splitPaymentInstallments,
                        currencySymbol = currencySymbol
                    )
                }
            }
            
            // Empty state message when no transactions
            if (transactionsByWeek.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.no_transactions_message),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
            
            // Weekly Summary Cards (sorted in descending order - most recent first)
            items(
                items = filteredWeeks.entries.sortedByDescending { it.key },
                key = { it.key }
            ) { (weekNumber, transactions) ->
                WeeklySummaryCard(
                    weekNumber = weekNumber,
                    transactions = transactions,
                    currencySymbol = currencySymbol,
                    weekTotal = transactions.sumOf { it.amount },
                    isExpanded = expandedWeeks.contains(weekNumber),
                    onToggleExpansion = {
                        expandedWeeks = if (expandedWeeks.contains(weekNumber)) {
                            (expandedWeeks - weekNumber) as Set<Int>
                        } else {
                            (expandedWeeks + weekNumber) as Set<Int>
                        }
                    }
                )
            }
            
            // Bottom spacing for FAB
            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

/**
 * Top summary card with month navigation and total amount
 */
@Composable
private fun TopSummaryCard(
    currentMonth: YearMonth,
    totalAmount: Double,
    currencySymbol: String,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Month navigation
            MonthNavigationRow(
                currentMonth = currentMonth,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
                isRtl = isRtl
            )
            
            // Right: Total amount
            AnimatedContent(
                targetState = totalAmount,
                transitionSpec = {
                    slideInHorizontally(
                        animationSpec = tween(300),
                        initialOffsetX = { if (isRtl) it else -it }
                    ) + fadeIn(animationSpec = tween(300)) togetherWith
                    slideOutHorizontally(
                        animationSpec = tween(300),
                        targetOffsetX = { if (isRtl) -it else it }
                    ) + fadeOut(animationSpec = tween(300))
                },
                label = "amount_animation"
            ) { amount ->
                Text(
                    text = "$currencySymbol${String.format("%.2f", amount)}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

/**
 * Inline month navigation row
 */
@Composable
private fun MonthNavigationRow(
    currentMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    isRtl: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Previous month button
        IconButton(
            onClick = onPreviousMonth,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = if (isRtl) Icons.Default.ChevronRight else Icons.Default.ChevronLeft,
                contentDescription = stringResource(R.string.previous_month),
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        
        // Month/year text using LocaleFormatter to get nominative case for Russian
        Text(
            text = LocaleFormatter.formatMonthYear(
                month = currentMonth.monthValue,
                year = currentMonth.year,
                context = context
            ),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        // Next month button
        IconButton(
            onClick = onNextMonth,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = if (isRtl) Icons.Default.ChevronLeft else Icons.Default.ChevronRight,
                contentDescription = stringResource(R.string.next_month),
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Weekly summary card with expand/collapse functionality
 * Now uses real Transaction data
 */
@Composable
private fun WeeklySummaryCard(
    weekNumber: Int,
    transactions: List<Transaction>,
    currencySymbol: String,
    weekTotal: Double,
    isExpanded: Boolean,
    onToggleExpansion: () -> Unit,
    modifier: Modifier = Modifier
) {
    
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
            // Week header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpansion() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Week number
                Text(
                    text = stringResource(R.string.week_format, weekNumber),
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
                        text = "$currencySymbol${String.format("%.2f", weekTotal)}",
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
            
            // Expanded content (transaction list)
            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                
                // Real transaction list
                transactions.sortedByDescending { it.date }.forEach { transaction ->
                    TransactionItem(
                        transaction = transaction,
                        currencySymbol = currencySymbol
                    )
                    
                    if (transaction != transactions.last()) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

/**
 * Individual transaction item in expanded weekly view
 * Uses real Transaction domain model
 */
@Composable
private fun TransactionItem(
    transaction: Transaction,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Payment method and description
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Payment method icon background
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        when (transaction.paymentMethod) {
                            com.pennywise.app.domain.model.PaymentMethod.CREDIT_CARD -> Color(0xFFFFD700).copy(alpha = 0.2f) // Gold
                            com.pennywise.app.domain.model.PaymentMethod.CASH -> Color(0xFF4CAF50).copy(alpha = 0.2f) // Green
                            com.pennywise.app.domain.model.PaymentMethod.CHEQUE -> Color(0xFF2196F3).copy(alpha = 0.2f) // Blue
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (transaction.paymentMethod) {
                        com.pennywise.app.domain.model.PaymentMethod.CREDIT_CARD -> "💳"
                        com.pennywise.app.domain.model.PaymentMethod.CASH -> "💵"
                        com.pennywise.app.domain.model.PaymentMethod.CHEQUE -> "🧾"
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            // Description
            Column {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = transaction.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    // Show payment number for split payments
                    if (transaction.installments != null && transaction.installments > 1) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            tonalElevation = 1.dp
                        ) {
                            Text(
                                text = "1/${transaction.installments}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                
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
 * Billing-aware summary card with payment method breakdown
 */
@Composable
private fun BillingAwareSummaryCard(
    currentMonth: YearMonth,
    totalAmount: Double,
    currencySymbol: String,
    paymentMethodSummaries: List<PaymentMethodSummary>,
    selectedPaymentMethod: PaymentMethodSummary?,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onPaymentMethodClick: (PaymentMethodSummary) -> Unit,
    modifier: Modifier = Modifier
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Month navigation and total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MonthNavigationRow(
                    currentMonth = currentMonth,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth,
                    isRtl = isRtl
                )
                
                Text(
                    text = "$currencySymbol${String.format("%.2f", totalAmount)}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }
            
            // Payment method summaries
            if (paymentMethodSummaries.isNotEmpty()) {
                HorizontalDivider()
                
                paymentMethodSummaries.forEach { summary ->
                    PaymentMethodSummaryItem(
                        summary = summary,
                        currencySymbol = currencySymbol,
                        isSelected = selectedPaymentMethod == summary,
                        onClick = { onPaymentMethodClick(summary) }
                    )
                }
            }
        }
    }
}

/**
 * Individual payment method summary item
 */
@Composable
private fun PaymentMethodSummaryItem(
    summary: PaymentMethodSummary,
    currencySymbol: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .background(
                if (isSelected) summary.accentColor.copy(alpha = 0.1f)
                else Color.Transparent
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Accent bar
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(40.dp)
                .background(summary.accentColor, RoundedCornerShape(2.dp))
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = summary.displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            
            summary.billingCycleText?.let { cycleText ->
                Text(
                    text = cycleText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Text(
            text = "$currencySymbol${String.format("%.2f", summary.totalAmount)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = if (isSelected) summary.accentColor else expense_red
        )
    }
}

/**
 * Compute payment method summaries with billing cycle information
 */
private fun computePaymentMethodSummaries(
    transactions: List<Transaction>,
    recurringTransactions: List<Transaction>,
    paymentMethodConfigs: List<PaymentMethodConfig>,
    currentMonth: YearMonth,
    context: android.content.Context
): List<PaymentMethodSummary> {
    val allTransactions = transactions + recurringTransactions
    val summaries = mutableListOf<PaymentMethodSummary>()
    
    // Handle CREDIT_CARD transactions - show separate entry for each card used
    val creditCardTransactions = allTransactions.filter { it.paymentMethod == com.pennywise.app.domain.model.PaymentMethod.CREDIT_CARD }
    
    // Group credit card transactions by paymentMethodConfigId
    val creditCardGroups = creditCardTransactions.groupBy { it.paymentMethodConfigId }
    
    creditCardGroups.forEach { (configId, transactionsForCard) ->
        val total = transactionsForCard.sumOf { it.amount }
        if (total > 0.0) {
            // Find the specific card config
            val cardConfig = configId?.let { id -> 
                paymentMethodConfigs.find { it.id == id }
            }
            
            val displayName = cardConfig?.alias?.ifBlank { "Credit Card" } ?: "Credit Card"
            val billingCycleText = cardConfig?.withdrawDay?.let { withdrawDay ->
                // Billing cycle: withdrawDay of current month → (withdrawDay - 1) of next month
                val cycleStart = currentMonth.atDay(withdrawDay)
                val cycleEnd = currentMonth.plusMonths(1).atDay(withdrawDay).minusDays(1)
                
                // Convert LocalDate to Date for formatting
                val cycleStartDate = java.util.Date.from(cycleStart.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant())
                val cycleEndDate = java.util.Date.from(cycleEnd.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant())
                
                "${LocaleFormatter.formatTransactionDate(cycleStartDate, context)} → ${LocaleFormatter.formatTransactionDate(cycleEndDate, context)}"
            }
            
            summaries.add(
                PaymentMethodSummary(
                    paymentMethod = com.pennywise.app.domain.model.PaymentMethod.CREDIT_CARD,
                    paymentMethodConfigId = configId,
                    displayName = displayName,
                    totalAmount = total,
                    billingCycleText = billingCycleText,
                    accentColor = Color(0xFFFFD700) // Gold
                )
            )
        }
    }
    
    // Handle CASH transactions
    val cashTotal = allTransactions
        .filter { it.paymentMethod == com.pennywise.app.domain.model.PaymentMethod.CASH }
        .sumOf { it.amount }
    
    if (cashTotal > 0.0) {
        summaries.add(
            PaymentMethodSummary(
                paymentMethod = com.pennywise.app.domain.model.PaymentMethod.CASH,
                displayName = "Cash",
                totalAmount = cashTotal,
                billingCycleText = null,
                accentColor = Color(0xFF4CAF50) // Green
            )
        )
    }
    
    // Handle CHEQUE transactions
    val chequeTotal = allTransactions
        .filter { it.paymentMethod == com.pennywise.app.domain.model.PaymentMethod.CHEQUE }
        .sumOf { it.amount }
    
    if (chequeTotal > 0.0) {
        summaries.add(
            PaymentMethodSummary(
                paymentMethod = com.pennywise.app.domain.model.PaymentMethod.CHEQUE,
                displayName = "Cheque",
                totalAmount = chequeTotal,
                billingCycleText = null,
                accentColor = Color(0xFF2196F3) // Blue
            )
        )
    }
    
    return summaries
}
