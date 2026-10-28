package com.example.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.BuildConfig
import com.example.network.GeminiApiService
import com.example.network.GroqApiService
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

enum class AiProvider(val key: String, val displayName: String, val subtitle: String, val iconBadge: String) {
    GROQ("groq", "Groq Cloud (LPU)", "Siêu tốc ~300 tok/s • Không lo cao tải server", "🚀"),
    GEMINI("gemini", "Google Gemini", "Đa năng • Phân tích lịch trình thông minh", "⚡")
}

data class AiModelInfo(
    val id: String,
    val provider: AiProvider,
    val displayName: String,
    val description: String,
    val isRecommended: Boolean = false
)

data class ApiKeyInfo(
    val key: String,
    val isRateLimited: Boolean = false,
    val rateLimitedUntil: Long = 0L,
    val isFromBuildConfig: Boolean = false,
    val provider: AiProvider = AiProvider.GEMINI
) {
    val maskedKey: String
        get() = if (key.length > 8) "${key.take(4)}...${key.takeLast(4)}" else key
}

object AiConfigHelper {
    private const val TAG = "AiConfigHelper"
    private const val PREFS_NAME = "gemini_ai_prefs"

    // Storage Keys
    private const val KEY_CUSTOM_API_KEYS = "custom_gemini_api_keys"
    private const val KEY_CUSTOM_API_KEY_LEGACY = "custom_gemini_api_key"
    private const val KEY_CUSTOM_GROQ_API_KEYS = "custom_groq_api_keys"
    private const val KEY_SELECTED_PROVIDER = "selected_ai_provider"
    private const val KEY_SELECTED_MODEL = "selected_ai_model"

    // Supported Models Catalog
    val AVAILABLE_MODELS = listOf(
        // Groq Models
        AiModelInfo(
            id = GroqApiService.MODEL_LLAMA_3_3_70B,
            provider = AiProvider.GROQ,
            displayName = "Groq Llama 3.3 70B Versatile",
            description = "Mạnh nhất, suy luận lịch trình chuẩn xác, tốc độ cực nhanh, không lo cao tải server",
            isRecommended = true
        ),
        AiModelInfo(
            id = GroqApiService.MODEL_LLAMA_3_1_8B,
            provider = AiProvider.GROQ,
            displayName = "Groq Llama 3.1 8B Instant",
            description = "Phản hồi gần như tức thì (~800 tok/s), thích hợp hội thoại nhanh",
            isRecommended = false
        ),
        AiModelInfo(
            id = GroqApiService.MODEL_MIXTRAL_8X7B,
            provider = AiProvider.GROQ,
            displayName = "Groq Mixtral 8x7B",
            description = "Kiến trúc MoE mạnh mẽ, ngữ cảnh rộng 32k tokens",
            isRecommended = false
        ),

        // Gemini Models (Chỉ giữ 3.5 Flash Lite và 3.1 Flash Lite)
        AiModelInfo(
            id = GeminiApiService.MODEL_GEMINI_3_5_FLASH_LITE,
            provider = AiProvider.GEMINI,
            displayName = "Gemini 3.5 Flash Lite",
            description = "Mô hình Gemini mặc định tối ưu, xử lý ngôn ngữ tự nhiên và phân tích thời gian biểu",
            isRecommended = true
        ),
        AiModelInfo(
            id = GeminiApiService.MODEL_GEMINI_3_1_FLASH_LITE,
            provider = AiProvider.GEMINI,
            displayName = "Gemini 3.1 Flash Lite (Preview)",
            description = "Bản xem trước thử nghiệm thế hệ mới, dự phòng khi 3.5 gặp sự cố",
            isRecommended = false
        )
    )

    @Volatile
    private var inMemoryGeminiKeys: List<String>? = null

    @Volatile
    private var inMemoryGroqKeys: List<String>? = null

