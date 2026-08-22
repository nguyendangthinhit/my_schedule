package com.example.viewmodel

import androidx.lifecycle.ViewModel
import com.example.models.Event
import com.example.models.EventCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

class ScheduleViewModel : ViewModel() {

    private val _categories = MutableStateFlow<List<EventCategory>>(EventCategory.DEFAULT_CATEGORIES)
    val categories: StateFlow<List<EventCategory>> = _categories.asStateFlow()

    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events.asStateFlow()

    private val _selectedDate = MutableStateFlow<LocalDate>(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    init {
        loadInitialSampleEvents()
    }

    private fun loadInitialSampleEvents() {
        val today = LocalDate.now()
        val sampleEvents = mutableListOf<Event>()

        // Events for today
        sampleEvents.add(
            Event(
                id = "today_1",
                title = "Tập thể dục buổi sáng",
                startTime = today.atTime(6, 0),
                endTime = today.atTime(7, 0),
                category = EventCategory.PLAY,
                isCompleted = true,
                reminderNote = "Chạy bộ 3km quanh công viên",
                hasReminder = true
            )
        )
        sampleEvents.add(
            Event(
                id = "today_2",
                title = "Họp dự án UI/UX với team",
                startTime = today.atTime(9, 0),
                endTime = today.atTime(10, 30),
                category = EventCategory.WORK,
                isCompleted = false,
                reminderNote = "Chuẩn bị slide thiết kế Figma mới nhất",
                hasReminder = true
            )
        )
        sampleEvents.add(
            Event(
                id = "today_3",
                title = "Học Jetpack Compose & Kotlin",
                startTime = today.atTime(11, 30),
                endTime = today.atTime(12, 30),
                category = EventCategory.STUDY,
                isCompleted = false,
                reminderNote = "Xem lại bài giảng về State Management",
                hasReminder = false
            )
        )
        sampleEvents.add(
            Event(
                id = "today_4",
                title = "Gặp gỡ khách hàng & đối tác",
                startTime = today.atTime(14, 0),
                endTime = today.atTime(15, 30),
                category = EventCategory.MEETING,
                isCompleted = false,
                reminderNote = "Bàn giao tài liệu thiết kế hợp đồng",
                hasReminder = true
            )
        )
        sampleEvents.add(
            Event(
                id = "today_5",
                title = "Đi bơi & rèn luyện sức khỏe",
                startTime = today.atTime(17, 30),
                endTime = today.atTime(19, 0),
                category = EventCategory.PLAY,
                isCompleted = false,
                reminderNote = "Mang theo đồ bơi và khăn",
                hasReminder = true
            )
        )
        sampleEvents.add(
            Event(
                id = "today_6",
                title = "Đọc sách và ôn tập buổi tối",
                startTime = today.atTime(21, 0),
                endTime = today.atTime(22, 30),
                category = EventCategory.STUDY,
                isCompleted = false,
                reminderNote = "Đọc chương 4 sách Clean Code",
                hasReminder = false
            )
        )

        // Events for other days of the week & month
        val monday = today.minusDays(today.dayOfWeek.value.toLong() - 1)
        for (i in 0..6) {
            val day = monday.plusDays(i.toLong())
            if (day != today) {
                sampleEvents.add(
                    Event(
                        title = "Lập kế hoạch công việc T${i + 2}",
                        startTime = day.atTime(8, 30),
                        endTime = day.atTime(9, 30),
                        category = EventCategory.WORK,
                        isCompleted = false
                    )
                )
                sampleEvents.add(
                    Event(
                        title = "Học ngoại ngữ & từ vựng",
                        startTime = day.atTime(13, 0),
                        endTime = day.atTime(14, 0),
                        category = EventCategory.STUDY,
                        isCompleted = false
                    )
                )
                if (i % 2 == 0) {
                    sampleEvents.add(
                        Event(
                            title = "Giao lưu thể thao",
                            startTime = day.atTime(16, 30),
                            endTime = day.atTime(18, 0),
                            category = EventCategory.PLAY,
                            isCompleted = false
                        )
                    )
                }
                if (i == 4) { // Friday
                    sampleEvents.add(
                        Event(
                            title = "Họp tổng kết tuần",
                            startTime = day.atTime(15, 0),
                            endTime = day.atTime(16, 30),
                            category = EventCategory.MEETING,
                            isCompleted = false
                        )
                    )
                    sampleEvents.add(
                        Event(
                            title = "Gặp gỡ bạn bè cuối tuần",
                            startTime = day.atTime(19, 0),
                            endTime = day.atTime(21, 30),
                            category = EventCategory.PLAY,
                            isCompleted = false
                        )
                    )
                }
            }
        }

        // Additional events across the month
        val currentMonth = YearMonth.from(today)
        for (d in listOf(5, 10, 15, 20, 25, 28)) {
            if (d <= currentMonth.lengthOfMonth()) {
                val mDate = currentMonth.atDay(d)
                if (mDate != today && !sampleEvents.any { it.date == mDate }) {
                    sampleEvents.add(
                        Event(
                            title = if (d == 28) "Nghỉ lễ Quốc gia" else "Đánh giá tiến độ tháng",
                            startTime = mDate.atTime(10, 0),
                            endTime = mDate.atTime(11, 30),
                            category = if (d == 28) EventCategory.HOLIDAY else EventCategory.WORK,
                            isCompleted = false
                        )
                    )
                    sampleEvents.add(
                        Event(
                            title = "Workshop chuyên môn",
                            startTime = mDate.atTime(14, 30),
                            endTime = mDate.atTime(16, 0),
                            category = EventCategory.STUDY,
                            isCompleted = false
                        )
                    )
                }
            }
        }

        _events.value = sampleEvents
    }

    fun setSelectedDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun toggleEventCompletion(eventId: String) {
        _events.value = _events.value.map { event ->
            if (event.id == eventId) {
                event.copy(isCompleted = !event.isCompleted)
            } else {
                event
            }
        }
    }

    fun addCategory(category: EventCategory) {
        _categories.value = _categories.value + category
    }

    fun addCategory(title: String, color: androidx.compose.ui.graphics.Color, bgColor: androidx.compose.ui.graphics.Color): EventCategory {
        val newCategory = EventCategory(
            title = title.trim(),
            color = color,
            bgColor = bgColor
        )
        _categories.value = _categories.value + newCategory
        return newCategory
    }

    fun deleteCategory(categoryId: String) {
        // Xóa danh mục và xóa luôn tất cả các event thuộc danh mục đó
        _events.value = _events.value.filter { it.category.id != categoryId }
        _categories.value = _categories.value.filter { it.id != categoryId }
    }

    fun addEvent(event: Event) {
        _events.value = _events.value + event
    }

    fun deleteEvent(eventId: String) {
        _events.value = _events.value.filter { it.id != eventId }
    }

    fun deleteEvents(eventIds: Collection<String>) {
        _events.value = _events.value.filterNot { it.id in eventIds }
    }
}
