package com.pennywise.app.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.pennywise.app.R
import com.pennywise.app.presentation.viewmodel.SettingsViewModel
import com.pennywise.app.presentation.util.LocaleManager
import javax.inject.Inject

/**
 * Settings screen with theme selection, language selection, and cloud backup placeholder
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val themeMode by viewModel.themeMode.collectAsState(initial = SettingsViewModel.ThemeMode.SYSTEM)
    val language by viewModel.language.collectAsState(initial = "")
    val currencyConversionEnabled by viewModel.currencyConversionEnabled.collectAsState(initial = false)
    val originalCurrency by viewModel.originalCurrency.collectAsState(initial = "")
    val defaultCurrencyState by viewModel.defaultCurrencyState.collectAsState(initial = SettingsViewModel.DefaultCurrencyState.Loading)
    val currencyUpdateState by viewModel.currencyUpdateState.collectAsState(initial = SettingsViewModel.CurrencyUpdateState.Idle)
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack, 
                            contentDescription = stringResource(R.string.back),
                            modifier = Modifier.graphicsLayer(
                                scaleX = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f
                            )
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
                Text(
                    text = stringResource(R.string.appearance),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                        
                        Divider(modifier = Modifier.padding(horizontal = 16.dp))
                        
                        ThemeOption(
                            title = stringResource(R.string.dark_theme),
                            selected = themeMode == SettingsViewModel.ThemeMode.DARK,
                            onClick = { viewModel.setThemeMode(SettingsViewModel.ThemeMode.DARK) },
                            icon = Icons.Default.DarkMode
                        )
                        
                        Divider(modifier = Modifier.padding(horizontal = 16.dp))
                        
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
                Text(
                    text = stringResource(R.string.language),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 24.dp, bottom = 16.dp)
                )
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        LanguageOption(
                            title = "English",
                            selected = language.isEmpty() || language == "en",
                            onClick = { viewModel.setLanguage("en") }
                        )
                        
                        Divider(modifier = Modifier.padding(horizontal = 16.dp))
                        
                        LanguageOption(
                            title = "עברית",
                            selected = language == "iw",
                            onClick = { viewModel.setLanguage("iw") }
                        )
                        
                        Divider(modifier = Modifier.padding(horizontal = 16.dp))
                        
                        LanguageOption(
                            title = "Русский",
                            selected = language == "ru",
                            onClick = { viewModel.setLanguage("ru") }
                        )
                    }
                }
            }
            
            // Default currency section
            item {
                Text(
                    text = stringResource(R.string.default_currency),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 24.dp, bottom = 16.dp)
                )
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                                
                                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                                
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
            
            // Currency conversion section
            item {
                Text(
                    text = stringResource(R.string.currency_conversion),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 24.dp, bottom = 16.dp)
                )
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        // Enable currency conversion toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setCurrencyConversionEnabled(!currencyConversionEnabled) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.enable_currency_conversion),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                            
                            Switch(
                                checked = currencyConversionEnabled,
                                onCheckedChange = { viewModel.setCurrencyConversionEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                        
                        // Original currency selection (only show if conversion is enabled)
                        if (currencyConversionEnabled) {
                            Divider(modifier = Modifier.padding(horizontal = 16.dp))
                            
                            Text(
                                text = stringResource(R.string.original_currency),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            
                            // Original currency options
                            CurrencyOption(
                                title = stringResource(R.string.currency_usd),
                                selected = originalCurrency.isEmpty() || originalCurrency == "USD",
                                onClick = { viewModel.setOriginalCurrency("USD") }
                            )
                            
                            CurrencyOption(
                                title = stringResource(R.string.currency_eur),
                                selected = originalCurrency == "EUR",
                                onClick = { viewModel.setOriginalCurrency("EUR") }
                            )
                            
                            CurrencyOption(
                                title = stringResource(R.string.currency_gbp),
                                selected = originalCurrency == "GBP",
                                onClick = { viewModel.setOriginalCurrency("GBP") }
                            )
                            
                            CurrencyOption(
                                title = stringResource(R.string.currency_ils),
                                selected = originalCurrency == "ILS",
                                onClick = { viewModel.setOriginalCurrency("ILS") }
                            )
                            
                            CurrencyOption(
                                title = stringResource(R.string.currency_rub),
                                selected = originalCurrency == "RUB",
                                onClick = { viewModel.setOriginalCurrency("RUB") }
                            )
                            
                            CurrencyOption(
                                title = stringResource(R.string.currency_jpy),
                                selected = originalCurrency == "JPY",
                                onClick = { viewModel.setOriginalCurrency("JPY") }
                            )
                            
                            CurrencyOption(
                                title = stringResource(R.string.currency_cad),
                                selected = originalCurrency == "CAD",
                                onClick = { viewModel.setOriginalCurrency("CAD") }
                            )
                            
                            CurrencyOption(
                                title = stringResource(R.string.currency_aud),
                                selected = originalCurrency == "AUD",
                                onClick = { viewModel.setOriginalCurrency("AUD") }
                            )
                            
                            CurrencyOption(
                                title = stringResource(R.string.currency_chf),
                                selected = originalCurrency == "CHF",
                                onClick = { viewModel.setOriginalCurrency("CHF") }
                            )
                            
                            CurrencyOption(
                                title = stringResource(R.string.currency_cny),
                                selected = originalCurrency == "CNY",
                                onClick = { viewModel.setOriginalCurrency("CNY") }
                            )
                            
                            CurrencyOption(
                                title = stringResource(R.string.currency_inr),
                                selected = originalCurrency == "INR",
                                onClick = { viewModel.setOriginalCurrency("INR") }
                            )
                        }
                    }
                }
            }
            
            // Cloud backup section
            item {
                Text(
                    text = stringResource(R.string.backup),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 24.dp, bottom = 16.dp)
                )
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.cloud_backup),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                        
                        Text(
                            text = stringResource(R.string.coming_soon),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        
                        Button(
                            onClick = { /* To be implemented in future */ },
                            enabled = false,
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.setup_backup))
                        }
                    }
                }
            }
            
            // App version info
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp, bottom = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Version 1.0.0",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
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
