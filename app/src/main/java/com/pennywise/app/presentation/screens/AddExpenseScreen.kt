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
import androidx.compose.material.icons.filled.Check

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import com.pennywise.app.R
import com.pennywise.app.domain.model.Currency
import com.pennywise.app.domain.model.RecurringPeriod
import com.pennywise.app.domain.model.TransactionType
import com.pennywise.app.presentation.components.CurrencySelectionDropdown
import com.pennywise.app.presentation.components.CurrencyAdapter
import com.pennywise.app.presentation.viewmodel.AddExpenseUiState
import com.pennywise.app.presentation.viewmodel.AddExpenseViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.LaunchedEffect
import java.text.SimpleDateFormat
import java.util.*

/**
 * Helper function to validate and format amount input based on currency
 */
fun validateAndFormatAmount(
    input: String,
    currency: Currency?
): String {
    if (currency == null) return input
    
    // Only allow digits and decimal point
    val filtered = input.filter { char -> char.isDigit() || char == '.' }
    
    return when (currency.decimalPlaces) {
        0 -> {
            // For currencies with no decimal places (JPY, KRW), only allow digits
            filtered.filter { char -> char.isDigit() }
        }
        else -> {
            // For currencies with decimal places, limit to the specified number
            val parts = filtered.split(".")
            if (parts.size > 1) {
                parts[0] + "." + parts[1].take(currency.decimalPlaces)
            } else {
                filtered
            }
        }
    }
}

/**
 * Visual transformation to format amount with currency symbol
 */
class CurrencyAmountTransformation(
    private val currency: Currency
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        val formattedText = if (originalText.isNotEmpty()) {
            "${currency.symbol}$originalText"
        } else {
            currency.symbol
        }
        
        return TransformedText(
            AnnotatedString(formattedText),
            OffsetMapping.Identity
        )
    }
}

/**
 * Update amount field formatting based on selected currency
 */
