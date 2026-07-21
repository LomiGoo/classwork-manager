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
import com.lomigoo.classworkmanager.data.ClassworkDbHelper
import com.lomigoo.classworkmanager.data.ThemePreference
import com.lomigoo.classworkmanager.data.UpdateManager
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

        val dbHelper = ClassworkDbHelper(applicationContext)
        val updateManager = UpdateManager(applicationContext)
        val themePreference = ThemePreference(applicationContext)
        val factory = ClassworkViewModelFactory(dbHelper, updateManager, themePreference)
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
        val manilaZone = ZoneId.of("Asia/Manila")
        val now = LocalDateTime.now(manilaZone)
        
        // Target times: 6 AM and 6 PM
        val target6AM = now.with(LocalTime.of(6, 0))
        val target6PM = now.with(LocalTime.of(18, 0))

        val nextRun = when {
            now.isBefore(target6AM) -> target6AM
            now.isBefore(target6PM) -> target6PM
            else -> target6AM.plusDays(1)
        }

        val initialDelay = Duration.between(now, nextRun).toMinutes()

        val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(12, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "DeadlineCheckWork",
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }
}
