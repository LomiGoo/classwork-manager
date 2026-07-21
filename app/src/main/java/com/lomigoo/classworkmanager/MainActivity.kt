package com.lomigoo.classworkmanager

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.lomigoo.classworkmanager.data.AppPreferences
import com.lomigoo.classworkmanager.data.ClassworkDbHelper
import com.lomigoo.classworkmanager.data.UpdateManager
import com.lomigoo.classworkmanager.data.notifications.NotificationOrchestrator
import com.lomigoo.classworkmanager.data.notifications.NotificationScheduler
import com.lomigoo.classworkmanager.data.notifications.NotificationWorker
import com.lomigoo.classworkmanager.ui.ClassworkApp
import com.lomigoo.classworkmanager.ui.ClassworkViewModel
import com.lomigoo.classworkmanager.ui.ClassworkViewModelFactory
import com.lomigoo.classworkmanager.ui.theme.ClassworkManagerTheme
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appPreferences = AppPreferences(applicationContext)
        val dbHelper = ClassworkDbHelper(applicationContext)
        val updateManager = UpdateManager(applicationContext, appPreferences)
        val factory = ClassworkViewModelFactory(dbHelper, updateManager, appPreferences)
        val viewModel = ViewModelProvider(this, factory).get(ClassworkViewModel::class.java)

        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsState()

            ClassworkManagerTheme(darkTheme = isDarkMode) {
                ClassworkApp(viewModel)
            }
        }

        // Setup notifications and permissions after UI initialization
        askNotificationPermission()
        try {
            scheduleDeadlineChecks()
        } catch (e: Exception) {
            // Silently fail if WorkManager initialization has issues
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun scheduleDeadlineChecks() {
        // 1. Immediate check/schedule for today
        NotificationScheduler.scheduleDailyReminders(applicationContext)

        // 2. Schedule a daily orchestrator to plan the next day
        val manilaZone = ZoneId.of("Asia/Manila")
        val now = LocalDateTime.now(manilaZone)
        val midnight = now.toLocalDate().plusDays(1).atStartOfDay()
        val initialDelay = Duration.between(now, midnight).toMinutes()

        val orchestratorRequest = PeriodicWorkRequestBuilder<NotificationOrchestrator>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "DailyNotificationOrchestrator",
            ExistingPeriodicWorkPolicy.REPLACE,
            orchestratorRequest
        )
    }
}
