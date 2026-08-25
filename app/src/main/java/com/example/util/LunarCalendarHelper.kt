package com.example.util

import java.time.LocalDate

/**
 * Data class representing a Vietnamese Lunar Date
 */
data class LunarDate(
    val day: Int,
    val month: Int,
    val year: Int,
    val isLeap: Boolean = false,
    val canChiYear: String = "",
    val canChiMonth: String = "",
    val canChiDay: String = "",
    val holidayName: String? = null,
    val isLunarHoliday: Boolean = false,
    val isImportantLunarDay: Boolean = false // Mùng 1 hoặc Rằm (15)
) {
    fun formatShort(): String {
        return if (day == 1 || day == 15) {
            "${day}/${month}"
        } else {
            "$day"
        }
    }

    fun formatFull(): String {
        val leapStr = if (isLeap) " (nhuận)" else ""
        return "Ngày $day tháng $month$leapStr năm $canChiYear"
    }
}

/**
 * Accurate Vietnamese Lunar Calendar converter using the astronomical algorithm (GMT+7).
 * Reference: Ho Ngoc Duc astronomical algorithm for Vietnamese Lunar Calendar.
 */
object LunarCalendarHelper {

    private val cache = java.util.concurrent.ConcurrentHashMap<LocalDate, LunarDate>()

    private val CAN = arrayOf("Giáp", "Ất", "Bính", "Đinh", "Mậu", "Kỷ", "Canh", "Tân", "Nhâm", "Quý")
    private val CHI = arrayOf("Tý", "Sửu", "Dần", "Mão", "Thìn", "Tỵ", "Ngọ", "Mùi", "Thân", "Dậu", "Tuất", "Hợi")

    // Solar Holidays (Dương lịch)
    private val SOLAR_HOLIDAYS = mapOf(
        "01-01" to "Tết Dương Lịch",
        "02-14" to "Lễ Tình Nhân (Valentine)",
        "03-08" to "Quốc tế Phụ nữ",
        "04-30" to "Ngày Giải phóng Miền Nam",
        "05-01" to "Quốc tế Lao động",
        "06-01" to "Quốc tế Thiếu nhi",
        "09-02" to "Quốc khánh Việt Nam",
        "10-20" to "Ngày Phụ nữ Việt Nam",
        "11-20" to "Ngày Nhà giáo Việt Nam",
        "12-24" to "Đêm Giáng sinh (Noel)",
        "12-25" to "Lễ Giáng sinh (Noel)"
    )

    // Lunar Holidays (Âm lịch)
    private val LUNAR_HOLIDAYS = mapOf(
        "01-01" to "Mùng 1 Tết Nguyên Đán",
        "01-02" to "Mùng 2 Tết Nguyên Đán",
        "01-03" to "Mùng 3 Tết Nguyên Đán",
        "01-15" to "Tết Nguyên Tiêu (Rằm tháng Giêng)",
        "03-10" to "Giỗ Tổ Hùng Vương",
        "04-15" to "Đại lễ Phật Đản",
        "05-05" to "Tết Đoan Ngọ",
        "07-15" to "Lễ Vu Lan (Rằm tháng 7)",
        "08-15" to "Tết Trung Thu",
        "12-23" to "Ông Táo chầu trời",
        "12-30" to "Đêm Giao Thừa (Tất Niên)",
        "12-29" to "Tất Niên (Tháng thiếu)"
    )

    private fun jdFromDate(dd: Int, mm: Int, yy: Int): Int {
        var y = yy
        var m = mm
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = y / 100
        val b = 2 - a + (a / 4)
        return (365.25 * (y + 4716)).toInt() + (30.6001 * (m + 1)).toInt() + dd + b - 1524
    }

