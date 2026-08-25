package com.example.models.dashboard

import androidx.compose.ui.graphics.Color
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

enum class PeriodType(val label: String) {
    DAY("Ngày"),
    WEEK("Tuần"),
    MONTH("Tháng")
}

@JsonClass(generateAdapter = true)
data class DashboardCategory(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String,
    @Json(name = "color_hex") val colorHex: String,
    @Json(name = "icon_emoji") val iconEmoji: String = getEmojiForCategoryName(name)
) {
    val title: String get() = name
    val shortTitle: String get() = name

    val composeColor: Color
        get() = try {
            Color(android.graphics.Color.parseColor(colorHex))
        } catch (e: Exception) {
            Color(0xFF6366F1)
        }

    val composeBgColor: Color
        get() = try {
            Color(android.graphics.Color.parseColor(colorHex)).copy(alpha = 0.14f)
        } catch (e: Exception) {
            Color(0xFFEEF2FF)
        }

    val colorHexLong: Long
        get() = try {
            android.graphics.Color.parseColor(colorHex).toLong() and 0xFFFFFFFFL
        } catch (e: Exception) {
            0xFF6366F1
        }

    val bgHexLong: Long
        get() = try {
            val c = android.graphics.Color.parseColor(colorHex)
            val r = android.graphics.Color.red(c)
            val g = android.graphics.Color.green(c)
            val b = android.graphics.Color.blue(c)
            // Light pastel background
            val lightR = (r + 255 * 4) / 5
            val lightG = (g + 255 * 4) / 5
            val lightB = (b + 255 * 4) / 5
            (0xFF000000L or (lightR.toLong() shl 16) or (lightG.toLong() shl 8) or lightB.toLong()) and 0xFFFFFFFFL
        } catch (e: Exception) {
            0xFFEEF2FF
        }

    companion object {
        fun getEmojiForCategoryName(name: String): String {
            val lower = name.lowercase()
            return when {
                lower.contains("học") || lower.contains("study") || lower.contains("toán") || lower.contains("sách") || lower.contains("bài") -> "📖"
                lower.contains("việc") || lower.contains("work") || lower.contains("công việc") || lower.contains("dự án") || lower.contains("họp") -> "💼"
                lower.contains("khỏe") || lower.contains("health") || lower.contains("gym") || lower.contains("thể thao") || lower.contains("chạy") || lower.contains("tập") -> "🏋️"
                lower.contains("triển") || lower.contains("dev") || lower.contains("kỹ năng") || lower.contains("đọc") || lower.contains("bản thân") -> "🧠"
                lower.contains("chơi") || lower.contains("play") || lower.contains("giải trí") || lower.contains("game") || lower.contains("phim") -> "🎮"
                lower.contains("nhà") || lower.contains("gia đình") || lower.contains("family") -> "🏠"
                lower.contains("tiền") || lower.contains("tài chính") || lower.contains("finance") -> "💰"
                lower.contains("ăn") || lower.contains("uống") || lower.contains("cafe") || lower.contains("cơm") -> "☕"
                lower.contains("lễ") || lower.contains("holiday") -> "🎉"
                else -> "📌"
            }
        }
    }
}

enum class DashboardCategoryType(
    val key: String,
    val title: String,
    val shortTitle: String,
    val iconEmoji: String,
    val colorHex: Long,
    val bgHex: Long
) {
    STUDY("Study", "Học tập", "Học tập", "📖", 0xFF10B981, 0xFFE6F4EA),
    WORK("Work", "Công việc", "Công việc", "💼", 0xFF3B82F6, 0xFFE8F0FE),
    HEALTH("Health", "Sức khỏe", "Sức khỏe", "🏋️", 0xFFF97316, 0xFFFEECE0),
    PERSONAL_DEV("Personal Development", "Phát triển bản thân", "Phát triển", "🧠", 0xFF8B5CF6, 0xFFF3E8FF);

    fun toDashboardCategory(id: Int = ordinal + 1): DashboardCategory {
        val hexStr = String.format("#%06X", (0xFFFFFF and colorHex.toInt()))
        return DashboardCategory(
            id = id,
            name = title,
            colorHex = hexStr,
            iconEmoji = iconEmoji
        )
    }

    companion object {
        fun fromKey(key: String): DashboardCategoryType {
            return entries.firstOrNull { it.key.equals(key, ignoreCase = true) || it.title.equals(key, ignoreCase = true) }
                ?: STUDY
        }
    }
}

data class CategoryProgressItem(
    val category: DashboardCategory,
    val rate: Int,
    val trendDiff: Int,
    val isPositiveTrend: Boolean,
    val completedCount: Int,
    val totalCount: Int
)

data class DayStreakItem(
    val dayLabel: String,
    val dateString: String,
    val isCompleted: Boolean,
    val hasTasks: Boolean,
    val isToday: Boolean = false
)

data class DayBarData(
    val dayLabel: String,
    val dateString: String,
    val percentage: Int,
    val completedCount: Int,
    val totalCount: Int,
    val isHighlighted: Boolean = false
)

data class TimeSlotDistribution(
    val slotName: String,
    val timeRange: String,
    val percentage: Int,
    val color: Color
)

data class FeaturedTaskItem(
    val id: Int,
    val title: String,
    val statusText: String,
    val isCompleted: Boolean,
    val priority: String,
    val timeFormatted: String
)

data class FourWeekPoint(
    val weekLabel: String,
    val completionRate: Int
)

data class PriorityCompletionStats(
    val priority: String,
    val total: Int,
    val completed: Int,
    val completionRate: Int
)

