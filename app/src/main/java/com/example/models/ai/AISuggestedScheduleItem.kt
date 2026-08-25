package com.example.models.ai

import com.example.models.Event
import com.example.models.EventCategory
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

data class AISuggestedScheduleItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val date: LocalDate = LocalDate.now(),
    val startTime: LocalTime = LocalTime.of(8, 0),
    val endTime: LocalTime = LocalTime.of(9, 0),
    val categoryType: String = "WORK", // WORK, STUDY, PLAY, MEETING
    val reminderNote: String = "",
    val isSelected: Boolean = true,
    val isAdded: Boolean = false
) {
    fun toEvent(availableCategories: List<EventCategory>): Event {
        val matchedCategory = when (categoryType.uppercase()) {
            "STUDY" -> availableCategories.find { it.title.contains("Học", ignoreCase = true) } ?: EventCategory.STUDY
            "PLAY", "ENTERTAINMENT", "EXERCISE", "FITNESS" -> availableCategories.find { 
                it.title.contains("chơi", ignoreCase = true) || it.title.contains("Giải trí", ignoreCase = true) 
            } ?: EventCategory.PLAY
            "MEETING" -> availableCategories.find { it.title.contains("họp", ignoreCase = true) } ?: EventCategory.MEETING
            else -> availableCategories.find { it.title.contains("Công việc", ignoreCase = true) } ?: EventCategory.WORK
        }

        val startDateTime = LocalDateTime.of(date, startTime)
        val endDateTime = if (endTime.isAfter(startTime)) {
            LocalDateTime.of(date, endTime)
        } else {
            startDateTime.plusHours(1)
        }

        return Event(
            id = 0,
            title = title,
            startTime = startDateTime,
            endTime = endDateTime,
            category = matchedCategory,
            isCompleted = false,
            reminderNote = if (reminderNote.isNotBlank()) reminderNote else "Lên lịch tự động bởi My Schedule AI",
            hasReminder = true,
            reminderTimeOffsetMins = 15
        )
    }
}