    private fun getNewMoonDay(k: Int, timeZone: Double = 7.0): Int {
        val t = k / 1236.85
        val t2 = t * t
        val t3 = t2 * t
        val dr = Math.PI / 180.0
        var jd = 2415020.75933 + 29.53058868 * k + 0.0001178 * t2 - 0.000000155 * t3
        jd += 0.00033 * Math.sin((166.56 + 132.87 * t - 0.009173 * t2) * dr)

        val m = 359.2242 + 29.10535608 * k - 0.0000333 * t2 - 0.00000347 * t3
        val mpr = 306.0253 + 385.81691806 * k + 0.0107306 * t2 + 0.00001236 * t3
        val f = 21.2964 + 390.67050646 * k - 0.0016528 * t2 - 0.00000239 * t3

        val c1 = (0.1734 - 0.000393 * t) * Math.sin(m * dr) + 0.0021 * Math.sin(2 * m * dr)
        val c2 = -0.4068 * Math.sin(mpr * dr) + 0.0161 * Math.sin(2 * mpr * dr)
        val c3 = -0.0004 * Math.sin(3 * mpr * dr)
        val c4 = 0.0104 * Math.sin(2 * f * dr) - 0.0051 * Math.sin((m + mpr) * dr)
        val c5 = -0.0040 * Math.sin((m - mpr) * dr) + 0.0004 * Math.sin((2 * f + m) * dr)
        val c6 = -0.0004 * Math.sin((2 * f - m) * dr) - 0.0006 * Math.sin((2 * f + mpr) * dr)
        val c7 = 0.0010 * Math.sin((2 * f - mpr) * dr) + 0.0005 * Math.sin((m + 2 * mpr) * dr)

        val deltaT = 0.0
        val jdf = jd + c1 + c2 + c3 + c4 + c5 + c6 + c7 - deltaT + 0.5 + (timeZone / 24.0)
        return Math.floor(jdf).toInt()
    }

    private fun getSunLongitude(dayNumber: Int, timeZone: Double = 7.0): Int {
        val t = (dayNumber - 0.5 - timeZone / 24.0 - 2451545.0) / 36525.0
        val t2 = t * t
        val dr = Math.PI / 180.0
        val l0 = 280.46646 + 36000.76983 * t + 0.0003032 * t2
        val m = 357.52911 + 35999.05029 * t - 0.0001537 * t2
        val c = (1.914602 - 0.004817 * t - 0.000014 * t2) * Math.sin(m * dr) +
                (0.019993 - 0.000101 * t) * Math.sin(2 * m * dr) +
                0.000289 * Math.sin(3 * m * dr)
        var l = l0 + c
        l *= dr
        l -= Math.PI * 2 * Math.floor(l / (Math.PI * 2))
        return Math.floor(l / (Math.PI / 6)).toInt()
    }

    private fun getLunarMonth11(yy: Int, timeZone: Double = 7.0): Int {
        val off = jdFromDate(31, 12, yy) - 2415021
        var k = Math.floor(off / 29.530588853).toInt()
        var nm = getNewMoonDay(k, timeZone)
        var sunLong = getSunLongitude(nm, timeZone)
        var count = 0
        while (sunLong >= 9 && count < 15) {
            k--
            nm = getNewMoonDay(k, timeZone)
            sunLong = getSunLongitude(nm, timeZone)
            count++
        }
        return k
    }

    private fun getLeapMonthOffset(a11: Int, timeZone: Double = 7.0): Int {
        var k = a11
        var last: Int
        var i = 1
        var arc = getSunLongitude(getNewMoonDay(k, timeZone), timeZone)
        do {
            last = arc
            k++
            arc = getSunLongitude(getNewMoonDay(k, timeZone), timeZone)
            i++
        } while (arc != last && i < 14)
        return i - 1
    }

    fun convertSolarToLunar(solarDate: LocalDate): LunarDate {
        return cache.getOrPut(solarDate) {
            calculateLunar(solarDate)
        }
    }

