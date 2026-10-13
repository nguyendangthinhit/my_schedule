package com.example.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.BuildConfig
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

data class ApiKeyInfo(
    val key: String,
    val isRateLimited: Boolean = false,
    val rateLimitedUntil: Long = 0L,
    val isFromBuildConfig: Boolean = false
) {
    val maskedKey: String
        get() = if (key.length > 8) "${key.take(4)}...${key.takeLast(4)}" else key
}

object AiConfigHelper {
    private const val TAG = "AiConfigHelper"
    private const val PREFS_NAME = "gemini_ai_prefs"
    private const val KEY_CUSTOM_API_KEYS = "custom_gemini_api_keys"
    private const val KEY_CUSTOM_API_KEY_LEGACY = "custom_gemini_api_key"

    @Volatile
    private var inMemoryApiKeys: List<String>? = null

    private val rateLimitedKeys = ConcurrentHashMap<String, Long>() // Key -> Expiration timestamp
    private val activeKeyIndex = AtomicInteger(0)

    // Cooldown duration for rate limited key (e.g. 2 minutes)
    private const val RATE_LIMIT_COOLDOWN_MS = 2 * 60 * 1000L

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Get raw stored API keys string from SharedPreferences
     */
    fun getRawSavedKeys(context: Context): String {
        val prefs = getPrefs(context)
        val keysStr = prefs.getString(KEY_CUSTOM_API_KEYS, "")?.trim() ?: ""
        if (keysStr.isNotBlank()) {
            return keysStr
        }
        // Fallback to legacy single key if exists
        return prefs.getString(KEY_CUSTOM_API_KEY_LEGACY, "")?.trim() ?: ""
    }

    /**
     * Save raw user input string containing one or multiple API keys (separated by newline, comma, or semicolon)
     */
    fun saveRawKeysInput(context: Context, rawInput: String) {
        val parsedKeys = parseKeysString(rawInput)
        inMemoryApiKeys = parsedKeys
        activeKeyIndex.set(0)

        val joined = parsedKeys.joinToString("\n")
        getPrefs(context).edit().putString(KEY_CUSTOM_API_KEYS, joined).apply()
        Log.d(TAG, "Saved ${parsedKeys.size} API keys to SharedPreferences")
    }

    fun saveApiKey(context: Context, key: String) {
        saveRawKeysInput(context, key)
    }

    fun setInMemoryKey(key: String) {
        setInMemoryKeys(key)
    }

    fun setInMemoryKeys(keysInput: String) {
        inMemoryApiKeys = parseKeysString(keysInput)
        activeKeyIndex.set(0)
    }

    /**
     * Parse raw string into distinct list of valid keys
     */
    fun parseKeysString(input: String): List<String> {
        if (input.isBlank()) return emptyList()
        return input.split("\n", ",", ";", " ")
            .map { it.trim() }
            .filter { key ->
                key.isNotBlank() &&
                        key != "null" &&
                        key != "GEMINI_API_KEY_DEFAULT_VALUE" &&
                        !key.startsWith("your_api_key")
            }
            .distinct()
    }

    /**
     * Get all configured API keys (User saved keys + BuildConfig key fallback)
     */
    fun getAllConfiguredKeys(context: Context? = null): List<String> {
        val resultList = mutableListOf<String>()

        // 1. Check in-memory keys
        inMemoryApiKeys?.let { keys ->
            if (keys.isNotEmpty()) {
                resultList.addAll(keys)
            }
        }

        // 2. Check SharedPreferences if empty
        if (resultList.isEmpty() && context != null) {
            val savedRaw = getRawSavedKeys(context)
            val savedList = parseKeysString(savedRaw)
            if (savedList.isNotEmpty()) {
                inMemoryApiKeys = savedList
                resultList.addAll(savedList)
            }
        }

        // 3. Add BuildConfig key if available
        val buildKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }?.trim()

        if (!buildKey.isNullOrBlank() && buildKey != "null" && buildKey != "GEMINI_API_KEY_DEFAULT_VALUE" && !buildKey.startsWith("your_api_key")) {
            if (!resultList.contains(buildKey)) {
                resultList.add(buildKey)
            }
        }

