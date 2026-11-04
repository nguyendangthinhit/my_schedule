package com.example.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.entities.BoardEntity
import com.example.data.entities.EventCompletionEntity
import com.example.data.entities.EventEntity
import com.example.data.entities.ReminderEntity
import com.example.util.NotificationHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SoftDeleteAndRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: EventRepository
    private lateinit var testTimeProvider: MutableTestTimeProvider

    class MutableTestTimeProvider(var currentTime: Long = 1000L) : TimeProvider {
        override fun currentTimeMillis(): Long = currentTime
    }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        testTimeProvider = MutableTestTimeProvider(1000L)
        repository = EventRepository(db.scheduleDao(), testTimeProvider)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testCentralizedRepository_injectsTimestampAndDirtyFlag() = runBlocking {
        testTimeProvider.currentTime = 1234567L
        val board = BoardEntity(
            id = DefaultBoards.BOARD_WORK_ID,
            name = "Công việc",
            colorHex = "#3B82F6",
            isVisible = true,
            sortOrder = 0
        )
        repository.insertBoard(board)

        val event = EventEntity(
            id = UUID.randomUUID().toString(),
            title = "Họp kiến trúc",
            boardId = board.id,
            startDate = "2026-11-03",
            startTime = "09:00",
            endTime = "10:00"
        )
        repository.insertEvent(event, emptyList())

        val dbEvents = repository.getAllEventsList()
        assertEquals(1, dbEvents.size)
        assertEquals(1234567L, dbEvents[0].updatedAt)
        assertTrue(dbEvents[0].dirty)
        assertNull(dbEvents[0].deletedAt)

        // Update event with advanced time
        testTimeProvider.currentTime = 9876543L
        repository.updateEvent(dbEvents[0].copy(title = "Họp kiến trúc v2"), emptyList())

        val updatedEvents = repository.getAllEventsList()
        assertEquals(1, updatedEvents.size)
        assertEquals("Họp kiến trúc v2", updatedEvents[0].title)
        assertEquals(9876543L, updatedEvents[0].updatedAt)
        assertTrue(updatedEvents[0].dirty)
    }

    @Test
    fun testSoftDeleteEvent_disappearsFromReadQueries_stillPresentInSqliteWithDeletedAtAndDirty() = runBlocking {
        // 1. Setup board, event, reminder, completion
        testTimeProvider.currentTime = 2000L
        val board = BoardEntity(
            id = DefaultBoards.BOARD_STUDY_ID,
            name = "Học tập",
            colorHex = "#10B981",
            isVisible = true,
            sortOrder = 1
        )
        repository.insertBoard(board)

        val eventId = UUID.randomUUID().toString()
        val event = EventEntity(
            id = eventId,
            title = "Thi kết thúc học phần",
            boardId = board.id,
            startDate = "2026-11-04",
            startTime = "14:00",
            endTime = "16:00",
            hasReminder = true
        )
        val reminder = ReminderEntity(
            id = UUID.randomUUID().toString(),
            eventId = eventId,
            remindBeforeMinutes = 30
        )
        repository.insertEvent(event, listOf(reminder))
        repository.setCompletion(eventId, "2026-11-04", true)

        // Verify all 3 are visible in active queries
        assertEquals(1, repository.getAllEventsList().size)
        assertEquals(1, repository.allEvents.first().size)
        assertEquals(1, repository.getRemindersForEvent(eventId).size)
        assertEquals(1, repository.allCompletions.first().size)

        // 2. Soft delete the event
        testTimeProvider.currentTime = 5000L
        repository.deleteEvent(event)

        // 3. Verify it completely disappeared from ALL read queries
        assertEquals(0, repository.getAllEventsList().size)
        assertEquals(0, repository.allEvents.first().size)
        assertNull(repository.getEventById(eventId))
        assertEquals(0, repository.getRemindersForEvent(eventId).size)
        assertEquals(0, repository.allCompletions.first().size)
        assertEquals(0, repository.getCompletionsForEvent(eventId).first().size)

        // 4. Verify raw SQLite rows still exist with deletedAt = 5000 and dirty = 1
        val sqliteDb = db.openHelper.readableDatabase

        // Event in SQLite
        val eventCursor = sqliteDb.query("SELECT id, title, deleted_at, dirty, updated_at FROM events WHERE id = '$eventId'")
        assertTrue(eventCursor.moveToFirst())
        assertEquals(eventId, eventCursor.getString(0))
        assertEquals("Thi kết thúc học phần", eventCursor.getString(1))
        assertEquals(5000L, eventCursor.getLong(2)) // deleted_at
        assertEquals(1, eventCursor.getInt(3))       // dirty = true
        assertEquals(5000L, eventCursor.getLong(4)) // updated_at
        eventCursor.close()

        // Reminder in SQLite
        val reminderCursor = sqliteDb.query("SELECT id, event_id, deleted_at, dirty, updated_at FROM reminders WHERE event_id = '$eventId'")
        assertTrue(reminderCursor.moveToFirst())
        assertEquals(5000L, reminderCursor.getLong(2)) // deleted_at
        assertEquals(1, reminderCursor.getInt(3))       // dirty = true
        assertEquals(5000L, reminderCursor.getLong(4)) // updated_at
        reminderCursor.close()

        // Completion in SQLite
        val completionCursor = sqliteDb.query("SELECT id, event_id, deleted_at, dirty, updated_at FROM event_completions WHERE event_id = '$eventId'")
        assertTrue(completionCursor.moveToFirst())
        assertEquals(5000L, completionCursor.getLong(2)) // deleted_at
        assertEquals(1, completionCursor.getInt(3))       // dirty = true
        assertEquals(5000L, completionCursor.getLong(4)) // updated_at
        completionCursor.close()
    }

    @Test
    fun testRevivingCompletion_recyclesSameRowInsteadOfInsertingDuplicates() = runBlocking {
        testTimeProvider.currentTime = 1000L
        val board = BoardEntity(
            id = DefaultBoards.BOARD_TASK_ID,
            name = "Việc cần làm",
            colorHex = "#6366F1",
            isVisible = true,
            sortOrder = 2
        )
        repository.insertBoard(board)

        val eventId = UUID.randomUUID().toString()
        val event = EventEntity(
            id = eventId,
            title = "Viết báo cáo chuyên đề",
            boardId = board.id,
            startDate = "2026-11-05"
        )
        repository.insertEvent(event, emptyList())

        // 1. First toggle -> Completed
        testTimeProvider.currentTime = 2000L
        repository.toggleCompletion(eventId, "2026-11-05")

        val sqliteDb = db.openHelper.readableDatabase
        var cursor = sqliteDb.query("SELECT id, is_completed, deleted_at, dirty, updated_at FROM event_completions WHERE event_id = '$eventId'")
        assertEquals(1, cursor.count)
        assertTrue(cursor.moveToFirst())
        val initialCompletionId = cursor.getString(0)
        assertEquals(1, cursor.getInt(1)) // is_completed = true
        assertTrue(cursor.isNull(2))      // deleted_at is null
        assertEquals(1, cursor.getInt(3)) // dirty = true
        assertEquals(2000L, cursor.getLong(4))
        cursor.close()

        // 2. Second toggle -> Unticked (soft-deleted)
        testTimeProvider.currentTime = 3000L
        repository.toggleCompletion(eventId, "2026-11-05")

        cursor = sqliteDb.query("SELECT id, is_completed, deleted_at, dirty, updated_at FROM event_completions WHERE event_id = '$eventId'")
        assertEquals(1, cursor.count) // STILL EXACTLY 1 ROW
        assertTrue(cursor.moveToFirst())
        assertEquals(initialCompletionId, cursor.getString(0)) // SAME ROW ID!
        assertEquals(0, cursor.getInt(1)) // is_completed = false
        assertEquals(3000L, cursor.getLong(2)) // deleted_at = 3000L
        assertEquals(1, cursor.getInt(3)) // dirty = true
        assertEquals(3000L, cursor.getLong(4))
        cursor.close()

        // Verify read query shows no active completions
        assertEquals(0, repository.allCompletions.first().size)

        // 3. Third toggle -> Ticked again (REVIVED / HỒI SINH!)
        testTimeProvider.currentTime = 4000L
        repository.toggleCompletion(eventId, "2026-11-05")

        cursor = sqliteDb.query("SELECT id, is_completed, deleted_at, dirty, updated_at FROM event_completions WHERE event_id = '$eventId'")
        assertEquals(1, cursor.count) // STILL EXACTLY 1 ROW! No duplicate inserted!
        assertTrue(cursor.moveToFirst())
        assertEquals(initialCompletionId, cursor.getString(0)) // RECYCLED SAME ID!
        assertEquals(1, cursor.getInt(1)) // is_completed = true
        assertTrue(cursor.isNull(2))      // deleted_at is revived back to NULL!
        assertEquals(1, cursor.getInt(3)) // dirty = true
        assertEquals(4000L, cursor.getLong(4)) // updated_at = 4000L
        cursor.close()

        // Read query immediately sees active completion again
        assertEquals(1, repository.allCompletions.first().size)
    }

    @Test
    fun testSoftDeleteBoard_cascadesToChildEventsRemindersAndCompletions() = runBlocking {
        testTimeProvider.currentTime = 1000L
        val board = BoardEntity(
            id = UUID.randomUUID().toString(),
            name = "Dự án phụ",
            colorHex = "#EC4899",
            isVisible = true,
            sortOrder = 10
        )
        repository.insertBoard(board)

        val eventId = UUID.randomUUID().toString()
        val event = EventEntity(
            id = eventId,
            title = "Task dự án phụ",
            boardId = board.id,
            startDate = "2026-11-06",
            hasReminder = true
        )
        val reminder = ReminderEntity(
            id = UUID.randomUUID().toString(),
            eventId = eventId,
            remindBeforeMinutes = 15
        )
        repository.insertEvent(event, listOf(reminder))
        repository.setCompletion(eventId, "2026-11-06", true)

        // Verify both board and event are active
        assertEquals(1, repository.getAllBoardsList().size)
        assertEquals(1, repository.getAllEventsList().size)

        // Soft delete board
        testTimeProvider.currentTime = 6000L
        repository.deleteBoard(board)

        // Read queries: board, events, reminders, completions are all excluded
        assertEquals(0, repository.getAllBoardsList().size)
        assertEquals(0, repository.getAllEventsList().size)
        assertEquals(0, repository.allEvents.first().size)
        assertEquals(0, repository.getRemindersForEvent(eventId).size)
        assertEquals(0, repository.allCompletions.first().size)

        // Raw SQLite queries: all rows preserved with deletedAt = 6000L and dirty = 1
        val sqliteDb = db.openHelper.readableDatabase

        val boardCursor = sqliteDb.query("SELECT deletedAt, dirty FROM boards WHERE id = '${board.id}'")
        assertTrue(boardCursor.moveToFirst())
        assertEquals(6000L, boardCursor.getLong(0))
        assertEquals(1, boardCursor.getInt(1))
        boardCursor.close()

        val eventCursor = sqliteDb.query("SELECT deleted_at, dirty FROM events WHERE id = '$eventId'")
        assertTrue(eventCursor.moveToFirst())
        assertEquals(6000L, eventCursor.getLong(0))
        assertEquals(1, eventCursor.getInt(1))
        eventCursor.close()

        val reminderCursor = sqliteDb.query("SELECT deleted_at, dirty FROM reminders WHERE event_id = '$eventId'")
        assertTrue(reminderCursor.moveToFirst())
        assertEquals(6000L, reminderCursor.getLong(0))
        assertEquals(1, reminderCursor.getInt(1))
        reminderCursor.close()

        val completionCursor = sqliteDb.query("SELECT deleted_at, dirty FROM event_completions WHERE event_id = '$eventId'")
        assertTrue(completionCursor.moveToFirst())
        assertEquals(6000L, completionCursor.getLong(0))
        assertEquals(1, completionCursor.getInt(1))
        completionCursor.close()
    }

    @Test
    fun testRescheduleAllAlarmsAndRefreshWidgets_executesCleanly() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val board = BoardEntity(
            id = DefaultBoards.BOARD_MEETING_ID,
            name = "Cuộc họp",
            colorHex = "#F59E0B",
            isVisible = true,
            sortOrder = 3
        )
        repository.insertBoard(board)

        val activeEventId = UUID.randomUUID().toString()
        val activeEvent = EventEntity(
            id = activeEventId,
            title = "Cuộc họp tuần tới",
            boardId = board.id,
            startDate = "2026-11-10",
            startTime = "10:00",
            hasReminder = true,
            isCompleted = false
        )
        val activeReminder = ReminderEntity(
            id = UUID.randomUUID().toString(),
            eventId = activeEventId,
            remindBeforeMinutes = 10
        )
        repository.insertEvent(activeEvent, listOf(activeReminder))

        // Create and delete another event to produce a soft-deleted event
        val deletedEventId = UUID.randomUUID().toString()
        val deletedEvent = EventEntity(
            id = deletedEventId,
            title = "Sự kiện đã hủy",
            boardId = board.id,
            startDate = "2026-11-10",
            startTime = "11:00",
            hasReminder = true
        )
        repository.insertEvent(deletedEvent, emptyList())
        repository.deleteEvent(deletedEvent)

        // Run rescheduleAllAlarmsAndRefreshWidgets
        NotificationHelper.rescheduleAllAlarmsAndRefreshWidgets(context, repository)

        val softDeletedIds = repository.getAllSoftDeletedEventIds()
        assertTrue(softDeletedIds.contains(deletedEventId))
        assertFalse(softDeletedIds.contains(activeEventId))

        val activeList = repository.getAllActiveEventsWithReminders()
        assertEquals(1, activeList.size)
        assertEquals(activeEventId, activeList[0].id)
    }
}
