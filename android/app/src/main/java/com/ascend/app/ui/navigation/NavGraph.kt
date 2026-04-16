package com.ascend.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
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
}

sealed class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
){
    data object Dashboard: BottomNavItem(Routes.DASHBOARD,"Home", Icons.Default.Home)
    data object Goals: BottomNavItem(Routes.GOALS,"Goals", Icons.Default.Star)
    data object Profile: BottomNavItem(Routes.PROFILE,"Profile", Icons.Default.Person)
}

val bottomNavItems = listOf(
    BottomNavItem.Dashboard,
    BottomNavItem.Goals,
    BottomNavItem.Profile
)