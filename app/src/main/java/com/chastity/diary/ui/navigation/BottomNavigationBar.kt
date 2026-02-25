package com.chastity.diary.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.chastity.diary.R

data class BottomNavItem(
    val screen: Screen,
    val icon: ImageVector,
    @StringRes val labelRes: Int
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.DailyEntry, Icons.Filled.Edit, R.string.nav_daily_entry),
    BottomNavItem(Screen.Dashboard, Icons.Filled.Dashboard, R.string.nav_dashboard),
    BottomNavItem(Screen.History, Icons.Filled.CalendarMonth, R.string.nav_history),
    BottomNavItem(Screen.Settings, Icons.Filled.Settings, R.string.nav_settings)
)

@Composable
fun BottomNavigationBar(currentRoute: String, onNavigate: (String) -> Unit) {
    NavigationBar {
        bottomNavItems.forEach { item ->
            val label = stringResource(item.labelRes)
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = label) },
                label = { Text(label) },
                selected = currentRoute == item.screen.route,
                onClick = { onNavigate(item.screen.route) }
            )
        }
    }
}
