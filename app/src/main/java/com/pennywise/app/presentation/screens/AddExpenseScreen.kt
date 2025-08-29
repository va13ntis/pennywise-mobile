package com.pennywise.app.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pennywise.app.R
import com.pennywise.app.domain.model.RecurringPeriod
import com.pennywise.app.domain.model.TransactionType
import com.pennywise.app.presentation.viewmodel.AddExpenseUiState
import com.pennywise.app.presentation.viewmodel.AddExpenseViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.LaunchedEffect
import java.text.SimpleDateFormat
import java.util.*

/**
 * Add Expense Screen with enhanced form state management and validation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    onNavigateBack: () -> Unit,
    onSaveExpense: (expenseData: ExpenseFormData) -> Unit,
    viewModel: AddExpenseViewModel = hiltViewModel()
) {
    // For now, we'll use a hardcoded user ID since we don't have user management fully set up
    val userId = 1L
    
    // Form state management using remember and mutableStateOf
    var merchant by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var isRecurring by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(Date()) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    // Validation states
    var merchantError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }
    var categoryError by remember { mutableStateOf<String?>(null) }
    
    // Form validation state
    var isFormValid by remember { mutableStateOf(false) }
    
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    
    val uiState by viewModel.uiState.collectAsState()
    
    // Pre-fetch string resources to avoid calling them from non-@Composable contexts
    val merchantRequiredText = stringResource(R.string.merchant_required)
    val amountRequiredText = stringResource(R.string.amount_required)
    val invalidAmountText = stringResource(R.string.invalid_amount)
    val categoryRequiredText = stringResource(R.string.category_required)
    
    // Date formatter
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    
    // Category options - pre-fetch string resources
    val categoryFood = stringResource(R.string.category_food)
    val categoryTransport = stringResource(R.string.category_transport)
    val categoryShopping = stringResource(R.string.category_shopping)
    val categoryEntertainment = stringResource(R.string.category_entertainment)
    val categoryUtilities = stringResource(R.string.category_utilities)
    val categoryHealth = stringResource(R.string.category_health)
    val categoryOther = stringResource(R.string.category_other)
    
    val categories = remember {
        listOf(
            categoryFood,
            categoryTransport,
            categoryShopping,
            categoryEntertainment,
            categoryUtilities,
            categoryHealth,
            categoryOther
        )
    }
    
    // Set default category
    LaunchedEffect(categoryFood) {
        if (category.isEmpty()) {
            category = categoryFood
        }
    }
    
    // Validation function
    val validateForm = {
        merchantError = if (merchant.isBlank()) merchantRequiredText else null
        amountError = when {
            amount.isBlank() -> amountRequiredText
            amount.toDoubleOrNull() == null -> invalidAmountText
            amount.toDoubleOrNull()!! <= 0 -> invalidAmountText
            else -> null
        }
        categoryError = if (category.isBlank()) categoryRequiredText else null
        
        isFormValid = merchantError == null && amountError == null && categoryError == null
    }
    
    // Validate form whenever any field changes
    LaunchedEffect(merchant, amount, category) {
        validateForm()
    }
    
    // Handle UI state changes from ViewModel
    LaunchedEffect(uiState) {
        when (uiState) {
            is AddExpenseUiState.Success -> {
                onNavigateBack()
            }
            is AddExpenseUiState.Error -> {
                // In a real app, you'd show a snackbar or dialog here
                // For now, we'll just reset the state
                viewModel.resetState()
            }
            else -> {}
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.new_expense)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Date Picker
            OutlinedTextField(
                value = dateFormatter.format(selectedDate),
                onValueChange = { },
                label = { Text(stringResource(R.string.select_date)) },
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
                leadingIcon = {
                    Icon(Icons.Default.CalendarToday, contentDescription = "Calendar")
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )
            
            // Merchant field
            OutlinedTextField(
                value = merchant,
                onValueChange = { 
                    merchant = it 
                    merchantError = if (it.isBlank()) merchantRequiredText else null
                },
                label = { Text(stringResource(R.string.merchant)) },
                isError = merchantError != null,
                supportingText = { merchantError?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                singleLine = true
            )
            
            // Amount field
            OutlinedTextField(
                value = amount,
                onValueChange = { 
                    // Only allow numbers and decimal point
                    val filtered = it.filter { char -> char.isDigit() || char == '.' }
                    amount = filtered
                    amountError = when {
                        filtered.isBlank() -> amountRequiredText
                        filtered.toDoubleOrNull() == null -> invalidAmountText
                        filtered.toDoubleOrNull()!! <= 0 -> invalidAmountText
                        else -> null
                    }
                },
                label = { Text(stringResource(R.string.amount)) },
                isError = amountError != null,
                supportingText = { amountError?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                singleLine = true
            )
            
            // Category dropdown
            var categoryExpanded by remember { mutableStateOf(false) }
            
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text(stringResource(R.string.category)) },
                    isError = categoryError != null,
                    supportingText = { categoryError?.let { Text(it) } },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )
                
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    categories.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                category = option
                                categoryError = if (option.isBlank()) categoryRequiredText else null
                                categoryExpanded = false
                                focusManager.moveFocus(FocusDirection.Down)
                            }
                        )
                    }
                }
            }
            
            // Payment Type Radio Buttons
            Text(
                text = stringResource(R.string.payment_type),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    RadioButton(
                        selected = !isRecurring,
                        onClick = { isRecurring = false }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.one_time))
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    RadioButton(
                        selected = isRecurring,
                        onClick = { isRecurring = true }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.recurring))
                }
            }
            
            // Notes field
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(R.string.notes)) },
                placeholder = { Text(stringResource(R.string.notes_hint)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Save button - connected to ViewModel's expenseState
            Button(
                onClick = {
                    // Final validation before submission
                    validateForm()
                    
                    // Only proceed if form is valid
                    if (isFormValid) {
                        val expenseData = ExpenseFormData(
                            merchant = merchant,
                            amount = amount.toDouble(),
                            category = category,
                            isRecurring = isRecurring,
                            notes = notes.ifBlank { null },
                            date = selectedDate
                        )
                        viewModel.saveExpense(expenseData, userId)
                    }
                },
                enabled = isFormValid && uiState !is AddExpenseUiState.Loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                if (uiState is AddExpenseUiState.Loading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(stringResource(R.string.save))
                }
            }
            
            // Display error messages from ViewModel
            if (uiState is AddExpenseUiState.Error) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = (uiState as AddExpenseUiState.Error).message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
    
    // Date picker dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.time
        )
        
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = Date(millis)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

/**
 * Data class to hold expense form data
 */
data class ExpenseFormData(
    val merchant: String,
    val amount: Double,
    val category: String,
    val isRecurring: Boolean,
    val notes: String?,
    val date: Date
)
