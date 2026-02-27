package com.chastity.diary

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.chastity.diary.util.Constants
import com.chastity.diary.data.datastore.PreferencesManager
import com.chastity.diary.domain.model.AppLanguage
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first

/**
 * Application class
 */
class DiaryApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Synchronously read persisted user settings once at startup and apply
        // the saved language to the application locales so the UI shows the
        // configured language immediately.
        try {
            val prefs = PreferencesManager(this)
            val userSettings = runBlocking { prefs.userSettingsFlow.first() }
            val tag = when (userSettings.language) {
                AppLanguage.TRADITIONAL_CHINESE -> "zh-TW"
                else -> "en"
            }
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
        } catch (e: Exception) {
            // If anything fails, fall back to system default and continue startup
        }

        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.notification_channel_description)
                enableVibration(true)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}
