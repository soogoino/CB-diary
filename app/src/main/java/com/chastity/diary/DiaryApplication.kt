package com.chastity.diary

import android.app.Application
import com.chastity.diary.util.NotificationHelper

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

        // E-6: Create ALL notification channels here (in Application.onCreate) to guarantee
        // they exist before any Worker fires, regardless of SettingsViewModel init order.
        NotificationHelper.createNotificationChannel(this)
    }
}
