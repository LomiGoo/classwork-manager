package com.lomigoo.classworkmanager.data.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lomigoo.classworkmanager.data.ClassworkDbHelper
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class NotificationWorker(
    context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): androidx.work.ListenableWorker.Result {
        val dbHelper = ClassworkDbHelper(applicationContext)
        val notificationHelper = NotificationHelper(applicationContext)
        val classworks = dbHelper.getAllClasswork().filter { !it.isCompleted }

        val manilaZone = ZoneId.of("Asia/Manila")
        val today = LocalDate.now(manilaZone)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        classworks.forEach { task ->
            try {
                val targetDate = LocalDate.parse(task.dateTarget, formatter)

                val message = when (ChronoUnit.DAYS.between(today, targetDate)) {
                    7L -> "I see you're taking your time, remember your task ${task.courseName} is due to ${task.dateTarget}"
                    3L -> "You're gonna get cooked if you dont do this task ${task.courseName}, its due at ${task.dateTarget}!"
                    1L -> "Batman's not gonna help you through this, your task ${task.courseName} is now due at ${task.dateTarget}!"
                    else -> null
                }

                message?.let {
                    notificationHelper.showNotification(
                        id = task.id,
                        title = "Deadline Reminder",
                        message = it
                    )
                }
            } catch (_: Exception) {
                // Ignore parsing errors for individual tasks
            }
        }

        return Result.success()
    }
}
