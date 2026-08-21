package com.example.models

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.CategoryPlay
import com.example.ui.theme.CategoryPlayBg
import com.example.ui.theme.CategoryStudy
import com.example.ui.theme.CategoryStudyBg
import com.example.ui.theme.CategoryWork
import com.example.ui.theme.CategoryWorkBg
import java.time.LocalDateTime

enum class EventCategory(val title: String, val color: Color, val bgColor: Color) {
    WORK("Công việc", CategoryWork, CategoryWorkBg),
    STUDY("Học tập", CategoryStudy, CategoryStudyBg),
    PLAY("Đi chơi", CategoryPlay, CategoryPlayBg)
}

data class Event(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val category: EventCategory,
    val isCompleted: Boolean = false,
    val reminderNote: String = "",
    val hasReminder: Boolean = false,
    val reminderTimeOffsetMins: Int = 15
)
