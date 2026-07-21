package com.lomigoo.classworkmanager.data.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class NotificationOrchestrator(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        NotificationScheduler.scheduleDailyReminders(applicationContext)
        return Result.success()
    }
}
