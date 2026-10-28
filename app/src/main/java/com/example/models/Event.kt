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
    val id: Int = 0,
    val title: String,
    val color: Color,
    val bgColor: Color,
    val isLocked: Boolean = false
) {
    // Helper to get text color that contrasts well with the category color/bg
    val contrastTextColor: Color
        get() {
            val lum = 0.299 * color.red + 0.587 * color.green + 0.114 * color.blue
            return if (lum > 0.75) Color(0xFF1E293B) else color
        }

    companion object {
        val WORK = EventCategory(
            id = 1,
            title = "Công việc",
            color = CategoryWork,
            bgColor = CategoryWorkBg
        )
        val STUDY = EventCategory(
            id = 2,
            title = "Học tập",
            color = CategoryStudy,
            bgColor = CategoryStudyBg
        )
        val PLAY = EventCategory(
            id = 3,
            title = "Đi chơi",
            color = CategoryPlay,
            bgColor = CategoryPlayBg
        )
        val MEETING = EventCategory(
            id = 4,
            title = "Cuộc họp",
            color = CategoryMeeting,
            bgColor = CategoryMeetingBg
        )
        val HOLIDAY = EventCategory(
            id = 5,
            title = "Ngày lễ",
            color = CategoryHoliday,
            bgColor = CategoryHolidayBg,
            isLocked = true
        )
        val PERSONAL = EventCategory(
            id = 6,
            title = "Cá nhân",
            color = Color(0xFF8B5CF6),
            bgColor = Color(0xFFF5F3FF)
        )
        val HEALTH = EventCategory(
            id = 7,
            title = "Sức khỏe",
            color = Color(0xFFEC4899),
            bgColor = Color(0xFFFDF2F8)
        )
        val FINANCE = EventCategory(
            id = 8,
            title = "Tài chính",
            color = Color(0xFF14B8A6),
            bgColor = Color(0xFFCCFBF1)
        )
        val TASK = EventCategory(
            id = 9,
            title = "Việc cần làm",
            color = Color(0xFF6366F1),
            bgColor = Color(0xFFEEF2FF)
        )
        val FAMILY = EventCategory(
            id = 10,
            title = "Gia đình",
            color = Color(0xFFF97316),
            bgColor = Color(0xFFFFEDD5)
        )

        // 10 default categories in total (including locked red Holiday)
        val DEFAULT_CATEGORIES = listOf(
            WORK, STUDY, PLAY, MEETING, HOLIDAY, PERSONAL, HEALTH, FINANCE, TASK, FAMILY
        )

        // Color presets available for user-created categories (White, Grays, Blues, Greens, Yellows, Pinks, Purples, etc.)
        val COLOR_PRESETS = listOf(
            CategoryColorPreset("Trắng tinh khôi", Color(0xFFFFFFFF), Color(0xFFF8FAFC)),
            CategoryColorPreset("Trắng xám ngọc", Color(0xFFE2E8F0), Color(0xFFF1F5F9)),
            CategoryColorPreset("Xám bạc", Color(0xFF94A3B8), Color(0xFFF8FAFC)),
            CategoryColorPreset("Đen than", Color(0xFF1E293B), Color(0xFFF1F5F9)),
            CategoryColorPreset("Xanh dương (Công việc)", Color(0xFF3B82F6), Color(0xFFEFF6FF)),
            CategoryColorPreset("Xanh navy đậm", Color(0xFF1D4ED8), Color(0xFFDBEAFE)),
            CategoryColorPreset("Xanh da trời", Color(0xFF0284C7), Color(0xFFE0F2FE)),
            CategoryColorPreset("Xanh lam (Đi chơi)", Color(0xFF06B6D4), Color(0xFFECFEFF)),
            CategoryColorPreset("Xanh mòng két", Color(0xFF0D9488), Color(0xFFCCFBF1)),
            CategoryColorPreset("Xanh lá (Học tập)", Color(0xFF10B981), Color(0xFFECFDF5)),
            CategoryColorPreset("Xanh chanh / Mint", Color(0xFF84CC16), Color(0xFFF7FEE7)),
            CategoryColorPreset("Vàng cam (Cuộc họp)", Color(0xFFF59E0B), Color(0xFFFEF3C7)),
            CategoryColorPreset("Vàng hoàng yến", Color(0xFFEAB308), Color(0xFFFEF9C3)),
            CategoryColorPreset("Cam rực rỡ", Color(0xFFF97316), Color(0xFFFFEDD5)),
            CategoryColorPreset("Nâu cam", Color(0xFFD97706), Color(0xFFFFFBEB)),
            CategoryColorPreset("Nâu cà phê", Color(0xFF78350F), Color(0xFFFEF3C7)),
            CategoryColorPreset("Hồng thạch anh", Color(0xFFF43F5E), Color(0xFFFFF1F2)),
            CategoryColorPreset("Hồng tươi", Color(0xFFEC4899), Color(0xFFFDF2F8)),
            CategoryColorPreset("Hồng cánh sen", Color(0xFFD946EF), Color(0xFFFDF4FF)),
            CategoryColorPreset("Tím mộng mơ", Color(0xFF8B5CF6), Color(0xFFF5F3FF)),
            CategoryColorPreset("Xanh tím / Tím chàm", Color(0xFF6366F1), Color(0xFFEEF2FF)),
            CategoryColorPreset("Tím thẫm", Color(0xFF7C3AED), Color(0xFFEDE9FE))
        )
    }
}

data class CategoryColorPreset(
    val name: String,
    val color: Color,
    val bgColor: Color
) {
    val isLight: Boolean
        get() = (0.299 * color.red + 0.587 * color.green + 0.114 * color.blue) > 0.75

    val contrastTextColor: Color
        get() = if (isLight) Color(0xFF1E293B) else color
}

data class Event(
    val id: Int = 0,
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

