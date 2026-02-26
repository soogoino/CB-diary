package com.chastity.diary.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.chastity.diary.MainActivity
import com.chastity.diary.R

object NotificationHelper {

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    Constants.NOTIFICATION_CHANNEL_ID,
                    context.getString(R.string.notification_channel_name),
                    importance
                ).apply {
                    description = context.getString(R.string.notification_channel_description)
                }
            )
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    Constants.MORNING_NOTIFICATION_CHANNEL_ID,
                    context.getString(R.string.morning_notification_channel_name),
                    importance
                ).apply {
                    description = context.getString(R.string.morning_notification_channel_description)
                }
            )
        }
    }

    fun showDailyReminderNotification(context: Context) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            Constants.PENDING_INTENT_DAILY,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(context.getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(Constants.NOTIFICATION_ID, notification)
    }

    fun showMorningReminderNotification(context: Context) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            Constants.PENDING_INTENT_MORNING,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        val notification =
            NotificationCompat.Builder(context, Constants.MORNING_NOTIFICATION_CHANNEL_ID)
                .setContentTitle(context.getString(R.string.morning_notification_title))
                .setContentText(context.getString(R.string.morning_notification_text))
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(Constants.MORNING_NOTIFICATION_ID, notification)
    }
}

