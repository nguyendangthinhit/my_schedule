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
}
