package com.pennywise.app.presentation.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pennywise.app.domain.model.Transaction
import com.pennywise.app.presentation.theme.expense_red
import com.pennywise.app.presentation.util.CategoryMapper
import com.pennywise.app.presentation.util.CurrencyFormatter
import com.pennywise.app.presentation.util.LocaleFormatter
import com.pennywise.app.presentation.util.PaymentMethodMapper
import com.pennywise.app.presentation.viewmodel.CardStatementViewModel
import java.util.Date

/**
 * Card Statement Screen - displays billing cycle-based transactions for a credit card
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardStatementScreen(
    cardId: Long,
    onNavigateBack: () -> Unit,
    viewModel: CardStatementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currencyCode by viewModel.currency.collectAsState()
    val context = LocalContext.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    
    // Initialize ViewModel with card ID
    LaunchedEffect(cardId) {
        viewModel.initialize(cardId)
    }
    
    // Get currency symbol from currency code
    val currencySymbol = remember(currencyCode) {
        CurrencyFormatter.getCurrencySymbol(currencyCode)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Card Statement") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            uiState.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.error ?: "Error loading card statement",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            uiState.cardConfig == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Card not found")
                }
            }
            
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Sticky Header Card
                    item {
                        StatementHeaderCard(
                            cardName = uiState.cardConfig?.alias ?: "Credit Card",
                            cycleStart = uiState.availableCycles.getOrNull(uiState.currentCycleIndex)?.first,
                            cycleEnd = uiState.availableCycles.getOrNull(uiState.currentCycleIndex)?.second,
                            totalAmount = uiState.totalAmount,
                            currencySymbol = currencySymbol,
                            canGoPrevious = uiState.currentCycleIndex > 0,
                            canGoNext = uiState.currentCycleIndex < uiState.availableCycles.size - 1,
                            onPreviousCycle = { viewModel.previousCycle() },
                            onNextCycle = { viewModel.nextCycle() },
                            isRtl = isRtl,
                            context = context
                        )
                    }
                    
                    // Cycle Navigation
                    if (uiState.availableCycles.size > 1) {
                        item {
                            CycleNavigationRow(
                                currentIndex = uiState.currentCycleIndex,
                                totalCycles = uiState.availableCycles.size,
                                onPrevious = { viewModel.previousCycle() },
                                onNext = { viewModel.nextCycle() },
                                canGoPrevious = uiState.currentCycleIndex > 0,
                                canGoNext = uiState.currentCycleIndex < uiState.availableCycles.size - 1,
                                isRtl = isRtl
                            )
                        }
                    }
                    
                    // Transaction List
                    if (uiState.transactions.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No transactions in this billing cycle",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(
                            items = uiState.transactions,
                            key = { it.id }
                        ) { transaction ->
                            StatementTransactionItem(
                                transaction = transaction,
                                currencySymbol = currencySymbol,
                                context = context
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Sticky header card showing card name, billing cycle range, and total amount
 */
@Composable
private fun StatementHeaderCard(
    cardName: String,
    cycleStart: Date?,
    cycleEnd: Date?,
    totalAmount: Double,
    currencySymbol: String,
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    onPreviousCycle: () -> Unit,
    onNextCycle: () -> Unit,
    isRtl: Boolean,
    context: android.content.Context,
    modifier: Modifier = Modifier
) {
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
            // Card name
            Text(
                text = cardName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // Billing cycle range with navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous cycle button
                IconButton(
                    onClick = onPreviousCycle,
                    enabled = canGoPrevious,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (isRtl) Icons.Default.ChevronRight else Icons.Default.ChevronLeft,
                        contentDescription = "Previous cycle",
                        tint = if (canGoPrevious) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        }
                    )
                }
                
                // Cycle range text
                if (cycleStart != null && cycleEnd != null) {
                    Text(
                        text = "${LocaleFormatter.formatTransactionDate(cycleStart, context)} – ${LocaleFormatter.formatTransactionDate(cycleEnd, context)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "No billing cycle",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Next cycle button
                IconButton(
                    onClick = onNextCycle,
                    enabled = canGoNext,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (isRtl) Icons.Default.ChevronLeft else Icons.Default.ChevronRight,
                        contentDescription = "Next cycle",
                        tint = if (canGoNext) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        }
                    )
                }
            }
            
            // Total amount
            Text(
                text = "$currencySymbol${String.format("%.2f", totalAmount)}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = expense_red
            )
        }
    }
}

/**
 * Cycle navigation row with cycle counter
 */
@Composable
private fun CycleNavigationRow(
    currentIndex: Int,
    totalCycles: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    isRtl: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${currentIndex + 1} / $totalCycles",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Transaction item for statement view
 * Matches the style from HomeScreen TransactionItem
 */
@Composable
private fun StatementTransactionItem(
    transaction: Transaction,
    currencySymbol: String,
    context: android.content.Context,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Payment method icon and transaction details
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Payment method icon (credit card only in this context)
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFFFD700).copy(alpha = 0.2f)), // Gold for credit card
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = PaymentMethodMapper.getPaymentMethodIcon(transaction.paymentMethod),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            // Transaction details
            Column {
                Text(
                    text = transaction.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // Purchase date
                Text(
                    text = LocaleFormatter.formatTransactionDate(transaction.date, context),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Category and delay indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (transaction.category.isNotEmpty()) {
                        Text(
                            text = CategoryMapper.getLocalizedCategory(transaction.category),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    // Delayed payment indicator
                    if (transaction.billingDelayDays > 0) {
                        Text(
                            text = "שוטף + ${transaction.billingDelayDays}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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

