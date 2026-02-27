package com.chastity.diary.domain.model

import com.chastity.diary.R

/**
 * 行動熱力圖的時間範圍選項
 * @param days       橫軸顯示的天數
 * @param labelResId UI 顯示字串資源 ID
 */
enum class HeatmapTimeRange(val days: Int, val labelResId: Int) {
    WEEK_1(7, R.string.heatmap_period_7d),
    WEEK_2(14, R.string.heatmap_period_14d),
    MONTH_1(30, R.string.heatmap_period_30d)
}

/**
 * 行動熱力圖固定題目清單（不可由使用者自訂）
 * 只包含每日都會詢問的項目：配戴鎖、春夢、晨勃（男性）、運動、清潔、打卡照片
 */
enum class HeatmapQuestion(
    val id: String,
    val labelResId: Int,
    val requiredGender: Gender? = null,
    val extractor: (DailyEntry) -> Boolean
) {
    DEVICE_CHECK(id = "check", labelResId = R.string.heatmap_label_locked, extractor = { it.deviceCheckPassed }),
    HAD_EROTIC_DREAM(id = "dream", labelResId = R.string.heatmap_label_dream, extractor = { it.hadEroticDream }),
    MORNING_ERECTION(id = "morning_erection", labelResId = R.string.heatmap_label_morning, requiredGender = Gender.MALE, extractor = { it.morningErection }),
    EXERCISED(id = "exercised", labelResId = R.string.heatmap_label_exercise, extractor = { it.exercised }),
    CLEANING(id = "cleaning", labelResId = R.string.heatmap_label_clean, extractor = { it.cleaningType != null }),
    HAS_PHOTO(id = "photo", labelResId = R.string.heatmap_label_photo, extractor = { it.photoPath != null });

    companion object {
        fun forGender(gender: Gender): List<HeatmapQuestion> =
            entries.filter { q -> q.requiredGender == null || q.requiredGender == gender }
    }
}
