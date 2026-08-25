package com.example.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object DateUtils {
    fun getVietnameseDayOfWeek(dayOfWeek: DayOfWeek, short: Boolean = false): String {
        return when (dayOfWeek) {
            DayOfWeek.MONDAY -> if (short) "T2" else "Thứ Hai"
            DayOfWeek.TUESDAY -> if (short) "T3" else "Thứ Ba"
            DayOfWeek.WEDNESDAY -> if (short) "T4" else "Thứ Tư"
            DayOfWeek.THURSDAY -> if (short) "T5" else "Thứ Năm"
            DayOfWeek.FRIDAY -> if (short) "T6" else "Thứ Sáu"
            DayOfWeek.SATURDAY -> if (short) "T7" else "Thứ Bảy"
            DayOfWeek.SUNDAY -> if (short) "CN" else "Chủ Nhật"
        }
    }

    fun formatDayOfWeekDot(dayOfWeek: DayOfWeek): String {
        return when (dayOfWeek) {
            DayOfWeek.MONDAY -> "T.2"
            DayOfWeek.TUESDAY -> "T.3"
            DayOfWeek.WEDNESDAY -> "T.4"
            DayOfWeek.THURSDAY -> "T.5"
            DayOfWeek.FRIDAY -> "T.6"
            DayOfWeek.SATURDAY -> "T.7"
            DayOfWeek.SUNDAY -> "CN"
        }
    }

    fun formatFullVietnameseDate(date: LocalDate): String {
        val dayOfWeek = getVietnameseDayOfWeek(date.dayOfWeek)
        return "$dayOfWeek, ngày ${date.dayOfMonth} tháng ${date.monthValue} năm ${date.year}"
    }

    fun formatShortVietnameseDate(date: LocalDate): String {
        val dayOfWeek = getVietnameseDayOfWeek(date.dayOfWeek)
        val dayStr = String.format("%02d", date.dayOfMonth)
        val monthStr = String.format("%02d", date.monthValue)
        return "$dayOfWeek, $dayStr/$monthStr/${date.year}"
    }

    fun formatTimeRange(start: LocalDateTime, end: LocalDateTime): String {
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        return "${start.format(timeFormatter)} - ${end.format(timeFormatter)}"
    }

    fun formatWidgetHeaderDate(date: LocalDate): String {
        val dayName = when (date.dayOfWeek) {
            DayOfWeek.MONDAY -> "Thứ 2"
            DayOfWeek.TUESDAY -> "Thứ 3"
            DayOfWeek.WEDNESDAY -> "Thứ 4"
            DayOfWeek.THURSDAY -> "Thứ 5"
            DayOfWeek.FRIDAY -> "Thứ 6"
            DayOfWeek.SATURDAY -> "Thứ 7"
            DayOfWeek.SUNDAY -> "Chủ Nhật"
        }
        return "$dayName, ${date.dayOfMonth} Thg ${date.monthValue}"
    }

    fun formatWidgetMicroMiniDate(date: LocalDate): String {
        val dayOfWeek = getVietnameseDayOfWeek(date.dayOfWeek)
        val dayStr = String.format("%02d", date.dayOfMonth)
        val monthStr = String.format("%02d", date.monthValue)
        return "$dayOfWeek, $dayStr/$monthStr"
    }

    fun formatWidgetFullDate(date: LocalDate): String {
        val dayOfWeek = getVietnameseDayOfWeek(date.dayOfWeek)
        val dayStr = String.format("%02d", date.dayOfMonth)
        val monthStr = String.format("%02d", date.monthValue)
        return "$dayOfWeek, $dayStr/$monthStr/${date.year}"
    }
}
