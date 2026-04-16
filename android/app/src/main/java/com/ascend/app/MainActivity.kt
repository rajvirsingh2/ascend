package com.ascend.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ascend.app.ui.auth.LoginScreen
import com.ascend.app.ui.auth.RegisterScreen
import com.ascend.app.ui.dashboard.DashboardScreen
import com.ascend.app.ui.goals.GoalsScreen
import com.ascend.app.ui.navigation.Routes
import com.ascend.app.ui.navigation.bottomNavItems
import com.ascend.app.ui.profile.ProfileScreen
import com.ascend.app.ui.splash.SplashScreen
import com.ascend.app.ui.theme.AscendTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AscendTheme {
                AscendNavHost()
            }
        }
    }
}

@Composable
fun AscendNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf(
        Routes.DASHBOARD, Routes.GOALS, Routes.PROFILE
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(Routes.DASHBOARD) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.SPLASH,
            modifier = Modifier.padding(padding)
        ) {

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
                    }
                )
            }

            composable(Routes.LOGIN) {
                LoginScreen(
                    onNavigateToDashboard = {
                        navController.navigate(Routes.DASHBOARD) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = { navController.navigate(Routes.REGISTER) }
                )
            }
            composable(Routes.REGISTER) {
                RegisterScreen(
                    onNavigateToDashboard = {
                        navController.navigate(Routes.DASHBOARD) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = { navController.popBackStack() }
                )
            }
            composable(Routes.DASHBOARD) { DashboardScreen() }
            composable(Routes.GOALS) { GoalsScreen() }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    onNavigateToLogin = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}