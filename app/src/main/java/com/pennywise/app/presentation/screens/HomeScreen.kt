package com.pennywise.app.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.pennywise.app.R
import com.pennywise.app.presentation.components.ExpenseSection
import com.pennywise.app.presentation.components.MonthlySummaryCard
import com.pennywise.app.presentation.components.RecurringExpensesSection
import com.pennywise.app.presentation.utils.DateFormatter
import com.pennywise.app.presentation.viewmodel.HomeViewModel
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Home screen that displays monthly expense summary with navigation and management features
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddExpense: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel
) {
    val transactions by viewModel.transactions.collectAsState()
    val recurringTransactions by viewModel.recurringTransactions.collectAsState()
    val splitPaymentInstallments by viewModel.splitPaymentInstallments.collectAsState()
    val currentMonth by viewModel.currentMonth.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val currency by viewModel.currency.collectAsState()
    val currencyConversionEnabled by viewModel.currencyConversionEnabled.collectAsState()
    val originalCurrency by viewModel.originalCurrency.collectAsState()
    val conversionState by viewModel.conversionState.collectAsState()
    
    // Reactive computed values
    val transactionsByWeek by viewModel.transactionsByWeek.collectAsState()
    val totalIncome by viewModel.totalIncome.collectAsState()
    val totalExpenses by viewModel.totalExpenses.collectAsState()
    val netBalance by viewModel.netBalance.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Show error messages in snackbar
    LaunchedEffect(error) {
        error?.let { errorMessage ->
            snackbarHostState.showSnackbar(message = errorMessage)
            viewModel.clearError()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.nav_settings)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddExpense,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.add_expense),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                // Loading state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // Content
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Monthly summary card with integrated month navigation
                    item {
                        MonthlySummaryCard(
                            totalExpenses = totalExpenses,
                            currentMonth = currentMonth,
                            currency = currency,
                            currencyConversionEnabled = currencyConversionEnabled,
                            originalCurrency = originalCurrency,
                            conversionState = conversionState,
                            onConvertAmount = { amount -> viewModel.convertAmount(amount) },
                            onPaymentMethodFilterChanged = { paymentMethod -> 
                                viewModel.setPaymentMethodFilter(paymentMethod) 
                            },
                            onPreviousMonth = { viewModel.changeMonth(-1) },
                            onNextMonth = { viewModel.changeMonth(1) },
                            onCurrentMonth = { viewModel.navigateToCurrentMonth() },
                            onBeginningOfYear = { viewModel.navigateToBeginningOfYear() },
                            onEndOfYear = { viewModel.navigateToEndOfYear() }
                        )
                    }
                    
                    // Recurring expenses section (includes both recurring transactions and split payment installments)
                    if (recurringTransactions.isNotEmpty() || splitPaymentInstallments.isNotEmpty()) {
                        item {
                            RecurringExpensesSection(
                                transactions = recurringTransactions,
                                splitPaymentInstallments = splitPaymentInstallments,
                                currency = currency,
                                currencyConversionEnabled = currencyConversionEnabled,
                                originalCurrency = originalCurrency,
                                conversionState = conversionState,
                                onConvertAmount = { amount -> viewModel.convertAmount(amount) }
                            )
                        }
                    }
                    
                    // Weekly expense sections
                    transactionsByWeek.forEach { (weekNumber, weekTransactions) ->
                        item {
                            ExpenseSection(
                                title = stringResource(R.string.week_format, weekNumber),
                                transactions = weekTransactions,
                                currency = currency,
                                currencyConversionEnabled = currencyConversionEnabled,
                                originalCurrency = originalCurrency,
                                conversionState = conversionState,
                                onConvertAmount = { amount -> viewModel.convertAmount(amount) }
                            )
                        }
                    }
                    
                    // Empty state if no transactions
                    if (transactions.isEmpty() && recurringTransactions.isEmpty() && splitPaymentInstallments.isEmpty()) {
                        item {
                            EmptyState()
                        }
                    }
                    
                }
            }
        }
    }
}


/**
 * Month navigation component with previous/next/current buttons
 */
@Composable
fun MonthNavigation(
    currentMonth: java.time.YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onCurrentMonth: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top row with navigation arrows and current month button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPreviousMonth,
                modifier = Modifier.padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = stringResource(R.string.previous_month),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.graphicsLayer(
                        scaleX = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f
                    )
                )
            }
            
            // Current month button
            Button(
                onClick = onCurrentMonth,
                modifier = Modifier.padding(horizontal = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Today,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = stringResource(R.string.current_month),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            IconButton(
                onClick = onNextMonth,
                modifier = Modifier.padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = stringResource(R.string.next_month),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.graphicsLayer(
                        scaleX = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f
                    )
                )
            }
        }
        
        // Month/Year display
        Text(
            text = DateFormatter.formatMonthYear(LocalContext.current, currentMonth),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

/**
 * Empty state when no transactions are available
 */
@Composable
fun EmptyState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.no_transactions_yet),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.add_your_first_expense),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
