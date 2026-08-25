package com.example.network

import android.content.Context
import android.util.Log
import com.example.data.AppDatabase
import com.example.data.entities.HolidayEntity
import com.example.models.Event
import com.example.models.EventCategory
import com.example.util.LunarCalendarHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

sealed class ApiSyncResult {
    data class Success(
        val holidays: List<HolidayEntity>,
        val rawDataSummary: String,
        val source: String
    ) : ApiSyncResult()

    data class Error(val message: String, val fallbackHolidays: List<HolidayEntity>) : ApiSyncResult()
}

class CalendarSyncRepository(
    private val context: Context,
    private val apiService: CalendarApiService = CalendarApiService.create()
) {
    private val db by lazy { AppDatabase.getDatabase(context) }
    private val dao by lazy { db.scheduleDao() }

    /**
     * Fetch holidays from API, extract the payload, save to Room DB, and return result
     */
    suspend fun fetchAndSyncHolidays(year: Int): ApiSyncResult = withContext(Dispatchers.IO) {
        try {
            Log.d("CalendarSync", "Calling Calendar API for year $year...")
            val dtoList = apiService.getPublicHolidays(year, "VN")
            
            val extractedHolidays = dtoList.map { dto ->
                HolidayEntity(
                    date = dto.date,
                    name = dto.localName ?: dto.name,
                    source = "Nager.Date API",
                    isLunarBased = dto.name.contains("Tet", ignoreCase = true) || 
                                   dto.name.contains("Hung", ignoreCase = true) ||
                                   dto.name.contains("Lunar", ignoreCase = true),
                    lastSyncedAt = System.currentTimeMillis()
                )
            }

            // Save to Room DB
            if (extractedHolidays.isNotEmpty()) {
                dao.insertHolidays(extractedHolidays)
            }

            val summary = StringBuilder().apply {
                append("✓ Đã nhận ${dtoList.size} ngày lễ từ API:\n")
                dtoList.forEach { dto ->
                    append("• [${dto.date}] ${dto.localName ?: dto.name} (${dto.name})\n")
                }
            }.toString()

            ApiSyncResult.Success(
                holidays = extractedHolidays,
                rawDataSummary = summary,
                source = "API Online (https://date.nager.at)"
            )
        } catch (e: Exception) {
            Log.e("CalendarSync", "API call failed, generating localized Vietnamese Lunar & Solar holidays: ${e.message}")
            
            // Fallback using high-precision Vietnamese Lunar Calendar calculation
            val fallbackList = generateLocalHolidaysForYear(year)
            dao.insertHolidays(fallbackList)

            val summary = StringBuilder().apply {
                append("ℹ API ngoại tuyến (${e.localizedMessage ?: "Network error"}).\n")
                append("✓ Đã tự động bóc tách & tính toán ${fallbackList.size} ngày lễ Âm - Dương chuẩn Việt Nam cho năm $year:\n")
                fallbackList.forEach { h ->
                    append("• [${h.date}] ${h.name} (${if (h.isLunarBased) "Âm lịch" else "Dương lịch"})\n")
                }
            }.toString()

            ApiSyncResult.Success(
                holidays = fallbackList,
                rawDataSummary = summary,
                source = "Thuật toán Lịch Âm - Dương Việt Nam (Hồ Ngọc Đức GMT+7)"
            )
        }
    }

    /**
     * Generate complete Vietnam holidays (Lunar & Solar) for a specific year
     */
    fun generateLocalHolidaysForYear(year: Int): List<HolidayEntity> {
        val list = mutableListOf<HolidayEntity>()
        val startDate = LocalDate.of(year, 1, 1)
        val endDate = LocalDate.of(year, 12, 31)

        var cur = startDate
        while (!cur.isAfter(endDate)) {
            val lunar = LunarCalendarHelper.convertSolarToLunar(cur)
            if (lunar.holidayName != null) {
                list.add(
                    HolidayEntity(
                        date = cur.format(DateTimeFormatter.ISO_LOCAL_DATE),
                        name = lunar.holidayName,
                        source = "Vietnamese Calendar Engine",
                        isLunarBased = !lunar.isLeap && (lunar.day == 1 || lunar.day == 15 || (lunar.month == 3 && lunar.day == 10) || (lunar.month == 8 && lunar.day == 15) || (lunar.month == 12 && lunar.day >= 23)),
                        lastSyncedAt = System.currentTimeMillis()
                    )
                )
            }
            cur = cur.plusDays(1)
        }
        return list
    }

    /**
     * Convert HolidayEntity into Event model for displaying on Calendar & Home
     */
    fun convertHolidayToEvent(holiday: HolidayEntity): Event {
        val date = LocalDate.parse(holiday.date)
        val eventId = if (holiday.id > 0) 100000 + holiday.id else ((holiday.date.hashCode() xor holiday.name.hashCode()) and 0x7FFFFFFF)
        return Event(
            id = eventId,
            title = " ${holiday.name}",
            startTime = date.atTime(0, 0),
            endTime = date.atTime(23, 59),
            category = EventCategory.HOLIDAY,
            isCompleted = false,
            reminderNote = "Ngày lễ: ${holiday.name} (${if (holiday.isLunarBased) "Dựa theo Âm lịch" else "Dương lịch"})",
            hasReminder = false
        )
    }
}
