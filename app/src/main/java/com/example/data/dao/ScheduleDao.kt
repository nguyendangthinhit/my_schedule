package com.example.data.dao

import androidx.room.*
import com.example.data.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
internal interface ScheduleDao {
    // ==========================================
    // BOARDS
    // ==========================================
    @Query("SELECT * FROM boards WHERE deletedAt IS NULL ORDER BY sortOrder ASC")
    fun getAllBoards(): Flow<List<BoardEntity>>

    @Query("SELECT * FROM boards WHERE deletedAt IS NULL ORDER BY sortOrder ASC")
    suspend fun getAllBoardsList(): List<BoardEntity>

    @Query("SELECT * FROM boards WHERE id = :boardId AND deletedAt IS NULL LIMIT 1")
    suspend fun getBoardById(boardId: String): BoardEntity?

    @Query("SELECT * FROM boards WHERE id = :boardId LIMIT 1")
    suspend fun getBoardByIdIncludeDeleted(boardId: String): BoardEntity?

    @Query("SELECT COUNT(*) FROM boards WHERE deletedAt IS NULL")
    suspend fun getBoardCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBoard(board: BoardEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBoards(boards: List<BoardEntity>)

    @Update
    suspend fun updateBoard(board: BoardEntity)

    @Query("UPDATE boards SET deletedAt = :now, dirty = 1, updatedAt = :now WHERE id = :boardId")
    suspend fun softDeleteBoard(boardId: String, now: Long)

    @Query("SELECT * FROM boards WHERE dirty = 1")
    suspend fun getDirtyBoards(): List<BoardEntity>

    // ==========================================
    // EVENTS
    // ==========================================
    @Query("""
        SELECT events.* FROM events 
        INNER JOIN boards ON events.board_id = boards.id 
        WHERE events.deleted_at IS NULL AND boards.deletedAt IS NULL 
        ORDER BY events.start_date ASC, events.start_time ASC
    """)
    fun getAllEvents(): Flow<List<EventEntity>>

    @Query("""
        SELECT events.* FROM events 
        INNER JOIN boards ON events.board_id = boards.id 
        WHERE events.deleted_at IS NULL AND boards.deletedAt IS NULL 
        ORDER BY events.start_date ASC, events.start_time ASC
    """)
    suspend fun getAllEventsList(): List<EventEntity>

    @Query("""
        SELECT events.* FROM events 
        INNER JOIN boards ON events.board_id = boards.id 
        WHERE events.id = :eventId AND events.deleted_at IS NULL AND boards.deletedAt IS NULL 
        LIMIT 1
    """)
    suspend fun getEventById(eventId: String): EventEntity?

    @Query("SELECT * FROM events WHERE id = :eventId LIMIT 1")
    suspend fun getEventByIdIncludeDeleted(eventId: String): EventEntity?

    @Query("""
        SELECT events.* FROM events 
        INNER JOIN boards ON events.board_id = boards.id 
        WHERE events.board_id = :boardId AND events.deleted_at IS NULL AND boards.deletedAt IS NULL 
        ORDER BY events.start_date ASC, events.start_time ASC
    """)
    suspend fun getEventsByBoard(boardId: String): List<EventEntity>

    @Query("""
        SELECT events.* FROM events 
        INNER JOIN boards ON events.board_id = boards.id 
        WHERE events.deleted_at IS NULL 
          AND boards.deletedAt IS NULL 
          AND events.has_reminder = 1 
          AND events.is_completed = 0
    """)
    suspend fun getAllActiveEventsWithReminders(): List<EventEntity>

    @Query("SELECT id FROM events WHERE deleted_at IS NOT NULL")
    suspend fun getAllSoftDeletedEventIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EventEntity)

    @Update
    suspend fun updateEvent(event: EventEntity)

    @Query("UPDATE events SET deleted_at = :now, dirty = 1, updated_at = :now WHERE id = :eventId")
    suspend fun softDeleteEvent(eventId: String, now: Long)

    @Query("UPDATE events SET deleted_at = :now, dirty = 1, updated_at = :now WHERE board_id = :boardId AND deleted_at IS NULL")
    suspend fun softDeleteEventsByBoard(boardId: String, now: Long)

    @Query("UPDATE events SET board_id = :newBoardId, dirty = 1, updated_at = :updatedAt WHERE board_id = :oldBoardId AND deleted_at IS NULL")
    suspend fun migrateEventsToNewBoard(oldBoardId: String, newBoardId: String, updatedAt: Long)

    @Query("SELECT * FROM events WHERE dirty = 1")
    suspend fun getDirtyEvents(): List<EventEntity>

    // ==========================================
    // REMINDERS
    // ==========================================
    @Query("""
        SELECT reminders.* FROM reminders 
        INNER JOIN events ON reminders.event_id = events.id 
        INNER JOIN boards ON events.board_id = boards.id 
        WHERE reminders.event_id = :eventId 
          AND reminders.deleted_at IS NULL 
          AND events.deleted_at IS NULL 
          AND boards.deletedAt IS NULL
    """)
    suspend fun getRemindersForEvent(eventId: String): List<ReminderEntity>

