package com.example.util

import com.example.models.Event
import com.example.models.EventCategory
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

/**
 * Intelligent helper to bridge Room Database events with Gemini AI Assistant.
 * Pre-computes weekly/daily statistics and prepares structured context so AI can
 * accurately answer queries like "how many study sessions did I have this week and last week?".
 */
object AIScheduleContextHelper {

    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    private fun getDayOfWeekNameVi(date: LocalDate): String {
        return when (date.dayOfWeek) {
            DayOfWeek.MONDAY -> "Thứ Hai"
            DayOfWeek.TUESDAY -> "Thứ Ba"
            DayOfWeek.WEDNESDAY -> "Thứ Tư"
            DayOfWeek.THURSDAY -> "Thứ Năm"
            DayOfWeek.FRIDAY -> "Thứ Sáu"
            DayOfWeek.SATURDAY -> "Thứ Bảy"
            DayOfWeek.SUNDAY -> "Chủ Nhật"
            null -> ""
        }
    }

    private fun isStudyEvent(event: Event): Boolean {
        val catTitle = event.category.title.lowercase()
        val title = event.title.lowercase()
        val note = event.reminderNote.lowercase()
        return event.category.id == EventCategory.STUDY.id ||
                catTitle.contains("học") ||
                catTitle.contains("study") ||
                title.contains("học") ||
                title.contains("study") ||
                title.contains("lớp") ||
                title.contains("thi") ||
                title.contains("ôn tập") ||
                title.contains("bài tập") ||
                note.contains("học")
    }

    private fun isWorkEvent(event: Event): Boolean {
        val catTitle = event.category.title.lowercase()
        val title = event.title.lowercase()
        return event.category.id == EventCategory.WORK.id ||
                catTitle.contains("việc") ||
                catTitle.contains("work") ||
                title.contains("việc") ||
                title.contains("làm") ||
                title.contains("dự án") ||
                title.contains("task") ||
                title.contains("báo cáo")
    }

    private fun isMeetingEvent(event: Event): Boolean {
        val catTitle = event.category.title.lowercase()
        val title = event.title.lowercase()
        return event.category.id == EventCategory.MEETING.id ||
                catTitle.contains("họp") ||
                catTitle.contains("meeting") ||
                title.contains("họp") ||
                title.contains("meeting") ||
                title.contains("gặp mặt")
    }

    private fun isPlayEvent(event: Event): Boolean {
        val catTitle = event.category.title.lowercase()
        val title = event.title.lowercase()
        return event.category.id == EventCategory.PLAY.id ||
                catTitle.contains("chơi") ||
                catTitle.contains("giải trí") ||
                title.contains("chơi") ||
                title.contains("thể thao") ||
                title.contains("gym") ||
                title.contains("chạy") ||
                title.contains("du lịch") ||
                title.contains("cafe")
    }

