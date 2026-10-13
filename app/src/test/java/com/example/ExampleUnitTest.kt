package com.example

import com.example.util.LunarCalendarHelper
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class ExampleUnitTest {
    @Test
    fun testLunarHelper() {
        val date = LocalDate.now()
        val lunar = LunarCalendarHelper.convertSolarToLunar(date)
        assertNotNull(lunar)
        assertTrue(lunar.day in 1..30)
        assertTrue(lunar.month in 1..12)
    }

    @Test
    fun testLunarMonth11() {
        val date2024 = LocalDate.of(2024, 10, 13)
        val lunar2024 = LunarCalendarHelper.convertSolarToLunar(date2024)
        assertEquals(11, lunar2024.day)
        assertEquals(9, lunar2024.month)

        val date2026 = LocalDate.of(2026, 10, 13)
        val lunar2026 = LunarCalendarHelper.convertSolarToLunar(date2026)
        assertEquals(4, lunar2026.day)
        assertEquals(9, lunar2026.month)

        val tet2024 = LocalDate.of(2024, 2, 10)
        val lunarTet2024 = LunarCalendarHelper.convertSolarToLunar(tet2024)
        assertEquals(1, lunarTet2024.day)
        assertEquals(1, lunarTet2024.month)
    }
}
