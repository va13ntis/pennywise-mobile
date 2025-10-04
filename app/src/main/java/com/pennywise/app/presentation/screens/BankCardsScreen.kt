package com.pennywise.app.presentation.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pennywise.app.R
import com.pennywise.app.domain.model.BankCard
import com.pennywise.app.presentation.viewmodel.BankCardViewModel
import com.pennywise.app.presentation.viewmodel.BankCardUiState

/**
 * Screen for managing bank cards
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankCardsScreen(
    onNavigateBack: () -> Unit,
    viewModel: BankCardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val bankCards by viewModel.bankCards.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedCard by remember { mutableStateOf<BankCard?>(null) }
    
    // Handle UI state changes
    LaunchedEffect(uiState) {
        when (uiState) {
            is BankCardUiState.Success -> {
                showAddDialog = false
                showEditDialog = false
                selectedCard = null
                viewModel.clearUiState()
            }
            is BankCardUiState.Error -> {
                // Error will be shown in the dialog
            }
            else -> {}
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.bank_cards_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Bank Card")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (bankCards.isEmpty()) {
                EmptyBankCardsState(
                    onAddCard = { showAddDialog = true }
                )
            } else {
                BankCardsList(
                    bankCards = bankCards,
                    onEditCard = { card ->
                        selectedCard = card
                        showEditDialog = true
                    },
                    onDeleteCard = { card ->
                        viewModel.deleteBankCard(card)
                    },
                    onToggleStatus = { card ->
                        viewModel.toggleBankCardStatus(card)
                    }
                )
            }
        }
    }
    
    // Add Bank Card Dialog
    if (showAddDialog) {
        AddBankCardDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { alias, lastFourDigits, paymentDay ->
                viewModel.addBankCard(alias, lastFourDigits, paymentDay)
            },
            uiState = uiState
        )
    }
    
    // Edit Bank Card Dialog
    if (showEditDialog && selectedCard != null) {
        EditBankCardDialog(
            bankCard = selectedCard!!,
            onDismiss = { 
                showEditDialog = false
                selectedCard = null
            },
            onConfirm = { alias, lastFourDigits, paymentDay ->
                viewModel.updateBankCard(selectedCard!!.id, alias, lastFourDigits, paymentDay)
            },
            uiState = uiState
        )
    }
}

/**
 * Empty state when no bank cards are present
 */
@Composable
private fun EmptyBankCardsState(
    onAddCard: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.CreditCard,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = stringResource(R.string.no_bank_cards_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = stringResource(R.string.no_bank_cards_description),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onAddCard,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.add_bank_card))
        }
    }
}

/**
 * List of bank cards
 */
@Composable
private fun BankCardsList(
    bankCards: List<BankCard>,
    onEditCard: (BankCard) -> Unit,
    onDeleteCard: (BankCard) -> Unit,
    onToggleStatus: (BankCard) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(bankCards) { card ->
            BankCardItem(
                bankCard = card,
                onEdit = { onEditCard(card) },
                onDelete = { onDeleteCard(card) },
                onToggleStatus = { onToggleStatus(card) }
            )
        }
    }
}

/**
 * Individual bank card item
 */
@Composable
private fun BankCardItem(
    bankCard: BankCard,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleStatus: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Card icon
            Icon(
                Icons.Default.CreditCard,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (bankCard.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Card details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = bankCard.alias,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = "**** **** **** ${bankCard.lastFourDigits}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = "Payment day: ${bankCard.paymentDay}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Status indicator
            Card(
                modifier = Modifier.size(12.dp),
                shape = RoundedCornerShape(6.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (bankCard.isActive) Color.Green else Color.Gray
                )
            ) {}
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Menu button
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
                }
                
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(if (bankCard.isActive) "Deactivate" else "Activate") },
                        onClick = {
                            onToggleStatus()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                if (bankCard.isActive) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    )
                    
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = {
                            onEdit()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null)
                        }
                    )
                    
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            onDelete()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, contentDescription = null)
                        }
                    )
                }
            }
        }
    }
}

/**
 * Dialog for adding a new bank card
 */
@Composable
private fun AddBankCardDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Int) -> Unit,
    uiState: BankCardUiState
) {
    var alias by remember { mutableStateOf("") }
    var lastFourDigits by remember { mutableStateOf("") }
    var paymentDay by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_bank_card)) },
        text = {
            Column {
                OutlinedTextField(
                    value = alias,
                    onValueChange = { alias = it },
                    label = { Text(stringResource(R.string.card_alias)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = lastFourDigits,
                    onValueChange = { 
                        if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                            lastFourDigits = it
                        }
                    },
                    label = { Text(stringResource(R.string.last_four_digits)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("1234") }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = paymentDay,
                    onValueChange = { 
                        if (it.isEmpty() || (it.toIntOrNull() != null && it.toInt() in 1..31)) {
                            paymentDay = it
                        }
                    },
                    label = { Text(stringResource(R.string.payment_day)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("15") }
                )
                
                if (uiState is BankCardUiState.Error) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val day = paymentDay.toIntOrNull() ?: 1
                    onConfirm(alias, lastFourDigits, day)
                },
                enabled = alias.isNotBlank() && lastFourDigits.length == 4 && paymentDay.isNotBlank() && uiState !is BankCardUiState.Loading
            ) {
                if (uiState is BankCardUiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(stringResource(R.string.add))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

/**
 * Dialog for editing an existing bank card
 */
@Composable
private fun EditBankCardDialog(
    bankCard: BankCard,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Int) -> Unit,
    uiState: BankCardUiState
) {
    var alias by remember { mutableStateOf(bankCard.alias) }
    var lastFourDigits by remember { mutableStateOf(bankCard.lastFourDigits) }
    var paymentDay by remember { mutableStateOf(bankCard.paymentDay.toString()) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_bank_card)) },
        text = {
            Column {
                OutlinedTextField(
                    value = alias,
                    onValueChange = { alias = it },
                    label = { Text(stringResource(R.string.card_alias)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = lastFourDigits,
                    onValueChange = { 
                        if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                            lastFourDigits = it
                        }
                    },
                    label = { Text(stringResource(R.string.last_four_digits)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("1234") }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = paymentDay,
                    onValueChange = { 
                        if (it.isEmpty() || (it.toIntOrNull() != null && it.toInt() in 1..31)) {
                            paymentDay = it
                        }
                    },
                    label = { Text(stringResource(R.string.payment_day)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("15") }
                )
                
                if (uiState is BankCardUiState.Error) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val day = paymentDay.toIntOrNull() ?: bankCard.paymentDay
                    onConfirm(alias, lastFourDigits, day)
                },
                enabled = alias.isNotBlank() && lastFourDigits.length == 4 && paymentDay.isNotBlank() && uiState !is BankCardUiState.Loading
            ) {
                if (uiState is BankCardUiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(stringResource(R.string.save))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
