package com.chastity.diary.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.chastity.diary.data.local.database.AppDatabase
import com.chastity.diary.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Worker that sends the morning check-in reminder notification.
 */
class MorningReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // U-4: Skip notification if morning check-in is already done for today
            val database = AppDatabase.getDatabase(applicationContext)
            val today = LocalDate.now()
            val existingEntry = database.dailyEntryDao().getByDate(today)
            if (existingEntry != null && existingEntry.morningCheckDone) {
                return@withContext Result.success()
            }
            NotificationHelper.showMorningReminderNotification(applicationContext)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
