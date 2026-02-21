package com.chastity.diary.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.chastity.diary.ui.screens.DailyEntryScreen
import com.chastity.diary.ui.screens.DashboardScreen
import com.chastity.diary.ui.screens.HistoryScreen
import com.chastity.diary.ui.screens.SettingsScreen
import com.chastity.diary.viewmodel.DailyEntryViewModel

@Composable
fun NavGraph(navController: NavHostController, outerPadding: PaddingValues = PaddingValues()) {
    // Shared so HistoryScreen can call selectDate before navigating
    val dailyEntryViewModel: DailyEntryViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = Screen.DailyEntry.route
    ) {
        composable(Screen.DailyEntry.route) {
            DailyEntryScreen(viewModel = dailyEntryViewModel, outerPadding = outerPadding)
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(outerPadding = outerPadding)
        }

        composable(Screen.History.route) {
            HistoryScreen(
                navController = navController,
                dailyEntryViewModel = dailyEntryViewModel,
                outerPadding = outerPadding
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(outerPadding = outerPadding)
        }
    }
}
