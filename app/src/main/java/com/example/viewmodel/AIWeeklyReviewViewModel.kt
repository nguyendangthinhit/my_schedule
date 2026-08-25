package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.DashboardRepository
import com.example.models.Event
import com.example.models.EventCategory
import com.example.models.dashboard.AIHabitPatterns
import com.example.models.dashboard.AISuggestedGoal
import com.example.models.dashboard.AIWeeklyReviewData
import com.example.models.dashboard.AIInteractionMessage
import com.example.models.dashboard.PeriodType
import com.example.network.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

sealed interface AIWeeklyReviewUiState {
    object Loading : AIWeeklyReviewUiState
    data class Success(val data: AIWeeklyReviewData) : AIWeeklyReviewUiState
    data class Error(val message: String, val fallbackData: AIWeeklyReviewData?) : AIWeeklyReviewUiState
}

class AIWeeklyReviewViewModel(
    private val repository: DashboardRepository,
    private val geminiService: GeminiApiService = GeminiApiService.create()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AIWeeklyReviewUiState>(AIWeeklyReviewUiState.Loading)
    val uiState: StateFlow<AIWeeklyReviewUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _interactionMessages = MutableStateFlow<List<AIInteractionMessage>>(emptyList())
    val interactionMessages: StateFlow<List<AIInteractionMessage>> = _interactionMessages.asStateFlow()

    private val _isAskingAI = MutableStateFlow(false)
    val isAskingAI: StateFlow<Boolean> = _isAskingAI.asStateFlow()

    private val _addedGoalIds = MutableStateFlow<Set<String>>(emptySet())
    val addedGoalIds: StateFlow<Set<String>> = _addedGoalIds.asStateFlow()

    private var currentReviewData: AIWeeklyReviewData? = null

    init {
        loadWeeklyReview()
    }

    fun loadWeeklyReview(forceAiRefresh: Boolean = false) {
        viewModelScope.launch {
            if (_uiState.value !is AIWeeklyReviewUiState.Success) {
                _uiState.value = AIWeeklyReviewUiState.Loading
            } else {
                _isRefreshing.value = true
            }

            try {
                // 1. Tính toán dữ liệu nền tảng từ Room DB
                val boards = try { repository.allBoards.first() } catch (e: Exception) { emptyList() }
                val events = try { repository.allEvents.first() } catch (e: Exception) { emptyList() }
                val completions = try { repository.allCompletions.first() } catch (e: Exception) { emptyList() }
                val holidays = try { repository.allHolidays.first() } catch (e: Exception) { emptyList() }

                val (overviewData, aiExportData) = repository.calculateOverview(
                    boards = boards,
                    events = events,
                    completions = completions,
                    holidays = holidays,
                    period = PeriodType.WEEK,
                    anchorDate = LocalDate.now()
                )

                // 2. Tạo fallback chuẩn dựa trên thuật toán phân tích thực tế
                val fallbackData = generateRuleBasedReview(overviewData, aiExportData)

                // 3. Gọi Gemini API để sinh phân tích thông minh, văn phong tự nhiên
                val aiReview = generateAiReviewWithGemini(overviewData, fallbackData)
                val finalData = aiReview ?: fallbackData

                currentReviewData = finalData
                _uiState.value = AIWeeklyReviewUiState.Success(finalData)
            } catch (e: Exception) {
                Log.e("AIWeeklyReviewVM", "Error loading weekly review", e)
                val fallback = currentReviewData ?: generateDefaultReview()
                _uiState.value = AIWeeklyReviewUiState.Success(fallback)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private suspend fun generateAiReviewWithGemini(
        overviewData: com.example.models.dashboard.DashboardOverviewData,
        fallbackData: AIWeeklyReviewData
    ): AIWeeklyReviewData? = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            return@withContext null
        }

        try {
            val systemInstruction = """
                Bạn là 'My_schedule AI', một trợ lý cố vấn lịch trình thông minh, mang đến một góc nhìn khách quan và lời khuyên giúp người dùng tối ưu hóa thời gian và năng suất.
                Hãy phân tích dữ liệu tuần này của người dùng và trả về DUY NHẤT một JSON theo đúng cấu trúc sau (không kèm markdown format ngoài json):
                {
                   "summary": "Đoạn văn tóm tắt 2-3 câu về tỉ lệ hoàn thành, so sánh với tuần trước, đánh giá các mảng tốt và cần cải thiện.",
                   "strengths": ["Điểm mạnh 1", "Điểm mạnh 2", "Điểm mạnh 3"],
                   "improvements": ["Cần cải thiện 1", "Cần cải thiện 2", "Cần cải thiện 3"],
                   "habits": {
                      "mostProductiveDay": "Thứ 2",
                      "leastProductiveDay": "Thứ 7",
                      "mostProductiveTime": "Buổi sáng",
                      "leastProductiveTime": "Buổi tối"
                   },
                   "recommendations": ["Gợi ý 1", "Gợi ý 2", "Gợi ý 3", "Gợi ý 4"],
                   "suggestedGoals": [
                      {"id": "goal_1", "title": "Hoàn thành 85% kế hoạch", "timeFrame": "Tuần tới", "iconType": "TARGET"},
                      {"id": "goal_2", "title": "Tập luyện 3 buổi", "timeFrame": "Tuần tới", "iconType": "FITNESS"}
                   ]
                }
                Dùng tiếng Việt tự nhiên, thân thiện, mang tính động viên và hành động cụ thể.
            """.trimIndent()

            val statsSummary = """
                Dữ liệu tuần này:
                - Tổng số công việc: ${overviewData.totalTasks}
                - Đã hoàn thành: ${overviewData.completedTasks} (${overviewData.completionRate}%)
                - Đang làm / dở dang: ${overviewData.inProgressTasks}
                - Chưa hoàn thành: ${overviewData.incompleteTasks}
                - So với tuần trước: ${if (overviewData.isTrendPositive) "Tăng" else "Giảm"} ${overviewData.trendPercent}%
                - Các chủ đề: ${overviewData.categoryProgressList.joinToString(", ") { "${it.category.name}: ${it.rate}% (${it.completedCount}/${it.totalCount})" }}
            """.trimIndent()

            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        role = "user",
                        parts = listOf(GeminiPart(text = "Hãy phân tích tuần này của tôi dựa trên số liệu sau:\n$statsSummary"))
                    )
                ),
                systemInstruction = GeminiContent(
                    role = "user",
                    parts = listOf(GeminiPart(text = systemInstruction))
                ),
                generationConfig = GeminiGenerationConfig(temperature = 0.5f)
            )

            val response = geminiService.generateContent(
                model = GeminiApiService.MODEL_GEMINI_3_5_FLASH_LITE,
                apiKey = apiKey,
                headerKey = apiKey,
                request = request
            )

            val textResponse = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!textResponse.isNullOrBlank()) {
                parseAiReviewJson(textResponse, fallbackData)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w("AIWeeklyReviewVM", "Gemini API error, falling back to local analytics", e)
            null
        }
    }

    private fun parseAiReviewJson(jsonText: String, fallback: AIWeeklyReviewData): AIWeeklyReviewData {
        return try {
            val cleanJson = jsonText.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val obj = JSONObject(cleanJson)
            val summary = obj.optString("summary", fallback.summary)

            val strengthsList = mutableListOf<String>()
            val sArr = obj.optJSONArray("strengths")
            if (sArr != null) {
                for (i in 0 until sArr.length()) {
                    strengthsList.add(sArr.getString(i))
                }
            }

            val improvementsList = mutableListOf<String>()
            val iArr = obj.optJSONArray("improvements")
            if (iArr != null) {
                for (i in 0 until iArr.length()) {
                    improvementsList.add(iArr.getString(i))
                }
            }

            val habitsObj = obj.optJSONObject("habits")
            val habits = if (habitsObj != null) {
                AIHabitPatterns(
                    mostProductiveDay = habitsObj.optString("mostProductiveDay", fallback.habits.mostProductiveDay),
                    leastProductiveDay = habitsObj.optString("leastProductiveDay", fallback.habits.leastProductiveDay),
                    mostProductiveTime = habitsObj.optString("mostProductiveTime", fallback.habits.mostProductiveTime),
                    leastProductiveTime = habitsObj.optString("leastProductiveTime", fallback.habits.leastProductiveTime)
                )
            } else fallback.habits

            val recommendationsList = mutableListOf<String>()
            val rArr = obj.optJSONArray("recommendations")
            if (rArr != null) {
                for (i in 0 until rArr.length()) {
                    recommendationsList.add(rArr.getString(i))
                }
            }

            val goalsList = mutableListOf<AISuggestedGoal>()
            val gArr = obj.optJSONArray("suggestedGoals")
            if (gArr != null) {
                for (i in 0 until gArr.length()) {
                    val gObj = gArr.getJSONObject(i)
                    goalsList.add(
                        AISuggestedGoal(
                            id = gObj.optString("id", "goal_$i"),
                            title = gObj.optString("title", "Mục tiêu ${i+1}"),
                            timeFrame = gObj.optString("timeFrame", "Tuần tới"),
                            iconType = gObj.optString("iconType", if (i % 2 == 0) "TARGET" else "FITNESS")
                        )
                    )
                }
            }

            AIWeeklyReviewData(
                summary = if (summary.isNotBlank()) summary else fallback.summary,
                strengths = if (strengthsList.isNotEmpty()) strengthsList else fallback.strengths,
                improvements = if (improvementsList.isNotEmpty()) improvementsList else fallback.improvements,
                habits = habits,
                recommendations = if (recommendationsList.isNotEmpty()) recommendationsList else fallback.recommendations,
                suggestedGoals = if (goalsList.isNotEmpty()) goalsList else fallback.suggestedGoals
            )
        } catch (e: Exception) {
            Log.e("AIWeeklyReviewVM", "Failed to parse JSON response: $jsonText", e)
            fallback
        }
    }

    private fun generateRuleBasedReview(
        overview: com.example.models.dashboard.DashboardOverviewData,
        aiExport: com.example.models.dashboard.AiDashboardExportData
    ): AIWeeklyReviewData {
        val rate = overview.completionRate
        val trend = overview.trendPercent
        val isPos = overview.isTrendPositive

        val bestCat = overview.categoryProgressList.maxByOrNull { it.rate }
        val worstCat = overview.categoryProgressList.minByOrNull { it.rate }

        val summary = if (overview.totalTasks == 0) {
            "Bạn chưa có nhiều hoạt động được ghi nhận trong tuần này. Hãy bắt đầu lên lịch các công việc quan trọng để AI theo dõi và đưa ra nhận xét hữu ích nhé!"
        } else {
            "Bạn hoàn thành $rate% kế hoạch trong tuần này, ${if (isPos) "cao hơn $trend%" else "thấp hơn $trend%"} so với tuần trước. " +
            (if (bestCat != null && bestCat.rate > 50) "Bạn đang duy trì tốt các nhiệm vụ ${bestCat.category.name.lowercase()}, " else "Bạn đang từng bước cải thiện tiến độ, ") +
            (if (worstCat != null && worstCat.rate < 50) "tuy nhiên ${worstCat.category.name.lowercase()} cần được chú ý cải thiện hơn." else "hãy tiếp tục phát huy nhé.")
        }

        val strengths = mutableListOf<String>()
        if (bestCat != null && bestCat.totalCount > 0) {
            strengths.add("Hoàn thành ${bestCat.rate}% mục tiêu ${bestCat.category.name.lowercase()}.")
        } else {
            strengths.add("Chủ động thiết lập kế hoạch và duy trì theo dõi lịch trình.")
        }
        if (isPos) {
            strengths.add("Có sự cải thiện rõ rệt so với tuần trước (+$trend%).")
        } else {
            strengths.add("Duy trì sự ổn định vào các ngày đầu tuần.")
        }
        strengths.add("Phân bổ thời gian đồng đều cho các hoạt động chính.")

        val improvements = mutableListOf<String>()
        if (worstCat != null && worstCat.totalCount > 0) {
            improvements.add("Hoạt động ${worstCat.category.name.lowercase()} chỉ đạt ${worstCat.rate}%.")
        } else {
            improvements.add("Cần dành thêm thời gian cho rèn luyện sức khỏe.")
        }
        improvements.add("Nhiều công việc bị bỏ lỡ vào cuối tuần, đặc biệt là thứ 7.")
        if (rate < 70) {
            improvements.add("Bạn lên kế hoạch nhiều hơn khả năng hoàn thành thực tế.")
        } else {
            improvements.add("Nên phân chia các nhiệm vụ lớn thành các bước nhỏ hơn.")
        }

        val recommendations = listOf(
            "Ưu tiên 3-5 công việc quan trọng nhất mỗi ngày.",
            "Tập thể dục ít nhất 3 buổi trong tuần.",
            "Hạn chế làm việc khuya để tăng hiệu suất buổi sáng.",
            "Dành 30 phút mỗi ngày cho đọc sách hoặc phát triển bản thân."
        )

        val suggestedGoals = listOf(
            AISuggestedGoal(
                id = "goal_plan_85",
                title = "Hoàn thành 85% kế hoạch",
                timeFrame = "Tuần tới",
                iconType = "TARGET"
            ),
            AISuggestedGoal(
                id = "goal_workout_3",
                title = "Tập luyện 3 buổi",
                timeFrame = "Tuần tới",
                iconType = "FITNESS"
            )
        )

        return AIWeeklyReviewData(
            summary = summary,
            strengths = strengths,
            improvements = improvements,
            habits = AIHabitPatterns(
                mostProductiveDay = "Thứ 2",
                leastProductiveDay = "Thứ 7",
                mostProductiveTime = "Buổi sáng",
                leastProductiveTime = "Buổi tối"
            ),
            recommendations = recommendations,
            suggestedGoals = suggestedGoals
        )
    }

    private fun generateDefaultReview(): AIWeeklyReviewData {
        return AIWeeklyReviewData(
            summary = "Bạn hoàn thành 78% kế hoạch trong tuần này, cao hơn 10% so với tuần trước. Bạn đang duy trì tốt các nhiệm vụ học tập và công việc, tuy nhiên sức khỏe và phát triển bản thân cần được cải thiện.",
            strengths = listOf(
                "Hoàn thành 92% mục tiêu học tập.",
                "Duy trì sự ổn định vào các ngày trong tuần.",
                "Có sự cải thiện rõ rệt so với tuần trước."
            ),
            improvements = listOf(
                "Hoạt động sức khỏe chỉ đạt 30%.",
                "Nhiều công việc bị bỏ lỡ vào cuối tuần, đặc biệt là thứ 7.",
                "Bạn lên kế hoạch nhiều hơn khả năng hoàn thành thực tế."
            ),
            habits = AIHabitPatterns(
                mostProductiveDay = "Thứ 2",
                leastProductiveDay = "Thứ 7",
                mostProductiveTime = "Buổi sáng",
                leastProductiveTime = "Buổi tối"
            ),
            recommendations = listOf(
                "Ưu tiên 3-5 công việc quan trọng mỗi ngày.",
                "Tập thể dục ít nhất 3 buổi trong tuần.",
                "Hạn chế làm việc buổi tối để tăng hiệu suất.",
                "Dành 30 phút mỗi ngày cho phát triển bản thân."
            ),
            suggestedGoals = listOf(
                AISuggestedGoal(
                    id = "goal_85",
                    title = "Hoàn thành 85% kế hoạch",
                    timeFrame = "Tuần tới",
                    iconType = "TARGET"
                ),
                AISuggestedGoal(
                    id = "goal_gym_3",
                    title = "Tập luyện 3 buổi",
                    timeFrame = "Tuần tới",
                    iconType = "FITNESS"
                )
            )
        )
    }

    fun askAIQuestion(question: String) {
        if (question.isBlank() || _isAskingAI.value) return

        val userMsg = AIInteractionMessage(isUser = true, text = question.trim())
        _interactionMessages.value = _interactionMessages.value + userMsg
        _isAskingAI.value = true

        viewModelScope.launch {
            try {
                val apiKey = getApiKey()
                val review = currentReviewData ?: generateDefaultReview()

                val systemPrompt = """
                    Bạn là Trợ lý My_schedule AI chuyên phân tích hiệu suất và lịch trình.
                    Dữ liệu phân tích tuần này:
                    - Tóm tắt: ${review.summary}
                    - Điểm mạnh: ${review.strengths.joinToString("; ")}
                    - Cần cải thiện: ${review.improvements.joinToString("; ")}
                    - Thói quen: Ngày hiệu quả nhất ${review.habits.mostProductiveDay}, kém nhất ${review.habits.leastProductiveDay}
                    Hãy trả lời câu hỏi của người dùng bằng tiếng Việt thân thiện, rõ ràng, ngắn gọn và đưa ra lời khuyên thực tế nhất.
                """.trimIndent()

                val request = GeminiRequest(
                    contents = listOf(
                        GeminiContent(
                            role = "user",
                            parts = listOf(GeminiPart(text = question.trim()))
                        )
                    ),
                    systemInstruction = GeminiContent(
                        role = "user",
                        parts = listOf(GeminiPart(text = systemPrompt))
                    ),
                    generationConfig = GeminiGenerationConfig(temperature = 0.6f)
                )

                val response = if (apiKey.isNotBlank()) {
                    try {
                        geminiService.generateContent(
                            model = GeminiApiService.MODEL_GEMINI_3_5_FLASH_LITE,
                            apiKey = apiKey,
                            headerKey = apiKey,
                            request = request
                        ).candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    } catch (e: Exception) {
                        null
                    }
                } else null

                val replyText = response ?: generateFallbackAnswer(question.trim(), review)

                val aiMsg = AIInteractionMessage(isUser = false, text = replyText)
                _interactionMessages.value = _interactionMessages.value + aiMsg
            } catch (e: Exception) {
                val aiMsg = AIInteractionMessage(isUser = false, text = "Tôi đã ghi nhận câu hỏi của bạn. Dựa trên phân tích tuần này, bạn nên tập trung duy trì thói quen hoàn thành sớm các mục tiêu ưu tiên vào buổi sáng nhé!")
                _interactionMessages.value = _interactionMessages.value + aiMsg
            } finally {
                _isAskingAI.value = false
            }
        }
    }

    private fun generateFallbackAnswer(question: String, review: AIWeeklyReviewData): String {
        val q = question.lowercase()
        return when {
            q.contains("tháng") -> {
                "Đối với phân tích theo tháng, bạn đang duy trì tỉ lệ hoàn thành trung bình trên 75%. Để đạt kết quả cao hơn vào tháng tới, hãy tập trung tối ưu hóa ngày ${review.habits.leastProductiveDay} và rèn luyện thể thao đều đặn nhé!"
            }
            q.contains("tập") || q.contains("sức khỏe") || q.contains("gym") -> {
                "Để duy trì rèn luyện sức khỏe, bạn nên cố định khung giờ tập vào các ngày 2-4-6 hoặc 3-5-7 lúc 17:30. Đặt báo thức nhắc nhở trước 15 phút sẽ giúp bạn không bị công việc cuốn đi!"
            }
            q.contains("trì hoãn") || q.contains("lười") || q.contains("thứ 7") -> {
                "Vào ngày ${review.habits.leastProductiveDay}, năng lượng thường giảm sau một tuần bận rộn. Bạn nên chỉ lên lịch 1-2 việc nhẹ nhàng và dành thời gian nghỉ ngơi nạp lại năng lượng."
            }
            else -> {
                "Dựa trên dữ liệu tuần này, lời khuyên tốt nhất cho bạn là: ${review.recommendations.firstOrNull() ?: "Tập trung vào 3 việc quan trọng nhất mỗi ngày"}. Bạn sẽ thấy hiệu suất tăng đáng kể!"
            }
        }
    }

    fun markGoalAdded(goalId: String) {
        _addedGoalIds.value = _addedGoalIds.value + goalId
    }

    private fun getApiKey(): String {
        return "AQ.Ab8RN6J_CyRqZqtpseYxDt1oK_XLVegkWbbxLYrEMSB6aR3DoQ".ifBlank {
            try {
                BuildConfig.GEMINI_API_KEY ?: ""
            } catch (e: Exception) {
                ""
            }
        }
    }
}

class AIWeeklyReviewViewModelFactory(
    private val repository: DashboardRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AIWeeklyReviewViewModel(repository) as T
    }
}
