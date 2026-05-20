package com.ascend.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ascend.app.data.realtime.WebSocketManager
import com.ascend.app.ui.auth.ForgotPasswordScreen
import com.ascend.app.ui.auth.LoginScreen
import com.ascend.app.ui.auth.OtpVerificationScreen
import com.ascend.app.ui.auth.RegisterScreen
import com.ascend.app.ui.dashboard.DashboardScreen
import com.ascend.app.ui.goals.GoalsScreen
import com.ascend.app.ui.history.HistoryScreen
import com.ascend.app.ui.interests.InterestsOnboardingScreen
import com.ascend.app.ui.navigation.AscendBottomNavBar
import com.ascend.app.ui.navigation.Routes
import com.ascend.app.ui.physique.PhysiqueOnboardingScreen
import com.ascend.app.ui.profile.ProfileScreen
import com.ascend.app.ui.settings.SettingsScreen
import com.ascend.app.ui.splash.SplashScreen
import com.ascend.app.ui.theme.AscendTheme
import com.ascend.app.ui.theme.DarkColors
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var wsManager: WebSocketManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        wsManager.connect(BuildConfig.BASE_URL)
        setContent {
            AscendTheme {
                AscendNavHost()
            }
        }
        // in onCreate, after setContent
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        wsManager.disconnect()
    }
}

@Composable
fun AscendNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    AscendMainLayout(
        currentRoute = currentRoute,
        onNavigateBottomBar = { route ->
            navController.navigate(route) {
                popUpTo(Routes.DASHBOARD) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.SPLASH,
            modifier = Modifier.padding(padding)
        ) {
            composable(
                route = "otp/{email}",
                arguments = listOf(navArgument("email") { type = NavType.StringType })
            ) { backStackEntry ->
                val email = backStackEntry.arguments?.getString("email") ?: ""
                OtpVerificationScreen(
                    email = email,
                    onVerified = {
                        // New registrant → go to interests onboarding, then dashboard
                        navController.navigate(Routes.INTERESTS) {
                            popUpTo(Routes.REGISTER) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.PHYSIQUE_SETUP) {
                PhysiqueOnboardingScreen(
                    onComplete = {
                        navController.navigate(Routes.DASHBOARD) {
                            popUpTo(Routes.PHYSIQUE_SETUP) { inclusive = true }
                        }
                    }
                )
            }
            composable(Routes.SPLASH) {
                SplashScreen(
                    onNavigateToLogin = {
                        navController.navigate(Routes.LOGIN){
                            popUpTo(Routes.SPLASH){inclusive=true}
                        }
                    },
                    onNavigateToDashboard = {
                        navController.navigate(Routes.DASHBOARD){
                            popUpTo(Routes.SPLASH){inclusive=true}
                        }
                    },
                    onNavigateToInterests = {
                        navController.navigate(Routes.INTERESTS) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.LOGIN) {
                LoginScreen(
                    onNavigateToDashboard = {
                        navController.navigate(Routes.SPLASH) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = { navController.navigate(Routes.REGISTER) },
                    navController = navController
                )
            }
            composable(Routes.REGISTER) {
                RegisterScreen(
                    navController,
                    onNavigateToLogin = { navController.popBackStack() }
                )
            }
            composable(Routes.DASHBOARD) { DashboardScreen(onNavigate = { route ->
                navController.navigate(route) {
                    launchSingleTop = true
                }
            }) }
            composable(Routes.GOALS) { GoalsScreen() }

            // --- UPDATED PROFILE COMPOSABLE ---
            composable(Routes.PROFILE) {
                ProfileScreen(
                    onNavigateToPhysiqueSetup = {
                        navController.navigate(Routes.PHYSIQUE_SETUP)
                    },
                    onNavigateToInterests = {
                        navController.navigate(Routes.INTERESTS)
                    },
                    onNavigateToLogin = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
            // ----------------------------------

            composable(Routes.SETTINGS) {
                SettingsScreen()
            }
            composable(Routes.HISTORY) {
                HistoryScreen()
            }

            composable(Routes.FORGOT_PASSWORD) {
                ForgotPasswordScreen(
                    onBack = { navController.popBackStack() },
                    onResetSuccess = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.FORGOT_PASSWORD) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.INTERESTS){
                InterestsOnboardingScreen(
                    onComplete =  {
                        navController.navigate(Routes.DASHBOARD){
                            popUpTo(Routes.INTERESTS) { inclusive=true }
                        }
                    }
                )
            }
        }
    }
}

// 2. Stateless App Shell (Safe for Previews!)
@Composable
fun AscendMainLayout(
    currentRoute: String?,
    onNavigateBottomBar: (String) -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    val showBottomBar = currentRoute in listOf(
        Routes.DASHBOARD, Routes.GOALS, Routes.PROFILE, Routes.SETTINGS, Routes.HISTORY
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                AscendBottomNavBar(
                    currentRoute = currentRoute,
                    onNavigate = onNavigateBottomBar
                )
            }
        },
        containerColor = DarkColors.Void // Setting standard background for the app shell
    ) { padding ->
        content(padding)
    }
}

@Preview(showBackground = true, name = "1. Main Layout (With Bottom Bar)")
@Composable
fun AscendMainLayoutPreview_WithNav() {
    AscendTheme {
        AscendMainLayout(
            currentRoute = Routes.DASHBOARD, // Tricks the layout into showing the bottom bar
            onNavigateBottomBar = {}
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(DarkColors.Void),
                contentAlignment = Alignment.Center
            ) {
                Text("Content Area (E.g. Dashboard)", color = DarkColors.TextMuted)
            }
        }
    }
}

@Preview(showBackground = true, name = "2. Main Layout (No Bottom Bar)")
@Composable
fun AscendMainLayoutPreview_NoNav() {
    AscendTheme {
        AscendMainLayout(
            currentRoute = Routes.LOGIN, // Tricks the layout into hiding the bottom bar
            onNavigateBottomBar = {}
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(DarkColors.Void),
                contentAlignment = Alignment.Center
            ) {
                Text("Full Screen Content (E.g. Login Screen)", color = DarkColors.TextMuted)
            }
        }
    }
}