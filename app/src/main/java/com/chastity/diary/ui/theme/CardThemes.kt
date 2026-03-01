package com.chastity.diary.ui.theme

import androidx.compose.ui.graphics.Color
import com.chastity.diary.R
import com.chastity.diary.domain.model.BackgroundSource
import com.chastity.diary.domain.model.CardTheme
import com.chastity.diary.domain.model.PatternType
import com.chastity.diary.domain.model.TextColorScheme

/**
 * All built-in card themes.
 *
 * Three free themes ("midnight", "dawn", "forest") are available to all users.
 * Three premium themes ("crimson", "ocean", "minimal") require a sponsor code.
 *
 * Themes are designed so that the text layer always remains readable:
 * - DARK background → TextColorScheme.LIGHT text
 * - LIGHT background → TextColorScheme.DARK text
 *
 * User-imported templates are stored separately in CardViewModel.userTemplates.
 */
object CardThemes {

    // ── Free ──────────────────────────────────────────────────────────────────

    val MIDNIGHT = CardTheme(
        id = "midnight",
        nameResId = R.string.card_theme_midnight,
        isPremium = false,
        backgroundSource = BackgroundSource.Gradient(
            colors = listOf(
                Color(0xFF0F0C29),
                Color(0xFF302B63),
                Color(0xFF24243E)
            ),
            angleDegrees = 135f
        ),
        accentColor = Color(0xFF9B59F5),
        textColorScheme = TextColorScheme.LIGHT
    )

    val DAWN = CardTheme(
        id = "dawn",
        nameResId = R.string.card_theme_dawn,
        isPremium = false,
        backgroundSource = BackgroundSource.Gradient(
            colors = listOf(
                Color(0xFFFF6B35),
                Color(0xFFFF8C61),
                Color(0xFFFFB899),
                Color(0xFFFFDEC5)
            ),
            angleDegrees = 135f
        ),
        accentColor = Color(0xFFE84A00),
        textColorScheme = TextColorScheme.DARK
    )

    val FOREST = CardTheme(
        id = "forest",
        nameResId = R.string.card_theme_forest,
        isPremium = false,
        backgroundSource = BackgroundSource.Gradient(
            colors = listOf(
                Color(0xFF134E5E),
                Color(0xFF71B280)
            ),
            angleDegrees = 150f
        ),
        accentColor = Color(0xFF4ADE80),
        textColorScheme = TextColorScheme.LIGHT
    )

    // ── Premium ───────────────────────────────────────────────────────────────

    val CRIMSON = CardTheme(
        id = "crimson",
        nameResId = R.string.card_theme_crimson,
        isPremium = true,
        backgroundSource = BackgroundSource.CanvasPattern(
            type = PatternType.DIAGONAL_LINES,
            baseColors = listOf(Color(0xFF1A0005), Color(0xFF5C001A)),
            patternColor = Color(0xFFFF1744),
            patternAlpha = 0.10f,
            angleDegrees = 135f
        ),
        accentColor = Color(0xFFFF5252),
        textColorScheme = TextColorScheme.LIGHT
    )

    val OCEAN = CardTheme(
        id = "ocean",
        nameResId = R.string.card_theme_ocean,
        isPremium = true,
        backgroundSource = BackgroundSource.CanvasPattern(
            type = PatternType.HEXAGON,
            baseColors = listOf(Color(0xFF005C97), Color(0xFF363795)),
            patternColor = Color(0xFF00E5FF),
            patternAlpha = 0.08f,
            angleDegrees = 135f
        ),
        accentColor = Color(0xFF00E5FF),
        textColorScheme = TextColorScheme.LIGHT
    )

    val MINIMAL = CardTheme(
        id = "minimal",
        nameResId = R.string.card_theme_minimal,
        isPremium = true,
        backgroundSource = BackgroundSource.CanvasPattern(
            type = PatternType.CROSS_HATCH,
            baseColors = listOf(Color(0xFFF5F5F5), Color(0xFFECECEC)),
            patternColor = Color(0xFF9E9E9E),
            patternAlpha = 0.15f,
            angleDegrees = 0f
        ),
        accentColor = Color(0xFF212121),
        textColorScheme = TextColorScheme.DARK
    )

    // ── Registry ──────────────────────────────────────────────────────────────

    val ALL: List<CardTheme> = listOf(MIDNIGHT, DAWN, FOREST, CRIMSON, OCEAN, MINIMAL)

    fun findById(id: String): CardTheme = ALL.find { it.id == id } ?: MIDNIGHT
}
