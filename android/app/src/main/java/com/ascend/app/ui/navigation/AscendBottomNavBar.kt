package com.ascend.app.ui.navigation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.ascend.app.ui.theme.CyanAccent
import com.ascend.app.ui.theme.DarkColors
import com.ascend.app.ui.theme.PurplePrimary
import com.ascend.app.ui.theme.TextMuted
@Composable
fun AscendBottomNavBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier.fillMaxWidth(),
        containerColor = DarkColors.Abyss, // Using your app's dark panel color
        contentColor = DarkColors.TextPrimary
    ) {
        bottomNavItems.forEach { item ->
            val isSelected = currentRoute == item.route

            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(item.route) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = {
                    Text(
                        text = item.label,
                        fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold
                        else androidx.compose.ui.text.font.FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = CyanAccent,
                    selectedTextColor = CyanAccent,
                    indicatorColor = PurplePrimary.copy(alpha = 0.2f), // Subtle neon glow behind selected icon
                    unselectedIconColor = TextMuted,
                    unselectedTextColor = TextMuted
                )
            )
        }
    }
}

@Preview(showBackground = true, name = "Interactive Bottom Navigation")
@Composable
fun AscendBottomNavBarPreview() {
    var selectedRoute by remember { mutableStateOf(Routes.DASHBOARD) }

    MaterialTheme {
        AscendBottomNavBar(
            currentRoute = selectedRoute,
            onNavigate = { newRoute ->
                selectedRoute = newRoute
            }
        )
    }
}