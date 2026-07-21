package com.lomigoo.classworkmanager.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubAsset(
    val name: String,
    @SerialName("browser_download_url") val downloadUrl: String,
)

@Serializable
data class GitHubRelease(
    @SerialName("tag_name") val tagName: String,
    @SerialName("html_url") val htmlUrl: String,
    val name: String? = null,
    val body: String? = null,
    val draft: Boolean = false,
    @SerialName("prerelease") val preRelease: Boolean = false,
    val assets: List<GitHubAsset> = emptyList(),
)

data class UpdateInfo(
    val isUpdateAvailable: Boolean,
    val latestVersion: String,
    val releaseUrl: String,
    val downloadUrl: String? = null,
    val releaseName: String? = null,
    val releaseNotes: String? = null,
    val isWhatsNewAvailable: Boolean = false,
)
