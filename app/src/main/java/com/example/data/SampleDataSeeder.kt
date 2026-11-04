package com.example.data

import android.content.Context
import com.example.data.dao.ScheduleDao
import com.example.data.entities.BoardEntity
import com.example.data.entities.EventCompletionEntity
import com.example.data.entities.EventEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SampleDataSeeder {

    private const val PREFS_NAME = "sample_data_prefs"
    private const val KEY_SEEDED_AUG_2026_V1 = "seeded_sample_data_aug_2026_v1"

    fun getSampleEvents(): List<EventEntity> {
        val events = mutableListOf<EventEntity>()

        // 1/8: Đi chơi ở Hà Nội (cả buổi sáng, cả chiều đến 23h00) (Đi chơi) - Hoàn thành
        events.add(
            EventEntity(
                title = "Đi chơi ở Hà Nội",
                boardId = DefaultBoards.BOARD_PLAY_ID, // Đi chơi
                startDate = "2026-08-01",
                startTime = "08:00",
                endTime = "23:00",
                isCompleted = true
            )
        )

        // 2/8: Mua sắm đồ ở KTX (buổi chiều và tối) (Công việc) - Hoàn thành
        events.add(
            EventEntity(
                title = "Mua sắm đồ ở KTX",
                boardId = DefaultBoards.BOARD_WORK_ID, // Công việc
                startDate = "2026-08-02",
                startTime = "14:00",
                endTime = "21:00",
                isCompleted = true
            )
        )

        // Từ 3/8 đến 7/8 (Thứ 2 đến Thứ 6):
        val week1Days = listOf("2026-08-03", "2026-08-04", "2026-08-05", "2026-08-06", "2026-08-07")
        for (date in week1Days) {
            // Sáng từ 8h30 đến 12h00
            events.add(
                EventEntity(
                    title = "Thực tập tại Samsung R&D Hà Nội",
                    boardId = DefaultBoards.BOARD_WORK_ID, // Công việc
                    startDate = date,
                    startTime = "08:30",
                    endTime = "12:00",
                    isCompleted = true
                )
            )
            // Chiều từ 13h00 đến 17h30
            events.add(
                EventEntity(
                    title = "Thực tập tại Samsung R&D Hà Nội",
                    boardId = DefaultBoards.BOARD_WORK_ID, // Công việc
                    startDate = date,
                    startTime = "13:00",
                    endTime = "17:30",
                    isCompleted = true
                )
            )
            // Tối 20h00 - 21h00 Học
            events.add(
                EventEntity(
                    title = "Học",
                    boardId = DefaultBoards.BOARD_STUDY_ID, // Học tập
                    startDate = date,
                    startTime = "20:00",
                    endTime = "21:00",
                    isCompleted = true
                )
            )
        }

        // 20h30 3/8: Xem bóng đá Việt Nam - Indonesia (Đi chơi) - Hoàn thành
        events.add(
            EventEntity(
                title = "Xem bóng đá Việt Nam - Indonesia",
                boardId = DefaultBoards.BOARD_PLAY_ID, // Đi chơi
                startDate = "2026-08-03",
                startTime = "20:30",
                endTime = "22:30",
                isCompleted = true
            )
        )

        // 8/8: Đi chơi Hà Nội (cả buổi sáng, cả chiều đến 23h00) (Đi chơi) - Hoàn thành
        events.add(
            EventEntity(
                title = "Đi chơi Hà Nội",
                boardId = DefaultBoards.BOARD_PLAY_ID, // Đi chơi
                startDate = "2026-08-08",
                startTime = "08:00",
                endTime = "23:00",
                isCompleted = true
            )
        )

        // Từ 10/8 đến 14/8:
        val week2Days = listOf("2026-08-10", "2026-08-11", "2026-08-12", "2026-08-13", "2026-08-14")
        for (date in week2Days) {
            // Sáng từ 8h30 đến 12h00
            events.add(
                EventEntity(
                    title = "Thực tập tại Samsung R&D Hà Nội",
                    boardId = DefaultBoards.BOARD_WORK_ID, // Công việc
                    startDate = date,
                    startTime = "08:30",
                    endTime = "12:00",
                    isCompleted = true
                )
            )
            // Chiều từ 13h00 đến 17h30
            events.add(
                EventEntity(
                    title = "Thực tập tại Samsung R&D Hà Nội",
                    boardId = DefaultBoards.BOARD_WORK_ID, // Công việc
                    startDate = date,
                    startTime = "13:00",
                    endTime = "17:30",
                    isCompleted = true
                )
            )
            // Tối 20h00 - 21h00 Học
            events.add(
                EventEntity(
                    title = "Học",
                    boardId = DefaultBoards.BOARD_STUDY_ID, // Học tập
                    startDate = date,
                    startTime = "20:00",
                    endTime = "21:00",
                    isCompleted = true
                )
            )
        }

        // 18h00 - 23h00 14/8: Đi chơi ở Hà Nội (Đi chơi) - Hoàn thành
        events.add(
            EventEntity(
                title = "Đi chơi ở Hà Nội",
                boardId = DefaultBoards.BOARD_PLAY_ID, // Đi chơi
                startDate = "2026-08-14",
                startTime = "18:00",
                endTime = "23:00",
                isCompleted = true
            )
        )

        // 15/8: Đi chơi Hà Nội (cả buổi sáng, cả chiều đến 23h00) (Đi chơi) - Hoàn thành
        events.add(
            EventEntity(
                title = "Đi chơi Hà Nội",
                boardId = DefaultBoards.BOARD_PLAY_ID, // Đi chơi
                startDate = "2026-08-15",
                startTime = "08:00",
                endTime = "23:00",
                isCompleted = true
            )
        )

        // Từ 17/8 đến 21/8:
        val week3Days = listOf("2026-08-17", "2026-08-18", "2026-08-19", "2026-08-20", "2026-08-21")
        for (date in week3Days) {
            // Sáng từ 8h30 đến 12h00
            events.add(
                EventEntity(
                    title = "Thực tập tại Samsung R&D Hà Nội",
                    boardId = DefaultBoards.BOARD_WORK_ID, // Công việc
                    startDate = date,
                    startTime = "08:30",
                    endTime = "12:00",
                    isCompleted = true
                )
            )
            // Chiều từ 13h00 đến 17h30
            events.add(
                EventEntity(
                    title = "Thực tập tại Samsung R&D Hà Nội",
                    boardId = DefaultBoards.BOARD_WORK_ID, // Công việc
                    startDate = date,
                    startTime = "13:00",
                    endTime = "17:30",
                    isCompleted = true
                )
            )
            // Tối 20h00 - 21h00 Học
            events.add(
                EventEntity(
                    title = "Học",
                    boardId = DefaultBoards.BOARD_STUDY_ID, // Học tập
                    startDate = date,
                    startTime = "20:00",
                    endTime = "21:00",
                    isCompleted = true
                )
            )
        }

        // 21h00 - 23h00 21/8: Hát Karaoke (Đi chơi) - Hoàn thành
        events.add(
            EventEntity(
                title = "Hát Karaoke",
                boardId = DefaultBoards.BOARD_PLAY_ID, // Đi chơi
                startDate = "2026-08-21",
                startTime = "21:00",
                endTime = "23:00",
                isCompleted = true
            )
        )

        // 22/8: Đi chơi Hà Nội (cả buổi sáng, cả chiều đến 18h00) (Đi chơi) - KHÔNG HOÀN THÀNH
        events.add(
            EventEntity(
                title = "Đi chơi Hà Nội",
                boardId = DefaultBoards.BOARD_PLAY_ID, // Đi chơi
                startDate = "2026-08-22",
                startTime = "08:00",
                endTime = "18:00",
                isCompleted = false // CHƯA HOÀN THÀNH
            )
        )

        // 22/8: 20h - 22h Xem bóng đá Việt Nam - Thái Lan (Đi chơi) - Hoàn thành
        events.add(
            EventEntity(
                title = "Xem bóng đá Việt Nam - Thái Lan",
                boardId = DefaultBoards.BOARD_PLAY_ID, // Đi chơi
                startDate = "2026-08-22",
                startTime = "20:00",
                endTime = "22:00",
                isCompleted = true
            )
        )

        return events
    }

    suspend fun seedSampleDataIfNeeded(context: Context, repository: EventRepository) = withContext(Dispatchers.IO) {
        // Ensure all 10 default categories exist in SQL boards table
        val defaultBoards = listOf(
            BoardEntity(id = DefaultBoards.BOARD_WORK_ID, name = "Công việc", colorHex = "#3B82F6", isVisible = true, sortOrder = 0),
            BoardEntity(id = DefaultBoards.BOARD_STUDY_ID, name = "Học tập", colorHex = "#10B981", isVisible = true, sortOrder = 1),
            BoardEntity(id = DefaultBoards.BOARD_PLAY_ID, name = "Đi chơi", colorHex = "#06B6D4", isVisible = true, sortOrder = 2),
            BoardEntity(id = DefaultBoards.BOARD_MEETING_ID, name = "Cuộc họp", colorHex = "#F59E0B", isVisible = true, sortOrder = 3),
            BoardEntity(id = DefaultBoards.BOARD_HOLIDAY_ID, name = "Ngày lễ", colorHex = "#EF4444", isVisible = true, sortOrder = 4),
            BoardEntity(id = DefaultBoards.BOARD_PERSONAL_ID, name = "Cá nhân", colorHex = "#8B5CF6", isVisible = true, sortOrder = 5),
            BoardEntity(id = DefaultBoards.BOARD_HEALTH_ID, name = "Sức khỏe", colorHex = "#EC4899", isVisible = true, sortOrder = 6),
            BoardEntity(id = DefaultBoards.BOARD_FINANCE_ID, name = "Tài chính", colorHex = "#14B8A6", isVisible = true, sortOrder = 7),
            BoardEntity(id = DefaultBoards.BOARD_TASK_ID, name = "Việc cần làm", colorHex = "#6366F1", isVisible = true, sortOrder = 8),
            BoardEntity(id = DefaultBoards.BOARD_FAMILY_ID, name = "Gia đình", colorHex = "#F97316", isVisible = true, sortOrder = 9)
        )
        val currentBoards = try { repository.getAllBoardsList() } catch (e: Exception) { emptyList() }
        val currentNames = currentBoards.map { it.name.trim().lowercase() }.toSet()
        val currentIds = currentBoards.map { it.id }.toSet()
        for (board in defaultBoards) {
            if (!currentNames.contains(board.name.trim().lowercase()) && !currentIds.contains(board.id)) {
                repository.insertBoard(board)
            }
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val alreadySeeded = prefs.getBoolean(KEY_SEEDED_AUG_2026_V1, false)

        val existingEvents = repository.getAllEventsList()

        // Deduplicate any existing duplicate events (e.g. from previous double initialization)
        if (existingEvents.isNotEmpty()) {
            val groups = existingEvents.groupBy { "${it.title}|${it.startDate}|${it.startTime}|${it.endTime}|${it.boardId}" }
            for ((_, group) in groups) {
                if (group.size > 1) {
                    for (duplicate in group.drop(1)) {
                        repository.deleteEventById(duplicate.id)
                    }
                }
            }
        }

        if (!alreadySeeded && existingEvents.isEmpty()) {
            val sampleEvents = getSampleEvents()
            for (event in sampleEvents) {
                repository.insertEvent(event, emptyList())
                if (event.isCompleted) {
                    repository.setCompletion(event.id, event.startDate, true)
                }
            }
        }

        prefs.edit().putBoolean(KEY_SEEDED_AUG_2026_V1, true).apply()
    }
}
