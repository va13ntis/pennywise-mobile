package com.pennywise.app.presentation.screens

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.pennywise.app.R
import com.pennywise.app.domain.model.Transaction
import com.pennywise.app.presentation.components.RecurringExpensesSection
import com.pennywise.app.presentation.util.CurrencyFormatter
import com.pennywise.app.presentation.util.CategoryMapper
import com.pennywise.app.presentation.util.PaymentMethodMapper
import com.pennywise.app.presentation.util.LocaleFormatter
import com.pennywise.app.presentation.viewmodel.HomeViewModel
import com.pennywise.app.presentation.viewmodel.SettingsViewModel
import com.pennywise.app.domain.model.PaymentMethodConfig
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.HorizontalDivider
import com.pennywise.app.domain.model.PaymentMethod
import com.pennywise.app.domain.model.TransactionType
import com.pennywise.app.domain.model.SplitPaymentInstallment
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Date
import java.util.Locale

/**
 * Data class for payment method summary with billing cycle info
 */
data class PaymentMethodSummary(
    val paymentMethod: PaymentMethod,
    val paymentMethodConfigId: Long? = null, // For credit cards, identifies which specific card
    val displayName: String,
    val totalAmount: Double,
    val billingCycleText: String? = null,
    val billingCycleStart: Date? = null,
    val billingCycleEnd: Date? = null,
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
    onEditExpense: (Long) -> Unit = {},
    onNavigateToCardStatement: (Long) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    // State collection from real ViewModel
    val currentMonth by viewModel.currentMonth.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val transactionsByWeek by viewModel.transactionsByWeek.collectAsState()
    val recurringTransactions by viewModel.recurringTransactions.collectAsState()
    val splitPaymentInstallments by viewModel.splitPaymentInstallments.collectAsState()
    val installmentParentTransactions by viewModel.installmentParentTransactions.collectAsState()
    val currencyCode by viewModel.currency.collectAsState()
    val convertedTransactionAmounts by viewModel.convertedTransactionAmounts.collectAsState()
    val convertedInstallmentAmounts by viewModel.convertedInstallmentAmounts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    // Payment method configs and filtering state
    val paymentMethodConfigs by settingsViewModel.paymentMethodConfigs.collectAsState(initial = emptyList())
    // Note: selectedPaymentMethodFilter is managed internally by the ViewModel
    // Store the full summary to support filtering by specific credit cards
    var selectedPaymentMethodFilter by remember { mutableStateOf<PaymentMethodSummary?>(null) }
    
    // Get context for date formatting
    val context = LocalContext.current
    val locale = context.resources.configuration.locales[0]
    val layoutDirection = LocalLayoutDirection.current

    
    val cardAccentColor = MaterialTheme.colorScheme.secondary
    // Compute payment method summaries with billing cycle info.
    // Use raw transactions (not transactionsByWeek) so credit cards can be filtered by billing cycle.
    val paymentMethodSummaries = remember(
        transactions,
        recurringTransactions,
        splitPaymentInstallments,
        installmentParentTransactions,
        currentMonth,
        paymentMethodConfigs,
        context,
        cardAccentColor,
        convertedTransactionAmounts,
        convertedInstallmentAmounts,
        layoutDirection
    ) {
        computePaymentMethodSummaries(
            transactions = transactions,
            recurringTransactions = recurringTransactions,
            splitPaymentInstallments = splitPaymentInstallments,
            transactionById = (transactions + recurringTransactions + installmentParentTransactions.values).associateBy { it.id },
            paymentMethodConfigs = paymentMethodConfigs,
            currentMonth = currentMonth,
            context = context,
            creditCardAccent = cardAccentColor,
            convertedTransactionAmounts = convertedTransactionAmounts,
            convertedInstallmentAmounts = convertedInstallmentAmounts,
            isRtl = layoutDirection == LayoutDirection.Rtl
        )
    }
    // Refresh data when the screen becomes visible (e.g., returning from AddExpense)
    LaunchedEffect(Unit) {
        viewModel.refreshData()
    }
    
    // Local state for week expansion (not in ViewModel)
    var expandedWeeks by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var transactionActionTarget by remember { mutableStateOf<Transaction?>(null) }
    var showTransactionActions by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    // UI state
    val lazyListState = rememberLazyListState()
    // When pager hasn't fired yet, use first summary so we never show "all" when a card is displayed
    val effectiveRecurringFilter = selectedPaymentMethodFilter ?: paymentMethodSummaries.firstOrNull()
    
    // Show transactions that match selected payment method and billing cycle (if available).
    val matchesPaymentMethodOnly: (Transaction) -> Boolean = { transaction ->
        effectiveRecurringFilter?.let { summary ->
            if (summary.paymentMethod == PaymentMethod.CREDIT_CARD) {
                transaction.paymentMethod == PaymentMethod.CREDIT_CARD &&
                    transaction.paymentMethodConfigId == summary.paymentMethodConfigId
            } else {
                transaction.paymentMethod == summary.paymentMethod
            }
        } ?: true
    }
    val matchesSelectedSummary: (Transaction) -> Boolean = { transaction ->
        selectedPaymentMethodFilter?.let { summary ->
            if (!matchesPaymentMethodOnly(transaction)) {
                return@let false
            }
            val cycleStart = summary.billingCycleStart?.time
            val cycleEnd = summary.billingCycleEnd?.time
            if (cycleStart != null && cycleEnd != null) {
                val billingTime = transaction.getBillingDate().time
                billingTime in cycleStart..cycleEnd
            } else {
                true
            }
        } ?: true
    }
    val selectedCycleStart = selectedPaymentMethodFilter?.billingCycleStart
    val selectedCycleEnd = selectedPaymentMethodFilter?.billingCycleEnd
    // When a credit card with billing cycle is selected, use raw transactions (which include
    // 4 months of data) so we capture the full billing cycle. transactionsByWeek filters by
    // calendar month and would exclude e.g. Jan 15-31 when cycle is Jan 15-Feb 14.
    val filteredWeeks: Map<Int, List<Transaction>> = if (selectedCycleStart != null && selectedCycleEnd != null) {
        transactions
            .filter { it.type == TransactionType.EXPENSE && !it.isRecurring && !it.hasDelayedBilling() }
            .filter(matchesSelectedSummary)
            .sortedByDescending { it.date }
            .groupBy { transaction ->
                getBillingCycleWeekNumber(transaction.getBillingDate(), selectedCycleStart)
            }
            .toSortedMap()
            .filterValues { it.isNotEmpty() }
    } else {
        transactionsByWeek
            .mapValues { entry ->
                entry.value.filter(matchesSelectedSummary)
            }
            .filterValues { it.isNotEmpty() }
    }
    // Recurring: when credit card selected, filter by payment method only - recurring "appears"
    // based on ViewModel's filterRecurringTransactionsForMonth; don't filter by billing date
    // (getBillingDate=creation date would exclude most recurring)
    val filteredRecurringTransactions = recurringTransactions.filter(matchesPaymentMethodOnly)
    val transactionById = remember(transactions, recurringTransactions, installmentParentTransactions) {
        (transactions + recurringTransactions + installmentParentTransactions.values).associateBy { it.id }
    }
    // Split installments: filter by installment.dueDate in cycle (not parent's billing date -
    // parent's date is when first installment was billed; installments 2+ have their own dueDate).
    // When parent is unknown and we have a filter, exclude - we can't verify payment method.
    val filteredSplitPaymentInstallments = splitPaymentInstallments.filter { installment ->
        val parent = transactionById[installment.parentTransactionId]
        if (parent == null) return@filter (effectiveRecurringFilter == null)
        if (!matchesPaymentMethodOnly(parent)) return@filter false
        val cycleStart = effectiveRecurringFilter?.billingCycleStart?.time
        val cycleEnd = effectiveRecurringFilter?.billingCycleEnd?.time
        if (cycleStart != null && cycleEnd != null) {
            installment.dueDate.time in cycleStart..cycleEnd
        } else {
            true
        }
    }
    // When multiple payment methods exist but no filter yet (summaries not loaded), hide section
    // to avoid showing mixed expenses from all cards
    val distinctPmKeys = (recurringTransactions.map { "${it.paymentMethod.name}:${it.paymentMethodConfigId}" } +
        splitPaymentInstallments.mapNotNull { transactionById[it.parentTransactionId]?.let { "${it.paymentMethod.name}:${it.paymentMethodConfigId}" } }).toSet()
    val hideRecurringUntilFilterReady = distinctPmKeys.size > 1 && effectiveRecurringFilter == null
    val displayRecurringTransactions = if (hideRecurringUntilFilterReady) emptyList() else filteredRecurringTransactions
    val displaySplitPaymentInstallments = if (hideRecurringUntilFilterReady) emptyList() else filteredSplitPaymentInstallments

    
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    if (showTransactionActions && transactionActionTarget != null) {
        val closeAlignment = if (layoutDirection == LayoutDirection.Rtl) {
            Alignment.TopStart
        } else {
            Alignment.TopEnd
        }
        Dialog(onDismissRequest = { showTransactionActions = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .width(140.dp)
                ) {
                    IconButton(
                        onClick = { showTransactionActions = false },
                        modifier = Modifier
                            .align(closeAlignment)
                            .offset(x = 6.dp, y = (-6).dp)
                            .size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.cancel),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(top = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            showTransactionActions = false
                            onEditExpense(transactionActionTarget?.id ?: return@IconButton)
                        }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.edit),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = {
                            showTransactionActions = false
                            showDeleteConfirm = true
                        }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.delete),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm && transactionActionTarget != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete_expense_title)) },
            text = { Text(stringResource(R.string.delete_expense_confirmation)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTransaction(transactionActionTarget!!)
                    showDeleteConfirm = false
                    transactionActionTarget = null
                }) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
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
                    currencyCode = currencyCode,
                    paymentMethodSummaries = paymentMethodSummaries,
                    selectedPaymentMethod = selectedPaymentMethodFilter,
                    onPreviousMonth = { viewModel.changeMonth(-1) },
                    onNextMonth = { viewModel.changeMonth(1) },
                    onPaymentMethodClick = { summary ->
                        // For credit cards, navigate to CardStatementScreen
                        if (summary.paymentMethod == PaymentMethod.CREDIT_CARD &&
                            summary.paymentMethodConfigId != null) {
                            onNavigateToCardStatement(summary.paymentMethodConfigId)
                        } else {
                            // For other payment methods, toggle filter
                            selectedPaymentMethodFilter = if (selectedPaymentMethodFilter == summary) null else summary
                        }
                    },
                    onPagerPageSelected = { summary ->
                        selectedPaymentMethodFilter = summary
                    }
                )
            }
            
            // Recurring Expenses Section (appears after top summary)
            if (displayRecurringTransactions.isNotEmpty() || displaySplitPaymentInstallments.isNotEmpty()) {
                item {
                    RecurringExpensesSection(
                        transactions = displayRecurringTransactions,
                        splitPaymentInstallments = displaySplitPaymentInstallments,
                        transactionById = transactionById,
                        paymentMethodConfigs = paymentMethodConfigs,
                        currencyCode = currencyCode,
                        convertedTransactionAmounts = convertedTransactionAmounts,
                        convertedInstallmentAmounts = convertedInstallmentAmounts
                    )
                }
            }
            
            // Empty state message when no transactions
            if (filteredWeeks.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(),
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
            val weekRangeText = remember(weekNumber, transactions, locale, selectedCycleStart, selectedCycleEnd) {
                if (selectedCycleStart != null && selectedCycleEnd != null) {
                    buildBillingCycleWeekRangeText(
                        weekNumber = weekNumber,
                        cycleStart = selectedCycleStart,
                        cycleEnd = selectedCycleEnd,
                        context = context
                    )
                } else {
                    buildWeekRangeText(transactions, locale, context)
                }
            }
                WeeklySummaryCard(
                    weekNumber = weekNumber,
                    weekRangeText = weekRangeText,
                    transactions = transactions,
                    currencyCode = currencyCode,
                    convertedTransactionAmounts = convertedTransactionAmounts,
                    weekTotal = transactions.sumOf { transaction ->
                        convertedTransactionAmounts[transaction.id] ?: transaction.amount
                    },
                    isExpanded = expandedWeeks.contains(weekNumber),
                    onToggleExpansion = {
                        expandedWeeks = if (expandedWeeks.contains(weekNumber)) {
                            expandedWeeks - weekNumber
                        } else {
                            expandedWeeks + weekNumber
                        }
                    },
                    onTransactionLongPress = { transaction ->
                        transactionActionTarget = transaction
                        showTransactionActions = true
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
    currencyCode: String,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val context = LocalContext.current
    
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
                    text = CurrencyFormatter.formatAmount(amount, currencyCode, context),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
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
    weekRangeText: String?,
    transactions: List<Transaction>,
    currencyCode: String,
    convertedTransactionAmounts: Map<Long, Double>,
    weekTotal: Double,
    isExpanded: Boolean,
    onToggleExpansion: () -> Unit,
    onTransactionLongPress: (Transaction) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
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
                // Left: Week number + date range
                Column {
                    Text(
                        text = stringResource(R.string.week_format, weekNumber),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!weekRangeText.isNullOrBlank()) {
                        Text(
                            text = weekRangeText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Right: Amount and expand/collapse arrow
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Amount
                    Text(
                        text = CurrencyFormatter.formatAmount(weekTotal, currencyCode, context),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
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
                        currencyCode = currencyCode,
                        convertedAmount = convertedTransactionAmounts[transaction.id],
                        onLongPress = onTransactionLongPress
                    )
                    
                    if (transaction != transactions.last()) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

private fun buildWeekRangeText(
    transactions: List<Transaction>,
    locale: Locale,
    context: Context
): String? {
    if (transactions.isEmpty()) {
        return null
    }
    val representativeDate = transactions.maxByOrNull { it.date }?.date ?: return null
    val localDate = representativeDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
    val weekFields = WeekFields.of(locale)
    val weekStart = localDate.with(TemporalAdjusters.previousOrSame(weekFields.firstDayOfWeek))
    val weekEnd = weekStart.plusDays(6)
    val weekStartDate = Date.from(weekStart.atStartOfDay(ZoneId.systemDefault()).toInstant())
    val weekEndDate = Date.from(weekEnd.atStartOfDay(ZoneId.systemDefault()).toInstant())
    return "${LocaleFormatter.formatTransactionDate(weekStartDate, context)} - " +
        LocaleFormatter.formatTransactionDate(weekEndDate, context)
}

private fun getBillingCycleWeekNumber(
    billingDate: Date,
    cycleStart: Date
): Int {
    val startDate = cycleStart.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
    val billingLocalDate = billingDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
    val dayOffset = (billingLocalDate.toEpochDay() - startDate.toEpochDay()).coerceAtLeast(0)
    return (dayOffset / 7).toInt() + 1
}

private fun buildBillingCycleWeekRangeText(
    weekNumber: Int,
    cycleStart: Date,
    cycleEnd: Date,
    context: Context
): String? {
    if (weekNumber < 1) {
        return null
    }
    val startDate = cycleStart.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
    val endDate = cycleEnd.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
    val weekStart = startDate.plusDays(((weekNumber - 1) * 7L))
    if (weekStart.isAfter(endDate)) {
        return null
    }
    val weekEnd = weekStart.plusDays(6).let { candidate ->
        if (candidate.isAfter(endDate)) endDate else candidate
    }
    val weekStartDate = Date.from(weekStart.atStartOfDay(ZoneId.systemDefault()).toInstant())
    val weekEndDate = Date.from(weekEnd.atStartOfDay(ZoneId.systemDefault()).toInstant())
    return "${LocaleFormatter.formatTransactionDate(weekStartDate, context)} - " +
        LocaleFormatter.formatTransactionDate(weekEndDate, context)
}

/**
 * Individual transaction item in expanded weekly view
 * Uses real Transaction domain model
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TransactionItem(
    transaction: Transaction,
    currencyCode: String,
    convertedAmount: Double?,
    onLongPress: (Transaction) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val formattedAmount = if (transaction.currency != currencyCode && convertedAmount != null) {
        CurrencyFormatter.formatAmountWithConversion(
            originalAmount = transaction.amount,
            convertedAmount = convertedAmount,
            originalCurrency = transaction.currency,
            targetCurrency = currencyCode,
            context = context
        )
    } else {
        CurrencyFormatter.formatAmount(transaction.amount, transaction.currency, context)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = { onLongPress(transaction) }
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Payment method and description
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Merchant initial badge
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                val merchantInitial = transaction.description
                    .trim()
                    .take(1)
                    .ifBlank { "?" }
                    .uppercase()
                Text(
                    text = merchantInitial,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            // Description
            Column {
                Text(
                    text = transaction.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = LocaleFormatter.formatTransactionShortMonthDay(transaction.date, context),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (transaction.installments != null && transaction.installments > 1) {
                    Text(
                        text = "1 / ${transaction.installments}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        // Right: Amount
        Text(
            text = formattedAmount,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Billing-aware summary card with payment method breakdown
 */
@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun BillingAwareSummaryCard(
    currentMonth: YearMonth,
    currencyCode: String,
    paymentMethodSummaries: List<PaymentMethodSummary>,
    selectedPaymentMethod: PaymentMethodSummary?,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onPaymentMethodClick: (PaymentMethodSummary) -> Unit,
    onPagerPageSelected: (PaymentMethodSummary) -> Unit,
    modifier: Modifier = Modifier
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val context = LocalContext.current
    if (paymentMethodSummaries.isEmpty()) {
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
                val headerTitle = LocaleFormatter.formatMonthYear(
                    month = currentMonth.monthValue,
                    year = currentMonth.year,
                    context = context
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                    Text(
                        text = headerTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
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
        }
        return
    }

    val pagerState = rememberPagerState(initialPage = 0) { paymentMethodSummaries.size }
    LaunchedEffect(pagerState.currentPage, paymentMethodSummaries) {
        val summary = paymentMethodSummaries.getOrNull(pagerState.currentPage)
        summary?.let { onPagerPageSelected(it) }
    }
    LaunchedEffect(paymentMethodSummaries.size) {
        if (paymentMethodSummaries.isEmpty()) {
            return@LaunchedEffect
        }
        if (pagerState.currentPage > paymentMethodSummaries.lastIndex) {
            pagerState.scrollToPage(0)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            pageSpacing = 12.dp,
            contentPadding = PaddingValues(horizontal = 0.dp),
            modifier = Modifier.fillMaxWidth(),
            key = { page ->
                val summary = paymentMethodSummaries[page]
                summary.paymentMethodConfigId ?: summary.paymentMethod.ordinal.toLong()
            }
        ) { page ->
            val summary = paymentMethodSummaries[page]
            SummaryCardPage(
                summary = summary,
                currentMonth = currentMonth,
                currencyCode = currencyCode,
                isRtl = isRtl,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
                onClick = { onPaymentMethodClick(summary) },
                isSelected = selectedPaymentMethod == summary
            )
        }

        PagerIndicatorRow(
            pageCount = paymentMethodSummaries.size,
            currentPage = pagerState.currentPage
        )
    }
}

@Composable
private fun PagerIndicatorRow(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(pageCount) { index ->
            val isSelected = index == currentPage
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp, vertical = 6.dp)
                    .size(if (isSelected) 8.dp else 6.dp)
                    .background(
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
private fun SummaryCardPage(
    summary: PaymentMethodSummary,
    currentMonth: YearMonth,
    currencyCode: String,
    isRtl: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onClick: () -> Unit,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val headerTitle = summary.billingCycleText
        ?: LocaleFormatter.formatMonthYear(
            month = currentMonth.monthValue,
            year = currentMonth.year,
            context = context
        )
    val cycleStartLabel = summary.billingCycleStart?.let {
        LocaleFormatter.formatTransactionDate(it, context)
    }
    val cycleEndLabel = summary.billingCycleEnd?.let {
        LocaleFormatter.formatTransactionDate(it, context)
    }
    val cycleArrow = if (isRtl) "\u2190" else "\u2192"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
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

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    if (cycleStartLabel != null && cycleEndLabel != null) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = cycleStartLabel,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = cycleArrow,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                                    .offset(y = (-2).dp)
                            )
                            Text(
                                text = cycleEndLabel,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else {
                        Text(
                            text = headerTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                    }
                }

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

            BillingCycleProgressBar(
                startDate = summary.billingCycleStart,
                endDate = summary.billingCycleEnd,
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(44.dp)
                        .background(summary.accentColor, RoundedCornerShape(2.dp))
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = summary.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }

                Text(
                    text = CurrencyFormatter.formatAmount(summary.totalAmount, currencyCode, context),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = when {
                        isSelected -> summary.accentColor
                        summary.paymentMethod == PaymentMethod.CREDIT_CARD -> MaterialTheme.colorScheme.onSurface
                        else -> summary.accentColor
                    }
                )
            }
        }
    }
}

@Composable
private fun BillingCycleProgressBar(
    startDate: Date?,
    endDate: Date?,
    modifier: Modifier = Modifier
) {
    if (startDate == null || endDate == null) {
        return
    }

    val context = LocalContext.current
    val start = startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
    val end = endDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
    val today = LocalDate.now()
    if (end.isBefore(start)) {
        return
    }
    if (today.isBefore(start) || today.isAfter(end)) {
        return
    }

    val totalDays = (end.toEpochDay() - start.toEpochDay()).coerceAtLeast(1)
    val elapsedDays = (today.toEpochDay() - start.toEpochDay()).coerceIn(0, totalDays)
    val progress = elapsedDays.toFloat() / totalDays.toFloat()

    val startLabel = LocaleFormatter.formatTransactionDate(startDate, context)
    val endLabel = LocaleFormatter.formatTransactionDate(endDate, context)
    val todayDate = Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant())
    val todayLabel = LocaleFormatter.formatTransactionDate(todayDate, context)
    var todayLabelWidthPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = startLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = endLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f))
                    .align(Alignment.Center)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .align(Alignment.CenterStart)
            )
            val labelWidthDp = with(density) { todayLabelWidthPx.toDp() }
            val availableWidth = (maxWidth - labelWidthDp).coerceAtLeast(0.dp)
            val markerOffset = availableWidth * progress.coerceIn(0f, 1f)
            Column(
                modifier = Modifier
                    .offset(x = markerOffset)
                    .align(Alignment.CenterStart),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = todayLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                        .onSizeChanged { todayLabelWidthPx = it.width }
                )
            }
        }
    }
}

