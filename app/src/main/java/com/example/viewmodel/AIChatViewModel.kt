package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.models.ai.AISuggestedScheduleItem
import com.example.network.GeminiApiService
import com.example.network.GeminiContent
import com.example.network.GeminiGenerationConfig
import com.example.network.GeminiPart
import com.example.network.GeminiRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.HttpException
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.regex.Pattern

enum class MessageRole {
    USER, MODEL
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: MessageRole,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val suggestedItems: List<AISuggestedScheduleItem> = emptyList()
)

class AIChatViewModel(
    private val geminiService: GeminiApiService = GeminiApiService.create()
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                role = MessageRole.MODEL,
                text = "Xin chào! 👋 Tôi là trợ lý AI (Gemini 3.5 Flash Lite) của bạn. Tôi có thể giúp bạn lập kế hoạch ngày mới, gợi ý phân bổ thời gian biểu và trực tiếp điền lịch trình vào ứng dụng giúp bạn! Bạn cần tôi lên lịch gì hôm nay?"
            )
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _customApiKey = MutableStateFlow("AQ.Ab8RN6J_CyRqZqtpseYxDt1oK_XLVegkWbbxLYrEMSB6aR3DoQ")
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    fun setCustomApiKey(key: String) {
        _customApiKey.value = key.trim()
    }

    fun getEffectiveApiKey(): String {
        val customKey = _customApiKey.value.trim()
        if (customKey.isNotBlank()) {
            return customKey
        }
        val buildKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }
        return if (!buildKey.isNullOrBlank() && buildKey != "null" && buildKey != "GEMINI_API_KEY_DEFAULT_VALUE") {
            buildKey.trim()
        } else {
            ""
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearHistory() {
        _messages.value = listOf(
            ChatMessage(
                role = MessageRole.MODEL,
                text = "Đã làm mới đoạn hội thoại. Tôi sẵn sàng hỗ trợ bạn lập lịch trình và giải đáp mọi công việc!"
            )
        )
        _errorMessage.value = null
    }

    fun toggleItemSelection(messageId: String, itemId: String) {
        _messages.value = _messages.value.map { msg ->
            if (msg.id == messageId) {
                val updatedItems = msg.suggestedItems.map { item ->
                    if (item.id == itemId && !item.isAdded) {
                        item.copy(isSelected = !item.isSelected)
                    } else {
                        item
                    }
                }
                msg.copy(suggestedItems = updatedItems)
            } else {
                msg
            }
        }
    }

    fun toggleSelectAll(messageId: String, selectAll: Boolean) {
        _messages.value = _messages.value.map { msg ->
            if (msg.id == messageId) {
                val updatedItems = msg.suggestedItems.map { item ->
                    if (!item.isAdded) {
                        item.copy(isSelected = selectAll)
                    } else {
                        item
                    }
                }
                msg.copy(suggestedItems = updatedItems)
            } else {
                msg
            }
        }
    }

    fun markItemsAsAdded(messageId: String, itemIds: Set<String>) {
        _messages.value = _messages.value.map { msg ->
            if (msg.id == messageId) {
                val updatedItems = msg.suggestedItems.map { item ->
                    if (itemIds.contains(item.id)) {
                        item.copy(isAdded = true, isSelected = false)
                    } else {
                        item
                    }
                }
                msg.copy(suggestedItems = updatedItems)
            } else {
                msg
            }
        }
    }

    fun sendMessage(userPrompt: String, scheduleSummary: String? = null) {
        val promptText = userPrompt.trim()
        if (promptText.isEmpty() || _isLoading.value) return

        val rawKey = getEffectiveApiKey()
        Log.d("GeminiApi", "Effective API Key prefix: ${rawKey.take(4)}, length: ${rawKey.length}")

        if (rawKey.isBlank()) {
            _errorMessage.value = "Chưa có Gemini API Key. Vui lòng nhập API Key vào biểu tượng 🔑."
            return
        }

        val userMessage = ChatMessage(
            role = MessageRole.USER,
            text = promptText
        )

        val loadingMessage = ChatMessage(
            role = MessageRole.MODEL,
            text = "",
            isLoading = true
        )

        _messages.value = _messages.value + userMessage + loadingMessage
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                // Build conversation history (contents MUST start with user turn)
                val rawHistory = _messages.value
                    .filter { !it.isLoading && !it.isError && it.text.isNotBlank() }
                    .takeLast(10)

                val validMessages = rawHistory.dropWhile { it.role == MessageRole.MODEL }

                val contents = if (validMessages.isNotEmpty()) {
                    validMessages.map { msg ->
                        GeminiContent(
                            role = if (msg.role == MessageRole.USER) "user" else "model",
                            parts = listOf(GeminiPart(text = msg.text))
                        )
                    }
                } else {
                    listOf(
                        GeminiContent(
                            role = "user",
                            parts = listOf(GeminiPart(text = promptText))
                        )
                    )
                }

                val today = LocalDate.now()
                val todayStr = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                val tomorrowStr = today.plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

                val systemPrompt = buildString {
                    append("Bạn là Trợ lý AI Lịch Trình thông minh của ứng dụng My Schedule (sử dụng Gemini 3.5 Flash Lite).\n")
                    append("Hôm nay là: $todayStr (${today.dayOfWeek.name}). Ngày mai là: $tomorrowStr.\n")
                    append("Hãy trả lời bằng tiếng Việt lịch sự, thân thiện, rõ ràng, đưa ra lời khuyên tối ưu năng suất và phân bổ thời gian hợp lý.\n\n")
                    append("ĐẶC BIỆT QUAN TRỌNG VỀ TÍNH NĂNG TỰ ĐỘNG ĐIỀN LỊCH TRÌNH:\n")
                    append("Khi người dùng yêu cầu:\n")
                    append("- Gợi ý phân bổ thời gian, sắp xếp thời gian biểu\n")
                    append("- Lập kế hoạch hoặc lịch trình cho ngày hôm nay, ngày mai hoặc các ngày tới\n")
                    append("- Chia nhỏ công việc, bài học, tập thể dục kèm khung giờ cụ thể\n")
                    append("Hãy đưa ra phần nhận xét ngắn gọn và LUÔN LUÔN kèm theo một khối JSON danh sách sự kiện ở cuối câu trả lời theo đúng định dạng sau:\n")
                    append("```schedule_json\n")
                    append("[\n")
                    append("  {\n")
                    append("    \"title\": \"Tên công việc hoặc hoạt động ngắn gọn\",\n")
                    append("    \"date\": \"$todayStr\",\n")
                    append("    \"startTime\": \"08:00\",\n")
                    append("    \"endTime\": \"09:30\",\n")
                    append("    \"category\": \"WORK\",\n")
                    append("    \"reminderNote\": \"Ghi chú nhắc nhở nếu có\"\n")
                    append("  }\n")
                    append("]\n")
                    append("```\n")
                    append("Trong đó: 'category' chỉ nhận 1 trong 4 giá trị: 'WORK' (Công việc), 'STUDY' (Học tập), 'PLAY' (Giải trí/Thể thao/Đi chơi), 'MEETING' (Cuộc họp).\n")
                    append("'date' định dạng 'yyyy-MM-dd' (nếu là hôm nay dùng '$todayStr', nếu ngày mai dùng '$tomorrowStr').\n")
                    if (!scheduleSummary.isNullOrBlank()) {
                        append("\nThông tin lịch trình hiện tại của người dùng: $scheduleSummary\n")
                    }
                }

                val request = GeminiRequest(
                    contents = contents,
                    systemInstruction = GeminiContent(
                        parts = listOf(GeminiPart(text = systemPrompt))
                    ),
                    generationConfig = GeminiGenerationConfig(
                        temperature = 0.7f,
                        topP = 0.95f,
                        topK = 40
                    )
                )

                val response = withContext(Dispatchers.IO) {
                    geminiService.generateContent(
                        model = GeminiApiService.MODEL_GEMINI_3_5_FLASH_LITE,
                        apiKey = rawKey,
                        headerKey = rawKey,
                        request = request
                    )
                }

                val rawReplyText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: "Không nhận được phản hồi từ AI. Vui lòng thử lại."

                // Parse structured schedule items if present
                val (cleanText, suggestedItems) = parseAiResponse(rawReplyText, promptText)

                _messages.value = _messages.value.filter { !it.isLoading } + ChatMessage(
                    role = MessageRole.MODEL,
                    text = cleanText,
                    suggestedItems = suggestedItems
                )
            } catch (e: Exception) {
                Log.e("GeminiApi", "Error calling Gemini API", e)
                var errorDesc = "Lỗi kết nối AI: ${e.localizedMessage ?: "Vui lòng thử lại"}"

                if (e is HttpException) {
                    val code = e.code()
                    val errorBody = try {
                        e.response()?.errorBody()?.string()
                    } catch (ex: Exception) {
                        null
                    }

                    var serverMsg = ""
                    if (!errorBody.isNullOrBlank()) {
                        try {
                            val json = JSONObject(errorBody)
                            if (json.has("error")) {
                                val errObj = json.getJSONObject("error")
                                serverMsg = errObj.optString("message", "")
                            }
                        } catch (_: Exception) {
                            serverMsg = errorBody
                        }
                    }

                    if (code == 401 || code == 400 || serverMsg.contains("API key", ignoreCase = true) || serverMsg.contains("UNAUTHENTICATED", ignoreCase = true)) {
                        errorDesc = "Lỗi xác thực ($code): ${if (serverMsg.isNotBlank()) serverMsg else "API Key không được máy chủ chấp nhận. Vui lòng kiểm tra lại mã API."}"
                    } else if (code == 429) {
                        errorDesc = "Đã vượt quá giới hạn lượt gọi API (429 Rate Limit). Vui lòng thử lại sau vài giây."
                    } else if (serverMsg.isNotBlank()) {
                        errorDesc = "Lỗi máy chủ ($code): $serverMsg"
                    }
                } else if (e.message?.contains("Unable to resolve host", ignoreCase = true) == true) {
                    errorDesc = "Không có kết nối mạng. Vui lòng kiểm tra lại Internet."
                }

                // If error occurs on a schedule planning prompt, provide smart local suggestions so user can still test adding
                val fallbackItems = generateFallbackSchedule(promptText)
                val isPlanningPrompt = promptText.contains("phân bổ", ignoreCase = true) ||
                        promptText.contains("lịch", ignoreCase = true) ||
                        promptText.contains("kế hoạch", ignoreCase = true) ||
                        promptText.contains("thời gian", ignoreCase = true)

                if (isPlanningPrompt && fallbackItems.isNotEmpty()) {
                    val helpfulText = "Tôi đã tạo sẵn khung lịch trình gợi ý phân bổ thời gian tối ưu cho bạn bên dưới. Bạn có thể tích chọn và nhấn 'AI thêm vào lịch trình' để lưu ngay!"
                    _messages.value = _messages.value.filter { !it.isLoading } + ChatMessage(
                        role = MessageRole.MODEL,
                        text = "$helpfulText\n\n*(Lưu ý: $errorDesc)*",
                        suggestedItems = fallbackItems
                    )
                } else {
                    _messages.value = _messages.value.filter { !it.isLoading } + ChatMessage(
                        role = MessageRole.MODEL,
                        text = "⚠️ $errorDesc",
                        isError = true
                    )
                }
                _errorMessage.value = errorDesc
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun parseAiResponse(rawText: String, userPrompt: String): Pair<String, List<AISuggestedScheduleItem>> {
        val suggestedList = mutableListOf<AISuggestedScheduleItem>()
        var cleanedText = rawText

        // 1. Try to match ```schedule_json [...] ``` or ```json [...] ```
        val jsonPattern = Pattern.compile("```(?:schedule_json|json)?\\s*(\\[[\\s\\S]*?\\])\\s*```", Pattern.CASE_INSENSITIVE)
        val matcher = jsonPattern.matcher(rawText)

        if (matcher.find()) {
            val jsonArrayStr = matcher.group(1)
            try {
                val jsonArr = JSONArray(jsonArrayStr)
                val defaultDate = if (userPrompt.contains("mai", ignoreCase = true) || userPrompt.contains("ngày mai", ignoreCase = true)) {
                    LocalDate.now().plusDays(1)
                } else {
                    LocalDate.now()
                }

                for (i in 0 until jsonArr.length()) {
                    val obj = jsonArr.getJSONObject(i)
                    val title = obj.optString("title", "Công việc ${i + 1}")
                    val dateStr = obj.optString("date", defaultDate.toString())
                    val parsedDate = try {
                        LocalDate.parse(dateStr)
                    } catch (e: Exception) {
                        defaultDate
                    }

                    val startTimeStr = obj.optString("startTime", "08:00")
                    val endTimeStr = obj.optString("endTime", "09:00")
                    val parsedStartTime = parseTime(startTimeStr, LocalTime.of(8 + i * 2, 0))
                    val parsedEndTime = parseTime(endTimeStr, parsedStartTime.plusMinutes(60))

                    val category = obj.optString("category", "WORK")
                    val reminderNote = obj.optString("reminderNote", "Gợi ý bởi My Schedule AI")

                    suggestedList.add(
                        AISuggestedScheduleItem(
                            title = title,
                            date = parsedDate,
                            startTime = parsedStartTime,
                            endTime = parsedEndTime,
                            categoryType = category,
                            reminderNote = reminderNote,
                            isSelected = true,
                            isAdded = false
                        )
                    )
                }

                // Remove the json block from display text so the user only sees clean explanation
                cleanedText = matcher.replaceAll("").trim()
            } catch (e: Exception) {
                Log.e("AIChatViewModel", "Error parsing schedule JSON", e)
            }
        }

        // 2. Fallback: If no JSON code block but user requested schedule and text has time patterns
        if (suggestedList.isEmpty()) {
            val isPlanningPrompt = userPrompt.contains("phân bổ", ignoreCase = true) ||
                    userPrompt.contains("lịch", ignoreCase = true) ||
                    userPrompt.contains("kế hoạch", ignoreCase = true) ||
                    userPrompt.contains("thời gian biểu", ignoreCase = true) ||
                    userPrompt.contains("thể dục", ignoreCase = true)

            if (isPlanningPrompt) {
                val extracted = extractTimesFromText(rawText, userPrompt)
                if (extracted.isNotEmpty()) {
                    suggestedList.addAll(extracted)
                } else {
                    suggestedList.addAll(generateFallbackSchedule(userPrompt))
                }
            }
        }

        return Pair(cleanedText.ifBlank { "Dưới đây là lịch trình phân bổ thời gian được đề xuất cho bạn:" }, suggestedList)
    }

    private fun parseTime(timeStr: String, defaultTime: LocalTime): LocalTime {
        return try {
            val parts = timeStr.trim().split(":")
            if (parts.size >= 2) {
                val hour = parts[0].filter { it.isDigit() }.toInt()
                val minute = parts[1].filter { it.isDigit() }.toInt()
                LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
            } else {
                defaultTime
            }
        } catch (e: Exception) {
            defaultTime
        }
    }

    private fun extractTimesFromText(text: String, userPrompt: String): List<AISuggestedScheduleItem> {
        val list = mutableListOf<AISuggestedScheduleItem>()
        val defaultDate = if (userPrompt.contains("mai", ignoreCase = true)) LocalDate.now().plusDays(1) else LocalDate.now()

        // Match patterns like: "08:00 - 09:30: Học lập trình" or "- 14:00 - 16:00 Tập thể dục"
        val linePattern = Pattern.compile("(\\d{1,2}:\\d{2})\\s*(?:-|–|đến|to)\\s*(\\d{1,2}:\\d{2})[:\\s-]*(.+)", Pattern.CASE_INSENSITIVE)
        val lines = text.split("\n")

        for (line in lines) {
            val matcher = linePattern.matcher(line.trim())
            if (matcher.find()) {
                val startStr = matcher.group(1) ?: "08:00"
                val endStr = matcher.group(2) ?: "09:00"
                var titleStr = matcher.group(3)?.trim() ?: "Hoạt động"
                titleStr = titleStr.removePrefix("•").removePrefix("-").removePrefix("*").trim()

                if (titleStr.isNotBlank()) {
                    val category = when {
                        titleStr.contains("học", ignoreCase = true) || titleStr.contains("đọc", ignoreCase = true) -> "STUDY"
                        titleStr.contains("tập", ignoreCase = true) || titleStr.contains("gym", ignoreCase = true) || titleStr.contains("chạy", ignoreCase = true) || titleStr.contains("chơi", ignoreCase = true) -> "PLAY"
                        titleStr.contains("họp", ignoreCase = true) -> "MEETING"
                        else -> "WORK"
                    }

                    list.add(
                        AISuggestedScheduleItem(
                            title = titleStr.take(50),
                            date = defaultDate,
                            startTime = parseTime(startStr, LocalTime.of(8, 0)),
                            endTime = parseTime(endStr, LocalTime.of(9, 30)),
                            categoryType = category,
                            reminderNote = "Đề xuất bởi AI",
                            isSelected = true,
                            isAdded = false
                        )
                    )
                }
            }
        }
        return list.take(8)
    }

    private fun generateFallbackSchedule(userPrompt: String): List<AISuggestedScheduleItem> {
        val targetDate = if (userPrompt.contains("mai", ignoreCase = true)) LocalDate.now().plusDays(1) else LocalDate.now()
        val q = userPrompt.lowercase()

        return when {
            q.contains("thể dục") || q.contains("gym") || q.contains("chạy") -> {
                listOf(
                    AISuggestedScheduleItem(
                        title = "Khởi động & Chạy bộ nhẹ nhàng",
                        date = targetDate,
                        startTime = LocalTime.of(6, 0),
                        endTime = LocalTime.of(6, 45),
                        categoryType = "PLAY",
                        reminderNote = "Mang theo nước uống và khởi động kỹ"
                    ),
                    AISuggestedScheduleItem(
                        title = "Tập thể hình / Gym buổi chiều",
                        date = targetDate,
                        startTime = LocalTime.of(17, 30),
                        endTime = LocalTime.of(18, 30),
                        categoryType = "PLAY",
                        reminderNote = "Duy trì 3-4 buổi mỗi tuần"
                    )
                )
            }
            q.contains("học") || q.contains("study") -> {
                listOf(
                    AISuggestedScheduleItem(
                        title = "Học lý thuyết & Ôn bài trọng tâm",
                        date = targetDate,
                        startTime = LocalTime.of(8, 0),
                        endTime = LocalTime.of(9, 30),
                        categoryType = "STUDY",
                        reminderNote = "Tập trung sâu, không dùng điện thoại"
                    ),
                    AISuggestedScheduleItem(
                        title = "Làm bài tập & Dự án thực hành",
                        date = targetDate,
                        startTime = LocalTime.of(14, 0),
                        endTime = LocalTime.of(16, 0),
                        categoryType = "STUDY",
                        reminderNote = "Áp dụng phương pháp Pomodoro 25p"
                    ),
                    AISuggestedScheduleItem(
                        title = "Đọc sách & Mở rộng kiến thức",
                        date = targetDate,
                        startTime = LocalTime.of(20, 0),
                        endTime = LocalTime.of(21, 0),
                        categoryType = "STUDY",
                        reminderNote = "Đọc 20-30 trang sách hữu ích"
                    )
                )
            }
            else -> {
                listOf(
                    AISuggestedScheduleItem(
                        title = "Xử lý 3 công việc quan trọng nhất (Deep Work)",
                        date = targetDate,
                        startTime = LocalTime.of(8, 30),
                        endTime = LocalTime.of(11, 30),
                        categoryType = "WORK",
                        reminderNote = "Khung giờ tập trung cao độ nhất trong ngày"
                    ),
                    AISuggestedScheduleItem(
                        title = "Họp nhóm & Trao đổi tiến độ",
                        date = targetDate,
                        startTime = LocalTime.of(13, 30),
                        endTime = LocalTime.of(14, 30),
                        categoryType = "MEETING",
                        reminderNote = "Ghi chép tóm tắt các hành động tiếp theo"
                    ),
                    AISuggestedScheduleItem(
                        title = "Học tập phát triển kỹ năng mới",
                        date = targetDate,
                        startTime = LocalTime.of(15, 0),
                        endTime = LocalTime.of(16, 30),
                        categoryType = "STUDY",
                        reminderNote = "Hoàn thành 1 bài học hoặc bài thực hành"
                    ),
                    AISuggestedScheduleItem(
                        title = "Tập thể dục & Thư giãn tái tạo năng lượng",
                        date = targetDate,
                        startTime = LocalTime.of(17, 30),
                        endTime = LocalTime.of(18, 30),
                        categoryType = "PLAY",
                        reminderNote = "Đi dạo, chạy bộ hoặc tập gym"
                    )
                )
            }
        }
    }
}

class AIChatViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AIChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AIChatViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
