package com.example.data

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
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
                boardId = 3, // Đi chơi
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
                boardId = 1, // Công việc
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
                    boardId = 1, // Công việc
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
                    boardId = 1, // Công việc
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
                    boardId = 2, // Học tập
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
                boardId = 3, // Đi chơi
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
                boardId = 3, // Đi chơi
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
                    boardId = 1, // Công việc
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
                    boardId = 1, // Công việc
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
                    boardId = 2, // Học tập
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
                boardId = 3, // Đi chơi
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
                boardId = 3, // Đi chơi
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
                    boardId = 1, // Công việc
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
                    boardId = 1, // Công việc
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
                    boardId = 2, // Học tập
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
                boardId = 3, // Đi chơi
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
                boardId = 3, // Đi chơi
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
                boardId = 3, // Đi chơi
                startDate = "2026-08-22",
                startTime = "20:00",
                endTime = "22:00",
                isCompleted = true
            )
        )

        return events
    }

    suspend fun seedSampleDataIfNeeded(context: Context, dao: ScheduleDao) = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val alreadySeeded = prefs.getBoolean(KEY_SEEDED_AUG_2026_V1, false)

        if (!alreadySeeded) {
            val sampleEvents = getSampleEvents()
            for (event in sampleEvents) {
                val id = dao.insertEvent(event).toInt()
                if (event.isCompleted) {
                    dao.insertCompletion(
                        EventCompletionEntity(
                            eventId = id,
                            occurrenceDate = event.startDate,
                            isCompleted = true,
                            completedAt = System.currentTimeMillis()
                        )
                    )
                }
            }
            prefs.edit().putBoolean(KEY_SEEDED_AUG_2026_V1, true).apply()
        }
    }

    fun seedDatabaseRaw(db: SupportSQLiteDatabase) {
        val sampleEvents = getSampleEvents()
        for (event in sampleEvents) {
            val completedInt = if (event.isCompleted) 1 else 0
            val sql = """
                INSERT INTO events (title, description, board_id, start_date, start_time, end_time, is_all_day, repeat_rule, is_completed, has_reminder, created_at, updated_at)
                VALUES ('${event.title.replace("'", "''")}', null, ${event.boardId}, '${event.startDate}', '${event.startTime}', '${event.endTime}', 0, 'NONE', $completedInt, 0, ${System.currentTimeMillis()}, ${System.currentTimeMillis()})
            """.trimIndent()
            db.execSQL(sql)
        }
    }
}
