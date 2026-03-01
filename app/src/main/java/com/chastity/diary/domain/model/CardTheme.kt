package com.chastity.diary.domain.model

import android.net.Uri
import androidx.compose.ui.graphics.Color

// ── Background source (sealed hierarchy) ──────────────────────────────────────

sealed class BackgroundSource {
    /** Simple linear gradient; angle in degrees (0 = top-to-bottom). */
    data class Gradient(
        val colors: List<Color>,
        val angleDegrees: Float = 135f
    ) : BackgroundSource()

    /** App-drawn geometric canvas pattern + base gradient. */
    data class CanvasPattern(
        val type: PatternType,
        val baseColors: List<Color>,
        val patternColor: Color,
        val patternAlpha: Float = 0.12f,
        val angleDegrees: Float = 135f
    ) : BackgroundSource()

    /** User-imported PNG + spec JSON from a .zip file. */
    data class ExternalAsset(
        val pngUri: Uri,
        val spec: CardTemplateSpec
    ) : BackgroundSource()
}

enum class PatternType { DOTS, HEXAGON, DIAGONAL_LINES, CROSS_HATCH, NONE }

/** Whether the data text uses light or dark colours (for contrast on the background). */
enum class TextColorScheme { LIGHT, DARK }

// ── Card theme ─────────────────────────────────────────────────────────────────

data class CardTheme(
    val id: String,
    val nameResId: Int,                        // R.string.*
    val isPremium: Boolean,
    val backgroundSource: BackgroundSource,
    val accentColor: Color,                    // used for numbers / rings
    val textColorScheme: TextColorScheme,
    /** 0.0 = no overlay; >0 = semi-transparent black/white scrim for readability */
    val overlayOpacity: Float = 0f,
    /** null = built-in; non-null = user-imported template id stored in filesDir */
    val userTemplateId: String? = null
)
