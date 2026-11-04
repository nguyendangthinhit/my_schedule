package com.example.data

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Migration3To4Test {

    private fun createInMemoryDb(): SupportSQLiteDatabase {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(null) // in-memory
            .callback(object : SupportSQLiteOpenHelper.Callback(3) {
                override fun onCreate(db: SupportSQLiteDatabase) {}
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()
        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        return helper.writableDatabase
    }

    @Test
    fun testMigration3To4_preservesAllDataAndConvertsToUuids() {
        val db = createInMemoryDb()

        // 1. Create v3 schema
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS boards (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                colorHex TEXT NOT NULL,
                isVisible INTEGER NOT NULL,
                sortOrder INTEGER NOT NULL
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS events (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                title TEXT NOT NULL,
                description TEXT,
                board_id INTEGER NOT NULL,
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
                FOREIGN KEY(board_id) REFERENCES boards(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS reminders (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                event_id INTEGER NOT NULL,
                remind_before_minutes INTEGER NOT NULL,
                is_triggered INTEGER NOT NULL,
                notification_id INTEGER,
                FOREIGN KEY(event_id) REFERENCES events(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS event_completions (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                event_id INTEGER NOT NULL,
                occurrence_date TEXT NOT NULL,
                is_completed INTEGER NOT NULL,
                completed_at INTEGER NOT NULL,
                FOREIGN KEY(event_id) REFERENCES events(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent())

        // 2. Insert v3 sample data
        db.execSQL("INSERT INTO boards (id, name, colorHex, isVisible, sortOrder) VALUES (1, 'Công việc', '#3B82F6', 1, 0)")
        db.execSQL("INSERT INTO boards (id, name, colorHex, isVisible, sortOrder) VALUES (2, 'Học tập', '#10B981', 1, 1)")
        db.execSQL("INSERT INTO boards (id, name, colorHex, isVisible, sortOrder) VALUES (99, 'Dự án riêng', '#A855F7', 1, 99)")

        db.execSQL("""
            INSERT INTO events (id, title, description, board_id, start_date, start_time, end_time, is_all_day, repeat_rule, repeat_days, repeat_end_date, is_completed, completed_at, has_reminder, created_at, updated_at)
            VALUES (10, 'Họp với khách hàng', 'Bàn về kiến trúc', 1, '2026-11-03', '09:00', '10:00', 0, 'NONE', NULL, NULL, 0, NULL, 1, 1700000000000, 1700000000000)
        """.trimIndent())

        db.execSQL("""
            INSERT INTO events (id, title, description, board_id, start_date, start_time, end_time, is_all_day, repeat_rule, repeat_days, repeat_end_date, is_completed, completed_at, has_reminder, created_at, updated_at)
            VALUES (20, 'Làm bài tập lớn', 'Phân tích dữ liệu', 2, '2026-11-04', '14:00', '16:00', 0, 'WEEKLY', 'WED', '2026-12-31', 1, 1700001000000, 0, 1700000500000, 1700001000000)
        """.trimIndent())

        db.execSQL("""
            INSERT INTO events (id, title, description, board_id, start_date, start_time, end_time, is_all_day, repeat_rule, repeat_days, repeat_end_date, is_completed, completed_at, has_reminder, created_at, updated_at)
            VALUES (30, 'Task dự án riêng', 'Viết API', 99, '2026-11-05', '20:00', '22:00', 0, 'NONE', NULL, NULL, 0, NULL, 0, 1700002000000, 1700002000000)
        """.trimIndent())

        db.execSQL("INSERT INTO reminders (id, event_id, remind_before_minutes, is_triggered, notification_id) VALUES (101, 10, 15, 0, 1001)")
        db.execSQL("INSERT INTO event_completions (id, event_id, occurrence_date, is_completed, completed_at) VALUES (201, 20, '2026-11-04', 1, 1700001000000)")

        // 3. Run migration 3 -> 4
        AppDatabase.MIGRATION_3_4.migrate(db)

        // 4. Assert boards
        val boardCursor = db.query("SELECT id, name, colorHex, isVisible, sortOrder, updatedAt, deletedAt, dirty FROM boards ORDER BY sortOrder ASC")
        val migratedBoards = mutableListOf<Map<String, Any?>>()
        while (boardCursor.moveToNext()) {
            migratedBoards.add(
                mapOf(
                    "id" to boardCursor.getString(0),
                    "name" to boardCursor.getString(1),
                    "colorHex" to boardCursor.getString(2),
                    "isVisible" to boardCursor.getInt(3),
                    "sortOrder" to boardCursor.getInt(4),
                    "updatedAt" to boardCursor.getLong(5),
                    "deletedAt" to if (boardCursor.isNull(6)) null else boardCursor.getLong(6),
                    "dirty" to boardCursor.getInt(7)
                )
            )
        }
        boardCursor.close()

        // 10 default boards + 1 custom board (99) = 11 boards
        assertEquals(11, migratedBoards.size)

        val workBoard = migratedBoards.first { it["name"] == "Công việc" }
        assertEquals(DefaultBoards.BOARD_WORK_ID, workBoard["id"])
        assertEquals(1, workBoard["dirty"])
        assertNull(workBoard["deletedAt"])
        assertTrue((workBoard["updatedAt"] as Long) > 0)

        val studyBoard = migratedBoards.first { it["name"] == "Học tập" }
        assertEquals(DefaultBoards.BOARD_STUDY_ID, studyBoard["id"])

        val customBoard = migratedBoards.first { it["name"] == "Dự án riêng" }
        val customBoardId = customBoard["id"] as String
        assertTrue(customBoardId.isNotBlank())
        assertNotEquals(DefaultBoards.BOARD_WORK_ID, customBoardId)

        // 5. Assert events
        val eventCursor = db.query("SELECT id, title, board_id, start_date, is_completed, dirty, deleted_at FROM events ORDER BY start_date ASC")
        val migratedEvents = mutableListOf<Map<String, Any?>>()
        while (eventCursor.moveToNext()) {
            migratedEvents.add(
                mapOf(
                    "id" to eventCursor.getString(0),
                    "title" to eventCursor.getString(1),
                    "board_id" to eventCursor.getString(2),
                    "start_date" to eventCursor.getString(3),
                    "is_completed" to eventCursor.getInt(4),
                    "dirty" to eventCursor.getInt(5),
                    "deleted_at" to if (eventCursor.isNull(6)) null else eventCursor.getLong(6)
                )
            )
        }
        eventCursor.close()

        assertEquals(3, migratedEvents.size)

        val event1 = migratedEvents.first { it["title"] == "Họp với khách hàng" }
        val event1Id = event1["id"] as String
        assertEquals(DefaultBoards.BOARD_WORK_ID, event1["board_id"])
        assertEquals(1, event1["dirty"])
        assertNull(event1["deleted_at"])

        val event2 = migratedEvents.first { it["title"] == "Làm bài tập lớn" }
        val event2Id = event2["id"] as String
        assertEquals(DefaultBoards.BOARD_STUDY_ID, event2["board_id"])
        assertEquals(1, event2["is_completed"])

        val event3 = migratedEvents.first { it["title"] == "Task dự án riêng" }
        assertEquals(customBoardId, event3["board_id"])

        // 6. Assert reminders
        val remCursor = db.query("SELECT id, event_id, remind_before_minutes, dirty, deleted_at FROM reminders")
        val migratedReminders = mutableListOf<Map<String, Any?>>()
        while (remCursor.moveToNext()) {
            migratedReminders.add(
                mapOf(
                    "id" to remCursor.getString(0),
                    "event_id" to remCursor.getString(1),
                    "remind_before_minutes" to remCursor.getInt(2),
                    "dirty" to remCursor.getInt(3),
                    "deleted_at" to if (remCursor.isNull(4)) null else remCursor.getLong(4)
                )
            )
        }
        remCursor.close()

        assertEquals(1, migratedReminders.size)
        assertEquals(event1Id, migratedReminders[0]["event_id"])
        assertEquals(15, migratedReminders[0]["remind_before_minutes"])
        assertEquals(1, migratedReminders[0]["dirty"])

        // 7. Assert event_completions
        val compCursor = db.query("SELECT id, event_id, occurrence_date, is_completed, dirty, deleted_at FROM event_completions")
        val migratedCompletions = mutableListOf<Map<String, Any?>>()
        while (compCursor.moveToNext()) {
            migratedCompletions.add(
                mapOf(
                    "id" to compCursor.getString(0),
                    "event_id" to compCursor.getString(1),
                    "occurrence_date" to compCursor.getString(2),
                    "is_completed" to compCursor.getInt(3),
                    "dirty" to compCursor.getInt(4),
                    "deleted_at" to if (compCursor.isNull(5)) null else compCursor.getLong(5)
                )
            )
        }
        compCursor.close()

        assertEquals(1, migratedCompletions.size)
        assertEquals(event2Id, migratedCompletions[0]["event_id"])
        assertEquals("2026-11-04", migratedCompletions[0]["occurrence_date"])
        assertEquals(1, migratedCompletions[0]["is_completed"])
        assertEquals(1, migratedCompletions[0]["dirty"])
    }
}
