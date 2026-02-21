package com.chastity.diary.utils

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
    
    private const val CHANNEL_ID = "daily_reminder"
    private const val CHANNEL_NAME = "每日提醒"
    private const val CHANNEL_DESCRIPTION = "提醒您記錄每日貞操日記"

    private const val MORNING_CHANNEL_ID = "morning_reminder"
    private const val MORNING_CHANNEL_NAME = "早晨 Check-in"
    private const val MORNING_CHANNEL_DESCRIPTION = "提醒您完成早晨睡眠與狀態記錄"
    
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            notificationManager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                    description = CHANNEL_DESCRIPTION
                }
            )
            notificationManager.createNotificationChannel(
                NotificationChannel(MORNING_CHANNEL_ID, MORNING_CHANNEL_NAME, importance).apply {
                    description = MORNING_CHANNEL_DESCRIPTION
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
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("記錄您的每日日記")
            .setContentText("別忘了記錄今天的貞操日記哦！")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1001, notification)
    }

    fun showMorningReminderNotification(context: Context) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 1, intent, PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, MORNING_CHANNEL_ID)
            .setContentTitle("☀️ 早晨 Check-in")
            .setContentText("記錄昨晚睡眠與今晨狀態，只需幾秒鐘！")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1002, notification)
    }
}
