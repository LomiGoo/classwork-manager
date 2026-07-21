package com.lomigoo.classworkmanager.data

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {
    private val sharedPrefs: SharedPreferences =
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_DARK_MODE = "is_dark_mode"
        private const val KEY_DATE_FORMAT = "date_format"
        private const val KEY_LAST_SEEN_VERSION = "last_seen_version"
        private const val KEY_LAST_SCHEDULED_DATE = "last_scheduled_date"
        private const val KEY_THEME = "app_theme"

        const val FORMAT_NUMBERED = "NUMBERED" // yyyy-MM-dd
        const val FORMAT_READABLE = "READABLE" // February 1, 2026

        const val THEME_JRU = "JRU"
        const val THEME_EMERALD = "EMERALD"
        const val THEME_CRIMSON = "CRIMSON"
        const val THEME_AMETHYST = "AMETHYST"
        const val THEME_NEUTRAL = "NEUTRAL"
    }

    fun isDarkMode(): Boolean = sharedPrefs.getBoolean(KEY_DARK_MODE, false)

    fun setDarkMode(isDark: Boolean) {
        sharedPrefs.edit().putBoolean(KEY_DARK_MODE, isDark).apply()
    }

    fun getDateFormat(): String = sharedPrefs.getString(KEY_DATE_FORMAT, FORMAT_NUMBERED) ?: FORMAT_NUMBERED

    fun setDateFormat(format: String) {
        sharedPrefs.edit().putString(KEY_DATE_FORMAT, format).apply()
    }

    fun getLastSeenVersion(): String = sharedPrefs.getString(KEY_LAST_SEEN_VERSION, "0.0.0") ?: "0.0.0"

    fun setLastSeenVersion(version: String) {
        sharedPrefs.edit().putString(KEY_LAST_SEEN_VERSION, version).apply()
    }

    fun getLastScheduledDate(): String = sharedPrefs.getString(KEY_LAST_SCHEDULED_DATE, "") ?: ""

    fun setLastScheduledDate(date: String) {
        sharedPrefs.edit().putString(KEY_LAST_SCHEDULED_DATE, date).apply()
    }

    fun getTheme(): String = sharedPrefs.getString(KEY_THEME, THEME_JRU) ?: THEME_JRU

    fun setTheme(theme: String) {
        sharedPrefs.edit().putString(KEY_THEME, theme).apply()
    }
}
