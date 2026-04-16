package com.ascend.app.ui.splash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val destination by viewModel.destination.collectAsStateWithLifecycle()

    LaunchedEffect(destination) {
        when (destination) {
            is SplashDestination.Login -> onNavigateToLogin()
            is SplashDestination.Dashboard -> onNavigateToDashboard()
            is SplashDestination.Loading -> Unit
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Ascend", style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary)
    }
}