        return resultList.distinct()
    }

    /**
     * Get list of ApiKeyInfo objects with masked keys and rate limit status for UI display
     */
    fun getApiKeyInfoList(context: Context? = null): List<ApiKeyInfo> {
        val buildKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }?.trim()

        val now = System.currentTimeMillis()
        val keys = getAllConfiguredKeys(context)

        return keys.map { key ->
            val blockedUntil = rateLimitedKeys[key] ?: 0L
            val isBlocked = blockedUntil > now
            val isBuildKey = (key == buildKey)
            ApiKeyInfo(
                key = key,
                isRateLimited = isBlocked,
                rateLimitedUntil = blockedUntil,
                isFromBuildConfig = isBuildKey
            )
        }
    }

    /**
     * Get currently active effective API key
     */
    fun getEffectiveApiKey(context: Context? = null): String {
        val keys = getAllConfiguredKeys(context)
        if (keys.isEmpty()) return ""

        val now = System.currentTimeMillis()
        // Find first key that is not currently rate-limited
        for (i in keys.indices) {
            val idx = (activeKeyIndex.get() + i) % keys.size
            val key = keys[idx]
            val blockedUntil = rateLimitedKeys[key] ?: 0L
            if (blockedUntil <= now) {
                return key
            }
        }

        // If all keys are rate-limited, return active key anyway so caller can attempt/fail cleanly
        val fallbackIdx = activeKeyIndex.get() % keys.size
        return keys[fallbackIdx]
    }

    /**
     * Mark a key as rate-limited (HTTP 429 / Resource Exhausted) and rotate index
     */
    fun markKeyAsRateLimited(key: String, cooldownMs: Long = RATE_LIMIT_COOLDOWN_MS) {
        if (key.isBlank()) return
        val until = System.currentTimeMillis() + cooldownMs
        rateLimitedKeys[key] = until
        Log.w(TAG, "Marked key ${key.take(4)}... as rate-limited until $until")
    }

    /**
     * Rotate active key index to next available key
     */
    fun rotateToNextKey(context: Context? = null, currentFailedKey: String? = null): String {
        if (!currentFailedKey.isNullOrBlank()) {
            markKeyAsRateLimited(currentFailedKey)
        }

        val keys = getAllConfiguredKeys(context)
        if (keys.isEmpty()) return ""

        val nextIdx = activeKeyIndex.incrementAndGet() % keys.size
        val newKey = keys[nextIdx]
        Log.i(TAG, "Rotated API key to index $nextIdx (prefix: ${newKey.take(4)}...)")
        return newKey
    }

    /**
     * Execute a block with automatic API key rotation on 429/403/401 rate-limit or key errors
     */
    suspend fun <T> executeWithKeyRotation(
        context: Context? = null,
        block: suspend (apiKey: String) -> T
    ): T {
        val keys = getAllConfiguredKeys(context)
        if (keys.isEmpty()) {
            // Attempt block with empty key to let standard missing-key handling trigger
            return block("")
        }

        var lastException: Exception? = null
        val attemptsCount = keys.size

        for (attempt in 0 until attemptsCount) {
            val currentKey = getEffectiveApiKey(context)
            if (currentKey.isBlank()) {
                break
            }

            try {
                Log.d(TAG, "Executing API call with key attempt ${attempt + 1}/$attemptsCount (prefix: ${currentKey.take(4)}...)")
                val result = block(currentKey)
                // Clear rate-limit flag if key succeeded
                rateLimitedKeys.remove(currentKey)
                return result
            } catch (e: Exception) {
                lastException = e
                val isRateLimitOrKeyError = isRateLimitOrKeyError(e)

                if (isRateLimitOrKeyError && attemptsCount > 1 && attempt < attemptsCount - 1) {
                    Log.w(TAG, "Key ${currentKey.take(4)}... hit error (${e.message}). Rotating to next key...")
                    rotateToNextKey(context, currentKey)
                } else {
                    // Not a rate-limit error or no more keys to rotate
                    throw e
                }
            }
        }

        throw lastException ?: IllegalStateException("Tất cả API keys đều bị lỗi hoặc không khả dụng.")
    }

    /**
     * Check if exception is caused by HTTP 429 (Rate Limit), 403 (Resource Exhausted), 400 or 401 (Key Error)
     */
    fun isRateLimitOrKeyError(e: Exception): Boolean {
        if (e is retrofit2.HttpException) {
            val code = e.code()
            if (code == 429 || code == 403 || code == 401 || code == 400) {
                return true
            }
        }
        val msg = e.message ?: ""
        return msg.contains("429") ||
                msg.contains("RESOURCE_EXHAUSTED", ignoreCase = true) ||
                msg.contains("Quota exceeded", ignoreCase = true) ||
                msg.contains("API key", ignoreCase = true)
    }
}

