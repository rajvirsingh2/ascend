package com.ascend.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector

object Routes{
    const val LOGIN="login"
    const val REGISTER="register"
    const val DASHBOARD="dashboard"
    const val QUEST_DETAIL="quest/{questId}"
    const val HABIT_LIST="habits"
    const val GOALS="goals"
    const val PROFILE="profile"
    const val SPLASH = "splash"
    const val SETTINGS="settings"
    const val OTP_VERIFY = "otp/{email}"
    const val HISTORY="history"
    const val FORGOT_PASSWORD="forgot-password"
    const val PHYSIQUE_SETUP = "physique-setup"
    const val INTERESTS="interests_onboarding"
}

sealed class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
){
    data object Dashboard: BottomNavItem(Routes.DASHBOARD,"Home", Icons.Default.Home)
    data object Goals: BottomNavItem(Routes.GOALS,"Goals", Icons.Default.Star)
    data object Profile: BottomNavItem(Routes.PROFILE,"Profile", Icons.Default.Person)
    data object Settings: BottomNavItem(Routes.SETTINGS, "Settings", Icons.Default.Settings)
    data object History : BottomNavItem(Routes.HISTORY, "History", Icons.Default.DateRange)
}

val bottomNavItems = listOf(
    BottomNavItem.Dashboard,
    BottomNavItem.Goals,
    BottomNavItem.Profile,
    BottomNavItem.Settings,
    BottomNavItem.History
)