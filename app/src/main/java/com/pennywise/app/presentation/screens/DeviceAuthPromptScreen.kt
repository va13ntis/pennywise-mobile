package com.pennywise.app.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.pennywise.app.R
import com.pennywise.app.presentation.viewmodel.DeviceAuthPromptViewModel

/**
 * Screen for device authentication prompt
 * Shows when app is launched and device auth is enabled
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceAuthPromptScreen(
    onAuthSuccess: () -> Unit,
    onAuthCancel: () -> Unit,
    viewModel: DeviceAuthPromptViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) {
            onAuthSuccess()
        }
    }
    
    LaunchedEffect(Unit) {
        // Automatically trigger device authentication when screen loads
        val activity = context as? FragmentActivity
        if (activity != null) {
            viewModel.authenticate(activity)
        } else {
            // If we can't get a FragmentActivity, show an error
        }
    }
    
    // If we're authenticating or already authenticated, avoid rendering any UI to prevent flashes
    if (uiState.isLoading || uiState.isAuthenticated) {
        return
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Security icon
        Icon(
            imageVector = Icons.Default.Security,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Title
        Text(
            text = stringResource(R.string.device_auth_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Subtitle
        Text(
            text = stringResource(R.string.device_auth_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Loading indicator
        if (uiState.isLoading) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Authenticating...",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        } else {
            // Manual authentication button
            Button(
                onClick = { 
                    val activity = context as? FragmentActivity
                    if (activity != null) {
                        viewModel.authenticate(activity)
                    } else {
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Authenticate")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Cancel button
        OutlinedButton(
            onClick = onAuthCancel,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.device_auth_cancel))
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
