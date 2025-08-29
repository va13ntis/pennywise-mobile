package com.pennywise.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import com.pennywise.app.presentation.viewmodel.AuthViewModel
import com.pennywise.app.presentation.viewmodel.LoginViewModel
import com.pennywise.app.presentation.viewmodel.RegisterViewModel
import com.pennywise.app.presentation.viewmodel.HomeViewModel
import com.pennywise.app.presentation.screens.LoginScreen
import com.pennywise.app.presentation.screens.RegisterScreen
import com.pennywise.app.presentation.screens.HomeScreen
import com.pennywise.app.presentation.navigation.LOGIN_ROUTE
import com.pennywise.app.presentation.navigation.REGISTER_ROUTE

/**
 * Main app navigation that handles authentication state and routes
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val isAuthenticated by authViewModel.isAuthenticated.collectAsState(initial = false)
    
    // Show loading state initially
    var isInitialized by remember { mutableStateOf(false) }
    
    // Initialize authentication state when the app starts
    LaunchedEffect(Unit) {
        try {
            authViewModel.initializeAuthState()
        } catch (e: Exception) {
            // If initialization fails, just continue with unauthenticated state
        } finally {
            isInitialized = true
        }
    }
    
    // Show loading screen while initializing
    if (!isInitialized) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }
    
    NavHost(
        navController = navController,
        startDestination = if (isAuthenticated) MAIN_ROUTE else LOGIN_ROUTE
    ) {
        // Login screen
        composable(LOGIN_ROUTE) {
            val viewModel = hiltViewModel<LoginViewModel>()
            LoginScreen(
                viewModel = viewModel,
                onNavigateToRegister = { navController.navigate(REGISTER_ROUTE) },
                onLoginSuccess = { user ->
                    // Navigation will be handled automatically by the isAuthenticated state change
                }
            )
        }
        
        // Register screen
        composable(REGISTER_ROUTE) {
            val viewModel = hiltViewModel<RegisterViewModel>()
            RegisterScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onRegisterSuccess = { navController.popBackStack() }
            )
        }
        
        // Main app content after authentication
        composable(MAIN_ROUTE) {
            val homeViewModel = hiltViewModel<HomeViewModel>()
            val currentUser = authViewModel.getCurrentUser()
            
            // Set the user ID for the HomeViewModel
            currentUser?.let { user ->
                homeViewModel.setUserId(user.id)
            }
            
            HomeScreen(
                onAddExpense = {
                    // TODO: Navigate to add expense screen when implemented
                },
                onNavigateToSettings = {
                    // TODO: Navigate to settings screen when implemented
                },
                onLogout = {
                    authViewModel.logout()
                }
            )
        }
    }
}

/**
 * Navigation routes for the main app
 */
const val MAIN_ROUTE = "main"


