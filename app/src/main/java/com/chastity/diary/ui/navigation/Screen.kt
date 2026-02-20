package com.chastity.diary.ui.navigation

/**
 * Navigation routes for the app
 */
sealed class Screen(val route: String) {
    object DailyEntry : Screen("daily_entry")
    object Dashboard : Screen("dashboard")
    object Settings : Screen("settings")
}
