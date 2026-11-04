package com.example.data

import com.example.data.dao.ScheduleDao
import com.example.data.entities.*
import kotlinx.coroutines.flow.Flow

class EventRepository internal constructor(
    internal val scheduleDao: ScheduleDao,
    private val timeProvider: TimeProvider = SystemTimeProvider
) {
    // ==========================================
    // FLOWS (READ)
    // ==========================================
    val allBoards: Flow<List<BoardEntity>> = scheduleDao.getAllBoards()
    val allEvents: Flow<List<EventEntity>> = scheduleDao.getAllEvents()
    val allHolidays: Flow<List<HolidayEntity>> = scheduleDao.getAllHolidays()
    val allCompletions: Flow<List<EventCompletionEntity>> = scheduleDao.getAllCompletions()
    val allWeatherForecasts: Flow<List<WeatherForecastEntity>> = scheduleDao.getAllWeatherForecasts()

    // ==========================================
    // BOARDS (CENTRALIZED WRITES & READS)
    // ==========================================
    suspend fun getAllBoardsList(): List<BoardEntity> = scheduleDao.getAllBoardsList()

    suspend fun getBoardById(boardId: String): BoardEntity? = scheduleDao.getBoardById(boardId)

    suspend fun insertBoard(board: BoardEntity): String {
        val now = timeProvider.currentTimeMillis()
        if (scheduleDao.getBoardCount() < 20) {
            val entity = board.copy(
                updatedAt = now,
                deletedAt = null,
                dirty = true
            )
            scheduleDao.insertBoard(entity)
            return entity.id
        }
        return board.id
    }

    suspend fun insertBoardDirect(board: BoardEntity) {
        val now = timeProvider.currentTimeMillis()
        scheduleDao.insertBoard(
            board.copy(
                updatedAt = now,
                deletedAt = null,
                dirty = true
            )
        )
    }

    suspend fun updateBoard(board: BoardEntity) {
        val now = timeProvider.currentTimeMillis()
        scheduleDao.updateBoard(
            board.copy(
                updatedAt = now,
                dirty = true
            )
        )
    }

    /**
     * Soft delete board. If migrateToBoardId is provided, migrates events to the target board.
     * Otherwise, soft-deletes all events belonging to this board (along with their reminders and completions).
     */
    suspend fun deleteBoard(board: BoardEntity, migrateToBoardId: String? = null) {
        val now = timeProvider.currentTimeMillis()
        if (migrateToBoardId != null) {
            scheduleDao.migrateEventsToNewBoard(board.id, migrateToBoardId, now)
        } else {
            // Cascade soft delete: reminders, completions, and events of the board
            scheduleDao.softDeleteRemindersByBoard(board.id, now)
            scheduleDao.softDeleteCompletionsByBoard(board.id, now)
            scheduleDao.softDeleteEventsByBoard(board.id, now)
        }
        scheduleDao.softDeleteBoard(board.id, now)
    }

    // ==========================================
    // EVENTS (CENTRALIZED WRITES & READS)
    // ==========================================
    suspend fun getAllEventsList(): List<EventEntity> = scheduleDao.getAllEventsList()

    suspend fun getEventById(eventId: String): EventEntity? = scheduleDao.getEventById(eventId)

    suspend fun getAllActiveEventsWithReminders(): List<EventEntity> =
        scheduleDao.getAllActiveEventsWithReminders()

    suspend fun getAllSoftDeletedEventIds(): List<String> =
        scheduleDao.getAllSoftDeletedEventIds()

    suspend fun insertEvent(event: EventEntity, reminders: List<ReminderEntity>): String {
        val now = timeProvider.currentTimeMillis()
        val eventToInsert = event.copy(
            createdAt = if (event.createdAt <= 0) now else event.createdAt,
            updatedAt = now,
            deletedAt = null,
            dirty = true
        )
        scheduleDao.insertEvent(eventToInsert)

        // Process reminders (revive or insert)
        reminders.forEach { reminder ->
            val existing = scheduleDao.getReminderIncludeDeleted(eventToInsert.id, reminder.remindBeforeMinutes)
            if (existing != null) {
                scheduleDao.insertReminder(
                    existing.copy(
                        isTriggered = false,
                        updatedAt = now,
                        deletedAt = null,
                        dirty = true
                    )
                )
            } else {
                scheduleDao.insertReminder(
                    reminder.copy(
                        eventId = eventToInsert.id,
                        updatedAt = now,
                        deletedAt = null,
                        dirty = true
                    )
                )
            }
        }
        return eventToInsert.id
    }

    suspend fun updateEvent(event: EventEntity, reminders: List<ReminderEntity>) {
        val now = timeProvider.currentTimeMillis()
        val eventToUpdate = event.copy(
            updatedAt = now,
            dirty = true
        )
        scheduleDao.updateEvent(eventToUpdate)

        val existingReminders = scheduleDao.getRemindersForEvent(event.id)
        val targetMinutes = reminders.map { it.remindBeforeMinutes }.toSet()

        // Soft-delete reminders that are no longer requested
        existingReminders.filter { it.remindBeforeMinutes !in targetMinutes }.forEach {
            scheduleDao.softDeleteReminder(it.id, now)
        }

        // Insert or revive requested reminders
        reminders.forEach { reminder ->
            val existing = scheduleDao.getReminderIncludeDeleted(event.id, reminder.remindBeforeMinutes)
            if (existing != null) {
                scheduleDao.insertReminder(
                    existing.copy(
                        isTriggered = false,
                        updatedAt = now,
                        deletedAt = null,
                        dirty = true
                    )
                )
            } else {
                scheduleDao.insertReminder(
                    reminder.copy(
                        eventId = event.id,
                        updatedAt = now,
                        deletedAt = null,
                        dirty = true
                    )
                )
            }
        }
    }

    suspend fun deleteEvent(event: EventEntity) {
        deleteEventById(event.id)
    }

    suspend fun deleteEventById(eventId: String) {
        val now = timeProvider.currentTimeMillis()
        // Soft delete child reminders & completions first
        scheduleDao.softDeleteRemindersForEvent(eventId, now)
        scheduleDao.softDeleteCompletionsForEvent(eventId, now)
        // Soft delete event
        scheduleDao.softDeleteEvent(eventId, now)
    }

    // ==========================================
    // REMINDERS (CENTRALIZED WRITES & READS)
    // ==========================================
    suspend fun getRemindersForEvent(eventId: String): List<ReminderEntity> =
        scheduleDao.getRemindersForEvent(eventId)

    suspend fun getActiveRemindersWithEvents(): List<ReminderEntity> =
        scheduleDao.getActiveRemindersWithEvents()

    // ==========================================
    // COMPLETIONS (CENTRALIZED WRITES & REVIVE)
    // ==========================================
    /**
     * Toggles completion for a specific event on a specific date.
     * If the completion already exists (even if soft-deleted), it revives it on check,
     * or soft-deletes it on uncheck. This prevents duplicate rows under unique constraints.
     */
    suspend fun toggleCompletion(eventId: String, date: String) {
        val now = timeProvider.currentTimeMillis()
        val completion = scheduleDao.getCompletionIncludeDeleted(eventId, date)
        if (completion != null) {
            if (completion.deletedAt == null && completion.isCompleted) {
                // Currently completed -> Untick (soft-delete)
                scheduleDao.insertCompletion(
                    completion.copy(
                        isCompleted = false,
                        deletedAt = now,
                        updatedAt = now,
                        dirty = true
                    )
                )
            } else {
                // Currently unticked or soft-deleted -> Revive!
                scheduleDao.insertCompletion(
                    completion.copy(
                        isCompleted = true,
                        completedAt = now,
                        updatedAt = now,
                        deletedAt = null,
                        dirty = true
                    )
                )
            }
        } else {
            // First time completing this date
            scheduleDao.insertCompletion(
                EventCompletionEntity(
                    eventId = eventId,
                    occurrenceDate = date,
                    isCompleted = true,
                    completedAt = now,
                    updatedAt = now,
                    deletedAt = null,
                    dirty = true
                )
            )
        }
    }

    suspend fun setCompletion(eventId: String, date: String, isCompleted: Boolean) {
        val now = timeProvider.currentTimeMillis()
        val completion = scheduleDao.getCompletionIncludeDeleted(eventId, date)
        if (completion != null) {
            if (isCompleted) {
                // Revive if needed
                scheduleDao.insertCompletion(
                    completion.copy(
                        isCompleted = true,
                        completedAt = now,
                        updatedAt = now,
                        deletedAt = null,
                        dirty = true
                    )
                )
            } else {
                // Soft delete
                scheduleDao.insertCompletion(
                    completion.copy(
                        isCompleted = false,
                        deletedAt = now,
                        updatedAt = now,
                        dirty = true
                    )
                )
            }
        } else {
            if (isCompleted) {
                scheduleDao.insertCompletion(
                    EventCompletionEntity(
                        eventId = eventId,
                        occurrenceDate = date,
                        isCompleted = true,
                        completedAt = now,
                        updatedAt = now,
                        deletedAt = null,
                        dirty = true
                    )
                )
            } else {
                scheduleDao.insertCompletion(
                    EventCompletionEntity(
                        eventId = eventId,
                        occurrenceDate = date,
                        isCompleted = false,
                        completedAt = now,
                        updatedAt = now,
                        deletedAt = now,
                        dirty = true
                    )
                )
            }
        }
    }

    /**
     * Atomically toggles completion on an event and its occurrence date,
     * updating both event.isCompleted and event_completions record.
     * Used by widgets & action receivers so they don't invoke DAO directly.
     */
    suspend fun toggleEventCompletion(eventId: String, date: String) {
        val now = timeProvider.currentTimeMillis()
        val event = scheduleDao.getEventById(eventId)
        if (event != null) {
            val newCompleted = !event.isCompleted
            scheduleDao.updateEvent(
                event.copy(
                    isCompleted = newCompleted,
                    updatedAt = now,
                    dirty = true
                )
            )
            setCompletion(eventId, date, newCompleted)
        }
    }

    fun getCompletionsForEvent(eventId: String): Flow<List<EventCompletionEntity>> =
        scheduleDao.getCompletionsForEvent(eventId)

    // ==========================================
    // HOLIDAYS & WEATHER (Local Cache)
    // ==========================================
    suspend fun insertHolidays(holidays: List<HolidayEntity>) {
        scheduleDao.insertHolidays(holidays)
    }

    fun getWeatherForecastsForRange(startDate: String, endDate: String): Flow<List<WeatherForecastEntity>> =
        scheduleDao.getWeatherForecastsForRange(startDate, endDate)

    suspend fun saveWeatherForecasts(forecasts: List<WeatherForecastEntity>, cutoffDate: String) {
        scheduleDao.deleteOldWeatherForecasts(cutoffDate)
        scheduleDao.insertWeatherForecasts(forecasts)
    }

    suspend fun getWeatherForDate(date: String): WeatherForecastEntity? =
        scheduleDao.getWeatherForDate(date)
}