fun updateAmountFieldForCurrency(
    currentAmount: String,
    selectedCurrency: Currency?
): String {
    if (selectedCurrency == null) return currentAmount
    
    return when (selectedCurrency.decimalPlaces) {
        0 -> {
            // For currencies with no decimal places (JPY, KRW), remove decimal point and everything after
            currentAmount.split(".")[0]
        }
        else -> {
            // For currencies with decimal places, limit to the specified number
            val parts = currentAmount.split(".")
            if (parts.size > 1) {
                parts[0] + "." + parts[1].take(selectedCurrency.decimalPlaces)
            } else {
                currentAmount
            }
        }
    }
}

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
    val selectedCurrency by viewModel.selectedCurrency.collectAsState()
    
    // Pre-fetch string resources to avoid calling them from non-@Composable contexts
    val merchantRequiredText = stringResource(R.string.merchant_required)
    val amountRequiredText = stringResource(R.string.amount_required)
    val invalidAmountText = stringResource(R.string.invalid_amount)
    val categoryRequiredText = stringResource(R.string.category_required)
    
    // Currency dropdown string resources
    val currencyLabelText = stringResource(R.string.currency)
    val currencyHintText = stringResource(R.string.currency_selection_hint)
    val currencyContentDesc = stringResource(R.string.select_currency)
    
    // Currency-specific validation messages
    val currencyValidationMessages = remember {
        mapOf(
            "JPY" to "Japanese Yen uses whole numbers only (no decimal places)",
            "KRW" to "Korean Won uses whole numbers only (no decimal places)"
        )
    }
    
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
        
        // Enhanced amount validation with currency-specific rules
        amountError = when {
            amount.isBlank() -> amountRequiredText
            amount.toDoubleOrNull() == null -> invalidAmountText
            amount.toDoubleOrNull()!! <= 0 -> invalidAmountText
            selectedCurrency?.let { currency ->
                when (currency.decimalPlaces) {
                    0 -> amount.contains(".")
                    else -> false
                }
            } == true -> currencyValidationMessages[selectedCurrency?.code] ?: "This currency doesn't use decimal places"
            else -> null
        }
        
        categoryError = if (category.isBlank()) categoryRequiredText else null
        
        // Form is valid only if all fields are valid AND currency is selected
        isFormValid = merchantError == null && amountError == null && categoryError == null && selectedCurrency != null
    }
    
    // Handle currency changes and update amount field formatting
    LaunchedEffect(selectedCurrency) {
        if (amount.isNotEmpty()) {
            // Update amount field formatting when currency changes
            val formattedAmount = validateAndFormatAmount(amount, selectedCurrency)
            if (formattedAmount != amount) {
                amount = formattedAmount
            }
        }
    }
    
    // Validate form whenever any field changes
    LaunchedEffect(merchant, amount, category, selectedCurrency) {
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
                        Icon(
                            Icons.Default.ArrowBack, 
                            contentDescription = "Back",
                            modifier = Modifier.graphicsLayer(
                                scaleX = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f
                            )
                        )
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
            
            // Amount field with currency-specific formatting
            OutlinedTextField(
                value = amount,
                onValueChange = { newValue ->
                    // Apply currency-specific validation and formatting
                    val formattedAmount = validateAndFormatAmount(newValue, selectedCurrency)
                    
                    amount = formattedAmount
                    amountError = when {
                        formattedAmount.isBlank() -> amountRequiredText
                        formattedAmount.toDoubleOrNull() == null -> invalidAmountText
                        formattedAmount.toDoubleOrNull()!! <= 0 -> invalidAmountText
                        else -> null
                    }
                },
                label = { Text(stringResource(R.string.amount)) },
                prefix = { 
                    selectedCurrency?.let { currency ->
                        Text(
                            text = currency.symbol,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                isError = amountError != null,
                supportingText = { 
                    amountError?.let { Text(it) } ?: 
                    selectedCurrency?.let { currency ->
                        Text("${currency.displayName} - ${currency.decimalPlaces} decimal places")
                    }
                },
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
            
            // Currency selection with ExposedDropdownMenu using CurrencyAdapter
            var currencyExpanded by remember { mutableStateOf(false) }
            val currencyAdapter = remember { CurrencyAdapter() }
            val currencies = remember { currencyAdapter.getSortedCurrencies() }
            
            val displayedCurrencyValue = currencyAdapter.getDisplayText(selectedCurrency)
            
            ExposedDropdownMenuBox(
                expanded = currencyExpanded,
                onExpandedChange = { currencyExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = displayedCurrencyValue,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text(currencyLabelText) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                        .semantics { contentDescription = currencyContentDesc },
                    singleLine = true,
                    supportingText = { Text(currencyHintText) }
                )
                
                ExposedDropdownMenu(
                    expanded = currencyExpanded,
                    onDismissRequest = { currencyExpanded = false },
                    modifier = Modifier
                        .exposedDropdownSize(true)
                        .heightIn(max = 350.dp)
                ) {
                    currencyAdapter.CurrencyDropdownMenu(
                        currencies = currencies,
                        selectedCurrency = selectedCurrency,
                        onCurrencySelected = { currency ->
                            viewModel.updateSelectedCurrency(currency)
                        },
                        onDismissRequest = { currencyExpanded = false }
                    )
                }
            }
            
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
                modifier = Modifier.padding(top = 8.dp),
                textAlign = TextAlign.Start
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
                    
                    // Only proceed if form is valid and currency is selected
                    if (isFormValid && selectedCurrency != null) {
                        val expenseData = ExpenseFormData(
                            merchant = merchant,
                            amount = amount.toDouble(),
                            currency = selectedCurrency!!.code,
                            category = category,
                            isRecurring = isRecurring,
                            notes = notes.ifBlank { null },
                            date = selectedDate
                        )
                        viewModel.saveExpense(expenseData, userId)
                    }
                },
                enabled = isFormValid && selectedCurrency != null && uiState !is AddExpenseUiState.Loading,
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
    val currency: String,
    val category: String,
    val isRecurring: Boolean,
    val notes: String?,
    val date: Date
)
