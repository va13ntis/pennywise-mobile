package com.pennywise.app.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

import androidx.compose.material3.*
import androidx.compose.runtime.*
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
