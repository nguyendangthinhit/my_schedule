package com.example.widget

import android.content.Context
import com.example.R
import com.example.data.AppDatabase
import com.example.models.Event
import com.example.models.EventCategory
import com.example.util.DateUtils
import com.example.util.LunarCalendarHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class WidgetDisplayData(
    val microMiniDate: String,
    val fullDate: String,
    val weatherFull: String,
    val weatherShort: String,
    val dateHeader: String,
    val lunarDateStr: String,
    val upcomingEvent: WidgetEventItem?,
    val completedCount: Int,
    val totalCount: Int,
    val footerText: String,
    val footerCountOnly: String,
    val tasks: List<WidgetTaskItem>,
    val standardTasks: List<WidgetTaskItem>
)

data class WidgetEventItem(
    val id: Int,
    val title: String,
    val timeRange: String,
    val startTimeOnly: String,
    val categoryTitle: String,
    val dotRes: Int,
    val isCompleted: Boolean
)

data class WidgetTaskItem(
    val id: Int,
    val title: String,
    val timeRange: String,
    val categoryTitle: String,
    val dotRes: Int,
    val isCompleted: Boolean,
    val isPast: Boolean = false,
    val isNext: Boolean = false
)

object WidgetDataHelper {

    fun getCategoryDot(categoryName: String, title: String = ""): Int {
        val lowerCat = categoryName.lowercase()
        val lowerTitle = title.lowercase()
        return when {
            lowerTitle.contains("học") || lowerTitle.contains("toán") || lowerTitle.contains("anh") || lowerTitle.contains("bài") || lowerCat.contains("học") || lowerCat.contains("study") -> R.drawable.widget_dot_green
            lowerTitle.contains("cơm") || lowerTitle.contains("ăn") || lowerTitle.contains("uống") || lowerTitle.contains("cafe") || lowerCat.contains("cá nhân") || lowerCat.contains("personal") -> R.drawable.widget_dot_blue
            lowerTitle.contains("chợ") || lowerTitle.contains("mua") || lowerTitle.contains("họp") || lowerCat.contains("công việc") || lowerCat.contains("work") -> R.drawable.widget_dot_red
            lowerTitle.contains("tập") || lowerTitle.contains("gym") || lowerTitle.contains("chạy") || lowerCat.contains("thể thao") -> R.drawable.widget_dot_orange
            else -> R.drawable.widget_dot_purple
        }
    }

    suspend fun loadWidgetData(context: Context): WidgetDisplayData = withContext(Dispatchers.IO) {
        val database = AppDatabase.getDatabase(context)
        val dao = database.scheduleDao()

        val boards = try { dao.getAllBoards().first() } catch (e: Exception) { emptyList() }
        val events = try { dao.getAllEvents().first() } catch (e: Exception) { emptyList() }
        val holidays = try { dao.getAllHolidays().first() } catch (e: Exception) { emptyList() }
        val completions = try { dao.getAllCompletions().first() } catch (e: Exception) { emptyList() }

        val boardMap = boards.associateBy { it.id }
        val today = LocalDate.now()
        val now = LocalDateTime.now()
        val todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)

        val completionMap = completions.filter { it.occurrenceDate == todayStr }.associateBy { it.eventId }

        // Filter events occurring today
        val todayEvents = mutableListOf<Event>()

