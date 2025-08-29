package com.pennywise.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.pennywise.app.domain.model.User
import com.pennywise.app.presentation.screens.LoginScreen
import com.pennywise.app.presentation.screens.RegisterScreen
import com.pennywise.app.presentation.viewmodel.LoginViewModel
import com.pennywise.app.presentation.viewmodel.RegisterViewModel

/**
 * Navigation routes for authentication flow
 */
const val AUTH_ROUTE = "auth"
const val LOGIN_ROUTE = "login"
const val REGISTER_ROUTE = "register"

/**
 * Authentication navigation graph that handles transitions between login and registration screens
 * 
 * Note: This function is kept for backward compatibility but is no longer used.
 * The navigation is now handled directly in AppNavigation.
 */
@Composable
fun AuthNavGraph(
    navController: NavHostController,
    onAuthenticationSuccess: (User) -> Unit
) {
    // Show login screen by default
    val viewModel = hiltViewModel<LoginViewModel>()
    LoginScreen(
        viewModel = viewModel,
        onNavigateToRegister = { navController.navigate(REGISTER_ROUTE) },
        onLoginSuccess = onAuthenticationSuccess
    )
}
