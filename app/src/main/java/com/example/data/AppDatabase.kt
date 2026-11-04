package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.ScheduleDao
import com.example.data.entities.*

@Database(
    entities = [
        BoardEntity::class,
        EventEntity::class,
        ReminderEntity::class,
        EventCompletionEntity::class,
        HolidayEntity::class,
        WeatherForecastEntity::class
    ],
    version = 4,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    internal abstract fun scheduleDao(): ScheduleDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Legacy v2->v3 migration
                db.execSQL("INSERT OR IGNORE INTO boards (id, name, colorHex, isVisible, sortOrder) VALUES (1, 'Công việc', '#3B82F6', 1, 0)")
                db.execSQL("INSERT OR IGNORE INTO boards (id, name, colorHex, isVisible, sortOrder) VALUES (2, 'Học tập', '#10B981', 1, 1)")
                db.execSQL("INSERT OR IGNORE INTO boards (id, name, colorHex, isVisible, sortOrder) VALUES (3, 'Đi chơi', '#06B6D4', 1, 2)")
                db.execSQL("INSERT OR IGNORE INTO boards (id, name, colorHex, isVisible, sortOrder) VALUES (4, 'Cuộc họp', '#F59E0B', 1, 3)")
                db.execSQL("INSERT OR IGNORE INTO boards (id, name, colorHex, isVisible, sortOrder) VALUES (5, 'Ngày lễ', '#EF4444', 1, 4)")
                db.execSQL("INSERT OR IGNORE INTO boards (id, name, colorHex, isVisible, sortOrder) VALUES (6, 'Cá nhân', '#8B5CF6', 1, 5)")
                db.execSQL("INSERT OR IGNORE INTO boards (id, name, colorHex, isVisible, sortOrder) VALUES (7, 'Sức khỏe', '#EC4899', 1, 6)")
                db.execSQL("INSERT OR IGNORE INTO boards (id, name, colorHex, isVisible, sortOrder) VALUES (8, 'Tài chính', '#14B8A6', 1, 7)")
                db.execSQL("INSERT OR IGNORE INTO boards (id, name, colorHex, isVisible, sortOrder) VALUES (9, 'Việc cần làm', '#6366F1', 1, 8)")
                db.execSQL("INSERT OR IGNORE INTO boards (id, name, colorHex, isVisible, sortOrder) VALUES (10, 'Gia đình', '#F97316', 1, 9)")
            }
        }

        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val now = System.currentTimeMillis()

                // 1. Create temporary new tables with v4 schema
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS boards_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        colorHex TEXT NOT NULL,
                        isVisible INTEGER NOT NULL,
                        sortOrder INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        deletedAt INTEGER,
                        dirty INTEGER NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS events_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        description TEXT,
                        board_id TEXT NOT NULL,
                        start_date TEXT NOT NULL,
                        start_time TEXT,
                        end_time TEXT,
                        is_all_day INTEGER NOT NULL,
                        repeat_rule TEXT,
                        repeat_days TEXT,
                        repeat_end_date TEXT,
                        is_completed INTEGER NOT NULL,
                        completed_at INTEGER,
                        has_reminder INTEGER NOT NULL,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL,
                        deleted_at INTEGER,
                        dirty INTEGER NOT NULL,
                        FOREIGN KEY(board_id) REFERENCES boards(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS reminders_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        event_id TEXT NOT NULL,
                        remind_before_minutes INTEGER NOT NULL,
                        is_triggered INTEGER NOT NULL,
                        notification_id INTEGER,
                        updated_at INTEGER NOT NULL,
                        deleted_at INTEGER,
                        dirty INTEGER NOT NULL,
                        FOREIGN KEY(event_id) REFERENCES events(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS event_completions_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        event_id TEXT NOT NULL,
                        occurrence_date TEXT NOT NULL,
                        is_completed INTEGER NOT NULL,
                        completed_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL,
                        deleted_at INTEGER,
                        dirty INTEGER NOT NULL,
                        FOREIGN KEY(event_id) REFERENCES events(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())

                // 2. Read and migrate boards
                val boardIdMap = mutableMapOf<Int, String>()
                val boardCursor = db.query("SELECT id, name, colorHex, isVisible, sortOrder FROM boards")
                while (boardCursor.moveToNext()) {
                    val oldId = boardCursor.getInt(0)
                    val name = boardCursor.getString(1)
                    val colorHex = boardCursor.getString(2)
                    val isVisible = boardCursor.getInt(3)
                    val sortOrder = boardCursor.getInt(4)

                    val newId = if (oldId in 1..10) {
                        DefaultBoards.getDefaultBoardUuid(oldId)
                    } else {
                        java.util.UUID.randomUUID().toString()
                    }
                    boardIdMap[oldId] = newId

                    db.execSQL(
                        "INSERT INTO boards_new (id, name, colorHex, isVisible, sortOrder, updatedAt, deletedAt, dirty) VALUES (?, ?, ?, ?, ?, ?, NULL, 1)",
                        arrayOf<Any?>(newId, name, colorHex, isVisible, sortOrder, now)
                    )
                }
                boardCursor.close()

                // Ensure all 10 default boards exist in boards_new with deterministic UUIDs
                for (i in 1..10) {
                    val defaultUuid = DefaultBoards.getDefaultBoardUuid(i)
                    val checkCursor = db.query("SELECT COUNT(*) FROM boards_new WHERE id = ?", arrayOf<Any?>(defaultUuid))
                    var exists = false
                    if (checkCursor.moveToNext()) {
                        exists = checkCursor.getInt(0) > 0
                    }
                    checkCursor.close()

                    if (!exists) {
                        val (defaultName, defaultColorHex) = DefaultBoards.getDefaultBoardInfo(i)
                        db.execSQL(
                            "INSERT INTO boards_new (id, name, colorHex, isVisible, sortOrder, updatedAt, deletedAt, dirty) VALUES (?, ?, ?, 1, ?, ?, NULL, 1)",
                            arrayOf<Any?>(defaultUuid, defaultName, defaultColorHex, i - 1, now)
                        )
                    }
                }

                // 3. Read and migrate events
                val eventIdMap = mutableMapOf<Int, String>()
                val eventCursor = db.query("SELECT id, title, description, board_id, start_date, start_time, end_time, is_all_day, repeat_rule, repeat_days, repeat_end_date, is_completed, completed_at, has_reminder, created_at, updated_at FROM events")
                while (eventCursor.moveToNext()) {
                    val oldEventId = eventCursor.getInt(0)
                    val title = eventCursor.getString(1)
                    val description = if (eventCursor.isNull(2)) null else eventCursor.getString(2)
                    val oldBoardId = eventCursor.getInt(3)
                    val startDate = eventCursor.getString(4)
                    val startTime = if (eventCursor.isNull(5)) null else eventCursor.getString(5)
                    val endTime = if (eventCursor.isNull(6)) null else eventCursor.getString(6)
                    val isAllDay = eventCursor.getInt(7)
                    val repeatRule = if (eventCursor.isNull(8)) null else eventCursor.getString(8)
                    val repeatDays = if (eventCursor.isNull(9)) null else eventCursor.getString(9)
                    val repeatEndDate = if (eventCursor.isNull(10)) null else eventCursor.getString(10)
                    val isCompleted = eventCursor.getInt(11)
                    val completedAt = if (eventCursor.isNull(12)) null else eventCursor.getLong(12)
                    val hasReminder = eventCursor.getInt(13)
                    val createdAt = eventCursor.getLong(14)
                    val oldUpdatedAt = eventCursor.getLong(15)

                    val newEventId = java.util.UUID.randomUUID().toString()
                    eventIdMap[oldEventId] = newEventId
                    val newBoardId = boardIdMap[oldBoardId] ?: DefaultBoards.BOARD_WORK_ID

                    db.execSQL(
                        """
                        INSERT INTO events_new (
                            id, title, description, board_id, start_date, start_time, end_time,
                            is_all_day, repeat_rule, repeat_days, repeat_end_date, is_completed,
                            completed_at, has_reminder, created_at, updated_at, deleted_at, dirty
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NULL, 1)
                        """.trimIndent(),
                        arrayOf<Any?>(
                            newEventId, title, description, newBoardId, startDate, startTime, endTime,
                            isAllDay, repeatRule, repeatDays, repeatEndDate, isCompleted,
                            completedAt, hasReminder, createdAt, if (oldUpdatedAt > 0) oldUpdatedAt else now
                        )
                    )
                }
                eventCursor.close()

                // 4. Read and migrate reminders
                val remCursor = db.query("SELECT id, event_id, remind_before_minutes, is_triggered, notification_id FROM reminders")
                while (remCursor.moveToNext()) {
                    val oldEventId = remCursor.getInt(1)
                    val remindBeforeMinutes = remCursor.getInt(2)
                    val isTriggered = remCursor.getInt(3)
                    val notifId = if (remCursor.isNull(4)) null else remCursor.getInt(4)

                    val newEventId = eventIdMap[oldEventId]
                    if (newEventId != null) {
                        val newRemId = java.util.UUID.randomUUID().toString()
                        db.execSQL(
                            "INSERT INTO reminders_new (id, event_id, remind_before_minutes, is_triggered, notification_id, updated_at, deleted_at, dirty) VALUES (?, ?, ?, ?, ?, ?, NULL, 1)",
                            arrayOf<Any?>(newRemId, newEventId, remindBeforeMinutes, isTriggered, notifId, now)
                        )
                    }
                }
                remCursor.close()

                // 5. Read and migrate event_completions
                val compCursor = db.query("SELECT id, event_id, occurrence_date, is_completed, completed_at FROM event_completions")
                while (compCursor.moveToNext()) {
                    val oldEventId = compCursor.getInt(1)
                    val occurrenceDate = compCursor.getString(2)
                    val isCompleted = compCursor.getInt(3)
                    val completedAt = compCursor.getLong(4)

                    val newEventId = eventIdMap[oldEventId]
                    if (newEventId != null) {
                        val newCompId = java.util.UUID.randomUUID().toString()
                        db.execSQL(
                            "INSERT INTO event_completions_new (id, event_id, occurrence_date, is_completed, completed_at, updated_at, deleted_at, dirty) VALUES (?, ?, ?, ?, ?, ?, NULL, 1)",
                            arrayOf<Any?>(newCompId, newEventId, occurrenceDate, isCompleted, completedAt, now)
                        )
                    }
                }
                compCursor.close()

                // 6. Drop old tables & rename new tables
                db.execSQL("DROP TABLE reminders")
                db.execSQL("DROP TABLE event_completions")
                db.execSQL("DROP TABLE events")
                db.execSQL("DROP TABLE boards")

                db.execSQL("ALTER TABLE boards_new RENAME TO boards")
                db.execSQL("ALTER TABLE events_new RENAME TO events")
                db.execSQL("ALTER TABLE reminders_new RENAME TO reminders")
                db.execSQL("ALTER TABLE event_completions_new RENAME TO event_completions")

                // 7. Create indexes matching Room Entity annotations exactly
                db.execSQL("CREATE INDEX IF NOT EXISTS index_events_start_date ON events(start_date)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_events_board_id ON events(board_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_events_deleted_at ON events(deleted_at)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_reminders_event_id ON reminders(event_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_event_completions_event_id ON event_completions(event_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_event_completions_occurrence_date ON event_completions(occurrence_date)")
            }
        }

        private fun insertDefaultBoardsSql(db: SupportSQLiteDatabase) {
            val now = System.currentTimeMillis()
            for (i in 1..10) {
                val (name, color) = DefaultBoards.getDefaultBoardInfo(i)
                val uuid = DefaultBoards.getDefaultBoardUuid(i)
                db.execSQL(
                    "INSERT OR IGNORE INTO boards (id, name, colorHex, isVisible, sortOrder, updatedAt, deletedAt, dirty) VALUES (?, ?, ?, 1, ?, ?, NULL, 1)",
                    arrayOf<Any?>(uuid, name, color, i - 1, now)
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "schedule_database"
                )
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                .fallbackToDestructiveMigration(true)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        insertDefaultBoardsSql(db)
                    }

                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        insertDefaultBoardsSql(db)
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
