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

        // Categorize tasks into urgency tiers
        val tier7Days = mutableListOf<com.lomigoo.classworkmanager.data.Classwork>()
        val tier3Days = mutableListOf<com.lomigoo.classworkmanager.data.Classwork>()
        val tier24Hours = mutableListOf<com.lomigoo.classworkmanager.data.Classwork>()

        classworks.forEach { task ->
            try {
                val targetDate = LocalDate.parse(task.dateTarget, formatter)
                when (ChronoUnit.DAYS.between(today, targetDate)) {
                    7L -> tier7Days.add(task)
                    3L -> tier3Days.add(task)
                    1L -> tier24Hours.add(task)
                }
            } catch (_: Exception) {
                // Ignore parsing errors
            }
        }

        // Send notifications for each tier (exactly one per tier)
        sendTierNotification(
            notificationHelper,
            NotificationHelper.ID_7_DAYS,
            tier7Days,
            "I see you're taking your time, remember your task %s is due to %s",
            "I see you're taking your time! You have %d tasks due in a week!"
        )

        sendTierNotification(
            notificationHelper,
            NotificationHelper.ID_3_DAYS,
            tier3Days,
            "You're gonna get cooked if you dont do this task %s, its due at %s!",
            "You're gonna get cooked! You have %d tasks due in 3 days!"
        )

        sendTierNotification(
            notificationHelper,
            NotificationHelper.ID_24_HOURS,
            tier24Hours,
            "Batman's not gonna help you through this, your task %s is now due at %s!",
            "Batman's not gonna help you through this! You have %d tasks due tomorrow!"
        )

        return Result.success()
    }

    private fun sendTierNotification(
        helper: NotificationHelper,
        id: Int,
        tasks: List<com.lomigoo.classworkmanager.data.Classwork>,
        singleMessageTemplate: String,
        summaryMessageTemplate: String
    ) {
        if (tasks.isEmpty()) return

        val message = if (tasks.size == 1) {
            val task = tasks[0]
            singleMessageTemplate.format(task.courseName, task.dateTarget)
        } else {
            summaryMessageTemplate.format(tasks.size)
        }

        helper.showNotification(
            id = id,
            title = "Deadline Reminder",
            message = message
        )
    }
}
