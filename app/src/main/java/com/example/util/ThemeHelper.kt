package com.example.util

import android.content.Context
import android.content.SharedPreferences

enum class ThemeMode(val key: String, val title: String, val subtitle: String) {
    LIGHT("light", "Chế độ Sáng", "Giao diện nền sáng rõ nét"),
    DARK("dark", "Chế độ Tối", "Giao diện tối dịu mắt, tiết kiệm pin"),
    SYSTEM("system", "Theo hệ thống", "Tự động theo giao diện điện thoại");

    companion object {
        fun fromKey(key: String): ThemeMode {
            return entries.find { it.key == key } ?: SYSTEM
        }
    }
}

object ThemeHelper {
    private const val PREFS_NAME = "app_theme_prefs"
    private const val KEY_THEME_MODE = "theme_mode"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getThemeMode(context: Context): ThemeMode {
        val key = getPrefs(context).getString(KEY_THEME_MODE, ThemeMode.SYSTEM.key) ?: ThemeMode.SYSTEM.key
        return ThemeMode.fromKey(key)
    }

    fun setThemeMode(context: Context, mode: ThemeMode) {
        getPrefs(context).edit().putString(KEY_THEME_MODE, mode.key).apply()
        com.example.widget.WidgetUpdateHelper.updateAllWidgets(context)
    }

    fun isDarkMode(context: Context): Boolean {
        return when (getThemeMode(context)) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> {
                val currentNightMode = context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
                currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
            }
        }
    }

    fun getThemedContext(context: Context): Context {
        val mode = getThemeMode(context)
        val targetNight = when (mode) {
            ThemeMode.LIGHT -> android.content.res.Configuration.UI_MODE_NIGHT_NO
            ThemeMode.DARK -> android.content.res.Configuration.UI_MODE_NIGHT_YES
            ThemeMode.SYSTEM -> return context
        }
        val config = android.content.res.Configuration(context.resources.configuration).apply {
            uiMode = (uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK.inv()) or targetNight
        }
        return context.createConfigurationContext(config)
    }
}