data class DashboardOverviewData(
    val period: PeriodType,
    val dateRangeText: String,
    val totalTasks: Int,
    val completedTasks: Int,
    val inProgressTasks: Int,
    val incompleteTasks: Int,
    val completionRate: Int,
    val previousCompletionRate: Int,
    val trendPercent: Int,
    val isTrendPositive: Boolean,
    val motivationMessage: String,
    val categoryProgressList: List<CategoryProgressItem>,
    val planningAccuracy: Int,
    val planningAccuracyAdvice: String,
    val planningAccuracyStatus: String,
    val streakDays: Int,
    val weekStreak: List<DayStreakItem>,
    val quickObservation: String,
    val bestDay: String,
    val worstDay: String
)

data class CategoryDetailData(
    val category: DashboardCategory,
    val periodSubtitle: String,
    val completionRate: Int,
    val completedCount: Int,
    val totalCount: Int,
    val weeklyBarData: List<DayBarData>,
    val thisWeekRate: Int,
    val lastWeekRate: Int,
    val trendDiff: Int,
    val isPositiveTrend: Boolean,
    val timeDistribution: List<TimeSlotDistribution>,
    val featuredTasks: List<FeaturedTaskItem>,
    val fourWeekTrend: List<FourWeekPoint>,
    val priorityStats: Map<String, PriorityCompletionStats>
)

// AI Export JSON Models (Persisted to json file for AI consumption)
@JsonClass(generateAdapter = true)
data class AiDashboardExportData(
    @Json(name = "export_timestamp") val exportTimestamp: Long,
    @Json(name = "export_date") val exportDate: String,
    @Json(name = "current_period") val currentPeriod: String,
    @Json(name = "date_range") val dateRange: String,
    @Json(name = "total_planned_tasks") val totalPlannedTasks: Int,
    @Json(name = "completed_tasks") val completedTasks: Int,
    @Json(name = "in_progress_tasks") val inProgressTasks: Int,
    @Json(name = "incomplete_tasks") val incompleteTasks: Int,
    @Json(name = "overall_completion_rate_percent") val overallCompletionRatePercent: Int,
    @Json(name = "previous_period_completion_rate_percent") val previousPeriodCompletionRatePercent: Int,
    @Json(name = "trend_diff_percent") val trendDiffPercent: Int,
    @Json(name = "is_trend_positive") val isTrendPositive: Boolean,
    @Json(name = "planning_accuracy_percent") val planningAccuracyPercent: Int,
    @Json(name = "planning_accuracy_status") val planningAccuracyStatus: String,
    @Json(name = "planning_accuracy_advice") val planningAccuracyAdvice: String,
    @Json(name = "active_streak_days") val activeStreakDays: Int,
    @Json(name = "weekly_streak_breakdown") val weeklyStreakBreakdown: List<AiDayStreakSummary>,
    @Json(name = "best_performance_day") val bestPerformanceDay: String,
    @Json(name = "lowest_performance_day") val lowestPerformanceDay: String,
    @Json(name = "category_summaries") val categorySummaries: List<AiCategorySummary>,
    @Json(name = "time_of_day_distribution") val timeOfDayDistribution: List<AiTimeSlotSummary>,
    @Json(name = "recent_featured_tasks") val recentFeaturedTasks: List<AiTaskSummary>,
    @Json(name = "four_week_performance_trend") val fourWeekPerformanceTrend: List<AiFourWeekTrendPoint>,
    @Json(name = "quick_observation_insight") val quickObservationInsight: String,
    @Json(name = "motivation_message") val motivationMessage: String
)

@JsonClass(generateAdapter = true)
data class AiCategorySummary(
    @Json(name = "category_id") val categoryId: Int,
    @Json(name = "category_name") val categoryName: String,
    @Json(name = "emoji") val emoji: String,
    @Json(name = "color_hex") val colorHex: String,
    @Json(name = "total_tasks") val totalTasks: Int,
    @Json(name = "completed_tasks") val completedTasks: Int,
    @Json(name = "completion_rate_percent") val completionRatePercent: Int,
    @Json(name = "trend_diff_percent") val trendDiffPercent: Int,
    @Json(name = "is_positive_trend") val isPositiveTrend: Boolean
)

@JsonClass(generateAdapter = true)
data class AiDayStreakSummary(
    @Json(name = "day_of_week") val dayOfWeek: String,
    @Json(name = "date") val date: String,
    @Json(name = "has_tasks") val hasTasks: Boolean,
    @Json(name = "is_completed") val isCompleted: Boolean,
    @Json(name = "completed_count") val completedCount: Int,
    @Json(name = "total_count") val totalCount: Int
)

@JsonClass(generateAdapter = true)
data class AiTimeSlotSummary(
    @Json(name = "slot_name") val slotName: String,
    @Json(name = "time_range") val timeRange: String,
    @Json(name = "percentage") val percentage: Int
)

@JsonClass(generateAdapter = true)
data class AiTaskSummary(
    @Json(name = "id") val id: Int,
    @Json(name = "title") val title: String,
    @Json(name = "category_name") val categoryName: String,
    @Json(name = "date") val date: String,
    @Json(name = "time_formatted") val timeFormatted: String,
    @Json(name = "is_completed") val isCompleted: Boolean,
    @Json(name = "priority") val priority: String
)

@JsonClass(generateAdapter = true)
data class AiFourWeekTrendPoint(
    @Json(name = "week_label") val weekLabel: String,
    @Json(name = "completion_rate_percent") val completionRatePercent: Int
)
