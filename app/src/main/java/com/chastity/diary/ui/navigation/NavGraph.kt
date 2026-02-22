package com.chastity.diary.ui.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
 * Keep-alive navigation: all 4 screens stay composed at all times.
 * Switching is instant — no destroy/recreate cost.
 * Invisible screens have alpha=0 and all pointer events consumed.
 */
@Composable
fun NavGraph(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    outerPadding: PaddingValues = PaddingValues()
) {
    // Shared ViewModel so HistoryScreen can call selectDate before switching
    val dailyEntryViewModel: DailyEntryViewModel = viewModel()

    Box(Modifier.fillMaxSize()) {
        KeepAliveScreen(currentRoute == Screen.DailyEntry.route) {
            DailyEntryScreen(viewModel = dailyEntryViewModel, outerPadding = outerPadding)
        }
        KeepAliveScreen(currentRoute == Screen.Dashboard.route) {
            DashboardScreen(outerPadding = outerPadding)
        }
        KeepAliveScreen(currentRoute == Screen.History.route) {
            HistoryScreen(
                dailyEntryViewModel = dailyEntryViewModel,
                onNavigateToDailyEntry = { onNavigate(Screen.DailyEntry.route) },
                outerPadding = outerPadding
            )
        }
        KeepAliveScreen(currentRoute == Screen.Settings.route) {
            SettingsScreen(outerPadding = outerPadding)
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
