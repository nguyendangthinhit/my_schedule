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
    fun testLunarImportantHolidays() {
        val testDate = LocalDate.of(2026, 2, 17)
        val lunar = LunarCalendarHelper.convertSolarToLunar(testDate)
        assertNotNull(lunar)
        assertTrue(lunar.canChiYear.isNotBlank())
    }
}
