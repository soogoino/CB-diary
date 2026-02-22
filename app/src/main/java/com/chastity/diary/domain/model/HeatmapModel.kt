package com.chastity.diary.domain.model

/**
 * 行動熱力圖的時間範圍選項
 * @param days   橫軸顯示的天數
 * @param label  UI 顯示文字
 */
enum class HeatmapTimeRange(val days: Int, val label: String) {
    WEEK_1(7, "7天"),
    WEEK_2(14, "14天"),
    MONTH_1(30, "30天")
}

/**
 * 行動熱力圖固定題目清單（不可由使用者自訂）
 * 只包含每日都會詢問的項目：配戴鎖、春夢、晨勃（男性）、運動、清潔、打卡照片
 */
enum class HeatmapQuestion(
    val id: String,
    val label: String,
    val requiredGender: Gender? = null,
    val extractor: (DailyEntry) -> Boolean
) {
    DEVICE_CHECK(id = "check", label = "配戴鎖", extractor = { it.deviceCheckPassed }),
    HAD_EROTIC_DREAM(id = "dream", label = "春夢", extractor = { it.hadEroticDream }),
    MORNING_ERECTION(id = "morning_erection", label = "晨勃", requiredGender = Gender.MALE, extractor = { it.morningErection }),
    EXERCISED(id = "exercised", label = "運動", extractor = { it.exercised }),
    CLEANING(id = "cleaning", label = "清潔", extractor = { it.cleaningType != null }),
    HAS_PHOTO(id = "photo", label = "打卡照片", extractor = { it.photoPath != null });

    companion object {
        fun forGender(gender: Gender): List<HeatmapQuestion> =
            entries.filter { q -> q.requiredGender == null || q.requiredGender == gender }
    }
}
