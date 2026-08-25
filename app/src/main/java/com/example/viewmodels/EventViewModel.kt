package com.example.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.EventRepository
import com.example.data.entities.*
import com.example.models.EventInstance
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class EventViewModel(private val repository: EventRepository) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    val boards = repository.allBoards.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val events = repository.allEvents.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    // Virtual instances for a range (e.g., current month)
    fun getInstancesForRange(startDate: LocalDate, endDate: LocalDate): Flow<List<EventInstance>> {
        return combine(
            repository.allEvents,
            repository.allBoards,
            repository.allCompletions
        ) { events, boards, completions ->
            val instances = mutableListOf<EventInstance>()
            val boardMap = boards.associateBy { it.id }
            val completionMap = completions.groupBy { it.eventId }
            
            events.forEach { event ->
                val eventCompletions = completionMap[event.id] ?: emptyList()
                generateInstances(event, boardMap[event.boardId], eventCompletions, startDate, endDate, instances)
            }
            instances
        }
    }

    private fun generateInstances(
        event: EventEntity,
        board: BoardEntity?,
        completions: List<EventCompletionEntity>,
        rangeStart: LocalDate,
        rangeEnd: LocalDate,
        output: MutableList<EventInstance>
    ) {
        val eventStart = LocalDate.parse(event.startDate)
        val repeatEnd = event.repeatEndDate?.let { LocalDate.parse(it) } ?: LocalDate.MAX
        
        val actualStart = if (eventStart.isAfter(rangeStart)) eventStart else rangeStart
        val actualEnd = if (repeatEnd.isBefore(rangeEnd)) repeatEnd else rangeEnd
        
        if (actualStart.isAfter(actualEnd)) return

        val completionDates = completions.filter { it.isCompleted }.map { it.occurrenceDate }.toSet()

        var current = eventStart
        while (current.isBefore(actualEnd.plusDays(1))) {
            if (current.isAfter(rangeStart.minusDays(1)) && current.isBefore(rangeEnd.plusDays(1))) {
                if (isDateMatchingRule(current, event)) {
                    val dateStr = current.format(DateTimeFormatter.ISO_DATE)
                    val isDone = if (event.repeatRule == "NONE") event.isCompleted else completionDates.contains(dateStr)
                    output.add(EventInstance(event, board, dateStr, isDone))
                }
            }
            
            // Optimization: jump to next possible date
            current = when (event.repeatRule) {
                "DAILY" -> current.plusDays(1)
                "WEEKLY" -> current.plusDays(1) // Can be optimized based on repeatDays
                "MONTHLY" -> current.plusMonths(1)
                else -> {
                    if (current == eventStart) current.plusDays(1) else break
                }
            }
            if (event.repeatRule == "NONE" && current.isAfter(eventStart)) break
        }
    }

    private fun isDateMatchingRule(date: LocalDate, event: EventEntity): Boolean {
        if (event.repeatRule == "NONE") return date == LocalDate.parse(event.startDate)
        if (event.repeatRule == "DAILY") return true
        if (event.repeatRule == "WEEKLY") {
            val days = event.repeatDays?.split(",")?.map { it.trim() } ?: return true
            val dayOfWeek = when (date.dayOfWeek.name) {
                "MONDAY" -> "MON"
                "TUESDAY" -> "TUE"
                "WEDNESDAY" -> "WED"
                "THURSDAY" -> "THU"
                "FRIDAY" -> "FRI"
                "SATURDAY" -> "SAT"
                "SUNDAY" -> "SUN"
                else -> ""
            }
            return days.contains(dayOfWeek)
        }
        if (event.repeatRule == "MONTHLY") {
            val eventStart = LocalDate.parse(event.startDate)
            return date.dayOfMonth == eventStart.dayOfMonth
        }
        return false
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun addEvent(event: EventEntity, reminders: List<ReminderEntity>) {
        viewModelScope.launch {
            repository.insertEvent(event, reminders)
        }
    }

    fun updateEvent(event: EventEntity, reminders: List<ReminderEntity>) {
        viewModelScope.launch {
            repository.updateEvent(event, reminders)
        }
    }

    fun deleteEvent(event: EventEntity) {
        viewModelScope.launch {
            repository.deleteEvent(event)
        }
    }

    fun toggleCompletion(eventId: Int, date: String) {
        viewModelScope.launch {
            repository.toggleCompletion(eventId, date)
        }
    }
}

class EventViewModelFactory(private val repository: EventRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EventViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EventViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