    private val rateLimitedKeys = ConcurrentHashMap<String, Long>() // Key -> Expiration timestamp
    private val activeGeminiKeyIndex = AtomicInteger(0)
    private val activeGroqKeyIndex = AtomicInteger(0)

    // Cooldown duration for rate limited key (e.g. 2 minutes)
    private const val RATE_LIMIT_COOLDOWN_MS = 2 * 60 * 1000L

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // -------------------------------------------------------------
    // Provider & Model Selection
    // -------------------------------------------------------------

    fun getSelectedProvider(context: Context): AiProvider {
        val saved = getPrefs(context).getString(KEY_SELECTED_PROVIDER, null)
        if (saved != null) {
            return try {
                AiProvider.valueOf(saved)
            } catch (_: Exception) {
                if (saved.equals("groq", ignoreCase = true)) AiProvider.GROQ else AiProvider.GEMINI
            }
        }
        // If user already has a Groq key configured, default to GROQ, else GEMINI
        val hasGroq = getAllConfiguredGroqKeys(context).isNotEmpty()
        return if (hasGroq) AiProvider.GROQ else AiProvider.GEMINI
    }

    fun setSelectedProvider(context: Context, provider: AiProvider) {
        getPrefs(context).edit().putString(KEY_SELECTED_PROVIDER, provider.name).apply()
        Log.d(TAG, "Selected AI Provider: ${provider.name}")
    }

    fun getSelectedModel(context: Context): String {
        val provider = getSelectedProvider(context)
        val saved = getPrefs(context).getString(KEY_SELECTED_MODEL, null)
        if (!saved.isNullOrBlank()) {
            // Verify model matches current provider
            val modelMatches = AVAILABLE_MODELS.any { it.id == saved && it.provider == provider }
            if (modelMatches) return saved
        }
        // Default based on provider
        return if (provider == AiProvider.GROQ) {
            GroqApiService.MODEL_LLAMA_3_3_70B
        } else {
            GeminiApiService.MODEL_GEMINI_3_5_FLASH_LITE
        }
    }

    fun setSelectedModel(context: Context, modelId: String) {
        getPrefs(context).edit().putString(KEY_SELECTED_MODEL, modelId).apply()
        Log.d(TAG, "Selected AI Model: $modelId")
    }

    // -------------------------------------------------------------
    // Gemini Keys Management
    // -------------------------------------------------------------

    fun getRawSavedKeys(context: Context): String {
        val prefs = getPrefs(context)
        val keysStr = prefs.getString(KEY_CUSTOM_API_KEYS, "")?.trim() ?: ""
        if (keysStr.isNotBlank()) {
            return keysStr
        }
        return prefs.getString(KEY_CUSTOM_API_KEY_LEGACY, "")?.trim() ?: ""
    }

    fun saveRawKeysInput(context: Context, rawInput: String) {
        val parsedKeys = parseKeysString(rawInput)
        inMemoryGeminiKeys = parsedKeys
        activeGeminiKeyIndex.set(0)

        val joined = parsedKeys.joinToString("\n")
        getPrefs(context).edit().putString(KEY_CUSTOM_API_KEYS, joined).apply()
        Log.d(TAG, "Saved ${parsedKeys.size} Gemini API keys to SharedPreferences")
    }

    fun saveApiKey(context: Context, key: String) {
        saveRawKeysInput(context, key)
    }

    fun setInMemoryKey(key: String) {
        setInMemoryKeys(key)
    }

    fun setInMemoryKeys(keysInput: String) {
        inMemoryGeminiKeys = parseKeysString(keysInput)
        activeGeminiKeyIndex.set(0)
    }

    fun getAllConfiguredKeys(context: Context? = null): List<String> {
        val resultList = mutableListOf<String>()

        inMemoryGeminiKeys?.let { keys ->
            if (keys.isNotEmpty()) resultList.addAll(keys)
        }

        if (resultList.isEmpty() && context != null) {
            val savedRaw = getRawSavedKeys(context)
            val savedList = parseKeysString(savedRaw)
            if (savedList.isNotEmpty()) {
                inMemoryGeminiKeys = savedList
                resultList.addAll(savedList)
            }
        }

        val buildKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Exception) {
            ""
        }?.trim()

