package com.chastity.diary.domain.model

/**
 * Supported UI languages.
 * The string value is stored in DataStore and also used as a BCP-47 language tag.
 */
enum class AppLanguage(val tag: String) {
    SYSTEM(""),                  // Follow device locale (empty tag → system default)
    ENGLISH("en"),
    TRADITIONAL_CHINESE("zh-TW")
}