    private fun calculateLunar(solarDate: LocalDate): LunarDate {
        val dd = solarDate.dayOfMonth
        val mm = solarDate.monthValue
        val yy = solarDate.year

        val timeZone = 7.0
        val dayNumber = jdFromDate(dd, mm, yy)
        val k = Math.floor((dayNumber - 2415021.076998695) / 29.530588853).toInt()

        var monthStart = getNewMoonDay(k + 1, timeZone)
        var currentK = k
        if (monthStart > dayNumber) {
            monthStart = getNewMoonDay(k, timeZone)
        } else {
            currentK = k + 1
        }

        val lunarDay = dayNumber - monthStart + 1

        val a11 = getLunarMonth11(if (mm >= 11) yy else yy - 1, timeZone)
        val b11 = getLunarMonth11(if (mm >= 11) yy - 1 else yy - 2, timeZone)

        val leapMonthDiff = a11 - b11
        var isLeap = false
        var lunarMonth: Int
        var lunarYear: Int

        if (leapMonthDiff <= 12) {
            var count = currentK - b11
            while (count < 0) count += 12
            lunarMonth = (count + 10) % 12 + 1
            lunarYear = yy
            if (lunarMonth >= 11 && mm <= 2) lunarYear = yy - 1
            if (lunarMonth <= 2 && mm >= 11) lunarYear = yy + 1
        } else {
            val leapOff = getLeapMonthOffset(b11, timeZone)
            val count = currentK - b11
            if (count == leapOff) {
                isLeap = true
                lunarMonth = safeMod(leapOff - 3, 12) + 1
            } else if (count > leapOff) {
                lunarMonth = safeMod(count - 4, 12) + 1
            } else {
                lunarMonth = safeMod(count - 3, 12) + 1
            }
            lunarYear = yy
            if (mm <= 2 && lunarMonth >= 11) lunarYear = yy - 1
            if (mm >= 11 && lunarMonth <= 2) lunarYear = yy + 1
        }

        // Clamp lunarDay and lunarMonth to valid ranges
        val clampedDay = lunarDay.coerceIn(1, 30)
        val clampedMonth = lunarMonth.coerceIn(1, 12)

        // Safe modulo for Can Chi
        val canChiYear = getCanChiYear(lunarYear)
        val canChiMonth = getCanChiMonth(clampedMonth, lunarYear)
        val canChiDay = getCanChiDay(dayNumber)

        val solarKey = String.format("%02d-%02d", mm, dd)
        val lunarKey = String.format("%02d-%02d", clampedMonth, clampedDay)
        val solarHoliday = SOLAR_HOLIDAYS[solarKey]
        val lunarHoliday = if (!isLeap) LUNAR_HOLIDAYS[lunarKey] else null
        val holiday = solarHoliday ?: lunarHoliday
        val isLunarHoliday = solarHoliday == null && lunarHoliday != null
        val isImportant = (clampedDay == 1 || clampedDay == 15)

        return LunarDate(
            day = clampedDay,
            month = clampedMonth,
            year = lunarYear,
            isLeap = isLeap,
            canChiYear = canChiYear,
            canChiMonth = canChiMonth,
            canChiDay = canChiDay,
            holidayName = holiday,
            isLunarHoliday = isLunarHoliday,
            isImportantLunarDay = isImportant
        )
    }

    private fun safeMod(n: Int, m: Int): Int {
        val r = n % m
        return if (r < 0) r + m else r
    }

    private fun getCanChiYear(year: Int): String {
        val can = CAN[safeMod(year + 6, 10)]
        val chi = CHI[safeMod(year + 8, 12)]
        return "$can $chi"
    }

    private fun getCanChiMonth(month: Int, year: Int): String {
        val can = CAN[safeMod(year * 12 + month + 3, 10)]
        val chi = CHI[safeMod(month + 1, 12)]
        return "$can $chi"
    }

    private fun getCanChiDay(dayNumber: Int): String {
        val can = CAN[safeMod(dayNumber + 9, 10)]
        val chi = CHI[safeMod(dayNumber + 1, 12)]
        return "$can $chi"
    }

    fun getHoliday(date: LocalDate): String? {
        return convertSolarToLunar(date).holidayName
    }
}

