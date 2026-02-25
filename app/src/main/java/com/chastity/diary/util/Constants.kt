package com.chastity.diary.util

object Constants {
    // Mood options (English — default locale)
    val MOODS = listOf(
        "😊 Happy", "😌 Calm", "😐 Neutral", "😔 Down",
        "😰 Anxious", "😤 Frustrated", "🥵 Excited", "😴 Bored",
        "😳 Ashamed", "😎 Proud", "🤩 Eager", "😬 Nervous",
        "🧘 Relaxed", "😕 Confused", "💪 Fulfilled", "😶 Empty"
    )

    // Exercise types
    val EXERCISE_TYPES = listOf(
        "Running", "Gym", "Swimming", "Yoga", "Cycling",
        "Ball sports", "Walking", "Weight training", "Cardio", "Other"
    )

    // Locations for exposed lock
    val EXPOSED_LOCATIONS = listOf(
        "Home", "Gym", "Swimming pool", "Public bath", "Outdoors",
        "Locker room", "Hospital", "Friend's place", "Workplace", "Other public place"
    )

    // Discomfort areas
    val DISCOMFORT_AREAS = listOf(
        "Penis", "Testicles", "Perineum", "Inner thigh", "Pubic area", "Urethra", "Other"
    )

    // Leakage amounts
    val LEAKAGE_AMOUNTS = listOf("Small", "Moderate", "Large")

    // Edging methods
    val EDGING_METHODS = listOf(
        "Visual", "Touch", "Audio", "Imagination", "Reading", "Video", "Other"
    )

    // Keyholder interaction types
    val INTERACTION_TYPES = listOf(
        "Text chat", "Voice call", "Video call", "In person",
        "Task assigned", "Reward", "Punishment", "Check-in", "Other"
    )

    // Cleaning types (index 0 = "no cleaning")
    val CLEANING_TYPES = listOf("No cleaning", "Quick rinse", "Deep clean", "Fully removed & cleaned")

    // Removal reasons
    val REMOVAL_REASONS = listOf(
        "Cleaning", "Medical", "Work requirement", "Emergency",
        "Keyholder approved", "Discomfort", "Other"
    )

    // Social activities
    val SOCIAL_ACTIVITIES = listOf(
        "Dining out", "Gym", "Swimming", "Family / friends gathering",
        "Work meeting", "Date", "Shopping", "Travel", "Other"
    )

    // Emotions (extended)
    val EMOTIONS = listOf(
        "Excited", "Anxious", "Down", "Calm", "Frustrated",
        "Satisfied", "Ashamed", "Proud", "Bored", "Eager",
        "Nervous", "Relaxed", "Confused", "Fulfilled", "Empty"
    )

    // Time duration quick options (minutes)
    val DURATION_OPTIONS = listOf(
        5, 10, 15, 30, 45, 60, 90, 120, 180, 240
    )

    // Night erection — index-based: 0=None, 1=Occasional, 2=Frequent
    // Displayed labels come from stringArrayResource(R.array.night_erection_options_array)
    val NIGHT_ERECTION_OPTIONS_KEYS = listOf("None", "Occasional", "Frequent")
    val NIGHT_ERECTION_SCORE_FOR_INDEX = listOf(0, 5, 10)

    // Time duration quick-pick chips shown in the UI (6 items, 2 rows of 3)
    val DURATION_QUICK_OPTIONS = listOf(5, 10, 15, 30, 60, 120)

    // Database
    const val DATABASE_NAME = "chastity_diary_db"
    const val DATABASE_VERSION = 5  // v5: added UNIQUE INDEX on daily_entries.date (C-1 perf)

    // Photo
    const val PREF_PHOTO_BLUR_ENABLED = "photo_blur_enabled"

    // DataStore
    const val DATASTORE_NAME = "user_preferences"

    // Notification — Daily reminder
    const val NOTIFICATION_CHANNEL_ID = "daily_reminder"
    const val NOTIFICATION_ID = 1001
    const val DEFAULT_REMINDER_HOUR = 21
    const val DEFAULT_REMINDER_MINUTE = 0

    // Notification — Morning reminder
    const val MORNING_NOTIFICATION_CHANNEL_ID = "morning_reminder"
    const val MORNING_NOTIFICATION_ID = 1002
    const val DEFAULT_MORNING_REMINDER_HOUR = 7
    const val DEFAULT_MORNING_REMINDER_MINUTE = 30

    // PendingIntent request codes
    const val PENDING_INTENT_DAILY = 0
    const val PENDING_INTENT_MORNING = 1

    // Encrypted SharedPreferences
    // A-1 fix: was "encrypted_prefs" — never matched the actual prefs file "secure_prefs"
    const val ENCRYPTED_PREFS_NAME = "secure_prefs"
    const val KEY_PIN_CODE = "pin_code"
    const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
    // A-3 fix: centralised key so all read/write sites stay in sync
    const val KEY_LOCK_ENABLED = "lock_enabled"

    // WorkManager unique work names
    // B-1 fix: was "daily_reminder_work" — mismatched SettingsViewModel's "daily_reminder"
    const val WORK_DAILY_REMINDER = "daily_reminder"
    const val WORK_MORNING_REMINDER = "morning_reminder"

    // PIN constraints
    const val PIN_MIN_LENGTH = 4
    const val PIN_MAX_LENGTH = 6

    // Dashboard chart
    const val CHART_MAX_DATA_POINTS = 14
}
