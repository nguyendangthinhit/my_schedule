package com.example.models.dashboard

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.UUID

@JsonClass(generateAdapter = true)
data class AIHabitPatterns(
    @Json(name = "mostProductiveDay") val mostProductiveDay: String = "Thứ 2",
    @Json(name = "leastProductiveDay") val leastProductiveDay: String = "Thứ 7",
    @Json(name = "mostProductiveTime") val mostProductiveTime: String = "Buổi sáng",
    @Json(name = "leastProductiveTime") val leastProductiveTime: String = "Buổi tối"
)

@JsonClass(generateAdapter = true)
data class AISuggestedGoal(
    @Json(name = "id") val id: String = UUID.randomUUID().toString(),
    @Json(name = "title") val title: String,
    @Json(name = "timeFrame") val timeFrame: String = "Tuần tới",
    @Json(name = "iconType") val iconType: String = "TARGET", // TARGET, FITNESS, STUDY, WORK
    val isAdded: Boolean = false
)

@JsonClass(generateAdapter = true)
data class AIWeeklyReviewData(
    @Json(name = "weekTitle") val weekTitle: String = "AI phân tích tuần này",
    @Json(name = "periodSubtitle") val periodSubtitle: String = "Phân tích và gợi ý được tạo bởi AI dựa trên dữ liệu lịch trình của bạn.",
    @Json(name = "summary") val summary: String,
    @Json(name = "strengths") val strengths: List<String> = emptyList(),
    @Json(name = "improvements") val improvements: List<String> = emptyList(),
    @Json(name = "habits") val habits: AIHabitPatterns = AIHabitPatterns(),
    @Json(name = "recommendations") val recommendations: List<String> = emptyList(),
    @Json(name = "suggestedGoals") val suggestedGoals: List<AISuggestedGoal> = emptyList()
)

data class AIInteractionMessage(
    val id: String = UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)