    /**
     * Build rich, multi-week database context for Gemini AI.
     */
    fun buildScheduleContext(events: List<Event>, userQuestion: String = ""): String {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val tomorrow = today.plusDays(1)

        val thisWeekMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val thisWeekSunday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))

        val lastWeekMonday = thisWeekMonday.minusWeeks(1)
        val lastWeekSunday = thisWeekMonday.minusDays(1)

        val nextWeekMonday = thisWeekMonday.plusWeeks(1)
        val nextWeekSunday = thisWeekSunday.plusWeeks(1)

        // Filter collections
        val thisWeekEvents = events.filter { !it.date.isBefore(thisWeekMonday) && !it.date.isAfter(thisWeekSunday) }
            .sortedBy { it.startTime }
        val lastWeekEvents = events.filter { !it.date.isBefore(lastWeekMonday) && !it.date.isAfter(lastWeekSunday) }
            .sortedBy { it.startTime }
        val todayEvents = events.filter { it.date == today }.sortedBy { it.startTime }
        val yesterdayEvents = events.filter { it.date == yesterday }.sortedBy { it.startTime }
        val tomorrowEvents = events.filter { it.date == tomorrow }.sortedBy { it.startTime }
        val nextWeekEvents = events.filter { !it.date.isBefore(nextWeekMonday) && !it.date.isAfter(nextWeekSunday) }
            .sortedBy { it.startTime }

        // Category stats
        val thisWeekStudy = thisWeekEvents.filter { isStudyEvent(it) }
        val lastWeekStudy = lastWeekEvents.filter { isStudyEvent(it) }
        val totalTwoWeeksStudy = thisWeekStudy.size + lastWeekStudy.size

        val thisWeekWork = thisWeekEvents.filter { isWorkEvent(it) }
        val lastWeekWork = lastWeekEvents.filter { isWorkEvent(it) }

        val thisWeekCompleted = thisWeekEvents.count { it.isCompleted }
        val lastWeekCompleted = lastWeekEvents.count { it.isCompleted }

        return buildString {
            append("=== [CƠ SỞ DỮ LIỆU LỊCH TRÌNH VÀ SỰ KIỆN CỦA NGƯỜI DÙNG] ===\n")
            append("Hôm nay: ${getDayOfWeekNameVi(today)}, ngày ${today.format(dateFormatter)}\n")
            append("Tuần này: Từ ${thisWeekMonday.format(dateFormatter)} đến ${thisWeekSunday.format(dateFormatter)}\n")
            append("Tuần trước: Từ ${lastWeekMonday.format(dateFormatter)} đến ${lastWeekSunday.format(dateFormatter)}\n")
            append("Tuần tới: Từ ${nextWeekMonday.format(dateFormatter)} đến ${nextWeekSunday.format(dateFormatter)}\n\n")

            append("--- [BẢNG TỔNG HỢP & THỐNG KÊ NHANH] ---\n")
            append("• THỐNG KÊ HỌC TẬP (STUDY):\n")
            append("  + Số buổi học tuần này: ${thisWeekStudy.size} buổi\n")
            append("  + Số buổi học tuần trước: ${lastWeekStudy.size} buổi\n")
            append("  + TỔNG CỘNG 2 TUẦN (Tuần này + Tuần trước): $totalTwoWeeksStudy buổi học\n")
            if (thisWeekStudy.isNotEmpty()) {
                append("  + Chi tiết các buổi học tuần này:\n")
                thisWeekStudy.forEach {
                    append("    - ${it.date.format(dateFormatter)} (${getDayOfWeekNameVi(it.date)}) ${it.startTime.format(timeFormatter)}-${it.endTime.format(timeFormatter)}: ${it.title} [${if (it.isCompleted) "Đã hoàn thành" else "Chưa hoàn thành"}]\n")
                }
            }
            if (lastWeekStudy.isNotEmpty()) {
                append("  + Chi tiết các buổi học tuần trước:\n")
                lastWeekStudy.forEach {
                    append("    - ${it.date.format(dateFormatter)} (${getDayOfWeekNameVi(it.date)}) ${it.startTime.format(timeFormatter)}-${it.endTime.format(timeFormatter)}: ${it.title} [${if (it.isCompleted) "Đã hoàn thành" else "Chưa hoàn thành"}]\n")
                }
            }

            append("\n• THỐNG KÊ CÔNG VIỆC (WORK):\n")
            append("  + Tuần này: ${thisWeekWork.size} công việc (Đã xong: $thisWeekCompleted / ${thisWeekEvents.size} tổng sự kiện)\n")
            append("  + Tuần trước: ${lastWeekWork.size} công việc (Đã xong: $lastWeekCompleted / ${lastWeekEvents.size} tổng sự kiện)\n\n")

            append("--- [CHI TIẾT LỊCH HÔM NAY (${today.format(dateFormatter)})] ---\n")
            if (todayEvents.isEmpty()) {
                append("Hôm nay chưa có lịch trình nào được lưu.\n")
            } else {
                todayEvents.forEach { ev ->
                    append("• [${ev.startTime.format(timeFormatter)} - ${ev.endTime.format(timeFormatter)}] ${ev.title} (Danh mục: ${ev.category.title}, Trạng thái: ${if (ev.isCompleted) "Đã hoàn thành" else "Chưa hoàn thành"})\n")
                }
            }

            append("\n--- [CHI TIẾT TOÀN BỘ SỰ KIỆN TUẦN NÀY (${thisWeekMonday.format(dateFormatter)} - ${thisWeekSunday.format(dateFormatter)})] ---\n")
            if (thisWeekEvents.isEmpty()) {
                append("Tuần này chưa có sự kiện nào được ghi nhận.\n")
            } else {
                thisWeekEvents.forEach { ev ->
                    append("• ${ev.date.format(dateFormatter)} (${getDayOfWeekNameVi(ev.date)}) [${ev.startTime.format(timeFormatter)} - ${ev.endTime.format(timeFormatter)}] ${ev.title} - ${ev.category.title} [${if (ev.isCompleted) "Đã xong" else "Chưa xong"}]\n")
                }
            }

            append("\n--- [CHI TIẾT TOÀN BỘ SỰ KIỆN TUẦN TRƯỚC (${lastWeekMonday.format(dateFormatter)} - ${lastWeekSunday.format(dateFormatter)})] ---\n")
            if (lastWeekEvents.isEmpty()) {
                append("Tuần trước chưa có sự kiện nào được ghi nhận trong cơ sở dữ liệu.\n")
            } else {
                lastWeekEvents.forEach { ev ->
                    append("• ${ev.date.format(dateFormatter)} (${getDayOfWeekNameVi(ev.date)}) [${ev.startTime.format(timeFormatter)} - ${ev.endTime.format(timeFormatter)}] ${ev.title} - ${ev.category.title} [${if (ev.isCompleted) "Đã xong" else "Chưa xong"}]\n")
                }
            }

            // Other events in DB
            val otherEvents = events.filter {
                it.date.isBefore(lastWeekMonday) || it.date.isAfter(thisWeekSunday)
            }.sortedBy { it.startTime }.take(40)

            if (otherEvents.isNotEmpty()) {
                append("\n--- [CÁC SỰ KIỆN KHÁC TRONG CƠ SỞ DỮ LIỆU (QUÁ KHỨ & TƯƠNG LAI)] ---\n")
                otherEvents.forEach { ev ->
                    append("• ${ev.date.format(dateFormatter)} (${getDayOfWeekNameVi(ev.date)}) [${ev.startTime.format(timeFormatter)} - ${ev.endTime.format(timeFormatter)}] ${ev.title} - ${ev.category.title}\n")
                }
            }

            // Highlighting based on user query
            val q = userQuestion.lowercase().trim()
            if (q.isNotBlank()) {
                append("\n--- [TRÍCH XUẤT ĐẶC BIỆT THEO CÂU HỎI HIỆN TẠI CỦA NGƯỜI DÙNG: \"$userQuestion\"] ---\n")
                when {
                    q.contains("học") || q.contains("study") -> {
                        append("=> Dữ liệu học tập: Tuần trước có ${lastWeekStudy.size} buổi học, tuần này có ${thisWeekStudy.size} buổi học. Tổng cộng cả 2 tuần là $totalTwoWeeksStudy buổi học.\n")
                    }
                    q.contains("hôm qua") -> {
                        append("=> Dữ liệu hôm qua (${yesterday.format(dateFormatter)}): ${yesterdayEvents.size} sự kiện:\n")
                        yesterdayEvents.forEach { append("   - ${it.startTime.format(timeFormatter)}: ${it.title} (${it.category.title})\n") }
                    }
                    q.contains("ngày mai") || q.contains("mai") -> {
                        append("=> Dữ liệu ngày mai (${tomorrow.format(dateFormatter)}): ${tomorrowEvents.size} sự kiện:\n")
                        tomorrowEvents.forEach { append("   - ${it.startTime.format(timeFormatter)}: ${it.title} (${it.category.title})\n") }
                    }
                    q.contains("chưa xong") || q.contains("chưa hoàn thành") -> {
                        val uncompleted = events.filter { !it.isCompleted }
                        append("=> Danh sách chưa hoàn thành: ${uncompleted.size} sự kiện.\n")
                    }
                }
            }
        }
    }

    /**
     * Local query solver: Directly and accurately answers common schedule queries
     * in case network/AI is temporarily unavailable or when providing instant answers.
     */
    fun answerDirectLocally(question: String, events: List<Event>): String? {
        val q = question.lowercase().trim()
        val today = LocalDate.now()
        val thisWeekMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val thisWeekSunday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        val lastWeekMonday = thisWeekMonday.minusWeeks(1)
        val lastWeekSunday = thisWeekMonday.minusDays(1)

        val thisWeekEvents = events.filter { !it.date.isBefore(thisWeekMonday) && !it.date.isAfter(thisWeekSunday) }
        val lastWeekEvents = events.filter { !it.date.isBefore(lastWeekMonday) && !it.date.isAfter(lastWeekSunday) }

        val thisWeekStudy = thisWeekEvents.filter { isStudyEvent(it) }
        val lastWeekStudy = lastWeekEvents.filter { isStudyEvent(it) }

        // Case 1: Query about study sessions in this week and last week
        if ((q.contains("học") || q.contains("buổi học")) &&
            (q.contains("tuần trước") || (q.contains("tuần") && q.contains("trước")))
        ) {
            val total = thisWeekStudy.size + lastWeekStudy.size
            val sb = StringBuilder()
            sb.append("Dựa trên cơ sở dữ liệu lịch trình của bạn:\n\n")
            sb.append("• **Tuần trước** (${lastWeekMonday.format(dateFormatter)} - ${lastWeekSunday.format(dateFormatter)}): Bạn có **${lastWeekStudy.size} buổi học**")
            if (lastWeekStudy.isNotEmpty()) {
                sb.append(":\n")
                lastWeekStudy.forEach {
                    sb.append("  - ${it.date.format(dateFormatter)} (${getDayOfWeekNameVi(it.date)}) ${it.startTime.format(timeFormatter)} - ${it.endTime.format(timeFormatter)}: *${it.title}*\n")
                }
            } else {
                sb.append(" (chưa có sự kiện học tập nào được ghi nhận).\n")
            }

            sb.append("\n• **Tuần này** (${thisWeekMonday.format(dateFormatter)} - ${thisWeekSunday.format(dateFormatter)}): Bạn có **${thisWeekStudy.size} buổi học**")
            if (thisWeekStudy.isNotEmpty()) {
                sb.append(":\n")
                thisWeekStudy.forEach {
                    sb.append("  - ${it.date.format(dateFormatter)} (${getDayOfWeekNameVi(it.date)}) ${it.startTime.format(timeFormatter)} - ${it.endTime.format(timeFormatter)}: *${it.title}*\n")
                }
            } else {
                sb.append(" (chưa có sự kiện học tập nào được ghi nhận).\n")
            }

            sb.append("\n🎯 **Tổng cộng cả tuần này và tuần trước**: Bạn có **$total buổi học** tất cả.")
            return sb.toString()
        }

        // Case 2: Query study sessions in this week only
        if ((q.contains("học") || q.contains("buổi học")) && q.contains("tuần này")) {
            val sb = StringBuilder()
            sb.append("Trong **tuần này** (${thisWeekMonday.format(dateFormatter)} - ${thisWeekSunday.format(dateFormatter)}), bạn có tổng cộng **${thisWeekStudy.size} buổi học**:\n")
            if (thisWeekStudy.isNotEmpty()) {
                thisWeekStudy.forEach {
                    sb.append("• ${it.date.format(dateFormatter)} (${getDayOfWeekNameVi(it.date)}) ${it.startTime.format(timeFormatter)} - ${it.endTime.format(timeFormatter)}: ${it.title} (${if (it.isCompleted) "Đã hoàn thành" else "Chưa hoàn thành"})\n")
                }
            } else {
                sb.append("Hiện chưa có lịch học nào được thêm vào tuần này.")
            }
            return sb.toString()
        }

        // Case 3: Query today's events
        if (q.contains("hôm nay") && (q.contains("lịch") || q.contains("làm gì") || q.contains("sự kiện") || q.contains("gì"))) {
            val todayEvents = events.filter { it.date == today }.sortedBy { it.startTime }
            val sb = StringBuilder()
            sb.append("Hôm nay (${today.format(dateFormatter)} - ${getDayOfWeekNameVi(today)}), ")
            if (todayEvents.isEmpty()) {
                sb.append("bạn chưa có lịch trình nào. Bạn có muốn tôi gợi ý một số công việc hoặc kế hoạch không?")
            } else {
                sb.append("bạn có **${todayEvents.size} công việc/sự kiện**:\n")
                todayEvents.forEach {
                    sb.append("• [${it.startTime.format(timeFormatter)} - ${it.endTime.format(timeFormatter)}] ${it.title} (${it.category.title}) - ${if (it.isCompleted) "✅ Đã xong" else "⏳ Chưa xong"}\n")
                }
            }
            return sb.toString()
        }

        return null
    }
}
