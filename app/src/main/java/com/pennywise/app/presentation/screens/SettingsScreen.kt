package com.pennywise.app.presentation.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.pennywise.app.R
import com.pennywise.app.presentation.viewmodel.SettingsViewModel
import com.pennywise.app.presentation.viewmodel.TestDataViewModel
import com.pennywise.app.presentation.util.LocaleManager
import com.pennywise.app.domain.model.PaymentMethod
import com.pennywise.app.domain.model.PaymentMethodConfig
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import javax.inject.Inject

/**
 * Credit card add/edit dialog (only for credit cards)
 */
@Composable
fun CreditCardDialog(
    isEdit: Boolean,
    config: PaymentMethodConfig?,
    onDismiss: () -> Unit,
    onSave: (String, Int?) -> Unit
) {
    var alias by remember { mutableStateOf(config?.alias ?: "") }
    var withdrawDay by remember { mutableStateOf(config?.withdrawDay?.toString() ?: "") }
    var aliasError by remember { mutableStateOf(false) }
    var withdrawDayError by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (isEdit) R.string.edit_credit_card else R.string.add_credit_card)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Alias field
                OutlinedTextField(
                    value = alias,
                    onValueChange = { 
                        alias = it
                        aliasError = false
                    },
                    label = { Text(stringResource(R.string.payment_method_alias)) },
                    placeholder = { Text(stringResource(R.string.alias_hint)) },
                    isError = aliasError,
                    supportingText = if (aliasError) {
                        { Text(stringResource(R.string.payment_method_alias_required)) }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Withdraw day field
                OutlinedTextField(
                    value = withdrawDay,
                    onValueChange = { 
                        if (it.isEmpty() || it.toIntOrNull() != null) {
                            withdrawDay = it
                            withdrawDayError = false
                        }
                    },
                    label = { Text(stringResource(R.string.withdraw_day)) },
                    placeholder = { Text(stringResource(R.string.withdraw_day_hint)) },
                    isError = withdrawDayError,
                    supportingText = if (withdrawDayError) {
                        { Text(stringResource(R.string.withdraw_day_required)) }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Validate
                    if (alias.isBlank()) {
                        aliasError = true
                        return@Button
                    }
                    
                    val day = withdrawDay.toIntOrNull()
                    if (day == null || day !in 1..31) {
                        withdrawDayError = true
                        return@Button
                    }
                    
                    onSave(alias, day)
                }
            ) {
                Text(stringResource(R.string.save))
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
 * Delete payment method confirmation dialog
 */
@Composable
fun DeletePaymentMethodDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete)) },
        text = { Text(stringResource(R.string.delete_payment_method_confirmation)) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.delete))
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
 * Collapsible section composable for settings groups
 */
@Composable
fun CollapsibleSection(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Header with title and expand/collapse icon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) stringResource(R.string.collapse) else stringResource(R.string.expand),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Animated content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(300)),
                exit = shrinkVertically(
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(300))
            ) {
                content()
            }
        }
    }
}

