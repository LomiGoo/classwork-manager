package com.lomigoo.classworkmanager.data

import android.content.Context
import android.content.SharedPreferences

class ThemePreference(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)

    fun isDarkMode(): Boolean {
        return sharedPreferences.getBoolean("is_dark_mode", false)
    }

    fun setDarkMode(isDark: Boolean) {
        sharedPreferences.edit().putBoolean("is_dark_mode", isDark).apply()
    }
}
