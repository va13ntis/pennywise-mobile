package com.pennywise.app.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pennywise.app.R
import com.pennywise.app.presentation.viewmodel.AuthMethod
import com.pennywise.app.presentation.viewmodel.FirstRunStep
import com.pennywise.app.domain.model.PaymentMethod
import com.pennywise.app.presentation.viewmodel.FirstRunSetupViewModel

/**
 * Screen for first-time app setup
 * Automatically configures user settings and presents device authentication setup
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirstRunSetupScreen(
    onSetupComplete: () -> Unit,
    viewModel: FirstRunSetupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    LaunchedEffect(uiState.isSetupComplete) {
        if (uiState.isSetupComplete) {
            onSetupComplete()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        
        // Welcome title
        Text(
            text = stringResource(R.string.welcome_to_pennywise),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Welcome description
        Text(
            text = stringResource(R.string.welcome_description),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Setup progress
        if (uiState.isLoading) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = uiState.loadingMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            when (uiState.step) {
                FirstRunStep.LANGUAGE -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Public,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = stringResource(R.string.language_region_title),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            val supported = remember { com.pennywise.app.presentation.util.LocaleManager().getSupportedLocales() }
                            supported.forEach { (code, name) ->
                                AuthMethodCard(
                                    title = name,
                                    description = "",
                                    icon = null,
                                    isSelected = uiState.selectedLanguage == code,
                                    onClick = { viewModel.setLanguage(code) }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // No back button for first step
                                Spacer(modifier = Modifier.width(0.dp))
                                
                                Button(
                                    onClick = { viewModel.continueFromLanguage() },
                                    enabled = uiState.selectedLanguage != null
                                ) {
                                    Text(stringResource(R.string.next_step))
                                }
                            }
                        }
                    }
                }
                
                FirstRunStep.AUTH -> {
                    // Device authentication setup
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = stringResource(R.string.device_auth_setup_title),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = stringResource(R.string.device_auth_setup_subtitle),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Text(
                                text = stringResource(R.string.device_auth_setup_description),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            // Authentication method selection
                            if (uiState.canUseBiometric || uiState.canUseDeviceCredentials) {
                                Text(
                                    text = stringResource(R.string.choose_auth_method),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                // Biometric option
                                if (uiState.canUseBiometric) {
                                    AuthMethodCard(
                                        title = stringResource(R.string.biometric_auth_title),
                                        description = stringResource(R.string.biometric_auth_description),
                                        icon = Icons.Default.Fingerprint,
                                        isSelected = uiState.selectedAuthMethod == AuthMethod.BIOMETRIC,
                                        onClick = { viewModel.selectAuthMethod(AuthMethod.BIOMETRIC) }
                                    )
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                
                                // Device credentials option
                                if (uiState.canUseDeviceCredentials) {
                                    AuthMethodCard(
                                        title = stringResource(R.string.device_credentials_title),
                                        description = stringResource(R.string.device_credentials_description),
                                        icon = Icons.Default.Lock,
                                        isSelected = uiState.selectedAuthMethod == AuthMethod.DEVICE_CREDENTIALS,
                                        onClick = { viewModel.selectAuthMethod(AuthMethod.DEVICE_CREDENTIALS) }
                                    )
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                
                                // No authentication option
                                AuthMethodCard(
                                    title = stringResource(R.string.no_auth_title),
                                    description = stringResource(R.string.no_auth_description),
                                    icon = Icons.Default.LockOpen,
                                    isSelected = uiState.selectedAuthMethod == AuthMethod.NONE,
                                    onClick = { viewModel.selectAuthMethod(AuthMethod.NONE) }
                                )
                                
                                Spacer(modifier = Modifier.height(20.dp))
                            }
                            
                            // Action buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Back button (not shown for first step)
                                if (uiState.step != FirstRunStep.LANGUAGE) {
                                    OutlinedButton(
                                        onClick = { viewModel.goBack() }
                                    ) {
                                        Text(stringResource(R.string.back))
                                    }
                                } else {
                                    Spacer(modifier = Modifier.width(0.dp))
                                }
                                
                                Button(
                                    onClick = { viewModel.setupSelectedAuth() },
                                    enabled = uiState.selectedAuthMethod != null
                                ) {
                                    Text(stringResource(R.string.next_step))
                                }
                            }
                            
                            if (!uiState.canUseBiometric && !uiState.canUseDeviceCredentials) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.device_auth_not_available),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
                
                FirstRunStep.CURRENCY -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Paid,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = stringResource(R.string.default_currency_title),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            listOf(
                                "USD" to "$",
                                "ILS" to "₪",
                                "RUB" to "₽",
                                "EUR" to "€"
                            ).forEach { (code, symbol) ->
                                AuthMethodCard(
                                    title = code,
                                    description = "",
                                    icon = null,
                                    currencySymbol = symbol,
                                    isSelected = uiState.selectedCurrency == code,
                                    onClick = { viewModel.setCurrency(code) }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.goBack() }
                                ) {
                                    Text(stringResource(R.string.back))
                                }
                                
                                Button(
                                    onClick = { viewModel.continueFromCurrency() },
                                    enabled = uiState.selectedCurrency != null
                                ) {
                                    Text(stringResource(R.string.next_step))
                                }
                            }
                        }
                    }
                }
                
                FirstRunStep.PAYMENT_METHOD -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = stringResource(R.string.default_payment_method),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            listOf(PaymentMethod.CASH, PaymentMethod.CREDIT_CARD, PaymentMethod.CHEQUE).forEach { method ->
                                val title = when (method) {
                                    PaymentMethod.CASH -> stringResource(R.string.payment_method_cash)
                                    PaymentMethod.CREDIT_CARD -> stringResource(R.string.payment_method_credit_card)
                                    PaymentMethod.CHEQUE -> stringResource(R.string.payment_method_cheque)
                                }
                                AuthMethodCard(
                                    title = title,
                                    description = "",
                                    icon = null,
                                    isSelected = uiState.selectedPaymentMethod == method,
                                    onClick = { viewModel.setPaymentMethod(method) }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.goBack() }
                                ) {
                                    Text(stringResource(R.string.back))
                                }
                                
                                Button(
                                    onClick = { viewModel.continueFromPaymentMethod() },
                                    enabled = uiState.selectedPaymentMethod != null
                                ) {
                                    Text(stringResource(R.string.next_step))
                                }
                            }
                        }
                    }
                }
                
                FirstRunStep.SUMMARY -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = stringResource(R.string.setup_summary_title),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(stringResource(R.string.setup_summary_language, uiState.selectedLanguage?.let { com.pennywise.app.presentation.util.LocaleManager().getLanguageDisplayName(it) } ?: stringResource(R.string.not_selected)))
                            Text(stringResource(R.string.setup_summary_currency, uiState.selectedCurrency ?: stringResource(R.string.not_selected)))
                            Text(stringResource(R.string.setup_summary_payment_method, uiState.selectedPaymentMethod?.let { method ->
                                when (method) {
                                    PaymentMethod.CASH -> stringResource(R.string.payment_method_cash)
                                    PaymentMethod.CREDIT_CARD -> stringResource(R.string.payment_method_credit_card)
                                    PaymentMethod.CHEQUE -> stringResource(R.string.payment_method_cheque)
                                }
                            } ?: stringResource(R.string.not_selected)))
                            Spacer(modifier = Modifier.height(20.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.goBack() }
                                ) {
                                    Text(stringResource(R.string.back))
                                }
                                
                                Button(onClick = { viewModel.finishSetup() }) {
                                    Text(stringResource(R.string.finish_setup))
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Error handling
        uiState.errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

/**
 * Card component for selecting authentication method
 */
@Composable
private fun AuthMethodCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    currencySymbol: String? = null,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = onClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(
                2.dp,
                MaterialTheme.colorScheme.primary
            )
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currencySymbol != null) {
                Text(
                    text = currencySymbol,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .size(24.dp),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.CenterVertically),
                    tint = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                
                if (description.isNotEmpty()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
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
}