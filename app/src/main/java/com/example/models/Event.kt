package com.example.models

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.CategoryHoliday
import com.example.ui.theme.CategoryHolidayBg
import com.example.ui.theme.CategoryMeeting
import com.example.ui.theme.CategoryMeetingBg
import com.example.ui.theme.CategoryPlay
import com.example.ui.theme.CategoryPlayBg
import com.example.ui.theme.CategoryStudy
import com.example.ui.theme.CategoryStudyBg
import com.example.ui.theme.CategoryWork
import com.example.ui.theme.CategoryWorkBg
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

data class EventCategory(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val color: Color,
    val bgColor: Color
) {
    companion object {
        val WORK = EventCategory(
            id = "cat_work",
            title = "Công việc",
            color = CategoryWork,
            bgColor = CategoryWorkBg
        )
        val STUDY = EventCategory(
            id = "cat_study",
            title = "Học tập",
            color = CategoryStudy,
            bgColor = CategoryStudyBg
        )
        val PLAY = EventCategory(
            id = "cat_play",
            title = "Đi chơi",
            color = CategoryPlay,
            bgColor = CategoryPlayBg
        )
        val MEETING = EventCategory(
            id = "cat_meeting",
            title = "Cuộc họp",
            color = CategoryMeeting,
            bgColor = CategoryMeetingBg
        )
        val HOLIDAY = EventCategory(
            id = "cat_holiday",
            title = "Ngày lễ",
            color = CategoryHoliday,
            bgColor = CategoryHolidayBg
        )

        val DEFAULT_CATEGORIES = listOf(WORK, STUDY, PLAY, MEETING, HOLIDAY)

        val COLOR_PRESETS = listOf(
            CategoryColorPreset("Đỏ (Ngày lễ)", Color(0xFFEF4444), Color(0xFFFEE2E2)),
            CategoryColorPreset("Xanh lam (Đi chơi)", Color(0xFF06B6D4), Color(0xFFECFEFF)),
            CategoryColorPreset("Xanh dương (Công việc)", Color(0xFF3B82F6), Color(0xFFEFF6FF)),
            CategoryColorPreset("Xanh lá (Học tập)", Color(0xFF10B981), Color(0xFFECFDF5)),
            CategoryColorPreset("Vàng cam (Cuộc họp)", Color(0xFFF59E0B), Color(0xFFFEF3C7)),
            CategoryColorPreset("Tím mộng mơ", Color(0xFF8B5CF6), Color(0xFFF5F3FF)),
            CategoryColorPreset("Hồng tươi", Color(0xFFEC4899), Color(0xFFFDF2F8)),
            CategoryColorPreset("Xanh tím", Color(0xFF6366F1), Color(0xFFEEF2FF)),
            CategoryColorPreset("Nâu cam", Color(0xFFD97706), Color(0xFFFFFBEB))
        )
    }
}

data class CategoryColorPreset(
    val name: String,
    val color: Color,
    val bgColor: Color
)

data class Event(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val category: EventCategory,
    val isCompleted: Boolean = false,
    val reminderNote: String = "",
    val hasReminder: Boolean = false,
    val reminderTimeOffsetMins: Int = 15
) {
    val date: LocalDate get() = startTime.toLocalDate()
    val startHour: Int get() = startTime.hour
    val endHour: Int get() = if (endTime.minute > 0) endTime.hour + 1 else endTime.hour
}

