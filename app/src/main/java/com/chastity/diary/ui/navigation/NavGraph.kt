package com.chastity.diary.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.chastity.diary.ui.screens.DailyEntryScreen
import com.chastity.diary.ui.screens.DashboardScreen
import com.chastity.diary.ui.screens.SettingsScreen

/**
 * Navigation graph
 */
@Composable
fun NavGraph(navController: NavHostController, outerPadding: PaddingValues = PaddingValues()) {
    NavHost(
        navController = navController,
        startDestination = Screen.DailyEntry.route
    ) {
        composable(Screen.DailyEntry.route) {
            DailyEntryScreen(outerPadding = outerPadding)
        }
        
        composable(Screen.Dashboard.route) {
            DashboardScreen(outerPadding = outerPadding)
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(outerPadding = outerPadding)
        }
    }
}
