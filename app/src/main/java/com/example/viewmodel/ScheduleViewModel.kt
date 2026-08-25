package com.example.viewmodel

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.EventRepository
import com.example.data.entities.BoardEntity
import com.example.data.entities.EventEntity
import com.example.data.entities.HolidayEntity
import com.example.data.entities.ReminderEntity
import com.example.data.entities.WeatherForecastEntity
import com.example.models.Event
import com.example.models.EventCategory
import com.example.network.ApiSyncResult
import com.example.network.CalendarSyncRepository
import com.example.network.WeatherSyncRepository
import com.example.util.NotificationHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ScheduleViewModel(private val repository: EventRepository) : ViewModel() {

    // Toggle for displaying Lunar calendar in views (Month, Week, Day)
    private val _showLunarCalendar = MutableStateFlow(true)
    val showLunarCalendar: StateFlow<Boolean> = _showLunarCalendar.asStateFlow()

    fun setShowLunarCalendar(show: Boolean) {
        _showLunarCalendar.value = show
    }

    // Theme Mode state (LIGHT, DARK, SYSTEM)
    private val _themeMode = MutableStateFlow(com.example.util.ThemeMode.SYSTEM)
    val themeMode: StateFlow<com.example.util.ThemeMode> = _themeMode.asStateFlow()

    fun initThemeSettings(context: Context) {
        _themeMode.value = com.example.util.ThemeHelper.getThemeMode(context)
    }

    fun setThemeMode(context: Context, mode: com.example.util.ThemeMode) {
        _themeMode.value = mode
        com.example.util.ThemeHelper.setThemeMode(context, mode)
        com.example.widget.WidgetUpdateHelper.updateAllWidgets(context)
    }

    // Notification and sound settings state
    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _soundEnabled = MutableStateFlow(true)
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    fun initNotificationSettings(context: Context) {
        _notificationsEnabled.value = NotificationHelper.isNotificationsEnabled(context)
        _soundEnabled.value = NotificationHelper.isSoundEnabled(context)
    }

    fun setNotificationsEnabled(context: Context, enabled: Boolean) {
        _notificationsEnabled.value = enabled
        NotificationHelper.setNotificationsEnabled(context, enabled)
        if (enabled) {
            // Reschedule active reminders
            events.value.filter { it.hasReminder && !it.isCompleted }.forEach { event ->
                NotificationHelper.scheduleEventReminder(
                    context = context,
                    eventId = event.id,
                    title = event.title,
                    note = event.reminderNote,
                    startTime = event.startTime,
                    offsetMinutes = event.reminderTimeOffsetMins
                )
            }
        }
    }

    fun setSoundEnabled(context: Context, enabled: Boolean) {
        _soundEnabled.value = enabled
        NotificationHelper.setSoundEnabled(context, enabled)
    }

    // Categories backed by Room boards
    val categories: StateFlow<List<EventCategory>> = repository.allBoards.map { entities ->
        if (entities.isEmpty()) {
            EventCategory.DEFAULT_CATEGORIES
        } else {
            entities.map { it.toUiModel() }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, EventCategory.DEFAULT_CATEGORIES)

    // Holidays stored in Room DB
    val holidays: StateFlow<List<HolidayEntity>> = repository.allHolidays
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // API Sync state
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncResult = MutableStateFlow<ApiSyncResult?>(null)
    val syncResult: StateFlow<ApiSyncResult?> = _syncResult.asStateFlow()

    // Combined events: User-created events + Holiday events from Room DB
    val events: StateFlow<List<Event>> = combine(
        repository.allEvents,
        repository.allBoards,
        repository.allHolidays
    ) { eventEntities, boardEntities, holidayEntities ->
        val boardMap = (if (boardEntities.isNotEmpty()) boardEntities.map { it.toUiModel() } else EventCategory.DEFAULT_CATEGORIES)
            .associateBy { it.id }

        val userEvents = eventEntities.map { entity ->
            val board = boardMap[entity.boardId] ?: EventCategory.WORK
            entity.toUiModel(board)
        }

        val holidayEvents = holidayEntities.map { holiday ->
            val date = try {
                LocalDate.parse(holiday.date)
            } catch (e: Exception) {
                LocalDate.now()
            }
            val eventId = if (holiday.id > 0) 100000 + holiday.id else ((holiday.date.hashCode() xor holiday.name.hashCode()) and 0x7FFFFFFF)
            Event(
                id = eventId,
                title = " ${holiday.name}",
                startTime = date.atTime(0, 0),
                endTime = date.atTime(23, 59),
                category = EventCategory.HOLIDAY,
                isCompleted = false,
                reminderNote = "Ngày lễ: ${holiday.name} (${if (holiday.isLunarBased) "Âm lịch" else "Dương lịch"})",
                hasReminder = false
            )
        }

        userEvents + holidayEvents
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _selectedDate = MutableStateFlow<LocalDate>(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    // 7-day Weather Forecast State (rolling from today to today + 6 days)
    val weatherMap: StateFlow<Map<LocalDate, WeatherForecastEntity>> = repository.allWeatherForecasts
        .map { list ->
            val today = LocalDate.now()
            val end7 = today.plusDays(6)
            list.mapNotNull { item ->
                try {
                    val date = LocalDate.parse(item.date)
                    if (!date.isBefore(today) && !date.isAfter(end7)) {
                        date to item
                    } else null
                } catch (e: Exception) {
                    null
                }
            }.toMap()
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    private val weatherSyncRepo = WeatherSyncRepository(repository)

    fun refreshWeather() {
        viewModelScope.launch {
            try {
                weatherSyncRepo.sync7DaysWeather()
            } catch (e: Exception) {
                // Keep existing cached weather if offline
            }
        }
    }

    init {
        // Populate initial boards if empty
        viewModelScope.launch {
            repository.allBoards.first().let { currentBoards ->
                if (currentBoards.isEmpty()) {
                    EventCategory.DEFAULT_CATEGORIES.take(4).forEach { cat ->
                        repository.insertBoard(cat.toEntity())
                    }
                }
            }
        }
        // Sync 7-day weather forecast into database
        refreshWeather()
    }

    fun setSelectedDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun syncHolidaysFromApi(context: Context, year: Int = LocalDate.now().year) {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val syncRepo = CalendarSyncRepository(context)
                val result = syncRepo.fetchAndSyncHolidays(year)
                _syncResult.value = result
            } catch (e: Exception) {
                _syncResult.value = ApiSyncResult.Error("Lỗi đồng bộ: ${e.localizedMessage}", emptyList())
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun clearSyncResult() {
        _syncResult.value = null
    }

    fun toggleEventCompletion(event: Event, context: Context? = null) {
        // If it's a holiday event, we don't necessarily update DB user event, or we can toggle
        if (event.category.id == EventCategory.HOLIDAY.id) return

        val newCompleted = !event.isCompleted
        viewModelScope.launch {
            repository.updateEvent(
                event.toEntity().copy(isCompleted = newCompleted),
                if (event.hasReminder) listOf(ReminderEntity(eventId = event.id, remindBeforeMinutes = event.reminderTimeOffsetMins)) else emptyList()
            )
            val dateStr = event.startTime.toLocalDate().toString()
            repository.setCompletion(event.id, dateStr, newCompleted)
            if (context != null) {
                com.example.widget.WidgetUpdateHelper.updateAllWidgets(context)
                if (newCompleted) {
                    NotificationHelper.cancelEventReminder(context, event.id)
                } else if (event.hasReminder) {
                    NotificationHelper.scheduleEventReminder(
                        context = context,
                        eventId = event.id,
                        title = event.title,
                        note = event.reminderNote,
                        startTime = event.startTime,
                        offsetMinutes = event.reminderTimeOffsetMins
                    )
                }
            }
        }
    }

    fun addEvent(event: Event, context: Context? = null) {
        viewModelScope.launch {
            val reminders = if (event.hasReminder) {
                listOf(ReminderEntity(eventId = 0, remindBeforeMinutes = event.reminderTimeOffsetMins))
            } else emptyList()
            val newId = repository.insertEvent(event.toEntity(), reminders)
            if (context != null) {
                com.example.widget.WidgetUpdateHelper.updateAllWidgets(context)
                if (event.hasReminder) {
                    NotificationHelper.scheduleEventReminder(
                        context = context,
                        eventId = newId,
                        title = event.title,
                        note = event.reminderNote,
                        startTime = event.startTime,
                        offsetMinutes = event.reminderTimeOffsetMins
                    )
                }
            }
        }
    }

    fun addEvents(events: List<Event>, context: Context? = null) {
        viewModelScope.launch {
            events.forEach { event ->
                val reminders = if (event.hasReminder) {
                    listOf(ReminderEntity(eventId = 0, remindBeforeMinutes = event.reminderTimeOffsetMins))
                } else emptyList()
                val newId = repository.insertEvent(event.toEntity(), reminders)
                if (context != null && event.hasReminder) {
                    NotificationHelper.scheduleEventReminder(
                        context = context,
                        eventId = newId,
                        title = event.title,
                        note = event.reminderNote,
                        startTime = event.startTime,
                        offsetMinutes = event.reminderTimeOffsetMins
                    )
                }
            }
            if (context != null) {
                com.example.widget.WidgetUpdateHelper.updateAllWidgets(context)
            }
        }
    }

    fun updateEvent(updatedEvent: Event, context: Context? = null) {
        viewModelScope.launch {
            val reminders = if (updatedEvent.hasReminder) {
                listOf(ReminderEntity(eventId = updatedEvent.id, remindBeforeMinutes = updatedEvent.reminderTimeOffsetMins))
            } else emptyList()
            repository.updateEvent(updatedEvent.toEntity(), reminders)
            if (context != null) {
                com.example.widget.WidgetUpdateHelper.updateAllWidgets(context)
                NotificationHelper.cancelEventReminder(context, updatedEvent.id)
                if (updatedEvent.hasReminder && !updatedEvent.isCompleted) {
                    NotificationHelper.scheduleEventReminder(
                        context = context,
                        eventId = updatedEvent.id,
                        title = updatedEvent.title,
                        note = updatedEvent.reminderNote,
                        startTime = updatedEvent.startTime,
                        offsetMinutes = updatedEvent.reminderTimeOffsetMins
                    )
                }
            }
        }
    }

    fun deleteEvent(eventId: Int, context: Context? = null) {
        viewModelScope.launch {
            val event = events.value.find { it.id == eventId }
            if (event != null && event.category.id != EventCategory.HOLIDAY.id) {
                repository.deleteEvent(event.toEntity())
                if (context != null) {
                    com.example.widget.WidgetUpdateHelper.updateAllWidgets(context)
                    NotificationHelper.cancelEventReminder(context, eventId)
                }
            }
        }
    }

    fun addCategory(title: String, color: Color, bgColor: Color): EventCategory? {
        val newCategory = EventCategory(
            title = title.trim(),
            color = color,
            bgColor = bgColor
        )
        viewModelScope.launch {
            repository.insertBoard(newCategory.toEntity())
        }
        return newCategory
    }

    fun deleteCategory(categoryId: Int) {
        viewModelScope.launch {
            val category = categories.value.find { it.id == categoryId }
            category?.let { repository.deleteBoard(it.toEntity()) }
        }
    }

    fun deleteEvents(eventIds: Collection<Int>) {
        viewModelScope.launch {
            eventIds.forEach { id ->
                val event = events.value.find { it.id == id }
                if (event != null && event.category.id != EventCategory.HOLIDAY.id) {
                    repository.deleteEvent(event.toEntity())
                }
            }
        }
    }

    // Mappers
    private fun BoardEntity.toUiModel() = EventCategory(
        id = id,
        title = name,
        color = try {
            Color(android.graphics.Color.parseColor(colorHex))
        } catch (e: Exception) {
            Color(0xFF6366F1)
        },
        bgColor = try {
            Color(android.graphics.Color.parseColor(colorHex)).copy(alpha = 0.12f)
        } catch (e: Exception) {
            Color(0xFFEEF2FF)
        }
    )

    private fun EventCategory.toEntity() = BoardEntity(
        id = id,
        name = title,
        colorHex = String.format("#%06X", (0xFFFFFF and color.toArgb()))
    )

    private fun EventEntity.toUiModel(category: EventCategory) = Event(
        id = id,
        title = title,
        startTime = try {
            LocalDateTime.parse("${startDate}T${startTime ?: "00:00"}")
        } catch (e: Exception) {
            LocalDate.parse(startDate).atStartOfDay()
        },
        endTime = try {
            LocalDateTime.parse("${startDate}T${endTime ?: "23:59"}")
        } catch (e: Exception) {
            LocalDate.parse(startDate).atTime(23, 59)
        },
        category = category,
        isCompleted = isCompleted,
        reminderNote = description ?: "",
        hasReminder = hasReminder
    )

    private fun Event.toEntity() = EventEntity(
        id = id,
        title = title,
        description = reminderNote,
        boardId = category.id,
        startDate = startTime.format(DateTimeFormatter.ISO_LOCAL_DATE),
        startTime = startTime.format(DateTimeFormatter.ISO_LOCAL_TIME).substring(0, 5),
        endTime = endTime.format(DateTimeFormatter.ISO_LOCAL_TIME).substring(0, 5),
        isCompleted = isCompleted,
        hasReminder = hasReminder
    )
}

class ScheduleViewModelFactory(private val repository: EventRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScheduleViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ScheduleViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
