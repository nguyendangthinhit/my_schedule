package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.EventRepository
import com.example.models.Event
import com.example.models.EventCategory
import com.example.util.DateUtils
import com.example.viewmodel.ScheduleViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: EventRepository

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = EventRepository(db.scheduleDao())
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("My_schedule", appName)
    }

    @Test
    fun `test date utils vietnamese translations`() {
        assertEquals("T2", DateUtils.getVietnameseDayOfWeek(DayOfWeek.MONDAY, short = true))
        assertEquals("Thứ Hai", DateUtils.getVietnameseDayOfWeek(DayOfWeek.MONDAY, short = false))
        assertEquals("CN", DateUtils.getVietnameseDayOfWeek(DayOfWeek.SUNDAY, short = true))
        assertEquals("Chủ Nhật", DateUtils.getVietnameseDayOfWeek(DayOfWeek.SUNDAY, short = false))

        val date = LocalDate.of(2026, 8, 22) // Saturday
        val formatted = DateUtils.formatFullVietnameseDate(date)
        assertTrue(formatted.contains("Thứ Bảy"))
        assertTrue(formatted.contains("22"))
        assertTrue(formatted.contains("8"))
        assertTrue(formatted.contains("2026"))
    }

    @Test
    fun `test lunar calendar toggle in viewmodel`() = runTest {
        val vm = ScheduleViewModel(repository)
        assertTrue(vm.showLunarCalendar.value)

        vm.setShowLunarCalendar(false)
        assertEquals(false, vm.showLunarCalendar.value)

        vm.setShowLunarCalendar(true)
        assertEquals(true, vm.showLunarCalendar.value)
    }
}
