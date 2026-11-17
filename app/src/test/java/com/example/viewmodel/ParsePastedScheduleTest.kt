package com.example.viewmodel

import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ParsePastedScheduleTest {

    private val vm = AIChatViewModel()
    private val today = LocalDate.of(2026, 11, 5)

    @Test
    fun `parses all 35 sessions of the real pasted schedule`() {
        val items = vm.parsePastedSchedule(FULL_SCHEDULE, today)
        assertEquals(35, items.size)
        assertEquals(LocalDate.of(2026, 11, 2), items.first().date)
        assertEquals(LocalDate.of(2026, 11, 28), items.last().date)
        assertTrue(items.all { it.endTime.isAfter(it.startTime) })
    }

    @Test
    fun `splits title and location at first comma outside parentheses`() {
        val item = vm.parsePastedSchedule(
            "Thứ 2 (02/11)\n- Hệ Phân Tán (J2EE, .NET), P. 506, 78A Phan Văn Trị — 09:15–11:15.",
            today
        ).single()
        assertEquals("Hệ Phân Tán (J2EE, .NET)", item.title)
        assertEquals("P. 506, 78A Phan Văn Trị", item.reminderNote)
        assertEquals(LocalTime.of(9, 15), item.startTime)
        assertEquals(LocalTime.of(11, 15), item.endTime)
        assertEquals(LocalDate.of(2026, 11, 2), item.date)
    }

    @Test
    fun `handles Chủ nhật header and location containing a hyphen`() {
        val items = vm.parsePastedSchedule(
            "Chủ nhật (08/11)\n- Hệ Phân Tán (J2EE, .NET), P. 1001, 254 Nguyễn Văn Linh — 17:45–19:45.\n" +
                "Thứ 2 (09/11)\n- Đồ Án Chuyên Ngành: Khoa Học Dữ Liệu, P. 108, Hòa Khánh Nam - Tòa nhà F — 17:45–21:00.",
            today
        )
        assertEquals(2, items.size)
        assertEquals(LocalDate.of(2026, 11, 8), items[0].date)
        assertEquals("Đồ Án Chuyên Ngành: Khoa Học Dữ Liệu", items[1].title)
        assertEquals("P. 108, Hòa Khánh Nam - Tòa nhà F", items[1].reminderNote)
    }

    @Test
    fun `returns empty for ordinary chat and for items without a date header`() {
        assertTrue(vm.parsePastedSchedule("phân bổ lịch học cho ngày mai giúp mình", today).isEmpty())
        assertTrue(vm.parsePastedSchedule("- Môn A — 09:00–10:00", today).isEmpty())
    }

    private companion object {
        val FULL_SCHEDULE = """
thêm lịch cho mình
Thứ 2 (02/11)
- Hệ Phân Tán (J2EE, .NET), P. 506, 78A Phan Văn Trị — 09:15–11:15.
- Đồ Án Chuyên Ngành: Khoa Học Dữ Liệu, P. 108, Hòa Khánh Nam - Tòa nhà F — 17:45–21:00.
Thứ 3 (03/11)
- Hệ Phân Tán (J2EE, .NET), P. 609, K7/25 Quang Trung — 09:15–11:15.
- Blockchain: Theory & Practices, P. 1102, 254 Nguyễn Văn Linh — 13:00–16:15.
Thứ 5 (05/11)
- Hệ Phân Tán (J2EE, .NET), Online 20 — 09:15–11:15.
Thứ 6 (06/11)
- Hệ Phân Tán (J2EE, .NET), P. 609, K7/25 Quang Trung — 09:15–11:15.
- Chủ Nghĩa Xã Hội Khoa Học, P. 504, Hòa Khánh Nam - Tòa nhà C — 17:45–21:00.
Thứ 7 (07/11)
- Đồ Án CDIO I, P. 305, 78A Phan Văn Trị — 07:00–11:15.
Chủ nhật (08/11)
- Hệ Phân Tán (J2EE, .NET), P. 1001, 254 Nguyễn Văn Linh — 17:45–19:45.
Thứ 2 (09/11)
- Hệ Phân Tán (J2EE, .NET), P. 506, 78A Phan Văn Trị — 09:15–11:15.
- Đồ Án Chuyên Ngành: Khoa Học Dữ Liệu, P. 108, Hòa Khánh Nam - Tòa nhà F — 17:45–21:00.
Thứ 3 (10/11)
- Hệ Phân Tán (J2EE, .NET), P. 609, K7/25 Quang Trung — 09:15–11:15.
- Blockchain: Theory & Practices, P. 503, 254 Nguyễn Văn Linh — 13:00–16:15.
Thứ 5 (12/11)
- Hệ Phân Tán (J2EE, .NET), Online 20 — 09:15–11:15.
- Đồ Án Chuyên Ngành: Khoa Học Dữ Liệu, P. 108, Hòa Khánh Nam - Tòa nhà F — 17:45–21:00.
Thứ 6 (13/11)
- Hệ Phân Tán (J2EE, .NET), P. 609, K7/25 Quang Trung — 09:15–11:15.
- Chủ Nghĩa Xã Hội Khoa Học, P. Bảo tàng Hoàng Sa (Thọ Quang - Sơn Trà), Đà Ngoại — 13:00–16:15.
- Đồ Án Chuyên Ngành: Khoa Học Dữ Liệu, P. 108, Hòa Khánh Nam - Tòa nhà F — 17:45–21:00.
Thứ 7 (14/11)
- Đồ Án CDIO I, P. 305, 78A Phan Văn Trị — 07:00–11:15.
Thứ 2 (16/11)
- Hệ Phân Tán (J2EE, .NET), P. 506, 78A Phan Văn Trị — 09:15–11:15.
- Đồ Án Chuyên Ngành: Khoa Học Dữ Liệu, P. 108, Hòa Khánh Nam - Tòa nhà F — 17:45–21:00.
Thứ 3 (17/11)
- Hệ Phân Tán (J2EE, .NET), P. 609, K7/25 Quang Trung — 09:15–11:15.
- Blockchain: Theory & Practices, P. 1102, 254 Nguyễn Văn Linh — 13:00–16:15.
Thứ 5 (19/11)
- Hệ Phân Tán (J2EE, .NET), Online 20 — 09:15–11:15.
- Đồ Án Chuyên Ngành: Khoa Học Dữ Liệu, P. 108, Hòa Khánh Nam - Tòa nhà F — 17:45–21:00.
Thứ 6 (20/11)
- Hệ Phân Tán (J2EE, .NET), P. 609, K7/25 Quang Trung — 09:15–11:15.
- Chủ Nghĩa Xã Hội Khoa Học, P. Bảo tàng Hoàng Sa (Thọ Quang - Sơn Trà), Đà Ngoại — 13:00–16:15.
Thứ 7 (21/11)
- Đồ Án CDIO I, P. 305, 78A Phan Văn Trị — 07:00–11:15.
Thứ 2 (23/11)
- Hệ Phân Tán (J2EE, .NET), P. 506, 78A Phan Văn Trị — 09:15–11:15.
- Đồ Án Chuyên Ngành: Khoa Học Dữ Liệu, P. 108, Hòa Khánh Nam - Tòa nhà F — 17:45–21:00.
Thứ 5 (26/11)
- Hệ Phân Tán (J2EE, .NET), Online 20 — 09:15–11:15.
- Blockchain: Theory & Practices, P. 411, Hòa Khánh Nam - Tòa nhà G — 17:45–21:00.
Thứ 6 (27/11)
- Hệ Phân Tán (J2EE, .NET), P. 609, K7/25 Quang Trung — 09:15–11:15.
- Chủ Nghĩa Xã Hội Khoa Học, P. 504, Hòa Khánh Nam - Tòa nhà C — 17:45–21:00.
Thứ 7 (28/11)
- Đồ Án CDIO I, P. 305, 78A Phan Văn Trị — 07:00–11:15.

        """.trimIndent()
    }
}