/**
 * Settings screen with theme selection, language selection, and cloud backup placeholder
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    testDataViewModel: TestDataViewModel = hiltViewModel()
) {
    val themeMode by viewModel.themeMode.collectAsState(initial = SettingsViewModel.ThemeMode.SYSTEM)
    val language by viewModel.language.collectAsState(initial = "")
    val defaultCurrencyState by viewModel.defaultCurrencyState.collectAsState(initial = SettingsViewModel.DefaultCurrencyState.Loading)
    val currencyUpdateState by viewModel.currencyUpdateState.collectAsState(initial = SettingsViewModel.CurrencyUpdateState.Idle)
    val merchantIconsEnabled by viewModel.merchantIconsEnabled.collectAsState(initial = false)
    val merchantIconsWifiOnly by viewModel.merchantIconsWifiOnly.collectAsState(initial = true)
    
    // Payment method states
    val paymentMethodConfigs by viewModel.paymentMethodConfigs.collectAsState(initial = emptyList())
    val defaultPaymentMethodState by viewModel.defaultPaymentMethodState.collectAsState(initial = SettingsViewModel.DefaultPaymentMethodState.Loading)
    val paymentMethodUpdateState by viewModel.paymentMethodUpdateState.collectAsState(initial = SettingsViewModel.PaymentMethodUpdateState.Idle)
    val defaultPaymentType by viewModel.defaultPaymentType.collectAsState(initial = PaymentMethod.CASH)
    
    // Authentication method states
    val currentAuthMethod by viewModel.currentAuthMethod.collectAsState(initial = null)
    val authMethodUpdateState by viewModel.authMethodUpdateState.collectAsState(initial = SettingsViewModel.AuthMethodUpdateState.Idle)
    val canUseBiometric = viewModel.canUseBiometric
    val canUseDeviceCredentials = viewModel.canUseDeviceCredentials
    
    // State for tracking which sections are expanded (all collapsed by default)
    var isAppearanceExpanded by remember { mutableStateOf(false) }
    var isLanguageExpanded by remember { mutableStateOf(false) }
    var isDefaultCurrencyExpanded by remember { mutableStateOf(false) }
    var isPaymentMethodsExpanded by remember { mutableStateOf(false) }
    var isCreditCardsExpanded by remember { mutableStateOf(false) }
    var isAccountExpanded by remember { mutableStateOf(false) }
    var isAdvancedExpanded by remember { mutableStateOf(false) }
    var isDeveloperOptionsExpanded by remember { mutableStateOf(false) }
    
    // Developer options state
    val developerOptionsEnabled by viewModel.developerOptionsEnabled.collectAsState(initial = false)
    
    // Test data state
    val testDataMessage by testDataViewModel.message.collectAsState(initial = "")
    val isSeeding by testDataViewModel.isSeeding.collectAsState(initial = false)
    val isClearing by testDataViewModel.isClearing.collectAsState(initial = false)

    // Backup/restore state
    val backupRestoreState by viewModel.backupRestoreState.collectAsState(
        initial = SettingsViewModel.BackupRestoreState.Idle
    )
    val isBackupInProgress = backupRestoreState is SettingsViewModel.BackupRestoreState.BackupInProgress
    val isRestoreInProgress = backupRestoreState is SettingsViewModel.BackupRestoreState.RestoreInProgress
    val backupRestoreMessage = when (backupRestoreState) {
        is SettingsViewModel.BackupRestoreState.Success -> {
            when ((backupRestoreState as SettingsViewModel.BackupRestoreState.Success).operation) {
                SettingsViewModel.BackupRestoreOperation.BACKUP -> stringResource(R.string.backup_success)
                SettingsViewModel.BackupRestoreOperation.RESTORE -> stringResource(R.string.restore_success)
            }
        }
        is SettingsViewModel.BackupRestoreState.Error -> {
            val error = (backupRestoreState as SettingsViewModel.BackupRestoreState.Error).message
            val errorDetail = error?.takeIf { it.isNotBlank() } ?: stringResource(R.string.unknown_error)
            when ((backupRestoreState as SettingsViewModel.BackupRestoreState.Error).operation) {
                SettingsViewModel.BackupRestoreOperation.BACKUP -> stringResource(
                    R.string.backup_failed_with_reason,
                    errorDetail
                )
                SettingsViewModel.BackupRestoreOperation.RESTORE -> stringResource(
                    R.string.restore_failed_with_reason,
                    errorDetail
                )
            }
        }
        else -> null
    }
    
    // Tap counter for app version
    var tapCount by remember { mutableStateOf(0) }
    var lastTapTime by remember { mutableStateOf(0L) }

    var showRestoreConfirm by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            viewModel.backupDatabase(uri)
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingRestoreUri = uri
            showRestoreConfirm = true
        }
    }
    
    // Dialog states for payment methods
    var showAddPaymentMethodDialog by remember { mutableStateOf(false) }
    var showEditPaymentMethodDialog by remember { mutableStateOf(false) }
    var showDeletePaymentMethodDialog by remember { mutableStateOf(false) }
    var selectedPaymentMethodConfig by remember { mutableStateOf<PaymentMethodConfig?>(null) }
    
    // Auto-expand credit cards when Credit Card is the default payment type
    LaunchedEffect(defaultPaymentType) {
        if (defaultPaymentType == PaymentMethod.CREDIT_CARD) {
            isCreditCardsExpanded = true
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Theme settings section
            item {
                CollapsibleSection(
                    title = stringResource(R.string.appearance),
                    isExpanded = isAppearanceExpanded,
                    onToggle = { isAppearanceExpanded = !isAppearanceExpanded }
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        ThemeOption(
                            title = stringResource(R.string.light_theme),
                            selected = themeMode == SettingsViewModel.ThemeMode.LIGHT,
                            onClick = { viewModel.setThemeMode(SettingsViewModel.ThemeMode.LIGHT) },
                            icon = Icons.Default.LightMode
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = DividerDefaults.Thickness,
                            color = DividerDefaults.color
                        )

                        ThemeOption(
                            title = stringResource(R.string.dark_theme),
                            selected = themeMode == SettingsViewModel.ThemeMode.DARK,
                            onClick = { viewModel.setThemeMode(SettingsViewModel.ThemeMode.DARK) },
                            icon = Icons.Default.DarkMode
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = DividerDefaults.Thickness,
                            color = DividerDefaults.color
                        )

                        ThemeOption(
                            title = stringResource(R.string.system_theme),
                            selected = themeMode == SettingsViewModel.ThemeMode.SYSTEM,
                            onClick = { viewModel.setThemeMode(SettingsViewModel.ThemeMode.SYSTEM) },
                            icon = Icons.Default.SettingsSuggest
                        )
                    }
                }
            }
            
            // Language settings section
            item {
                CollapsibleSection(
                    title = stringResource(R.string.language),
                    isExpanded = isLanguageExpanded,
                    onToggle = { isLanguageExpanded = !isLanguageExpanded }
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        LanguageOption(
                            title = stringResource(R.string.language_english),
                            selected = language.isEmpty() || language == "en",
                            onClick = { viewModel.setLanguage("en") }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = DividerDefaults.Thickness,
                            color = DividerDefaults.color
                        )

                        LanguageOption(
                            title = stringResource(R.string.language_hebrew),
                            selected = language == "iw",
                            onClick = { viewModel.setLanguage("iw") }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = DividerDefaults.Thickness,
                            color = DividerDefaults.color
                        )

                        LanguageOption(
                            title = stringResource(R.string.language_russian),
                            selected = language == "ru",
                            onClick = { viewModel.setLanguage("ru") }
                        )
                    }
                }
            }
            
            // Default currency section
            item {
                CollapsibleSection(
                    title = stringResource(R.string.default_currency),
                    isExpanded = isDefaultCurrencyExpanded,
                    onToggle = { isDefaultCurrencyExpanded = !isDefaultCurrencyExpanded }
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        when (defaultCurrencyState) {
                            is SettingsViewModel.DefaultCurrencyState.Loading -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.loading),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                            is SettingsViewModel.DefaultCurrencyState.Success -> {
                                Text(
                                    text = stringResource(R.string.current_default_currency),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                                
                                Text(
                                    text = (defaultCurrencyState as SettingsViewModel.DefaultCurrencyState.Success).currencyCode,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                )
                                
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                                
                                Text(
                                    text = stringResource(R.string.change_default_currency),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                                
                                // Currency options for default currency
                                CurrencyOption(
                                    title = stringResource(R.string.currency_usd),
                                    selected = (defaultCurrencyState as SettingsViewModel.DefaultCurrencyState.Success).currencyCode == "USD",
                                    onClick = { viewModel.updateDefaultCurrency("USD") }
                                )
                                
                                CurrencyOption(
                                    title = stringResource(R.string.currency_eur),
                                    selected = (defaultCurrencyState as SettingsViewModel.DefaultCurrencyState.Success).currencyCode == "EUR",
                                    onClick = { viewModel.updateDefaultCurrency("EUR") }
                                )
                                
                                CurrencyOption(
                                    title = stringResource(R.string.currency_gbp),
                                    selected = (defaultCurrencyState as SettingsViewModel.DefaultCurrencyState.Success).currencyCode == "GBP",
                                    onClick = { viewModel.updateDefaultCurrency("GBP") }
                                )
                                
                                CurrencyOption(
                                    title = stringResource(R.string.currency_ils),
                                    selected = (defaultCurrencyState as SettingsViewModel.DefaultCurrencyState.Success).currencyCode == "ILS",
                                    onClick = { viewModel.updateDefaultCurrency("ILS") }
                                )
                                
                                CurrencyOption(
                                    title = stringResource(R.string.currency_rub),
                                    selected = (defaultCurrencyState as SettingsViewModel.DefaultCurrencyState.Success).currencyCode == "RUB",
                                    onClick = { viewModel.updateDefaultCurrency("RUB") }
                                )
                                
                                CurrencyOption(
                                    title = stringResource(R.string.currency_jpy),
                                    selected = (defaultCurrencyState as SettingsViewModel.DefaultCurrencyState.Success).currencyCode == "JPY",
                                    onClick = { viewModel.updateDefaultCurrency("JPY") }
                                )
                                
                                CurrencyOption(
                                    title = stringResource(R.string.currency_cad),
                                    selected = (defaultCurrencyState as SettingsViewModel.DefaultCurrencyState.Success).currencyCode == "CAD",
                                    onClick = { viewModel.updateDefaultCurrency("CAD") }
                                )
                                
                                CurrencyOption(
                                    title = stringResource(R.string.currency_aud),
                                    selected = (defaultCurrencyState as SettingsViewModel.DefaultCurrencyState.Success).currencyCode == "AUD",
                                    onClick = { viewModel.updateDefaultCurrency("AUD") }
                                )
                                
                                CurrencyOption(
                                    title = stringResource(R.string.currency_chf),
                                    selected = (defaultCurrencyState as SettingsViewModel.DefaultCurrencyState.Success).currencyCode == "CHF",
                                    onClick = { viewModel.updateDefaultCurrency("CHF") }
                                )
                                
                                CurrencyOption(
                                    title = stringResource(R.string.currency_cny),
                                    selected = (defaultCurrencyState as SettingsViewModel.DefaultCurrencyState.Success).currencyCode == "CNY",
                                    onClick = { viewModel.updateDefaultCurrency("CNY") }
                                )
                                
                                CurrencyOption(
                                    title = stringResource(R.string.currency_inr),
                                    selected = (defaultCurrencyState as SettingsViewModel.DefaultCurrencyState.Success).currencyCode == "INR",
                                    onClick = { viewModel.updateDefaultCurrency("INR") }
                                )
                            }
                            is SettingsViewModel.DefaultCurrencyState.Error -> {
                                Text(
                                    text = (defaultCurrencyState as SettingsViewModel.DefaultCurrencyState.Error).message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                )
                            }
                        }
                        
                        // Show update state
                        when (currencyUpdateState) {
                            is SettingsViewModel.CurrencyUpdateState.Loading -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.updating_currency),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                            is SettingsViewModel.CurrencyUpdateState.Success -> {
                                Text(
                                    text = stringResource(R.string.currency_updated_successfully),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                                LaunchedEffect(Unit) {
                                    kotlinx.coroutines.delay(2000)
                                    viewModel.resetCurrencyUpdateState()
                                }
                            }
                            is SettingsViewModel.CurrencyUpdateState.Error -> {
                                Text(
                                    text = (currencyUpdateState as SettingsViewModel.CurrencyUpdateState.Error).message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                            else -> { /* Idle state, do nothing */ }
                        }
                    }
                }
            }
            
            // Payment Methods section (Two-level hierarchy)
            item {
                CollapsibleSection(
                    title = stringResource(R.string.payment_methods),
                    isExpanded = isPaymentMethodsExpanded,
                    onToggle = { isPaymentMethodsExpanded = !isPaymentMethodsExpanded }
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        // Level 1: Default Payment Type Selection
                        Text(
                            text = stringResource(R.string.default_payment_type),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        
                        // Cash option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setDefaultPaymentType(PaymentMethod.CASH) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = defaultPaymentType == PaymentMethod.CASH,
                                onClick = { viewModel.setDefaultPaymentType(PaymentMethod.CASH) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.payment_method_cash),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        
                        // Credit Card option with inline expandable management
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setDefaultPaymentType(PaymentMethod.CREDIT_CARD) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = defaultPaymentType == PaymentMethod.CREDIT_CARD,
                                    onClick = { viewModel.setDefaultPaymentType(PaymentMethod.CREDIT_CARD) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.payment_method_credit_card),
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                
                                // Expand/collapse icon (only visible when Credit Card is selected)
                                if (defaultPaymentType == PaymentMethod.CREDIT_CARD) {
                                    IconButton(
                                        onClick = { isCreditCardsExpanded = !isCreditCardsExpanded }
                                    ) {
                                        Icon(
                                            imageVector = if (isCreditCardsExpanded) 
                                                Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = if (isCreditCardsExpanded) 
                                                stringResource(R.string.collapse) else stringResource(R.string.expand),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            
                            // Credit card management content (only when Credit Card is selected)
                            AnimatedVisibility(
                                visible = defaultPaymentType == PaymentMethod.CREDIT_CARD && isCreditCardsExpanded,
                                enter = expandVertically(animationSpec = tween(300, easing = FastOutSlowInEasing)) + fadeIn(animationSpec = tween(300)),
                                exit = shrinkVertically(animationSpec = tween(300, easing = FastOutSlowInEasing)) + fadeOut(animationSpec = tween(300))
                            ) {
                                Column(
                                    modifier = Modifier.padding(start = 48.dp, end = 16.dp, top = 8.dp, bottom = 16.dp)
                                ) {
                        // Show current credit card configurations
                        when (defaultPaymentMethodState) {
                            is SettingsViewModel.DefaultPaymentMethodState.Loading -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.loading),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                            is SettingsViewModel.DefaultPaymentMethodState.Success -> {
                                val defaultConfig = (defaultPaymentMethodState as SettingsViewModel.DefaultPaymentMethodState.Success).config
                                
                                if (paymentMethodConfigs.isEmpty()) {
                                    // No payment methods configured
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CreditCard,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = stringResource(R.string.no_payment_methods_title),
                                            style = MaterialTheme.typography.titleMedium,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = stringResource(R.string.no_payment_methods_description),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(
                                            onClick = { showAddPaymentMethodDialog = true }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(stringResource(R.string.add_payment_method))
                                        }
                                    }
                                } else {
                                    // Show all credit cards
                                    paymentMethodConfigs.forEach { config ->
                                        PaymentMethodConfigItem(
                                            config = config,
                                            isDefault = config.id == defaultConfig?.id,
                                            onSetAsDefault = { viewModel.setDefaultPaymentMethodConfig(config.id) },
                                            onEdit = { 
                                                selectedPaymentMethodConfig = config
                                                showEditPaymentMethodDialog = true
                                            },
                                            onDelete = { 
                                                selectedPaymentMethodConfig = config
                                                showDeletePaymentMethodDialog = true
                                            }
                                        )
                                    }
                                    
                                    // Add new credit card button
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = { showAddPaymentMethodDialog = true },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(stringResource(R.string.add_credit_card))
                                    }
                                }
                            }
                            is SettingsViewModel.DefaultPaymentMethodState.Error -> {
                                Text(
                                    text = (defaultPaymentMethodState as SettingsViewModel.DefaultPaymentMethodState.Error).message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                )
                            }
                        }
                        
                        // Show update state
                        when (paymentMethodUpdateState) {
                            is SettingsViewModel.PaymentMethodUpdateState.Loading -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.updating),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                            is SettingsViewModel.PaymentMethodUpdateState.Success -> {
                                Text(
                                    text = (paymentMethodUpdateState as SettingsViewModel.PaymentMethodUpdateState.Success).message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                                LaunchedEffect(Unit) {
                                    kotlinx.coroutines.delay(2000)
                                    viewModel.resetPaymentMethodUpdateState()
                                }
                            }
                            is SettingsViewModel.PaymentMethodUpdateState.Error -> {
                                Text(
                                    text = (paymentMethodUpdateState as SettingsViewModel.PaymentMethodUpdateState.Error).message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                            else -> { /* Idle state, do nothing */ }
                        }
                                }
                            }
                        }
                        
                        // Cheque option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setDefaultPaymentType(PaymentMethod.CHEQUE) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = defaultPaymentType == PaymentMethod.CHEQUE,
                                onClick = { viewModel.setDefaultPaymentType(PaymentMethod.CHEQUE) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.payment_method_cheque),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
            
            // Security section
            item {
                CollapsibleSection(
                    title = stringResource(R.string.security),
                    isExpanded = isAccountExpanded,
                    onToggle = { isAccountExpanded = !isAccountExpanded }
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.authentication_method),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        
                        // Biometric option
                        if (canUseBiometric) {
                            AuthMethodOption(
                                title = stringResource(R.string.auth_method_biometric),
                                description = stringResource(R.string.auth_method_biometric_description),
                                icon = Icons.Default.Fingerprint,
                                isSelected = currentAuthMethod == SettingsViewModel.AuthMethod.BIOMETRIC,
                                onClick = { viewModel.updateAuthMethod(SettingsViewModel.AuthMethod.BIOMETRIC) }
                            )
                        }
                        
                        // Device credentials option
                        if (canUseDeviceCredentials) {
                            AuthMethodOption(
                                title = stringResource(R.string.auth_method_device_credentials),
                                description = stringResource(R.string.auth_method_device_credentials_description),
                                icon = Icons.Default.Lock,
                                isSelected = currentAuthMethod == SettingsViewModel.AuthMethod.DEVICE_CREDENTIALS,
                                onClick = { viewModel.updateAuthMethod(SettingsViewModel.AuthMethod.DEVICE_CREDENTIALS) }
                            )
                        }
                        
                        // No authentication option
                        AuthMethodOption(
                            title = stringResource(R.string.auth_method_none),
                            description = stringResource(R.string.auth_method_none_description),
                            icon = Icons.Default.LockOpen,
                            isSelected = currentAuthMethod == SettingsViewModel.AuthMethod.NONE,
                            onClick = { viewModel.updateAuthMethod(SettingsViewModel.AuthMethod.NONE) }
                        )
                        
                        // Show update state
                        when (authMethodUpdateState) {
                            is SettingsViewModel.AuthMethodUpdateState.Loading -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.updating_auth_method),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                            is SettingsViewModel.AuthMethodUpdateState.Success -> {
                                Text(
                                    text = stringResource(R.string.auth_method_updated),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                                LaunchedEffect(Unit) {
                                    kotlinx.coroutines.delay(2000)
                                    viewModel.resetAuthMethodUpdateState()
                                }
                            }
                            is SettingsViewModel.AuthMethodUpdateState.Error -> {
                                Text(
                                    text = (authMethodUpdateState as SettingsViewModel.AuthMethodUpdateState.Error).message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                            else -> { /* Idle state, do nothing */ }
                        }
                    }
                }
            }

            // Advanced section
            item {
                CollapsibleSection(
                    title = stringResource(R.string.advanced),
                    isExpanded = isAdvancedExpanded,
                    onToggle = { isAdvancedExpanded = !isAdvancedExpanded }
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.merchant_icons),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = stringResource(R.string.merchant_icons_description),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = merchantIconsEnabled,
                                onCheckedChange = { viewModel.setMerchantIconsEnabled(it) }
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.merchant_icons_wifi_only),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = stringResource(R.string.merchant_icons_wifi_only_description),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = merchantIconsWifiOnly,
                                onCheckedChange = { viewModel.setMerchantIconsWifiOnly(it) },
                                enabled = merchantIconsEnabled
                            )
                        }
                    }
                }
            }
            
            // Developer options section (only show if enabled)
            if (developerOptionsEnabled) {
                item {
                    CollapsibleSection(
                        title = stringResource(R.string.developer_options),
                        isExpanded = isDeveloperOptionsExpanded,
                        onToggle = { isDeveloperOptionsExpanded = !isDeveloperOptionsExpanded }
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.test_data_controls),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            
                            // Seed test data button
                            Button(
                                onClick = { testDataViewModel.seedTestData() },
                                enabled = !isSeeding && !isClearing,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                            ) {
                                if (isSeeding) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(
                                    text = if (isSeeding) stringResource(R.string.seeding_data) else stringResource(R.string.seed_test_data)
                                )
                            }
                            
                            // Clear test data button
                            Button(
                                onClick = { testDataViewModel.clearTestData() },
                                enabled = !isSeeding && !isClearing,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                if (isClearing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = MaterialTheme.colorScheme.onError
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(
                                    text = if (isClearing) stringResource(R.string.clearing_data) else stringResource(R.string.clear_test_data)
                                )
                            }
                            
                            // Show test data messages
                            testDataMessage?.let { message ->
                                if (message.isNotEmpty()) {
                                    Text(
                                        text = message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (message.startsWith("✅")) 
                                            MaterialTheme.colorScheme.primary 
                                        else 
                                            MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

                            Text(
                                text = stringResource(R.string.database_backup_restore),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )

                            Button(
                                onClick = { backupLauncher.launch("pennywise_backup.db") },
                                enabled = !isBackupInProgress && !isRestoreInProgress,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                            ) {
                                if (isBackupInProgress) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(
                                    text = if (isBackupInProgress) {
                                        stringResource(R.string.backing_up_database)
                                    } else {
                                        stringResource(R.string.backup_database)
                                    }
                                )
                            }

                            Button(
                                onClick = {
                                    restoreLauncher.launch(
                                        arrayOf(
                                            "application/octet-stream",
                                            "application/x-sqlite3",
                                            "application/vnd.sqlite3"
                                        )
                                    )
                                },
                                enabled = !isBackupInProgress && !isRestoreInProgress,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                if (isRestoreInProgress) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = MaterialTheme.colorScheme.onError
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(
                                    text = if (isRestoreInProgress) {
                                        stringResource(R.string.restoring_database)
                                    } else {
                                        stringResource(R.string.restore_database)
                                    }
                                )
                            }

                            backupRestoreMessage?.let { message ->
                                Text(
                                    text = message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (backupRestoreState is SettingsViewModel.BackupRestoreState.Success) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.error
                                    },
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
            
            // App version info with tap counter
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp, bottom = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.app_version),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable {
                            val currentTime = System.currentTimeMillis()
                            
                            // Reset counter if more than 2 seconds have passed
                            if (currentTime - lastTapTime > 2000) {
                                tapCount = 0
                            }
                            
                            tapCount++
                            lastTapTime = currentTime
                            
                            // Enable developer options after 12 taps
                            if (tapCount >= 12 && !developerOptionsEnabled) {
                                viewModel.setDeveloperOptionsEnabled(true)
                                tapCount = 0
                            }
                            // Disable developer options after 6 taps (when already enabled)
                            else if (tapCount >= 6 && developerOptionsEnabled) {
                                viewModel.setDeveloperOptionsEnabled(false)
                                isDeveloperOptionsExpanded = false
                                tapCount = 0
                            }
                        }
                    )
                }
            }
            
            // Copyright
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.copyright),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
    
    // Credit Card Dialogs
    if (showAddPaymentMethodDialog) {
        CreditCardDialog(
            isEdit = false,
            config = null,
            onDismiss = { showAddPaymentMethodDialog = false },
            onSave = { alias, withdrawDay ->
                viewModel.addPaymentMethodConfig(PaymentMethod.CREDIT_CARD, alias, withdrawDay)
                showAddPaymentMethodDialog = false
            }
        )
    }

    if (showEditPaymentMethodDialog && selectedPaymentMethodConfig != null) {
        CreditCardDialog(
            isEdit = true,
            config = selectedPaymentMethodConfig,
            onDismiss = { 
                showEditPaymentMethodDialog = false
                selectedPaymentMethodConfig = null
            },
            onSave = { alias, withdrawDay ->
                val updated = selectedPaymentMethodConfig!!.copy(
                    paymentMethod = PaymentMethod.CREDIT_CARD,
                    alias = alias,
                    withdrawDay = withdrawDay,
                    updatedAt = System.currentTimeMillis()
                )
                viewModel.updatePaymentMethodConfig(updated)
                showEditPaymentMethodDialog = false
                selectedPaymentMethodConfig = null
            }
        )
    }

    if (showDeletePaymentMethodDialog && selectedPaymentMethodConfig != null) {
        DeletePaymentMethodDialog(
            onDismiss = { 
                showDeletePaymentMethodDialog = false
                selectedPaymentMethodConfig = null
            },
            onConfirm = {
                viewModel.deletePaymentMethodConfig(selectedPaymentMethodConfig!!.id)
                showDeletePaymentMethodDialog = false
                selectedPaymentMethodConfig = null
            }
        )
    }

    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = {
                showRestoreConfirm = false
                pendingRestoreUri = null
            },
            title = { Text(stringResource(R.string.restore_database_title)) },
            text = { Text(stringResource(R.string.restore_database_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        val uri = pendingRestoreUri
                        if (uri != null) {
                            viewModel.restoreDatabase(uri)
                        }
                        showRestoreConfirm = false
                        pendingRestoreUri = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.restore))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRestoreConfirm = false
                        pendingRestoreUri = null
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun ThemeOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .padding(start = 16.dp)
                .weight(1f)
        )
        
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
fun LanguageOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
fun CurrencyOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

/**
 * Payment method configuration item composable
 */
@Composable
fun PaymentMethodConfigItem(
    config: PaymentMethodConfig,
    isDefault: Boolean,
    onSetAsDefault: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Payment method icon
                Icon(
                    imageVector = when (config.paymentMethod) {
                        PaymentMethod.CASH -> Icons.Default.AccountBalanceWallet
                        PaymentMethod.CREDIT_CARD -> Icons.Default.CreditCard
                        PaymentMethod.CHEQUE -> Icons.Default.Description
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Payment method info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        // Show only the alias for credit cards (payment type is implied)
                        text = config.alias.ifBlank { config.paymentMethod.displayName },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isDefault) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                    )
                    
                    if (config.withdrawDay != null) {
                        Text(
                            text = "${stringResource(R.string.withdraw_day)}: ${config.withdrawDay}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Star icon - filled for default, outline for non-default
                IconButton(
                    onClick = onSetAsDefault,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (isDefault) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = stringResource(R.string.set_as_default),
                        tint = if (isDefault) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.edit),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Authentication method option composable
 */
@Composable
fun AuthMethodOption(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        
        Column(
            modifier = Modifier
                .padding(start = 16.dp)
                .weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}
