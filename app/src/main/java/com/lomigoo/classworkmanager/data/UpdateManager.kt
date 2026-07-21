package com.lomigoo.classworkmanager.data

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.lomigoo.classworkmanager.data.notifications.NotificationWorker
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

class UpdateManager(private val context: Context, private val appPreferences: AppPreferences) {
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val TAG = "UpdateManager"
    }

    suspend fun checkForUpdates(): UpdateInfo = withContext(Dispatchers.IO) {
        val currentVersion = getCurrentVersion()
        val releases = fetchReleases()
        val lastSeenVersion = appPreferences.getLastSeenVersion()

        val latestRelease = releases.firstOrNull { !it.preRelease && !it.draft }

        if (latestRelease != null) {
            val latestVersion = latestRelease.tagName.removePrefix("v").trim()
            val isUpdateAvailable = isVersionNewer(currentVersion, latestVersion)
            
            Log.d(TAG, "Detection Check: Local=$currentVersion, GitHub=$latestVersion, UpdateAvailable=$isUpdateAvailable")

            val apkAsset = latestRelease.assets.find { it.name.endsWith(".apk") }
            
            UpdateInfo(
                isUpdateAvailable = isUpdateAvailable,
                latestVersion = latestVersion,
                releaseUrl = latestRelease.htmlUrl,
                downloadUrl = apkAsset?.downloadUrl,
                releaseName = latestRelease.name ?: "Version $latestVersion",
                releaseNotes = latestRelease.body ?: "Bug fixes and performance improvements.",
                isWhatsNewAvailable = currentVersion == latestVersion // Available if notes haven't been shown for this version
            )
        } else {
            Log.d(TAG, "Detection Check: No releases found on GitHub.")
            UpdateInfo(
                isUpdateAvailable = false, 
                latestVersion = currentVersion, 
                releaseUrl = "",
                releaseNotes = "Unable to fetch release notes from GitHub."
            )
        }
    }

    fun markVersionAsSeen(version: String) {
        appPreferences.setLastSeenVersion(version)
    }

    fun downloadAndInstallApk(url: String): Flow<Int> = callbackFlow {
        val destination = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            "update.apk"
        )
        if (destination.exists()) destination.delete()

        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Downloading Update")
            .setDescription("Classwork Manager is downloading a new version.")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(destination))

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(request)

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (id == downloadId) {
                    installApk(destination)
                    trySend(100)
                    close()
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(
                receiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                Context.RECEIVER_EXPORTED
            )
        } else {
            context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }

        awaitClose {
            context.unregisterReceiver(receiver)
        }
    }

    private fun installApk(file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    }

    fun getCurrentVersion(): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                ).versionName ?: "1.0"
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
            }
        } catch (_: Exception) {
            "1.0"
        }
    }

    private fun fetchReleases(): List<GitHubRelease> {
        val request = Request.Builder()
            .url("https://api.github.com/repos/LomiGoo/classwork-manager/releases")
            .header("Accept", "application/vnd.github.v3+json")
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string()?.let { body ->
                        try {
                            json.decodeFromString<List<GitHubRelease>>(body)
                        } catch (e: Exception) {
                            Log.e(TAG, "Parsing error: ${e.message}")
                            emptyList()
                        }
                    } ?: emptyList()
                } else {
                    Log.e(TAG, "API Error: ${response.code}")
                    emptyList()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error: ${e.message}")
            emptyList()
        }
    }

    fun triggerNotificationTest() {
        val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>().build()
        WorkManager.getInstance(context).enqueue(workRequest)
    }

    private fun isVersionNewer(current: String, latest: String): Boolean {
        // Clean strings and split by dots/dashes
        val currentParts = current.lowercase().removePrefix("v").split(".", "-").mapNotNull { it.toIntOrNull() }
        val latestParts = latest.lowercase().removePrefix("v").split(".", "-").mapNotNull { it.toIntOrNull() }

        val maxLength = maxOf(currentParts.size, latestParts.size)
        for (i in 0 until maxLength) {
            val currentPart = if (i < currentParts.size) currentParts[i] else 0
            val latestPart = if (i < latestParts.size) latestParts[i] else 0
            
            if (latestPart > currentPart) return true
            if (latestPart < currentPart) return false
        }
        return false
    }
}
