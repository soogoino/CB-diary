package com.chastity.diary.util

object Constants {
    // Mood options
    val MOODS = listOf(
        "😊 開心", "😌 平靜", "😐 普通", "😔 沮喪",
        "😰 焦慮", "😤 挫折", "🥵 興奮", "😴 無聊", 
        "😳 羞恥", "😎 自豪", "🤩 期待", "😬 緊張", 
        "🧘 放鬆", "😕 困惑", "💪 充實", "😶 空虛"
    )
    
    // Exercise types
    val EXERCISE_TYPES = listOf(
        "跑步", "健身", "游泳", "瑜伽", "騎車", 
        "球類運動", "散步", "重訓", "有氧運動", "其他"
    )
    
    // Locations for exposed lock
    val EXPOSED_LOCATIONS = listOf(
        "家中", "健身房", "游泳池", "公共浴室", "戶外", 
        "更衣室", "醫院", "朋友家", "工作場所", "其他公共場所"
    )
    
    // Discomfort areas
    val DISCOMFORT_AREAS = listOf(
        "陰莖", "睪丸", "會陰", "大腿根部", "恥骨", "尿道", "其他"
    )
    
    // Leakage amounts
    val LEAKAGE_AMOUNTS = listOf("少量", "中等", "大量")
    
    // Edging methods
    val EDGING_METHODS = listOf(
        "視覺刺激", "觸摸", "聲音", "想像", "閱讀", "影片", "其他"
    )
    
    // Keyholder interaction types
    val INTERACTION_TYPES = listOf(
        "訊息聊天", "語音通話", "視訊", "實體見面", 
        "任務指派", "獎勵", "懲罰", "檢查", "其他"
    )
    
    // Cleaning types
    val CLEANING_TYPES = listOf("未清潔", "簡單沖洗", "深度清潔", "完全取下清潔")
    
    // Removal reasons
    val REMOVAL_REASONS = listOf(
        "清潔", "醫療", "工作需求", "緊急狀況", 
        "Keyholder允許", "不適", "其他"
    )
    
    // Social activities
    val SOCIAL_ACTIVITIES = listOf(
        "外出用餐", "健身房", "游泳", "親友聚會", 
        "工作會議", "約會", "購物", "旅行", "其他"
    )
    
    // Emotions (extended)
    val EMOTIONS = listOf(
        "興奮", "焦慮", "沮喪", "平靜", "挫折", 
        "滿足", "羞恥", "自豪", "無聊", "期待",
        "緊張", "放鬆", "困惑", "充實", "空虛"
    )
    
    // Time duration quick options (minutes)
    val DURATION_OPTIONS = listOf(
        5, 10, 15, 30, 45, 60, 90, 120, 180, 240
    )
    
    // Night erection quick options (maps to nightErections: Int?)
    val NIGHT_ERECTION_OPTIONS = listOf("無", "偶爾", "頻繁")
    val NIGHT_ERECTION_VALUES = mapOf("無" to 0, "偶爾" to 5, "頻繁" to 10)

    // Database
    const val DATABASE_NAME = "chastity_diary_db"
    const val DATABASE_VERSION = 4

    // Photo
    const val PREF_PHOTO_BLUR_ENABLED = "photo_blur_enabled"
    
    // DataStore
    const val DATASTORE_NAME = "user_preferences"
    
    // Notification
    const val NOTIFICATION_CHANNEL_ID = "daily_reminder"
    const val NOTIFICATION_ID = 1001
    const val DEFAULT_REMINDER_HOUR = 21
    const val DEFAULT_REMINDER_MINUTE = 0
    
    // Encrypted SharedPreferences
    const val ENCRYPTED_PREFS_NAME = "encrypted_prefs"
    const val KEY_PIN_CODE = "pin_code"
    const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
    
    // WorkManager
    const val WORK_DAILY_REMINDER = "daily_reminder_work"
}
