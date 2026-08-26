package com.example.util

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig

object AiConfigHelper {
    private const val PREFS_NAME = "gemini_ai_prefs"
    private const val KEY_CUSTOM_API_KEY = "custom_gemini_api_key"

    @Volatile
    private var inMemoryApiKey: String? = null

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getSavedApiKey(context: Context): String {
        return getPrefs(context).getString(KEY_CUSTOM_API_KEY, "")?.trim() ?: ""
    }

    fun saveApiKey(context: Context, key: String) {
        val trimmed = key.trim()
        inMemoryApiKey = trimmed
        getPrefs(context).edit().putString(KEY_CUSTOM_API_KEY, trimmed).apply()
    }

    fun setInMemoryKey(key: String) {
        inMemoryApiKey = key.trim()
    }

    fun getEffectiveApiKey(context: Context? = null): String {
        // 1. Check in-memory key first
        inMemoryApiKey?.let {
            if (it.isNotBlank()) return it
        }

        // 2. Check SharedPreferences if context is available
        if (context != null) {
            val savedKey = getSavedApiKey(context)
            if (savedKey.isNotBlank()) {
                inMemoryApiKey = savedKey
                return savedKey
            }
        }

        // 3. Check BuildConfig.GEMINI_API_KEY injected by environment
        val buildKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (!buildKey.isNullOrBlank() && buildKey != "null" && buildKey != "GEMINI_API_KEY_DEFAULT_VALUE") {
            return buildKey.trim()
        }

        return ""
    }
}
