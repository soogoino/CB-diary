package com.chastity.diary.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.chastity.diary.data.local.database.AppDatabase
import com.chastity.diary.utils.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Worker that sends daily reminder notifications
 */
class DailyReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Check if entry already exists for today
            val database = AppDatabase.getDatabase(applicationContext)
            val today = LocalDate.now()
            val existingEntry = database.dailyEntryDao().getByDate(today)
            
            // Only send notification if no entry exists
            if (existingEntry == null) {
                NotificationHelper.showDailyReminderNotification(applicationContext)
            }
            
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