    @Query("""
        SELECT reminders.* FROM reminders 
        INNER JOIN events ON reminders.event_id = events.id 
        INNER JOIN boards ON events.board_id = boards.id 
        WHERE reminders.deleted_at IS NULL 
          AND events.deleted_at IS NULL 
          AND boards.deletedAt IS NULL 
          AND events.is_completed = 0
    """)
    suspend fun getActiveRemindersWithEvents(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE event_id = :eventId AND remind_before_minutes = :remindBeforeMinutes LIMIT 1")
    suspend fun getReminderIncludeDeleted(eventId: String, remindBeforeMinutes: Int): ReminderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity)

    @Update
    suspend fun updateReminder(reminder: ReminderEntity)

    @Query("UPDATE reminders SET deleted_at = :now, dirty = 1, updated_at = :now WHERE id = :reminderId")
    suspend fun softDeleteReminder(reminderId: String, now: Long)

    @Query("UPDATE reminders SET deleted_at = :now, dirty = 1, updated_at = :now WHERE event_id = :eventId AND deleted_at IS NULL")
    suspend fun softDeleteRemindersForEvent(eventId: String, now: Long)

    @Query("""
        UPDATE reminders SET deleted_at = :now, dirty = 1, updated_at = :now 
        WHERE event_id IN (SELECT id FROM events WHERE board_id = :boardId) AND deleted_at IS NULL
    """)
    suspend fun softDeleteRemindersByBoard(boardId: String, now: Long)

    @Query("SELECT * FROM reminders WHERE dirty = 1")
    suspend fun getDirtyReminders(): List<ReminderEntity>

    // ==========================================
    // EVENT COMPLETIONS
    // ==========================================
    @Query("""
        SELECT event_completions.* FROM event_completions 
        INNER JOIN events ON event_completions.event_id = events.id 
        INNER JOIN boards ON events.board_id = boards.id 
        WHERE event_completions.event_id = :eventId 
          AND event_completions.occurrence_date = :date 
          AND event_completions.deleted_at IS NULL 
          AND events.deleted_at IS NULL 
          AND boards.deletedAt IS NULL 
        LIMIT 1
    """)
    suspend fun getCompletion(eventId: String, date: String): EventCompletionEntity?

    @Query("SELECT * FROM event_completions WHERE event_id = :eventId AND occurrence_date = :date LIMIT 1")
    suspend fun getCompletionIncludeDeleted(eventId: String, date: String): EventCompletionEntity?

    @Query("""
        SELECT event_completions.* FROM event_completions 
        INNER JOIN events ON event_completions.event_id = events.id 
        INNER JOIN boards ON events.board_id = boards.id 
        WHERE event_completions.event_id = :eventId 
          AND event_completions.deleted_at IS NULL 
          AND events.deleted_at IS NULL 
          AND boards.deletedAt IS NULL
    """)
    fun getCompletionsForEvent(eventId: String): Flow<List<EventCompletionEntity>>

    @Query("""
        SELECT event_completions.* FROM event_completions 
        INNER JOIN events ON event_completions.event_id = events.id 
        INNER JOIN boards ON events.board_id = boards.id 
        WHERE event_completions.deleted_at IS NULL 
          AND events.deleted_at IS NULL 
          AND boards.deletedAt IS NULL
    """)
    fun getAllCompletions(): Flow<List<EventCompletionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletion(completion: EventCompletionEntity)

    @Update
    suspend fun updateCompletion(completion: EventCompletionEntity)

    @Query("UPDATE event_completions SET deleted_at = :now, dirty = 1, updated_at = :now WHERE id = :completionId")
    suspend fun softDeleteCompletion(completionId: String, now: Long)

    @Query("UPDATE event_completions SET deleted_at = :now, dirty = 1, updated_at = :now WHERE event_id = :eventId AND deleted_at IS NULL")
    suspend fun softDeleteCompletionsForEvent(eventId: String, now: Long)

    @Query("""
        UPDATE event_completions SET deleted_at = :now, dirty = 1, updated_at = :now 
        WHERE event_id IN (SELECT id FROM events WHERE board_id = :boardId) AND deleted_at IS NULL
    """)
    suspend fun softDeleteCompletionsByBoard(boardId: String, now: Long)

    @Query("SELECT * FROM event_completions WHERE dirty = 1")
    suspend fun getDirtyCompletions(): List<EventCompletionEntity>

    // ==========================================
    // HOLIDAYS (Local cache only, excluded from sync)
    // ==========================================
    @Query("SELECT * FROM holidays")
    fun getAllHolidays(): Flow<List<HolidayEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHolidays(holidays: List<HolidayEntity>)

    // ==========================================
    // WEATHER FORECASTS (Local cache only, excluded from sync)
    // ==========================================
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
