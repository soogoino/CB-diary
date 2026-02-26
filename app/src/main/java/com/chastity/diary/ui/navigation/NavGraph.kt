package com.chastity.diary.ui.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chastity.diary.ui.screens.DailyEntryScreen
import com.chastity.diary.ui.screens.DashboardScreen
import com.chastity.diary.ui.screens.HistoryScreen
import com.chastity.diary.ui.screens.SettingsScreen
import com.chastity.diary.viewmodel.DailyEntryViewModel

/**
 * Keep-alive navigation: screens stay composed once visited.
 * DailyEntry is always pre-composed (default screen).
 * Other screens are lazily first-composed on first visit, then kept alive.
 *
 * PERF: Composing all 4 screens simultaneously caused HistoryScreen's
 * MoodCalendarSection to be JIT-compiled immediately on startup, blocking
 * the render thread for ~8 seconds (4.2 MB JIT compile). With lazy
 * first-compose, that cost only occurs when the user explicitly navigates
 * to History for the first time.
 */
@Composable
fun NavGraph(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    outerPadding: PaddingValues = PaddingValues()
) {
    // Shared ViewModel so HistoryScreen can call selectDate before switching
    val dailyEntryViewModel: DailyEntryViewModel = viewModel()

    // Track which screens have been visited at least once.
    // DailyEntry is pre-seeded so it's always composed from the start.
    var visitedRoutes by remember { mutableStateOf(setOf(Screen.DailyEntry.route)) }
    LaunchedEffect(currentRoute) {
        if (currentRoute !in visitedRoutes) {
            visitedRoutes = visitedRoutes + currentRoute
        }
    }

    Box(Modifier.fillMaxSize()) {
        // DailyEntry: always pre-composed (warm ViewModel + DB data before unlock)
        KeepAliveScreen(currentRoute == Screen.DailyEntry.route) {
            DailyEntryScreen(viewModel = dailyEntryViewModel, outerPadding = outerPadding)
        }
        // Remaining screens: compose on first visit, then stay alive
        if (Screen.Dashboard.route in visitedRoutes) {
            KeepAliveScreen(currentRoute == Screen.Dashboard.route) {
                DashboardScreen(outerPadding = outerPadding)
            }
        }
        if (Screen.History.route in visitedRoutes) {
            KeepAliveScreen(currentRoute == Screen.History.route) {
                HistoryScreen(
                    dailyEntryViewModel = dailyEntryViewModel,
                    onNavigateToDailyEntry = { onNavigate(Screen.DailyEntry.route) },
                    outerPadding = outerPadding
                )
            }
        }
        if (Screen.Settings.route in visitedRoutes) {
            KeepAliveScreen(currentRoute == Screen.Settings.route) {
                SettingsScreen(outerPadding = outerPadding)
            }
        }
    }
}

/**
 * Composes [content] always (keep-alive) and crossfades it in/out with a short tween so
 * bottom-nav switching feels smooth instead of an abrupt alpha snap.
 * Invisible screens stay at alpha 0 and swallow all pointer events.
 */
@Composable
private fun KeepAliveScreen(visible: Boolean, content: @Composable () -> Unit) {
    // Smooth 220 ms crossfade; FastOutSlowInEasing gives a natural deceleration feel
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "screenAlpha"
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            // Visible screen sits on top so its touch targets win
            .zIndex(if (visible) 1f else 0f)
            .alpha(alpha)
            // Block all pointer input while fading out / fully invisible
            .pointerInput(visible) {
                if (!visible) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent(pass = PointerEventPass.Initial)
                                .changes.forEach { it.consume() }
                        }
                    }
                }
            }
    ) {
        content()
    }
}