        if (!buildKey.isNullOrBlank() && buildKey != "null" && buildKey != "GEMINI_API_KEY_DEFAULT_VALUE" && !buildKey.startsWith("your_api_key")) {
            if (!resultList.contains(buildKey)) {
                resultList.add(buildKey)
            }
        }

        return resultList.distinct()
    }

    fun getEffectiveApiKey(context: Context? = null): String {
        val keys = getAllConfiguredKeys(context)
        if (keys.isEmpty()) return ""

        val now = System.currentTimeMillis()
        for (i in keys.indices) {
            val idx = (activeGeminiKeyIndex.get() + i) % keys.size
            val key = keys[idx]
            val blockedUntil = rateLimitedKeys[key] ?: 0L
            if (blockedUntil <= now) {
                return key
            }
        }

        val fallbackIdx = activeGeminiKeyIndex.get() % keys.size
        return keys[fallbackIdx]
    }

    fun rotateToNextKey(context: Context? = null, currentFailedKey: String? = null): String {
        if (!currentFailedKey.isNullOrBlank()) {
            markKeyAsRateLimited(currentFailedKey)
        }

        val keys = getAllConfiguredKeys(context)
        if (keys.isEmpty()) return ""

        val nextIdx = activeGeminiKeyIndex.incrementAndGet() % keys.size
        val newKey = keys[nextIdx]
        Log.i(TAG, "Rotated Gemini API key to index $nextIdx (prefix: ${newKey.take(4)}...)")
        return newKey
    }

    // -------------------------------------------------------------
    // Groq Keys Management
    // -------------------------------------------------------------

    fun getRawSavedGroqKeys(context: Context): String {
        val prefs = getPrefs(context)
        return prefs.getString(KEY_CUSTOM_GROQ_API_KEYS, "")?.trim() ?: ""
    }

    fun saveRawGroqKeysInput(context: Context, rawInput: String) {
        val parsedKeys = parseKeysString(rawInput)
        inMemoryGroqKeys = parsedKeys
        activeGroqKeyIndex.set(0)

        val joined = parsedKeys.joinToString("\n")
        getPrefs(context).edit().putString(KEY_CUSTOM_GROQ_API_KEYS, joined).apply()
        Log.d(TAG, "Saved ${parsedKeys.size} Groq API keys to SharedPreferences")
    }

    fun setInMemoryGroqKeys(keysInput: String) {
        inMemoryGroqKeys = parseKeysString(keysInput)
        activeGroqKeyIndex.set(0)
    }

    fun getAllConfiguredGroqKeys(context: Context? = null): List<String> {
        val resultList = mutableListOf<String>()

        inMemoryGroqKeys?.let { keys ->
            if (keys.isNotEmpty()) resultList.addAll(keys)
        }

        if (resultList.isEmpty() && context != null) {
            val savedRaw = getRawSavedGroqKeys(context)
            val savedList = parseKeysString(savedRaw)
            if (savedList.isNotEmpty()) {
                inMemoryGroqKeys = savedList
                resultList.addAll(savedList)
            }
        }

        val buildKey = try {
            BuildConfig::class.java.getField("GROQ_API_KEY").get(null) as? String
        } catch (_: Throwable) {
            ""
        }?.trim()

        if (!buildKey.isNullOrBlank() && buildKey != "null" && buildKey != "GROQ_API_KEY_DEFAULT_VALUE" && !buildKey.startsWith("your_")) {
            if (!resultList.contains(buildKey)) {
                resultList.add(buildKey)
            }
        }

        return resultList.distinct()
    }

    fun getEffectiveGroqKey(context: Context? = null): String {
        val keys = getAllConfiguredGroqKeys(context)
        if (keys.isEmpty()) return ""

        val now = System.currentTimeMillis()
        for (i in keys.indices) {
            val idx = (activeGroqKeyIndex.get() + i) % keys.size
            val key = keys[idx]
            val blockedUntil = rateLimitedKeys[key] ?: 0L
            if (blockedUntil <= now) {
                return key
            }
        }

        val fallbackIdx = activeGroqKeyIndex.get() % keys.size
        return keys[fallbackIdx]
    }

    fun rotateToNextGroqKey(context: Context? = null, currentFailedKey: String? = null): String {
        if (!currentFailedKey.isNullOrBlank()) {
            markKeyAsRateLimited(currentFailedKey)
        }

        val keys = getAllConfiguredGroqKeys(context)
        if (keys.isEmpty()) return ""

        val nextIdx = activeGroqKeyIndex.incrementAndGet() % keys.size
        val newKey = keys[nextIdx]
        Log.i(TAG, "Rotated Groq API key to index $nextIdx (prefix: ${newKey.take(4)}...)")
        return newKey
    }

    // -------------------------------------------------------------
    // Smart Key Auto-Detection & Unified Info List
    // -------------------------------------------------------------

    /**
     * Smart save: Automatically routes keys starting with `gsk_` to Groq,
     * and keys starting with `AIza` to Gemini. Otherwise saves to active provider.
     */
    fun smartSaveKeysInput(context: Context, rawInput: String) {
        val allKeys = parseKeysString(rawInput)
        val groqKeys = mutableListOf<String>()
        val geminiKeys = mutableListOf<String>()

        allKeys.forEach { key ->
            when {
                key.startsWith("gsk_", ignoreCase = true) -> groqKeys.add(key)
                key.startsWith("AIza", ignoreCase = true) -> geminiKeys.add(key)
                else -> {
                    val currentProvider = getSelectedProvider(context)
                    if (currentProvider == AiProvider.GROQ) {
                        groqKeys.add(key)
                    } else {
                        geminiKeys.add(key)
                    }
                }
            }
        }

        if (groqKeys.isNotEmpty()) {
            val existing = parseKeysString(getRawSavedGroqKeys(context))
            val combined = (existing + groqKeys).distinct()
            saveRawGroqKeysInput(context, combined.joinToString("\n"))
        }

        if (geminiKeys.isNotEmpty()) {
            val existing = parseKeysString(getRawSavedKeys(context))
            val combined = (existing + geminiKeys).distinct()
            saveRawKeysInput(context, combined.joinToString("\n"))
        }
    }

    fun parseKeysString(input: String): List<String> {
        if (input.isBlank()) return emptyList()
        return input.split("\n", ",", ";", " ")
            .map { it.trim() }
            .filter { key ->
                key.isNotBlank() &&
                        key != "null" &&
                        key != "GEMINI_API_KEY_DEFAULT_VALUE" &&
                        key != "GROQ_API_KEY_DEFAULT_VALUE" &&
                        !key.startsWith("your_")
            }
            .distinct()
    }

    fun getApiKeyInfoList(context: Context? = null, provider: AiProvider = AiProvider.GEMINI): List<ApiKeyInfo> {
        val now = System.currentTimeMillis()
        val keys = if (provider == AiProvider.GROQ) getAllConfiguredGroqKeys(context) else getAllConfiguredKeys(context)

        val buildKey = if (provider == AiProvider.GEMINI) {
            try { BuildConfig.GEMINI_API_KEY } catch (_: Exception) { "" }?.trim()
        } else {
            try { BuildConfig::class.java.getField("GROQ_API_KEY").get(null) as? String } catch (_: Throwable) { "" }?.trim()
        }

        return keys.map { key ->
            val blockedUntil = rateLimitedKeys[key] ?: 0L
            val isBlocked = blockedUntil > now
            val isBuildKey = (key == buildKey)
            ApiKeyInfo(
                key = key,
                isRateLimited = isBlocked,
                rateLimitedUntil = blockedUntil,
                isFromBuildConfig = isBuildKey,
                provider = provider
            )
        }
    }

    fun markKeyAsRateLimited(key: String, cooldownMs: Long = RATE_LIMIT_COOLDOWN_MS) {
        if (key.isBlank()) return
        val until = System.currentTimeMillis() + cooldownMs
        rateLimitedKeys[key] = until
        Log.w(TAG, "Marked key ${key.take(4)}... as rate-limited until $until")
    }

    // -------------------------------------------------------------
    // Execution with Key Rotation
    // -------------------------------------------------------------

    suspend fun <T> executeWithKeyRotation(
        context: Context? = null,
        block: suspend (apiKey: String) -> T
    ): T {
        val keys = getAllConfiguredKeys(context)
        if (keys.isEmpty()) {
            return block("")
        }

        var lastException: Exception? = null
        val attemptsCount = keys.size

        for (attempt in 0 until attemptsCount) {
            val currentKey = getEffectiveApiKey(context)
            if (currentKey.isBlank()) break

            try {
                val result = block(currentKey)
                rateLimitedKeys.remove(currentKey)
                return result
            } catch (e: Exception) {
                lastException = e
                if (isOverloadOrRateLimit(e) && attemptsCount > 1 && attempt < attemptsCount - 1) {
                    Log.w(TAG, "Gemini key ${currentKey.take(4)}... hit error (${e.message}). Rotating to next key...")
                    rotateToNextKey(context, currentKey)
                } else {
                    throw e
                }
            }
        }

        throw lastException ?: IllegalStateException("Tất cả Gemini API keys đều bị lỗi hoặc không khả dụng.")
    }

    suspend fun <T> executeGroqWithKeyRotation(
        context: Context? = null,
        block: suspend (apiKey: String) -> T
    ): T {
        val keys = getAllConfiguredGroqKeys(context)
        if (keys.isEmpty()) {
            return block("")
        }

        var lastException: Exception? = null
        val attemptsCount = keys.size

        for (attempt in 0 until attemptsCount) {
            val currentKey = getEffectiveGroqKey(context)
            if (currentKey.isBlank()) break

            try {
                val result = block(currentKey)
                rateLimitedKeys.remove(currentKey)
                return result
            } catch (e: Exception) {
                lastException = e
                if (isOverloadOrRateLimit(e) && attemptsCount > 1 && attempt < attemptsCount - 1) {
                    Log.w(TAG, "Groq key ${currentKey.take(4)}... hit error (${e.message}). Rotating to next key...")
                    rotateToNextGroqKey(context, currentKey)
                } else {
                    throw e
                }
            }
        }

        throw lastException ?: IllegalStateException("Tất cả Groq API keys đều bị lỗi hoặc không khả dụng.")
    }

    /**
     * Checks if error is caused by rate-limiting (429), server high-load/overload (503/502/504),
     * or authentication issues (401/400).
     */
    fun isOverloadOrRateLimit(e: Exception): Boolean {
        if (e is retrofit2.HttpException) {
            val code = e.code()
            if (code == 429 || code == 503 || code == 502 || code == 504 || code == 403 || code == 401 || code == 400) {
                return true
            }
        }
        val msg = e.message ?: ""
        return msg.contains("429") ||
                msg.contains("503") ||
                msg.contains("overloaded", ignoreCase = true) ||
                msg.contains("high load", ignoreCase = true) ||
                msg.contains("RESOURCE_EXHAUSTED", ignoreCase = true) ||
                msg.contains("Quota exceeded", ignoreCase = true) ||
                msg.contains("Service Unavailable", ignoreCase = true)
    }

    fun isRateLimitOrKeyError(e: Exception): Boolean {
        return isOverloadOrRateLimit(e)
    }
}
