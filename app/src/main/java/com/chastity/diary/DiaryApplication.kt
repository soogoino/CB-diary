package com.chastity.diary

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.chastity.diary.util.Constants

/**
 * Application class
 */
class DiaryApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // AppCompat automatically restores the per-app locale from its own
        // storage (SharedPreferences on API < 33, system store on API 33+ via
        // android:autoStoreLocales="true").  No manual restore is needed here;
        // calling setApplicationLocales() again would conflict with AppCompat's
        // own mechanism and cause locale resets after process death.

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
