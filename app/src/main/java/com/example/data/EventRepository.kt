package com.example.data

import com.example.data.dao.ScheduleDao
import com.example.data.entities.*
import kotlinx.coroutines.flow.Flow

class EventRepository(private val scheduleDao: ScheduleDao) {
    val allBoards: Flow<List<BoardEntity>> = scheduleDao.getAllBoards()
    val allEvents: Flow<List<EventEntity>> = scheduleDao.getAllEvents()
    val allHolidays: Flow<List<HolidayEntity>> = scheduleDao.getAllHolidays()
    val allCompletions: Flow<List<EventCompletionEntity>> = scheduleDao.getAllCompletions()

    suspend fun insertHolidays(holidays: List<HolidayEntity>) {
        scheduleDao.insertHolidays(holidays)
    }

    suspend fun insertBoard(board: BoardEntity) {
        if (scheduleDao.getBoardCount() < 5) {
            scheduleDao.insertBoard(board)
        }
    }

    suspend fun updateBoard(board: BoardEntity) = scheduleDao.updateBoard(board)

    suspend fun deleteBoard(board: BoardEntity, migrateToBoardId: Int? = null) {
        if (migrateToBoardId != null) {
            scheduleDao.migrateEventsToNewBoard(board.id, migrateToBoardId)
        } else {
            scheduleDao.deleteEventsByBoard(board.id)
        }
        scheduleDao.deleteBoard(board)
    }

    suspend fun insertEvent(event: EventEntity, reminders: List<ReminderEntity>): Int {
        val eventId = scheduleDao.insertEvent(event).toInt()
        reminders.forEach { scheduleDao.insertReminder(it.copy(eventId = eventId)) }
        return eventId
    }

    suspend fun updateEvent(event: EventEntity, reminders: List<ReminderEntity>) {
        scheduleDao.updateEvent(event)
        scheduleDao.deleteRemindersForEvent(event.id)
        reminders.forEach { scheduleDao.insertReminder(it.copy(eventId = event.id)) }
    }

    suspend fun deleteEvent(event: EventEntity) = scheduleDao.deleteEvent(event)

    suspend fun toggleCompletion(eventId: Int, date: String) {
        val completion = scheduleDao.getCompletion(eventId, date)
        if (completion != null) {
            scheduleDao.insertCompletion(completion.copy(isCompleted = !completion.isCompleted, completedAt = System.currentTimeMillis()))
        } else {
            scheduleDao.insertCompletion(EventCompletionEntity(eventId = eventId, occurrenceDate = date, isCompleted = true))
        }
    }

    suspend fun setCompletion(eventId: Int, date: String, isCompleted: Boolean) {
        val completion = scheduleDao.getCompletion(eventId, date)
        if (completion != null) {
            scheduleDao.insertCompletion(completion.copy(isCompleted = isCompleted, completedAt = System.currentTimeMillis()))
        } else {
            scheduleDao.insertCompletion(EventCompletionEntity(eventId = eventId, occurrenceDate = date, isCompleted = isCompleted))
        }
    }

    fun getCompletionsForEvent(eventId: Int): Flow<List<EventCompletionEntity>> = 
        scheduleDao.getCompletionsForEvent(eventId)

    // Weather Forecasts
    val allWeatherForecasts: Flow<List<WeatherForecastEntity>> = scheduleDao.getAllWeatherForecasts()

    fun getWeatherForecastsForRange(startDate: String, endDate: String): Flow<List<WeatherForecastEntity>> =
        scheduleDao.getWeatherForecastsForRange(startDate, endDate)

    suspend fun saveWeatherForecasts(forecasts: List<WeatherForecastEntity>, cutoffDate: String) {
        scheduleDao.deleteOldWeatherForecasts(cutoffDate)
        scheduleDao.insertWeatherForecasts(forecasts)
    }

    suspend fun getWeatherForDate(date: String): WeatherForecastEntity? =
        scheduleDao.getWeatherForDate(date)
}