/**
 * Compute payment method summaries with billing cycle information.
 * Credit cards: filter by billing cycle (previousMonth.withdrawDay .. currentMonth.withdrawDay-1).
 * Cash/Cheque: filter by calendar month.
 * Includes split payment installments (due in cycle) for credit card totals.
 */
private fun computePaymentMethodSummaries(
    transactions: List<Transaction>,
    recurringTransactions: List<Transaction>,
    splitPaymentInstallments: List<SplitPaymentInstallment>,
    transactionById: Map<Long, Transaction>,
    paymentMethodConfigs: List<PaymentMethodConfig>,
    currentMonth: YearMonth,
    context: Context,
    creditCardAccent: Color,
    convertedTransactionAmounts: Map<Long, Double>,
    convertedInstallmentAmounts: Map<Long, Double>,
    isRtl: Boolean
): List<PaymentMethodSummary> {
    val startOfMonth = currentMonth.atDay(1).atStartOfDay(ZoneId.systemDefault())
    val endOfMonth = currentMonth.atEndOfMonth().atTime(23, 59, 59).atZone(ZoneId.systemDefault())
    val summaries = mutableListOf<PaymentMethodSummary>()
    
    // Handle CREDIT_CARD transactions - filter by billing cycle per card
    val creditCardTransactions = transactions
        .filter { it.type == TransactionType.EXPENSE && !it.isRecurring && !it.hasDelayedBilling() }
        .filter { it.paymentMethod == PaymentMethod.CREDIT_CARD }
    val creditCardGroups = creditCardTransactions.groupBy { it.paymentMethodConfigId }
    
    creditCardGroups.forEach { (configId, transactionsForCard) ->
        val cardConfig = configId?.let { id -> paymentMethodConfigs.find { it.id == id } }
        val billingCycleRange = cardConfig?.withdrawDay?.let { withdrawDay ->
            val previousMonth = currentMonth.minusMonths(1)
            val validDayPrev = minOf(withdrawDay, previousMonth.lengthOfMonth())
            val validDayCur = minOf(withdrawDay, currentMonth.lengthOfMonth())
            val cycleStart = previousMonth.atDay(validDayPrev)
            val cycleEnd = currentMonth.atDay(validDayCur).minusDays(1)
            val cycleStartDate = Date.from(cycleStart.atStartOfDay(ZoneId.systemDefault()).toInstant())
            val cycleEndDate = Date.from(cycleEnd.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant())
            val startText = LocaleFormatter.formatTransactionDate(cycleStartDate, context)
            val endText = LocaleFormatter.formatTransactionDate(cycleEndDate, context)
            val cycleText = if (isRtl) "$endText \u2190 $startText" else "$startText \u2192 $endText"
            Triple(cycleStartDate, cycleEndDate, cycleText)
        }
        val cycleStartTime = billingCycleRange?.first?.time
        val cycleEndTime = billingCycleRange?.second?.time
        val inCycle = { t: Transaction ->
            if (cycleStartTime != null && cycleEndTime != null) {
                val billingTime = t.getBillingDate().time
                billingTime in cycleStartTime..cycleEndTime
            } else true
        }
        val transactionsInCycle = transactionsForCard.filter(inCycle)
        // Recurring: use ViewModel's filter (by currentMonth); don't filter by cycle - recurring
        // has a single date and "appears" monthly, so currentMonth filter is sufficient
        val recurringForCard = recurringTransactions
            .filter { it.paymentMethod == PaymentMethod.CREDIT_CARD && it.paymentMethodConfigId == configId }
        // Split installments: include those whose parent is for this card and dueDate is in cycle
        val installmentsForCard = splitPaymentInstallments.filter { installment ->
            val parent = transactionById[installment.parentTransactionId]
            parent != null &&
                parent.paymentMethod == PaymentMethod.CREDIT_CARD &&
                parent.paymentMethodConfigId == configId &&
                cycleStartTime != null && cycleEndTime != null &&
                installment.dueDate.time in cycleStartTime..cycleEndTime
        }
        val total = (transactionsInCycle + recurringForCard).sumOf { transaction ->
            convertedTransactionAmounts[transaction.id] ?: transaction.amount
        } + installmentsForCard.sumOf { installment ->
            convertedInstallmentAmounts[installment.id] ?: installment.amount
        }
        if (total > 0.0) {
            val displayName = cardConfig?.alias
                ?.trim()
                ?.replace("\\s+".toRegex(), " ")
                ?.ifBlank { "Credit Card" }
                ?: "Credit Card"
            summaries.add(
                PaymentMethodSummary(
                    paymentMethod = PaymentMethod.CREDIT_CARD,
                    paymentMethodConfigId = configId,
                    displayName = displayName,
                    totalAmount = total,
                    billingCycleText = billingCycleRange?.third,
                    billingCycleStart = billingCycleRange?.first,
                    billingCycleEnd = billingCycleRange?.second,
                    accentColor = creditCardAccent
                )
            )
        }
    }
    
    // Handle CASH transactions - filter by calendar month
    val cashInMonth = transactions
        .filter { it.type == TransactionType.EXPENSE && !it.isRecurring && it.paymentMethod == PaymentMethod.CASH }
        .filter { t ->
            val billingInstant = t.getBillingDate().toInstant().atZone(ZoneId.systemDefault())
            !billingInstant.isBefore(startOfMonth) && !billingInstant.isAfter(endOfMonth)
        }
    val cashRecurring = recurringTransactions.filter { it.paymentMethod == PaymentMethod.CASH }
    val cashInstallments = splitPaymentInstallments.filter { installment ->
        val parent = transactionById[installment.parentTransactionId]
        if (parent?.paymentMethod != PaymentMethod.CASH) return@filter false
        val dueInstant = installment.dueDate.toInstant().atZone(ZoneId.systemDefault())
        !dueInstant.isBefore(startOfMonth) && !dueInstant.isAfter(endOfMonth)
    }
    val cashTotal = (cashInMonth + cashRecurring).sumOf {
        convertedTransactionAmounts[it.id] ?: it.amount
    } + cashInstallments.sumOf { convertedInstallmentAmounts[it.id] ?: it.amount }
    if (cashTotal > 0.0) {
        summaries.add(
            PaymentMethodSummary(
                paymentMethod = PaymentMethod.CASH,
                displayName = "Cash",
                totalAmount = cashTotal,
                billingCycleText = null,
                accentColor = Color(0xFF4CAF50) // Green
            )
        )
    }
    
    // Handle CHEQUE transactions - filter by calendar month
    val chequeInMonth = transactions
        .filter { it.type == TransactionType.EXPENSE && !it.isRecurring && it.paymentMethod == PaymentMethod.CHEQUE }
        .filter { t ->
            val billingInstant = t.getBillingDate().toInstant().atZone(ZoneId.systemDefault())
            !billingInstant.isBefore(startOfMonth) && !billingInstant.isAfter(endOfMonth)
        }
    val chequeRecurring = recurringTransactions.filter { it.paymentMethod == PaymentMethod.CHEQUE }
    val chequeInstallments = splitPaymentInstallments.filter { installment ->
        val parent = transactionById[installment.parentTransactionId]
        if (parent?.paymentMethod != PaymentMethod.CHEQUE) return@filter false
        val dueInstant = installment.dueDate.toInstant().atZone(ZoneId.systemDefault())
        !dueInstant.isBefore(startOfMonth) && !dueInstant.isAfter(endOfMonth)
    }
    val chequeTotal = (chequeInMonth + chequeRecurring).sumOf {
        convertedTransactionAmounts[it.id] ?: it.amount
    } + chequeInstallments.sumOf { convertedInstallmentAmounts[it.id] ?: it.amount }
    if (chequeTotal > 0.0) {
        summaries.add(
            PaymentMethodSummary(
                paymentMethod = PaymentMethod.CHEQUE,
                displayName = "Cheque",
                totalAmount = chequeTotal,
                billingCycleText = null,
                accentColor = Color(0xFF2196F3) // Blue
            )
        )
    }
    
    return summaries
}

