package com.chastity.diary.domain.model

import com.google.gson.annotations.SerializedName

/**
 * Parsed representation of `card_template_spec.json` bundled inside a user-imported .zip template.
 *
 * Example JSON:
 * ```json
 * {
 *   "format_version": 1,
 *   "canvas": { "width": 1080, "height": 1350 },
 *   "text_color_scheme": "light",
 *   "overlay_opacity": 0.0,
 *   "safe_zones": {
 *     "header": { "top": 60,  "left": 60, "right": 60, "height": 100 },
 *     "streak":  { "top": 220, "left": 60, "right": 60, "height": 200 },
 *     "stats":   { "top": 460, "left": 60, "right": 60, "height": 600 },
 *     "footer":  { "bottom": 60, "left": 60, "right": 60, "height": 80 }
 *   }
 * }
 * ```
 */
data class CardTemplateSpec(
    @SerializedName("format_version") val formatVersion: Int = 1,
    val canvas: CanvasSize = CanvasSize(),
    @SerializedName("text_color_scheme") val textColorScheme: String = "light",
    @SerializedName("overlay_opacity") val overlayOpacity: Float = 0f,
    @SerializedName("safe_zones") val safeZones: SafeZones = SafeZones()
) {
    data class CanvasSize(val width: Int = 1080, val height: Int = 1920)

    data class SafeZones(
        val header: ZoneRect = ZoneRect(top = 60, left = 60, right = 60, height = 100),
        val streak: ZoneRect = ZoneRect(top = 220, left = 60, right = 60, height = 200),
        val stats: ZoneRect = ZoneRect(top = 460, left = 60, right = 60, height = 600),
        val footer: ZoneRect = ZoneRect(bottom = 60, left = 60, right = 60, height = 80)
    )

    data class ZoneRect(
        val top: Int = 0,
        val bottom: Int = 0,
        val left: Int = 60,
        val right: Int = 60,
        val height: Int = 0
    )

    companion object {
        val DEFAULT = CardTemplateSpec()
    }
}
