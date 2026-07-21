package com.lomigoo.classworkmanager.data.notifications

import android.content.Context
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.lomigoo.classworkmanager.data.AppPreferences
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import kotlin.random.Random

object NotificationScheduler {
    private const val TAG = "NotificationScheduler"

    fun scheduleDailyReminders(context: Context) {
        val prefs = AppPreferences(context)
        val manilaZone = ZoneId.of("Asia/Manila")
        val today = LocalDate.now(manilaZone)
        val todayStr = today.toString()

        if (prefs.getLastScheduledDate() == todayStr) {
            Log.d(TAG, "Reminders already scheduled for today: $todayStr")
            return
        }

        val now = LocalDateTime.now(manilaZone)
        val workManager = WorkManager.getInstance(context)

        // Morning Window: 06:00 - 12:00
        scheduleRandomWork(workManager, "MORNING", 6, 12, now)

        // Night Window: 18:00 - 23:59
        scheduleRandomWork(workManager, "NIGHT", 18, 23, now)

        prefs.setLastScheduledDate(todayStr)
        Log.d(TAG, "Successfully scheduled 2 morning and 2 night reminders for $todayStr")
    }

    private fun scheduleRandomWork(
        workManager: WorkManager,
        windowName: String,
        startHour: Int,
        endHour: Int,
        now: LocalDateTime,
    ) {
        val today = now.toLocalDate()
        
        // Pick 2 random times in the window
        val randomTimes = List(2) {
            val hour = Random.nextInt(startHour, endHour + 1)
            val minute = if (hour == endHour) 0 else Random.nextInt(0, 60)
            LocalTime.of(hour, minute)
        }.sorted()

        randomTimes.forEachIndexed { index, time ->
            val scheduledTime = today.atTime(time)
            
            if (scheduledTime.isAfter(now)) {
                val delay = Duration.between(now, scheduledTime).toMinutes()
                
                val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
                    .setInitialDelay(delay, TimeUnit.MINUTES)
                    .addTag("DeadlineReminder")
                    .build()

                workManager.enqueueUniqueWork(
                    "${windowName}_$index",
                    ExistingWorkPolicy.REPLACE,
                    workRequest
                )
                Log.d(TAG, "Enqueued $windowName reminder $index for $scheduledTime")
            } else {
                Log.d(TAG, "Skipping $windowName reminder $index as $scheduledTime is in the past")
            }
        }
    }
}
