package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.models.Event
import com.example.models.ai.AISuggestedScheduleItem
import com.example.network.GeminiApiService
import com.example.network.GeminiContent
import com.example.network.GeminiGenerationConfig
import com.example.network.GeminiPart
import com.example.network.GeminiRequest
import com.example.network.GroqApiService
import com.example.network.GroqChatRequest
import com.example.network.GroqMessage
import com.example.util.AiConfigHelper
import com.example.util.AiModelInfo
import com.example.util.AiProvider
import com.example.util.AIScheduleContextHelper
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

/**
 * Ordered list of models to try for one chat request: the selected model, then the other model of
 * the same provider, then the other provider's models. A provider without API keys is skipped.
 */
internal fun buildAiAttemptChain(
    selectedProvider: AiProvider,
    selectedModel: String,
    hasGeminiKeys: Boolean,
    hasGroqKeys: Boolean
): List<AiModelInfo> {
    fun usable(p: AiProvider) = if (p == AiProvider.GROQ) hasGroqKeys else hasGeminiKeys
    val other = if (selectedProvider == AiProvider.GROQ) AiProvider.GEMINI else AiProvider.GROQ
    val same = AiConfigHelper.AVAILABLE_MODELS.filter { it.provider == selectedProvider }
        .sortedByDescending { it.id == selectedModel }
    val others = AiConfigHelper.AVAILABLE_MODELS.filter { it.provider == other }
    return same.filter { usable(selectedProvider) } + others.filter { usable(other) }
}

enum class MessageRole {
    USER, MODEL
}

enum class DeletionStatus { PENDING, DONE, CANCELLED }

/** An event the AI proposes to delete; shown to the user for confirmation first. */
data class DeletionCandidate(val eventId: String, val title: String, val whenText: String)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: MessageRole,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val suggestedItems: List<AISuggestedScheduleItem> = emptyList(),
    val pendingDeletion: List<DeletionCandidate> = emptyList(),
    val deletionStatus: DeletionStatus = DeletionStatus.PENDING
)