        for (entity in events) {
            val eventDate = try { LocalDate.parse(entity.startDate) } catch (e: Exception) { null } ?: continue
            val isRecurring = entity.repeatRule != null && entity.repeatRule != "NONE"
            var occursToday = false

            if (!isRecurring) {
                occursToday = eventDate == today
            } else {
                val repeatEndDate = entity.repeatEndDate?.let {
                    try { LocalDate.parse(it) } catch (e: Exception) { null }
                }
                if (repeatEndDate == null || !today.isAfter(repeatEndDate)) {
                    when (entity.repeatRule) {
                        "DAILY" -> occursToday = !today.isBefore(eventDate)
                        "WEEKLY" -> {
                            if (!today.isBefore(eventDate)) {
                                val dayOfWeek = today.dayOfWeek.name.take(3)
                                occursToday = entity.repeatDays?.contains(dayOfWeek, ignoreCase = true) == true || eventDate.dayOfWeek == today.dayOfWeek
                            }
                        }
                        "MONTHLY" -> occursToday = !today.isBefore(eventDate) && today.dayOfMonth == eventDate.dayOfMonth
                    }
                }
            }

            if (occursToday) {
                val startTime = try {
                    val time = entity.startTime?.let { LocalTime.parse(it) } ?: LocalTime.of(8, 40)
                    today.atTime(time)
                } catch (e: Exception) {
                    today.atTime(8, 40)
                }

                val endTime = try {
                    val time = entity.endTime?.let { LocalTime.parse(it) } ?: startTime.plusHours(1).plusMinutes(30).toLocalTime()
                    today.atTime(time)
                } catch (e: Exception) {
                    startTime.plusHours(1).plusMinutes(30)
                }

                val isCompleted = completionMap[entity.id]?.isCompleted ?: entity.isCompleted
                val board = boardMap[entity.boardId]
                val categoryTitle = board?.name ?: "Học tập"
                val category = if (board != null) {
                    EventCategory(id = board.id, title = board.name, color = androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(board.colorHex)), bgColor = androidx.compose.ui.graphics.Color.White)
                } else EventCategory.STUDY

                todayEvents.add(
                    Event(
                        id = entity.id,
                        title = entity.title,
                        startTime = startTime,
                        endTime = endTime,
                        category = category,
                        isCompleted = isCompleted,
                        reminderNote = entity.description ?: ""
                    )
                )
            }
        }

        // Add today's holidays if any
        for (holiday in holidays) {
            val holidayDate = try { LocalDate.parse(holiday.date) } catch (e: Exception) { null }
            if (holidayDate == today) {
                todayEvents.add(
                    Event(
                        id = if (holiday.id > 0) 100000 + holiday.id else 999999,
                        title = holiday.name,
                        startTime = today.atTime(8, 0),
                        endTime = today.atTime(22, 0),
                        category = EventCategory.HOLIDAY,
                        isCompleted = false,
                        reminderNote = "Ngày lễ"
                    )
                )
            }
        }

        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        // Find upcoming uncompleted event nearest to current time
        val upcomingCandidate = todayEvents
            .filter { !it.isCompleted && it.endTime.isAfter(now.minusMinutes(15)) }
            .minByOrNull { it.startTime }
            ?: todayEvents.filter { !it.isCompleted }.minByOrNull { it.startTime }
            ?: todayEvents.firstOrNull()

        val upcomingItem = upcomingCandidate?.let { event ->
            val cleanTitle = event.title
            val categoryName = if (event.category.title.isNotBlank()) event.category.title else "Học tập"
            WidgetEventItem(
                id = event.id,
                title = cleanTitle,
                timeRange = "${event.startTime.format(timeFormatter)} - ${event.endTime.format(timeFormatter)}",
                startTimeOnly = event.startTime.format(timeFormatter),
                categoryTitle = categoryName,
                dotRes = getCategoryDot(categoryName, cleanTitle),
                isCompleted = event.isCompleted
            )
        }

        // General Task Items: sorted by completed status then startTime
        val sortedTasks = todayEvents
            .sortedWith(compareBy({ it.isCompleted }, { it.startTime }))
            .map { event ->
                val categoryName = if (event.category.title.isNotBlank()) event.category.title else "Học tập"
                WidgetTaskItem(
                    id = event.id,
                    title = event.title,
                    timeRange = "${event.startTime.format(timeFormatter)} - ${event.endTime.format(timeFormatter)}",
                    categoryTitle = categoryName,
                    dotRes = getCategoryDot(categoryName, event.title),
                    isCompleted = event.isCompleted
                )
            }

        // Standard Widget Task Items:
        // "hiển thị 1 công việc trước đó gần với thời gian hiện tại nhất và các công việc còn lại trong ngày"
        val pastEvents = todayEvents.filter { it.endTime.isBefore(now) }
        val remainingEvents = todayEvents.filter { !it.endTime.isBefore(now) }

        val closestPastEvent = pastEvents.maxByOrNull { it.endTime }

        val standardList = mutableListOf<WidgetTaskItem>()

        // 1 công việc trước đó gần nhất
        if (closestPastEvent != null) {
            val categoryName = if (closestPastEvent.category.title.isNotBlank()) closestPastEvent.category.title else "Học tập"
            standardList.add(
                WidgetTaskItem(
                    id = closestPastEvent.id,
                    title = closestPastEvent.title,
                    timeRange = "${closestPastEvent.startTime.format(timeFormatter)} - ${closestPastEvent.endTime.format(timeFormatter)}",
                    categoryTitle = categoryName,
                    dotRes = getCategoryDot(categoryName, closestPastEvent.title),
                    isCompleted = closestPastEvent.isCompleted,
                    isPast = true,
                    isNext = false
                )
            )
        }

        // Các công việc còn lại trong ngày (sắp xếp theo startTime)
        val sortedRemaining = remainingEvents.sortedBy { it.startTime }
        val nextCandidateId = sortedRemaining.firstOrNull { !it.isCompleted }?.id ?: sortedRemaining.firstOrNull()?.id
        for (event in sortedRemaining) {
            val categoryName = if (event.category.title.isNotBlank()) event.category.title else "Học tập"
            standardList.add(
                WidgetTaskItem(
                    id = event.id,
                    title = event.title,
                    timeRange = "${event.startTime.format(timeFormatter)} - ${event.endTime.format(timeFormatter)}",
                    categoryTitle = categoryName,
                    dotRes = getCategoryDot(categoryName, event.title),
                    isCompleted = event.isCompleted,
                    isPast = false,
                    isNext = event.id == nextCandidateId
                )
            )
        }

        val finalStandardTasks = if (standardList.isNotEmpty()) {
            standardList
        } else {
            todayEvents.sortedBy { it.startTime }.map { event ->
                val categoryName = if (event.category.title.isNotBlank()) event.category.title else "Học tập"
                WidgetTaskItem(
                    id = event.id,
                    title = event.title,
                    timeRange = "${event.startTime.format(timeFormatter)} - ${event.endTime.format(timeFormatter)}",
                    categoryTitle = categoryName,
                    dotRes = getCategoryDot(categoryName, event.title),
                    isCompleted = event.isCompleted,
                    isPast = event.endTime.isBefore(now),
                    isNext = false
                )
            }
        }

        val completedCount = todayEvents.count { it.isCompleted }
        val totalCount = todayEvents.size

        val lunarDate = LunarCalendarHelper.convertSolarToLunar(today)
        val lunarStr = "ÂL ${lunarDate.day}/${lunarDate.month}"

        val todayWeather = try {
            dao.getWeatherForDate(todayStr)
        } catch (e: Exception) {
            null
        }

        val weatherEmoji = todayWeather?.weatherEmoji ?: "☀️"
        val weatherTemp = "${todayWeather?.tempAvg ?: 32}°C"
        val weatherDesc = todayWeather?.conditionDescription ?: "Nắng quang đãng"
        val weatherFull = "$weatherEmoji $weatherTemp • $weatherDesc"
        val weatherShort = "$weatherEmoji $weatherTemp"

        WidgetDisplayData(
            microMiniDate = DateUtils.formatWidgetMicroMiniDate(today),
            fullDate = DateUtils.formatWidgetFullDate(today),
            weatherFull = weatherFull,
            weatherShort = weatherShort,
            dateHeader = DateUtils.formatWidgetHeaderDate(today),
            lunarDateStr = lunarStr,
            upcomingEvent = upcomingItem,
            completedCount = completedCount,
            totalCount = totalCount,
            footerText = "$completedCount/$totalCount đã hoàn thành",
            footerCountOnly = "$completedCount/$totalCount",
            tasks = sortedTasks,
            standardTasks = finalStandardTasks
        )
    }
}
