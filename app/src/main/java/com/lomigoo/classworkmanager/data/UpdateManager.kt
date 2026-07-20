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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException

@Serializable
data class GitHubAsset(
    val name: String,
    @SerialName("browser_download_url") val downloadUrl: String,
)

@Serializable
data class GitHubRelease(
    @SerialName("tag_name") val tagName: String,
    @SerialName("html_url") val htmlUrl: String,
    val body: String? = null,
    val assets: List<GitHubAsset> = emptyList(),
)

class UpdateManager(private val context: Context) {
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    data class UpdateInfo(
        val isUpdateAvailable: Boolean,
        val latestVersion: String,
        val releaseUrl: String,
        val downloadUrl: String? = null,
    )

    suspend fun checkForUpdates(): UpdateInfo = withContext(Dispatchers.IO) {
        val currentVersion = getCurrentVersion()
        val latestRelease = fetchLatestRelease()

        if (latestRelease != null) {
            val latestVersion = latestRelease.tagName.removePrefix("v")
            val isAvailable = isVersionNewer(currentVersion, latestVersion)
            val apkAsset = latestRelease.assets.find { it.name.endsWith(".apk") }
            UpdateInfo(isAvailable, latestVersion, latestRelease.htmlUrl, apkAsset?.downloadUrl)
        } else {
            UpdateInfo(isUpdateAvailable = false, latestVersion = currentVersion, releaseUrl = "")
        }
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

    private fun fetchLatestRelease(): GitHubRelease? {
        val request = Request.Builder()
            .url("https://api.github.com/repos/LomiGoo/classwork-manager/releases/latest")
            .header("Accept", "application/vnd.github.v3+json")
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string()?.let { body ->
                        json.decodeFromString<GitHubRelease>(body)
                    }
                } else null
            }
        } catch (e: IOException) {
            null
        }
    }

    private fun isVersionNewer(current: String, latest: String): Boolean {
        val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }
        val latestParts = latest.split(".").mapNotNull { it.toIntOrNull() }

        for (i in 0 until minOf(currentParts.size, latestParts.size)) {
            if (latestParts[i] > currentParts[i]) return true
            if (latestParts[i] < currentParts[i]) return false
        }
        return latestParts.size > currentParts.size
    }
}
