package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.models.Event
import com.example.models.EventCategory
import com.example.util.DateUtils
import com.example.viewmodel.ScheduleViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

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
    fun `test schedule view model operations`() {
        val vm = ScheduleViewModel()
        val initialCount = vm.events.value.size
        assertTrue(initialCount > 0)

        // Test add event
        val testEvent = Event(
            id = "test_event_123",
            title = "Test Automation Event",
            startTime = LocalDateTime.now(),
            endTime = LocalDateTime.now().plusHours(1),
            category = EventCategory.WORK,
            isCompleted = false
        )
        vm.addEvent(testEvent)
        assertEquals(initialCount + 1, vm.events.value.size)

        // Test toggle completion
        vm.toggleEventCompletion("test_event_123")
        val updated = vm.events.value.first { it.id == "test_event_123" }
        assertTrue(updated.isCompleted)

        vm.toggleEventCompletion("test_event_123")
        val updatedAgain = vm.events.value.first { it.id == "test_event_123" }
        assertFalse(updatedAgain.isCompleted)

        // Test delete event
        vm.deleteEvent("test_event_123")
        assertEquals(initialCount, vm.events.value.size)
    }

    @Test
    fun `test category management and cascade deletion of events`() {
        val vm = ScheduleViewModel()
        val initialCategoriesCount = vm.categories.value.size
        assertTrue(initialCategoriesCount >= 4)

        // 1. Add custom category
        val customCat = vm.addCategory(
            title = "Dự án mới",
            color = androidx.compose.ui.graphics.Color(0xFF6366F1),
            bgColor = androidx.compose.ui.graphics.Color(0xFFEEF2FF)
        )
        assertEquals(initialCategoriesCount + 1, vm.categories.value.size)
        assertTrue(vm.categories.value.any { it.id == customCat.id })

        // 2. Add events with this new category
        val event1 = Event(
            id = "custom_event_1",
            title = "Task trong dự án mới",
            startTime = LocalDateTime.now(),
            endTime = LocalDateTime.now().plusHours(1),
            category = customCat
        )
        val event2 = Event(
            id = "custom_event_2",
            title = "Review dự án mới",
            startTime = LocalDateTime.now().plusHours(2),
            endTime = LocalDateTime.now().plusHours(3),
            category = customCat
        )
        vm.addEvent(event1)
        vm.addEvent(event2)

        assertTrue(vm.events.value.any { it.id == "custom_event_1" })
        assertTrue(vm.events.value.any { it.id == "custom_event_2" })

        // 3. Delete the category - should delete category and cascade delete all associated events
        vm.deleteCategory(customCat.id)

        // Verify category is removed
        assertFalse(vm.categories.value.any { it.id == customCat.id })
        assertEquals(initialCategoriesCount, vm.categories.value.size)

        // Verify associated events are removed
        assertFalse(vm.events.value.any { it.id == "custom_event_1" })
        assertFalse(vm.events.value.any { it.id == "custom_event_2" })
    }
}
