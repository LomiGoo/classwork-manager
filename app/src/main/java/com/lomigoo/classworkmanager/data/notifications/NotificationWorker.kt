package com.lomigoo.classworkmanager.data.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lomigoo.classworkmanager.data.ClassworkDbHelper

class NotificationWorker(
    context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): androidx.work.ListenableWorker.Result {
        val dbHelper = ClassworkDbHelper(applicationContext)
        val notificationHelper = NotificationHelper(applicationContext)
        val classworks = dbHelper.getAllClasswork().filter { !it.isCompleted }

        if (classworks.isEmpty()) return androidx.work.ListenableWorker.Result.success()

        // Get the most urgent task (or just any pending task)
        val task = classworks.minByOrNull { it.dateTarget } ?: classworks[0]
        
        val templates = listOf(
            "I see you're taking your time, remember your task %s is due to %s",
            "You're gonna get cooked if you dont do this task %s, its due at %s!",
            "Batman's not gonna help you through this, your task %s is now due at %s!"
        )
        
        val randomTemplate = templates.random()
        val message = randomTemplate.format(task.courseName, task.dateTarget)

        notificationHelper.showNotification(
            id = 1000, // Single ID so new random reminders replace old ones
            title = "Deadline Reminder",
            message = message
        )

        return androidx.work.ListenableWorker.Result.success()
    }
}