class AIChatViewModel(
    private val geminiService: GeminiApiService = GeminiApiService.create(),
    private val groqService: GroqApiService = GroqApiService.create()
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                role = MessageRole.MODEL,
                text = "Xin chào! 👋 Tôi là trợ lý AI Lịch Trình. Tôi hỗ trợ nhiều mô hình AI siêu tốc (Groq GPT-OSS 120B, Gemini 2.5 Flash...) giúp bạn lập kế hoạch, phân bổ thời gian biểu và trực tiếp điền vào lịch! Bạn cần tôi hỗ trợ gì hôm nay?"
            )
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _activeProvider = MutableStateFlow(AiProvider.GROQ)
    val activeProvider: StateFlow<AiProvider> = _activeProvider.asStateFlow()

    private val _activeModel = MutableStateFlow(GroqApiService.MODEL_GPT_OSS_120B)
    val activeModel: StateFlow<String> = _activeModel.asStateFlow()

    private val _customApiKey = MutableStateFlow(AiConfigHelper.getEffectiveApiKey())
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    /** Set by the UI: deletes events by id from the real schedule database. */
    var eventDeleter: ((Collection<String>) -> Unit)? = null

    /** User confirmed the AI's proposed deletion card: really delete those events. */
    fun confirmDeletion(messageId: String) {
        val msg = _messages.value.find { it.id == messageId } ?: return
        if (msg.deletionStatus != DeletionStatus.PENDING || msg.pendingDeletion.isEmpty()) return
        eventDeleter?.invoke(msg.pendingDeletion.map { it.eventId })
        setDeletionStatus(messageId, DeletionStatus.DONE)
    }

    fun cancelDeletion(messageId: String) {
        setDeletionStatus(messageId, DeletionStatus.CANCELLED)
    }

    private fun setDeletionStatus(messageId: String, status: DeletionStatus) {
        _messages.value = _messages.value.map {
            if (it.id == messageId) it.copy(deletionStatus = status) else it
        }
    }

    private val deletePattern = Pattern.compile("(?:```)?\\s*delete_json\\s*(\\[[^\\]]*\\])\\s*(?:```)?", Pattern.CASE_INSENSITIVE)

    /** Extracts the AI's proposed deletions (short refs E1, E2...) and maps them to real events. */
    private fun extractDeletions(
        rawText: String,
        refMap: Map<String, String>,
        allEvents: List<Event>
    ): Pair<String, List<DeletionCandidate>> {
        val m = deletePattern.matcher(rawText)
        if (!m.find()) return rawText to emptyList()
        val refs = try {
            val arr = JSONArray(m.group(1))
            (0 until arr.length()).map { arr.optString(it).trim().uppercase() }
        } catch (_: Exception) {
            emptyList()
        }
        val byId = allEvents.associateBy { it.id }
        val timeFmt = DateTimeFormatter.ofPattern("HH:mm")
        val dateFmt = DateTimeFormatter.ofPattern("dd/MM")
        val candidates = refs.distinct().mapNotNull { ref ->
            val ev = refMap[ref]?.let { byId[it] } ?: return@mapNotNull null
            DeletionCandidate(
                eventId = ev.id,
                title = ev.title,
                whenText = "${ev.startTime.format(dateFmt)} ${ev.startTime.format(timeFmt)}-${ev.endTime.format(timeFmt)}"
            )
        }
        return m.replaceAll("").trim() to candidates
    }

    fun initConfig(context: android.content.Context) {
        val provider = AiConfigHelper.getSelectedProvider(context)
        val model = AiConfigHelper.getSelectedModel(context)
        _activeProvider.value = provider
        _activeModel.value = model
    }

    fun setProvider(provider: AiProvider, context: android.content.Context? = null) {
        _activeProvider.value = provider
        if (context != null) {
            AiConfigHelper.setSelectedProvider(context, provider)
            val currentModel = AiConfigHelper.getSelectedModel(context)
            _activeModel.value = currentModel
        } else {
            _activeModel.value = if (provider == AiProvider.GROQ) GroqApiService.MODEL_GPT_OSS_120B else GeminiApiService.MODEL_GEMINI_3_5_FLASH_LITE
        }
    }

    fun setModel(modelId: String, context: android.content.Context? = null) {
        _activeModel.value = modelId
        if (context != null) {
            AiConfigHelper.setSelectedModel(context, modelId)
        }
    }

    fun setCustomApiKey(key: String) {
        val trimmed = key.trim()
        _customApiKey.value = trimmed
        AiConfigHelper.setInMemoryKey(trimmed)
    }

    fun getEffectiveApiKey(): String {
        return if (_activeProvider.value == AiProvider.GROQ) {
            AiConfigHelper.getEffectiveGroqKey()
        } else {
            val current = _customApiKey.value.trim()
            if (current.isNotBlank()) current else AiConfigHelper.getEffectiveApiKey()
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

    fun sendMessage(
        userPrompt: String,
        scheduleSummary: String? = null,
        allEvents: List<Event> = emptyList(),
        context: android.content.Context? = null
    ) {
        val promptText = userPrompt.trim()
        if (promptText.isEmpty() || _isLoading.value) return

        val provider = _activeProvider.value
        val modelToUse = _activeModel.value

        val rawKey = getEffectiveApiKey()
        Log.d("AIChat", "Provider: $provider, Model: $modelToUse, Key prefix: ${rawKey.take(4)}, length: ${rawKey.length}")

        if (rawKey.isBlank()) {
            if (provider == AiProvider.GROQ) {
                _errorMessage.value = "Chưa có Groq API Key. Vui lòng bấm vào biểu tượng 🔑 để nhập Groq API Key (bắt đầu bằng gsk_...) hoặc chuyển sang Gemini."
            } else {
                _errorMessage.value = "Chưa có Gemini API Key. Vui lòng bấm vào biểu tượng 🔑 để nhập Gemini API Key hoặc chuyển sang Groq."
            }
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

                val today = LocalDate.now()
                val todayStr = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                val tomorrowStr = today.plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

                val dbScheduleContext = AIScheduleContextHelper.buildScheduleContext(allEvents, promptText)
                val (actionableText, refMap) = AIScheduleContextHelper.buildActionableList(allEvents)

                val providerLabel = if (provider == AiProvider.GROQ) "Groq ($modelToUse)" else "Gemini ($modelToUse)"
                val systemPrompt = buildString {
                    append("Bạn là Trợ lý AI Lịch Trình thông minh của ứng dụng My Schedule (đang vận hành trên $providerLabel).\n")
                    append("Hôm nay là: $todayStr (${today.dayOfWeek.name}). Ngày mai là: $tomorrowStr.\n")
                    append("Hãy trả lời bằng tiếng Việt lịch sự, thân thiện, rõ ràng, đưa ra lời khuyên tối ưu năng suất và phân bổ thời gian hợp lý.\n\n")

                    append("QUY TẮC CỐT LÕI VỀ TRUY VẤN VÀ CƠ SỞ DỮ LIỆU LỊCH TRÌNH:\n")
                    append("1. BẠN ĐÃ ĐƯỢC TÍCH HỢP TRỰC TIẾP VỚI CƠ SỞ DỮ LIỆU SQLite/Room CỦA NGƯỜI DÙNG.\n")
                    append("2. Toàn bộ các sự kiện, lịch trình (quá khứ, tuần trước, tuần này, tương lai) và bảng số liệu thống kê tổng hợp đã được trích xuất trực tiếp từ cơ sở dữ liệu và cung cấp đầy đủ trong phần [CƠ SỞ DỮ LIỆU LỊCH TRÌNH VÀ SỰ KIỆN CỦA NGƯỜI DÙNG] bên dưới.\n")
                    append("3. TUYỆT ĐỐI KHÔNG BAO GIỜ nói 'Tôi không có quyền truy cập cơ sở dữ liệu', 'Tôi là trợ lý AI không xem được dữ liệu cá nhân', hay yêu cầu người dùng dán/copy danh sách sự kiện vào đây.\n")
                    append("4. Khi người dùng hỏi bất kỳ câu hỏi nào về lịch trình (ví dụ: 'trong tuần và tuần trước mình có tổng cộng bao nhiêu buổi học', 'tuần này có bao nhiêu việc chưa xong', 'hôm qua mình làm gì', 'ngày mai có lịch gì'...):\n")
                    append("   - Hãy trực tiếp tra cứu từ [CƠ SỞ DỮ LIỆU LỊCH TRÌNH VÀ SỰ KIỆN CỦA NGƯỜI DÙNG] bên dưới.\n")
                    append("   - Nêu số lượng chính xác, liệt kê chi tiết từng buổi học / sự kiện (kèm ngày, thứ, khung giờ và trạng thái hoàn thành).\n")
                    append("   - Trả lời đầy đủ, tự tin, thân thiện và chính xác tuyệt đối theo dữ liệu thực tế.\n\n")

                    append("ĐẶC BIỆT QUAN TRỌNG VỀ TÍNH NĂNG TỰ ĐỘNG LÊN LỊCH TRÌNH VÀ BÓC TÁCH CÔNG VIỆC:\n")
                    append("Khi người dùng đưa vào một danh sách các môn học, buổi học, công việc, đồ án hoặc kế hoạch dài hạn (kể cả 20, 30, 50 sự kiện kéo dài cả tháng):\n\n")
                    append("NGUYÊN TẮC BẮT BUỘC:\n")
                    append("1. BẠN BẮT BUỘC PHẢI LÊN LỊCH ĐẦY ĐỦ 100% TẤT CẢ CÁC SỰ KIỆN TỪ ĐẦU ĐẾN CUỐI. TUYỆT ĐỐI KHÔNG ĐƯỢC TỰ Ý DỪNG Ở TUẦN ĐẦU TIÊN! TUYỆT ĐỐI KHÔNG ĐƯỢC BỎ BỚT BẤT KỲ MÔN NÀO!\n")
                    append("2. TUYỆT ĐỐI KHÔNG BAO GIỜ viết những câu bào chữa như '(Lưu ý: Do giới hạn hiển thị, mình chỉ nhập tuần đầu tiên...)' hay yêu cầu người dùng yêu cầu tiếp tuần sau. Hệ thống có khả năng nạp không giới hạn số lượng sự kiện trong một lần, hãy trả về toàn bộ tất cả các tuần (từ ngày đầu đến ngày cuối cùng người dùng cung cấp) trong CÙNG MỘT KHỐI JSON DUY NHẤT.\n")
                    append("3. TUYỆT ĐỐI KHÔNG IN MÃ JSON THÔ HAY CÁC DÒNG CẤU TRÚC JSON (như {\"title\": ...}) RA NỘI DUNG TRÒ CHUYỆN. Người dùng không muốn nhìn thấy văn bản JSON thô. Ứng dụng đã có sẵn giao diện thẻ sự kiện tích chọn riêng rất đẹp.\n")
                    append("   Vì vậy, trong phần văn bản trả lời, bạn CHỈ CẦN đưa ra lời chào và nhận xét/động viên ngắn gọn (1-2 câu). Sau đó, đặt TOÀN BỘ mảng dữ liệu sự kiện vào trong đúng MỘT khối duy nhất dạng:\n")
                    append("```schedule_json\n")
                    append("[\n")
                    append("  {\n")
                    append("    \"title\": \"Tên môn học hoặc công việc ngắn gọn\",\n")
                    append("    \"date\": \"2026-11-02\",\n")
                    append("    \"startTime\": \"09:15\",\n")
                    append("    \"endTime\": \"11:15\",\n")
                    append("    \"category\": \"STUDY\",\n")
                    append("    \"reminderNote\": \"Phòng học, địa điểm hoặc ghi chú nếu có\"\n")
                    append("  }\n")
                    append("]\n")
                    append("```\n")
                    append("\nQUY TẮC XÓA / SỬA / THAY THẾ / DỜI LỊCH (RẤT QUAN TRỌNG):\n")
                    append("- Bạn không trực tiếp sửa cơ sở dữ liệu, nhưng bạn ĐỀ XUẤT các sự kiện cần xóa; ứng dụng sẽ hiện thẻ xác nhận và chỉ xóa khi người dùng bấm xác nhận.\n")
                    append("- Khi người dùng muốn xóa/sửa/thay/dời lịch: hãy HIỂU NGỮ CẢNH toàn bộ cuộc trò chuyện (không dựa vào một từ khóa), rồi xác định CHÍNH XÁC những sự kiện người dùng đang nói tới trong [DANH SÁCH SỰ KIỆN CÓ THỂ THAO TÁC].\n")
                    append("- Đề xuất xóa bằng đúng MỘT khối chứa mảng MÃ sự kiện (E1, E2...) trong danh sách đó:\n")
                    append("```delete_json\n[\"E3\", \"E7\"]\n```\n")
                    append("  Trong văn bản, nói ngắn gọn bạn định xóa những lịch nào và vì sao. TUYỆT ĐỐI KHÔNG nói 'đã xóa' (vì chưa xóa cho tới khi người dùng xác nhận).\n")
                    append("- Chỉ chọn những sự kiện thật sự khớp ý người dùng, KHÔNG xóa dư. Chỉ xóa 'tất cả/hết' khi người dùng nói rõ phạm vi đó.\n")
                    append("- Nếu mơ hồ (có nhiều lịch có thể khớp, hoặc không tìm thấy lịch nào phù hợp): KHÔNG xuất khối delete_json, hãy hỏi lại người dùng để làm rõ.\n")
                    append("- Sửa/thay thế/dời lịch = xóa lịch cũ (delete_json) + thêm lịch mới (schedule_json) trong CÙNG một câu trả lời. Chỉ đưa vào schedule_json những lịch MỚI cần thêm, không lặp lại các lịch đã có.\n")
                    append("- Chỉ dùng mã E# của danh sách trong lượt này; các mã nhắc trong tin nhắn trước đã cũ, không được dùng lại. Không bịa mã.\n")
                    append("4. Khi người dùng cung cấp ngày tháng dạng '02/11', '03/11'... hoặc 'Thứ 2 (02/11)', hãy tự động suy luận đúng năm hiện tại (${today.year}) và định dạng chuẩn 'yyyy-MM-dd' (ví dụ: '${today.year}-11-02').\n")
                    append("5. Trường 'category' chỉ nhận 1 trong 4 giá trị: 'STUDY' (Học tập/Đồ án), 'WORK' (Công việc), 'PLAY' (Giải trí/Thể thao), 'MEETING' (Họp hành).\n\n")

                    append(dbScheduleContext)
                    append("\n")
                    append(actionableText)
                    append("\n")
                    if (!scheduleSummary.isNullOrBlank()) {
                        append("\nThông tin tóm tắt bổ sung: $scheduleSummary\n")
                    }
                }

                var rawReplyText: String = ""
                var failoverNote: String? = null

                // Request bodies are provider-specific but model-independent: build them once.
                val groqMessages = mutableListOf<GroqMessage>()
                groqMessages.add(GroqMessage(role = "system", content = systemPrompt))
                validMessages.forEach { msg ->
                    groqMessages.add(
                        GroqMessage(
                            role = if (msg.role == MessageRole.USER) "user" else "assistant",
                            content = msg.text
                        )
                    )
                }
                if (validMessages.isEmpty()) {
                    groqMessages.add(GroqMessage(role = "user", content = promptText))
                }

                val geminiContents = if (validMessages.isNotEmpty()) {
                    validMessages.map { msg ->
                        GeminiContent(
                            role = if (msg.role == MessageRole.USER) "user" else "model",
                            parts = listOf(GeminiPart(text = msg.text))
                        )
                    }
                } else {
                    listOf(GeminiContent(role = "user", parts = listOf(GeminiPart(text = promptText))))
                }
                val geminiRequest = GeminiRequest(
                    contents = geminiContents,
                    systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemPrompt))),
                    generationConfig = GeminiGenerationConfig(
                        temperature = 0.7f,
                        topP = 0.95f,
                        topK = 40,
                        maxOutputTokens = 8192
                    )
                )

                // Order: selected model -> other model of the same provider -> other provider's models.
                // Each model tries all of its provider's keys (key rotation) before we move on.
                val attempts = buildAiAttemptChain(
                    selectedProvider = provider,
                    selectedModel = modelToUse,
                    hasGeminiKeys = AiConfigHelper.getAllConfiguredKeys(context).isNotEmpty(),
                    hasGroqKeys = AiConfigHelper.getAllConfiguredGroqKeys(context).isNotEmpty()
                )

                var firstError: Exception? = null
                var succeeded = false
                for (attempt in attempts) {
                    try {
                        rawReplyText = withContext(Dispatchers.IO) {
                            if (attempt.provider == AiProvider.GROQ) {
                                val groqRequest = GroqChatRequest(
                                    model = attempt.id,
                                    messages = groqMessages,
                                    temperature = 0.7f,
                                    maxTokens = 8192
                                )
                                val response = AiConfigHelper.executeGroqWithKeyRotation(context) { keyToUse ->
                                    groqService.chatCompletion(
                                        authorization = "Bearer $keyToUse",
                                        request = groqRequest
                                    )
                                }
                                response.choices?.firstOrNull()?.message?.content
                                    ?: "Không nhận được phản hồi từ Groq. Vui lòng thử lại."
                            } else {
                                val response = AiConfigHelper.executeWithKeyRotation(context) { keyToUse ->
                                    geminiService.generateContent(
                                        model = attempt.id,
                                        apiKey = keyToUse,
                                        request = geminiRequest
                                    )
                                }
                                // A reply can be split across several parts (and may include "thought" parts):
                                // join every visible text part instead of keeping only the first one.
                                val candidate = response.candidates?.firstOrNull()
                                Log.d("AIChat", "Gemini finishReason=${candidate?.finishReason}, parts=${candidate?.content?.parts?.size}")
                                candidate?.content?.parts
                                    ?.filter { it.thought != true }
                                    ?.mapNotNull { it.text }
                                    ?.joinToString("")
                                    ?.takeIf { it.isNotBlank() }
                                    ?: "Không nhận được phản hồi từ AI. Vui lòng thử lại."
                            }
                        }
                        if (attempt.provider != provider || attempt.id != modelToUse) {
                            failoverNote = "\n\n*(⚡ Chú thích: ${attempt.displayName} được dùng thay thế vì model bạn chọn đang bị giới hạn hoặc gặp lỗi, hệ thống đã tự động chuyển.)*"
                        }
                        succeeded = true
                        break
                    } catch (e: java.io.IOException) {
                        // No connectivity: every other model would fail the same way.
                        throw e
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.w("AIChat", "${attempt.provider}/${attempt.id} failed: ${e.message}. Trying next model...")
                        // Keep the ORIGINAL error so the user sees the real cause if everything fails.
                        if (firstError == null) firstError = e
                    }
                }
                if (!succeeded) throw firstError ?: IllegalStateException("Không có model AI nào khả dụng.")

                // Parse structured schedule items if present
                val (textWithoutDelete, deletions) = extractDeletions(rawReplyText, refMap, allEvents)
                val (cleanText, suggestedItems) = parseAiResponse(
                    textWithoutDelete, promptText, allowFallback = deletions.isEmpty()
                )
                val finalText = if (failoverNote != null) cleanText + failoverNote else cleanText

                _messages.value = _messages.value.filter { !it.isLoading } + ChatMessage(
                    role = MessageRole.MODEL,
                    text = finalText,
                    suggestedItems = suggestedItems,
                    pendingDeletion = deletions
                )
            } catch (e: Exception) {
                Log.e("AIChat", "Error calling AI API", e)
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

                    if (code == 503 || serverMsg.contains("overloaded", ignoreCase = true) || serverMsg.contains("Service Unavailable", ignoreCase = true)) {
                        errorDesc = "Máy chủ Google Gemini đang gặp tình trạng quá tải (HTTP 503 Overloaded). Khuyên bạn nên bấm vào biểu tượng ⚙️/🔑 ở góc trên để chuyển sang mô hình Groq GPT-OSS 120B (miễn phí, tốc độ LPU cực nhanh, không bao giờ bị nghẽn tải server) hoặc chọn Gemini 3.1 Flash Lite!"
                    } else if (code == 401 || code == 400 || serverMsg.contains("API key", ignoreCase = true) || serverMsg.contains("UNAUTHENTICATED", ignoreCase = true) || serverMsg.contains("API_KEY_INVALID", ignoreCase = true)) {
                        errorDesc = "Lỗi xác thực (401/400): API Key chưa chính xác hoặc đã hết hạn. Vui lòng bấm vào biểu tượng 🔑 ở góc phải trên để nhập API Key hợp lệ (Groq: gsk_... hoặc Gemini: AIzaSy...)."
                    } else if (code == 429) {
                        errorDesc = "Đã chạm giới hạn lượt gọi API (429 Rate Limit). Bạn có thể đổi sang Groq Cloud hoặc thêm key dự phòng trong bảng cấu hình."
                    } else if (serverMsg.isNotBlank()) {
                        errorDesc = "Lỗi máy chủ ($code): $serverMsg"
                    }
                } else if (e.message?.contains("Unable to resolve host", ignoreCase = true) == true) {
                    errorDesc = "Không có kết nối mạng. Vui lòng kiểm tra lại Internet."
                }

                // If query is about database metrics or study sessions, provide accurate direct local answer immediately
                val directLocalAnswer = AIScheduleContextHelper.answerDirectLocally(promptText, allEvents)
                if (directLocalAnswer != null) {
                    _messages.value = _messages.value.filter { !it.isLoading } + ChatMessage(
                        role = MessageRole.MODEL,
                        text = "$directLocalAnswer\n\n*(Thông tin được trích xuất trực tiếp từ cơ sở dữ liệu nội bộ)*"
                    )
                    return@launch
                }

                // If the user pasted an explicit schedule ("Thứ 2 (02/11) - Môn — 09:15–11:15"), read it
                // locally so it still works while the AI is down.
                val pastedItems = parsePastedSchedule(promptText)
                if (pastedItems.isNotEmpty()) {
                    _messages.value = _messages.value.filter { !it.isLoading } + ChatMessage(
                        role = MessageRole.MODEL,
                        text = "AI đang không phản hồi được, nên mình đã đọc trực tiếp danh sách bạn gửi: tìm thấy ${pastedItems.size} buổi. Bạn kiểm tra, tích chọn và nhấn 'AI thêm vào lịch trình' để lưu nhé!\n\n*(Lưu ý: $errorDesc)*",
                        suggestedItems = pastedItems
                    )
                    _errorMessage.value = errorDesc
                    return@launch
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

    private fun parseAiResponse(
        rawText: String,
        userPrompt: String,
        allowFallback: Boolean = true
    ): Pair<String, List<AISuggestedScheduleItem>> {
        val suggestedList = mutableListOf<AISuggestedScheduleItem>()
        var cleanedText = rawText

        // 1. Try to match ```schedule_json [...] ``` or ```json [...] ``` or raw [...]
        val jsonPattern = Pattern.compile("```(?:schedule_json|json)?\\s*(\\[[\\s\\S]*?\\])\\s*```", Pattern.CASE_INSENSITIVE)
        val matcher = jsonPattern.matcher(rawText)

        var jsonArrayStr: String? = null
        if (matcher.find()) {
            jsonArrayStr = matcher.group(1)
            cleanedText = matcher.replaceAll("").trim()
        } else {
            // Check for raw JSON array if markdown backticks were omitted (or the reply was truncated
            // before the closing "]" / closing fence)
            val startIdx = rawText.indexOf('[')
            val lastBracket = rawText.lastIndexOf(']')
            val endIdx = if (lastBracket > startIdx) lastBracket else rawText.lastIndexOf('}')
            if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
                val candidate = rawText.substring(startIdx, endIdx + 1).let {
                    if (it.trimEnd().endsWith("]")) it else "$it]"
                }
                if (candidate.contains("\"title\"") && (candidate.contains("\"startTime\"") || candidate.contains("\"date\""))) {
                    jsonArrayStr = candidate
                    cleanedText = (rawText.substring(0, startIdx) + rawText.substring(minOf(endIdx + 1, rawText.length))).trim()
                }
            }
        }

        if (jsonArrayStr != null) {
            val jsonArr = try {
                JSONArray(jsonArrayStr)
            } catch (e: Exception) {
                // Attempt auto-repair for truncated JSON: find the last completed object `}`
                val lastBrace = jsonArrayStr.lastIndexOf('}')
                if (lastBrace != -1) {
                    try {
                        val repaired = jsonArrayStr.substring(0, lastBrace + 1) + "]"
                        JSONArray(repaired)
                    } catch (_: Exception) {
                        null
                    }
                } else null
            }

            if (jsonArr != null) {
                val defaultDate = if (userPrompt.contains("mai", ignoreCase = true) || userPrompt.contains("ngày mai", ignoreCase = true)) {
                    LocalDate.now().plusDays(1)
                } else {
                    LocalDate.now()
                }

                for (i in 0 until jsonArr.length()) {
                    val obj = jsonArr.optJSONObject(i) ?: continue
                    val title = obj.optString("title", "Công việc ${i + 1}")
                    val dateStr = obj.optString("date", defaultDate.toString()).trim()
                    val parsedDate = try {
                        LocalDate.parse(dateStr)
                    } catch (e: Exception) {
                        try {
                            val currentYear = LocalDate.now().year
                            if (dateStr.contains("/")) {
                                val parts = dateStr.split("/")
                                if (parts.size == 2) {
                                    val day = parts[0].filter { it.isDigit() }.toInt()
                                    val month = parts[1].filter { it.isDigit() }.toInt()
                                    LocalDate.of(currentYear, month, day)
                                } else if (parts.size == 3) {
                                    val day = parts[0].filter { it.isDigit() }.toInt()
                                    val month = parts[1].filter { it.isDigit() }.toInt()
                                    val year = parts[2].filter { it.isDigit() }.toInt().let { if (it < 100) 2000 + it else it }
                                    LocalDate.of(year, month, day)
                                } else defaultDate
                            } else if (dateStr.contains("-")) {
                                val parts = dateStr.split("-")
                                if (parts.size == 3) {
                                    if (parts[0].length == 4) {
                                        LocalDate.of(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
                                    } else {
                                        LocalDate.of(parts[2].toInt(), parts[1].toInt(), parts[0].toInt())
                                    }
                                } else defaultDate
                            } else defaultDate
                        } catch (_: Exception) {
                            defaultDate
                        }
                    }

                    val startTimeStr = obj.optString("startTime", "08:00")
                    val endTimeStr = obj.optString("endTime", "09:00")
                    val parsedStartTime = parseTime(startTimeStr, LocalTime.of((8 + i * 2) % 24, 0))
                    val parsedEndTime = parseTime(endTimeStr, parsedStartTime.plusMinutes(60))

                    val category = obj.optString("category", "STUDY")
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
            }
        }

        // 2. Fallback: If no JSON code block but user requested schedule and text has time patterns
        if (suggestedList.isEmpty() && allowFallback) {
            val isPlanningPrompt = userPrompt.contains("phân bổ", ignoreCase = true) ||
                    userPrompt.contains("lịch", ignoreCase = true) ||
                    userPrompt.contains("kế hoạch", ignoreCase = true) ||
                    userPrompt.contains("thời gian biểu", ignoreCase = true) ||
                    userPrompt.contains("thể dục", ignoreCase = true) ||
                    userPrompt.contains("thêm", ignoreCase = true) ||
                    userPrompt.contains("việc", ignoreCase = true) ||
                    userPrompt.contains("task", ignoreCase = true) ||
                    userPrompt.contains("danh sách", ignoreCase = true) ||
                    userPrompt.contains("list", ignoreCase = true)

            if (isPlanningPrompt) {
                val extracted = extractTimesFromText(rawText, userPrompt)
                if (extracted.isNotEmpty()) {
                    suggestedList.addAll(extracted)
                } else {
                    suggestedList.addAll(generateFallbackSchedule(userPrompt))
                }
            }
        }

        // Clean up any remaining JSON tags, backticks or boilerplate disclaimers from the text
        cleanedText = cleanedText
            .replace("schedule_json", "", ignoreCase = true)
            .replace("```", "")
            .replace(Regex("(?i)\\(?\\s*Lưu ý:?\\s*Do giới hạn hiển thị[\\s\\S]*?\\)?"), "")
            .replace(Regex("(?i)Dưới đây là danh sách các sự kiện đã được cập nhật[\\s\\S]*?:?"), "")
            .replace(Regex("\\[\\s*\\{[\\s\\S]*?\\}\\s*\\]"), "")
            .trim()

        val finalText = if (suggestedList.isNotEmpty()) {
            if (cleanedText.isBlank() || cleanedText.length < 15) {
                "Tôi đã lên lịch trình chi tiết cho tất cả ${suggestedList.size} sự kiện/buổi học của bạn bên dưới. Bạn có thể kiểm tra từng sự kiện, tích chọn và nhấn nút 'AI thêm vào lịch trình' để lưu ngay vào ứng dụng nhé!"
            } else {
                cleanedText
            }
        } else {
            cleanedText.ifBlank { "Dưới đây là thông tin lịch trình cho bạn:" }
        }

        return Pair(finalText, suggestedList)
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

        // Match patterns like: "08:00 - 09:30: Học lập trình" or "- 14:00 - 16:00 Tập thể dục" or "1. Học Toán 8h-10h"
        val linePattern = Pattern.compile("(\\d{1,2}(?::\\d{2}|h\\d{0,2}))\\s*(?:-|–|đến|to)\\s*(\\d{1,2}(?::\\d{2}|h\\d{0,2}))[:\\s-]*(.+)", Pattern.CASE_INSENSITIVE)
        val lines = text.split("\n")

        for (line in lines) {
            val matcher = linePattern.matcher(line.trim())
            if (matcher.find()) {
                val startStr = matcher.group(1)?.replace("h", ":") ?: "08:00"
                val endStr = matcher.group(2)?.replace("h", ":") ?: "09:00"
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
        return list.take(100)
    }

    /**
     * Parses a schedule the user typed/pasted themselves, e.g.
     *   Thứ 2 (02/11)
     *   - Hệ Phân Tán (J2EE, .NET), P. 506, 78A Phan Văn Trị — 09:15–11:15.
     * Day headers set the date; item lines give "title[, location] — start–end".
     */
    internal fun parsePastedSchedule(text: String, today: LocalDate = LocalDate.now()): List<AISuggestedScheduleItem> {
        val headerRegex = Regex("""^\s*(?:thứ\s*\d|chủ\s*nhật|cn)\b.*?\((\d{1,2})/(\d{1,2})(?:/(\d{2,4}))?\)""", RegexOption.IGNORE_CASE)
        val itemRegex = Regex("""^\s*[-•*]\s*(.+?)\s+[—–-]\s+(\d{1,2}[:h]\d{2})\s*[—–-]\s*(\d{1,2}[:h]\d{2})""")

        val items = mutableListOf<AISuggestedScheduleItem>()
        var currentDate: LocalDate? = null

        for (line in text.lines()) {
            headerRegex.find(line)?.let { m ->
                val day = m.groupValues[1].toInt()
                val month = m.groupValues[2].toInt()
                val year = m.groupValues[3].takeIf { it.isNotEmpty() }?.toInt()?.let { if (it < 100) 2000 + it else it } ?: today.year
                currentDate = try { LocalDate.of(year, month, day) } catch (_: Exception) { null }
                return@let
            } ?: run {
                val date = currentDate ?: return@run
                val m = itemRegex.find(line) ?: return@run
                val (rawTitle, startStr, endStr) = m.destructured

                // Split "title, location" at the first comma that is not inside parentheses.
                var depth = 0
                var splitAt = -1
                for ((i, c) in rawTitle.withIndex()) {
                    when (c) {
                        '(' -> depth++
                        ')' -> if (depth > 0) depth--
                        ',' -> if (depth == 0 && splitAt < 0) splitAt = i
                    }
                }
                val title = (if (splitAt >= 0) rawTitle.substring(0, splitAt) else rawTitle).trim()
                val location = if (splitAt >= 0) rawTitle.substring(splitAt + 1).trim() else ""

                val start = parseTime(startStr.replace('h', ':'), LocalTime.of(8, 0))
                val end = parseTime(endStr.replace('h', ':'), start.plusMinutes(60))
                val category = when {
                    title.contains("họp", ignoreCase = true) -> "MEETING"
                    title.contains("gym", ignoreCase = true) || title.contains("thể dục", ignoreCase = true) -> "PLAY"
                    else -> "STUDY"
                }

                items.add(
                    AISuggestedScheduleItem(
                        title = title,
                        date = date,
                        startTime = start,
                        endTime = end,
                        categoryType = category,
                        reminderNote = location.ifBlank { "Nhập từ danh sách bạn gửi" },
                        isSelected = true,
                        isAdded = false
                    )
                )
            }
        }
        return items
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
