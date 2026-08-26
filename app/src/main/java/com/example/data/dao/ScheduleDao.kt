package com.example.data.dao

import androidx.room.*
import com.example.data.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    // Boards
    @Query("SELECT * FROM boards ORDER BY sortOrder ASC")
    fun getAllBoards(): Flow<List<BoardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBoard(board: BoardEntity)

    @Update
    suspend fun updateBoard(board: BoardEntity)

    @Delete
    suspend fun deleteBoard(board: BoardEntity)

    @Query("SELECT COUNT(*) FROM boards")
    suspend fun getBoardCount(): Int

    // Events
    @Query("SELECT * FROM events")
    fun getAllEvents(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events")
    suspend fun getAllEventsList(): List<EventEntity>

    @Query("SELECT * FROM events WHERE id = :eventId")
    suspend fun getEventById(eventId: Int): EventEntity?

    @Query("SELECT * FROM events WHERE board_id = :boardId")
    suspend fun getEventsByBoard(boardId: Int): List<EventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EventEntity): Long

    @Update
    suspend fun updateEvent(event: EventEntity)

    @Delete
    suspend fun deleteEvent(event: EventEntity)

    @Query("DELETE FROM events WHERE id = :eventId")
    suspend fun deleteEventById(eventId: Int)

    @Query("UPDATE events SET board_id = :newBoardId WHERE board_id = :oldBoardId")
    suspend fun migrateEventsToNewBoard(oldBoardId: Int, newBoardId: Int)

    @Query("DELETE FROM events WHERE board_id = :boardId")
    suspend fun deleteEventsByBoard(boardId: Int)

    // Reminders
    @Query("SELECT * FROM reminders WHERE event_id = :eventId")
    suspend fun getRemindersForEvent(eventId: Int): List<ReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE event_id = :eventId")
    suspend fun deleteRemindersForEvent(eventId: Int)

    // Event Completions
    @Query("SELECT * FROM event_completions WHERE event_id = :eventId AND occurrence_date = :date")
    suspend fun getCompletion(eventId: Int, date: String): EventCompletionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletion(completion: EventCompletionEntity)

    @Query("SELECT * FROM event_completions WHERE event_id = :eventId")
    fun getCompletionsForEvent(eventId: Int): Flow<List<EventCompletionEntity>>

    @Query("SELECT * FROM event_completions")
    fun getAllCompletions(): Flow<List<EventCompletionEntity>>

    // Holidays
    @Query("SELECT * FROM holidays")
    fun getAllHolidays(): Flow<List<HolidayEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHolidays(holidays: List<HolidayEntity>)

    // Weather Forecasts (7-day window)
    @Query("SELECT * FROM weather_forecasts WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    fun getWeatherForecastsForRange(startDate: String, endDate: String): Flow<List<WeatherForecastEntity>>

    @Query("SELECT * FROM weather_forecasts ORDER BY date ASC")
    fun getAllWeatherForecasts(): Flow<List<WeatherForecastEntity>>

    @Query("SELECT * FROM weather_forecasts WHERE date = :date LIMIT 1")
    suspend fun getWeatherForDate(date: String): WeatherForecastEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeatherForecasts(forecasts: List<WeatherForecastEntity>)

    @Query("DELETE FROM weather_forecasts WHERE date < :cutoffDate")
    suspend fun deleteOldWeatherForecasts(cutoffDate: String)
}
