package com.chastity.diary.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

data class BottomNavItem(
    val screen: Screen,
    val icon: ImageVector,
    val label: String
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.DailyEntry, Icons.Filled.Edit, "每日記錄"),
    BottomNavItem(Screen.Dashboard, Icons.Filled.Dashboard, "儀表板"),
    BottomNavItem(Screen.History, Icons.Filled.CalendarMonth, "歷史紀錄"),
    BottomNavItem(Screen.Settings, Icons.Filled.Settings, "設定")
)

@Composable
fun BottomNavigationBar(currentRoute: String, onNavigate: (String) -> Unit) {
    NavigationBar {
        bottomNavItems.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = currentRoute == item.screen.route,
                onClick = { onNavigate(item.screen.route) }
            )
        }
    }
}
