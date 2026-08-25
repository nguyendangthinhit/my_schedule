package com.example.analytics

import androidx.compose.ui.graphics.Color
import com.example.data.entities.BoardEntity
import com.example.data.entities.EventCompletionEntity
import com.example.data.entities.EventEntity
import com.example.data.entities.HolidayEntity
import com.example.models.dashboard.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

data class ComputedTaskOccurrence(
    val id: Int,
    val title: String,
    val boardId: Int,
    val categoryName: String,
    val categoryEmoji: String,
    val colorHex: String,
    val date: String, // yyyy-MM-dd
    val startTime: String, // HH:mm
    val endTime: String, // HH:mm
    val isCompleted: Boolean,
    val priority: String
)

class AnalyticsEngine {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val displayDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    /**
     * Compute comprehensive Dashboard Overview analytics from Room Database entities
     */
    fun calculateOverviewFromDatabase(
        boards: List<BoardEntity>,
        events: List<EventEntity>,
        completions: List<EventCompletionEntity>,
        holidays: List<HolidayEntity>,
        period: PeriodType = PeriodType.WEEK,
        anchorDate: LocalDate = LocalDate.now()
    ): Pair<DashboardOverviewData, AiDashboardExportData> {
        val categories = mapBoardsToCategories(boards)
        val boardMap = categories.associateBy { it.id }

        // Compute date range for current period and previous period
        val (currStart, currEnd, dateRangeStr) = getPeriodRange(period, anchorDate)
        val (prevStart, prevEnd, _) = getPreviousPeriodRange(period, anchorDate)

        // Expand all events across the required window
        val lookbackStart = prevStart.minusWeeks(3) // 4 weeks before for trend line
        val allOccurrences = expandEventOccurrences(events, completions, boardMap, lookbackStart, currEnd)

        val currentTasks = allOccurrences.filter {
            val d = parseDate(it.date)
            d != null && !d.isBefore(currStart) && !d.isAfter(currEnd)
        }

        val previousTasks = allOccurrences.filter {
            val d = parseDate(it.date)
            d != null && !d.isBefore(prevStart) && !d.isAfter(prevEnd)
        }

        val totalTasks = currentTasks.size
        val completedTasks = currentTasks.count { it.isCompleted }
        val inProgressTasks = currentTasks.count { !it.isCompleted && it.date == anchorDate.format(dateFormatter) }
        val incompleteTasks = (totalTasks - completedTasks - inProgressTasks).coerceAtLeast(0)

        val completionRate = if (totalTasks > 0) ((completedTasks.toFloat() / totalTasks) * 100).toInt() else 0

        val prevTotal = previousTasks.size
        val prevCompleted = previousTasks.count { it.isCompleted }
        val prevCompletionRate = if (prevTotal > 0) ((prevCompleted.toFloat() / prevTotal) * 100).toInt() else 0

        val trendDiff = completionRate - prevCompletionRate
        val isTrendPositive = trendDiff >= 0

        val motivationMessage = if (totalTasks == 0) {
            "Hãy bắt đầu lên kế hoạch và hoàn thành các mục tiêu của bạn! ✨"
        } else if (isTrendPositive) {
            "Bạn đang làm tốt hơn ${Math.abs(trendDiff)}% so với giai đoạn trước. Cố gắng duy trì nhé! 💪"
        } else {
            "Tiến độ giảm ${Math.abs(trendDiff)}% so với giai đoạn trước. Hãy cố gắng tập trung hơn nhé! 🎯"
        }

        // Category progress breakdown
        val categoryProgressList = categories.map { cat ->
            val catCurrent = currentTasks.filter { it.boardId == cat.id }
            val catPrev = previousTasks.filter { it.boardId == cat.id }

            val cTot = catCurrent.size
            val cDone = catCurrent.count { it.isCompleted }
            val cRate = if (cTot > 0) ((cDone.toFloat() / cTot) * 100).toInt() else 0

            val pTot = catPrev.size
            val pDone = catPrev.count { it.isCompleted }
            val pRate = if (pTot > 0) ((pDone.toFloat() / pTot) * 100).toInt() else 0

            val diff = cRate - pRate
            CategoryProgressItem(
                category = cat,
                rate = cRate,
                trendDiff = Math.abs(diff),
                isPositiveTrend = diff >= 0,
                completedCount = cDone,
                totalCount = cTot
            )
        }

        // Planning accuracy
        val planningAccuracy = if (totalTasks > 0) {
            ((completedTasks.toFloat() / totalTasks) * 100).toInt()
        } else 0

        val (planningAdvice, planningStatus) = when {
            totalTasks == 0 -> Pair(
                "Chưa có công việc nào được lên kế hoạch trong giai đoạn này.",
                "Chưa có dữ liệu"
            )
            planningAccuracy < 65 -> Pair(
                "Bạn thường lên kế hoạch nhiều hơn khả năng hoàn thành thực tế.",
                "Cần cải thiện"
            )
            planningAccuracy < 80 -> Pair(
                "Kế hoạch của bạn tương đối cân đối với tiến độ thực tế.",
                "Tốt"
            )
            else -> Pair(
                "Khả năng thực hiện và bám sát kế hoạch rất xuất sắc!",
                "Xuất sắc"
            )
        }

        // Streak analysis for current week (Monday to Sunday)
        val monday = anchorDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekStreak = (0..6).map { offset ->
            val dayDate = monday.plusDays(offset.toLong())
            val dateStr = dayDate.format(dateFormatter)
            val dayTasks = allOccurrences.filter { it.date == dateStr }
            val hasTasks = dayTasks.isNotEmpty()
            val dayCompleted = hasTasks && dayTasks.any { it.isCompleted }
            val dayLabel = when (offset) {
                0 -> "T2"
                1 -> "T3"
                2 -> "T4"
                3 -> "T5"
                4 -> "T6"
                5 -> "T7"
                else -> "CN"
            }
            DayStreakItem(
                dayLabel = dayLabel,
                dateString = dateStr,
                isCompleted = dayCompleted,
                hasTasks = hasTasks,
                isToday = dayDate == anchorDate
            )
        }

        val streakDays = weekStreak.count { it.isCompleted }

        // Best and Worst Day
        val dayGroups = currentTasks.groupBy { it.date }
        var bestDay = "Thứ Hai"
        var worstDay = "Chủ Nhật"
        var maxCompleted = -1
        var minCompleted = Int.MAX_VALUE

        dayGroups.forEach { (dateStr, tList) ->
            val comp = tList.count { it.isCompleted }
            val d = parseDate(dateStr)
            val dayName = d?.let { getVietnameseDayOfWeek(it.dayOfWeek) } ?: "T2"
            if (comp > maxCompleted) {
                maxCompleted = comp
                bestDay = dayName
            }
            if (comp < minCompleted) {
                minCompleted = comp
                worstDay = dayName
            }
        }

        val lowCategories = categoryProgressList.filter { it.totalCount > 0 && it.rate < 60 }
        val quickObservation = if (lowCategories.isNotEmpty()) {
            val names = lowCategories.joinToString(" và ") { it.category.name }
            "Bạn nên tập trung hơn vào các công việc $names trong giai đoạn tới."
        } else if (totalTasks > 0) {
            "Hiệu suất các chủ đề đang duy trì ở mức ổn định và tích cực."
        } else {
            "Chưa có lịch trình được tạo trong giai đoạn này. Hãy thêm công việc mới!"
        }

        val overviewData = DashboardOverviewData(
            period = period,
            dateRangeText = dateRangeStr,
            totalTasks = totalTasks,
            completedTasks = completedTasks,
            inProgressTasks = inProgressTasks,
            incompleteTasks = incompleteTasks,
            completionRate = completionRate,
            previousCompletionRate = prevCompletionRate,
            trendPercent = Math.abs(trendDiff),
            isTrendPositive = isTrendPositive,
            motivationMessage = motivationMessage,
            categoryProgressList = categoryProgressList,
            planningAccuracy = planningAccuracy,
            planningAccuracyAdvice = planningAdvice,
            planningAccuracyStatus = planningStatus,
            streakDays = streakDays,
            weekStreak = weekStreak,
            quickObservation = quickObservation,
            bestDay = bestDay,
            worstDay = worstDay
        )

        // Generate AI Export Model
        val timeDist = calculateTimeDistribution(currentTasks.ifEmpty { allOccurrences })
        val fourWeekPoints = (3 downTo 0).map { weeksAgo ->
            val wStart = monday.minusWeeks(weeksAgo.toLong())
            val wEnd = wStart.plusDays(6)
            val wTasks = allOccurrences.filter {
                val d = parseDate(it.date)
                d != null && !d.isBefore(wStart) && !d.isAfter(wEnd)
            }
            val wTot = wTasks.size
            val wDone = wTasks.count { it.isCompleted }
            val wRate = if (wTot > 0) ((wDone.toFloat() / wTot) * 100).toInt() else 0
            AiFourWeekTrendPoint(
                weekLabel = "Tuần ${4 - weeksAgo}",
                completionRatePercent = wRate
            )
        }

        val aiExport = AiDashboardExportData(
            exportTimestamp = System.currentTimeMillis(),
            exportDate = anchorDate.format(dateFormatter),
            currentPeriod = period.label,
            dateRange = dateRangeStr,
            totalPlannedTasks = totalTasks,
            completedTasks = completedTasks,
            inProgressTasks = inProgressTasks,
            incompleteTasks = incompleteTasks,
            overallCompletionRatePercent = completionRate,
            previousPeriodCompletionRatePercent = prevCompletionRate,
            trendDiffPercent = Math.abs(trendDiff),
            isTrendPositive = isTrendPositive,
            planningAccuracyPercent = planningAccuracy,
            planningAccuracyStatus = planningStatus,
            planningAccuracyAdvice = planningAdvice,
            activeStreakDays = streakDays,
            weeklyStreakBreakdown = weekStreak.map {
                val dayTasks = allOccurrences.filter { t -> t.date == it.dateString }
                AiDayStreakSummary(
                    dayOfWeek = it.dayLabel,
                    date = it.dateString,
                    hasTasks = it.hasTasks,
                    isCompleted = it.isCompleted,
                    completedCount = dayTasks.count { t -> t.isCompleted },
                    totalCount = dayTasks.size
                )
            },
            bestPerformanceDay = bestDay,
            lowestPerformanceDay = worstDay,
            categorySummaries = categoryProgressList.map {
                AiCategorySummary(
                    categoryId = it.category.id,
                    categoryName = it.category.name,
                    emoji = it.category.iconEmoji,
                    colorHex = it.category.colorHex,
                    totalTasks = it.totalCount,
                    completedTasks = it.completedCount,
                    completionRatePercent = it.rate,
                    trendDiffPercent = it.trendDiff,
                    isPositiveTrend = it.isPositiveTrend
                )
            },
            timeOfDayDistribution = timeDist.map {
                AiTimeSlotSummary(
                    slotName = it.slotName,
                    timeRange = it.timeRange,
                    percentage = it.percentage
                )
            },
            recentFeaturedTasks = currentTasks.take(15).map {
                AiTaskSummary(
                    id = it.id,
                    title = it.title,
                    categoryName = it.categoryName,
                    date = it.date,
                    timeFormatted = "${it.startTime} - ${it.endTime}",
                    isCompleted = it.isCompleted,
                    priority = it.priority
                )
            },
            fourWeekPerformanceTrend = fourWeekPoints,
            quickObservationInsight = quickObservation,
            motivationMessage = motivationMessage
        )

        return Pair(overviewData, aiExport)
    }

    /**
     * Compute detailed analytics for a specific Category from Room Database entities
     */
    fun calculateCategoryDetailFromDatabase(
        boards: List<BoardEntity>,
        events: List<EventEntity>,
        completions: List<EventCompletionEntity>,
        holidays: List<HolidayEntity>,
        categoryId: Int,
        anchorDate: LocalDate = LocalDate.now()
    ): CategoryDetailData {
        val categories = mapBoardsToCategories(boards)
        val selectedCategory = categories.firstOrNull { it.id == categoryId }
            ?: categories.firstOrNull()
            ?: DashboardCategory(1, "Công việc", "#4285F4")

        val boardMap = categories.associateBy { it.id }

        val monday = anchorDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val sunday = anchorDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))

        val prevMonday = monday.minusWeeks(1)
        val prevSunday = sunday.minusWeeks(1)

        val lookbackStart = monday.minusWeeks(3)
        val allOccurrences = expandEventOccurrences(events, completions, boardMap, lookbackStart, sunday)
        val catOccurrences = allOccurrences.filter { it.boardId == selectedCategory.id }

        // This week tasks
        val thisWeekTasks = catOccurrences.filter {
            val d = parseDate(it.date)
            d != null && !d.isBefore(monday) && !d.isAfter(sunday)
        }
        val thisTotal = thisWeekTasks.size
        val thisCompleted = thisWeekTasks.count { it.isCompleted }
        val thisWeekRate = if (thisTotal > 0) ((thisCompleted.toFloat() / thisTotal) * 100).toInt() else 0

        // Last week tasks
        val lastWeekTasks = catOccurrences.filter {
            val d = parseDate(it.date)
            d != null && !d.isBefore(prevMonday) && !d.isAfter(prevSunday)
        }
        val lastTotal = lastWeekTasks.size
        val lastCompleted = lastWeekTasks.count { it.isCompleted }
        val lastWeekRate = if (lastTotal > 0) ((lastCompleted.toFloat() / lastTotal) * 100).toInt() else 0

        val trendDiff = thisWeekRate - lastWeekRate

        // Weekly bar chart per day (T2..CN)
        val weeklyBarData = (0..6).map { offset ->
            val dayDate = monday.plusDays(offset.toLong())
            val dateStr = dayDate.format(dateFormatter)
            val dayTasks = thisWeekTasks.filter { it.date == dateStr }
            val dTotal = dayTasks.size
            val dComp = dayTasks.count { it.isCompleted }
            val pct = if (dTotal > 0) ((dComp.toFloat() / dTotal) * 100).toInt() else 0
            val label = when (offset) {
                0 -> "T2"
                1 -> "T3"
                2 -> "T4"
                3 -> "T5"
                4 -> "T6"
                5 -> "T7"
                else -> "CN"
            }
            DayBarData(
                dayLabel = label,
                dateString = dateStr,
                percentage = pct,
                completedCount = dComp,
                totalCount = dTotal,
                isHighlighted = offset < 6
            )
        }

        // Time slot distribution
        val timeDistribution = calculateTimeDistribution(thisWeekTasks.ifEmpty { catOccurrences })

        // Featured tasks for this category
        val featuredTasks = (thisWeekTasks.ifEmpty { catOccurrences }).take(10).map {
            FeaturedTaskItem(
                id = it.id,
                title = it.title,
                statusText = if (it.isCompleted) "Hoàn thành" else "Chưa hoàn thành",
                isCompleted = it.isCompleted,
                priority = it.priority,
                timeFormatted = "${it.startTime} - ${it.endTime}"
            )
        }

        // 4-Week Trend calculation
        val fourWeekTrend = (3 downTo 0).map { weeksAgo ->
            val wStart = monday.minusWeeks(weeksAgo.toLong())
            val wEnd = wStart.plusDays(6)
            val wTasks = catOccurrences.filter {
                val d = parseDate(it.date)
                d != null && !d.isBefore(wStart) && !d.isAfter(wEnd)
            }
            val wTot = wTasks.size
            val wDone = wTasks.count { it.isCompleted }
            val wRate = if (wTot > 0) ((wDone.toFloat() / wTot) * 100).toInt() else {
                if (weeksAgo == 0) thisWeekRate else 0
            }
            FourWeekPoint(
                weekLabel = "Tuần ${4 - weeksAgo}",
                completionRate = wRate
            )
        }

        // Priority breakdown
        val priorityStats = listOf("Cao", "Trung bình", "Thấp").associateWith { prio ->
            val prioTasks = thisWeekTasks.filter { it.priority.equals(prio, ignoreCase = true) }
            val tot = prioTasks.size
            val done = prioTasks.count { it.isCompleted }
            val rate = if (tot > 0) ((done.toFloat() / tot) * 100).toInt() else 0
            PriorityCompletionStats(
                priority = prio,
                total = tot,
                completed = done,
                completionRate = rate
            )
        }

        return CategoryDetailData(
            category = selectedCategory,
            periodSubtitle = "Tuần này",
            completionRate = thisWeekRate,
            completedCount = thisCompleted,
            totalCount = thisTotal,
            weeklyBarData = weeklyBarData,
            thisWeekRate = thisWeekRate,
            lastWeekRate = lastWeekRate,
            trendDiff = Math.abs(trendDiff),
            isPositiveTrend = trendDiff >= 0,
            timeDistribution = timeDistribution,
            featuredTasks = featuredTasks,
            fourWeekTrend = fourWeekTrend,
            priorityStats = priorityStats
        )
    }

    private fun mapBoardsToCategories(boards: List<BoardEntity>): List<DashboardCategory> {
        if (boards.isEmpty()) {
            return listOf(
                DashboardCategory(1, "Công việc", "#4285F4"),
                DashboardCategory(2, "Học tập", "#34A853"),
                DashboardCategory(3, "Đi chơi", "#E91E63")
            )
        }
        return boards.map { board ->
            DashboardCategory(
                id = board.id,
                name = board.name,
                colorHex = board.colorHex,
                iconEmoji = DashboardCategory.getEmojiForCategoryName(board.name)
            )
        }
    }

    private fun expandEventOccurrences(
        events: List<EventEntity>,
        completions: List<EventCompletionEntity>,
        boardMap: Map<Int, DashboardCategory>,
        rangeStart: LocalDate,
        rangeEnd: LocalDate
    ): List<ComputedTaskOccurrence> {
        val completionMap = completions.groupBy { it.eventId }
        val results = mutableListOf<ComputedTaskOccurrence>()

        events.forEach { event ->
            val eventStart = try { LocalDate.parse(event.startDate) } catch (e: Exception) { null } ?: return@forEach
            val repeatEnd = event.repeatEndDate?.let { try { LocalDate.parse(it) } catch (e: Exception) { null } } ?: LocalDate.MAX
            val actualStart = if (eventStart.isAfter(rangeStart)) eventStart else rangeStart
            val actualEnd = if (repeatEnd.isBefore(rangeEnd)) repeatEnd else rangeEnd

            if (actualStart.isAfter(actualEnd)) return@forEach

            val eventCompletions = completionMap[event.id] ?: emptyList()
            val completedDateSet = eventCompletions.filter { it.isCompleted }.map { it.occurrenceDate }.toSet()

            val cat = boardMap[event.boardId]
            val catName = cat?.name ?: "Khác"
            val emoji = cat?.iconEmoji ?: "📌"
            val color = cat?.colorHex ?: "#6366F1"

            val prio = when {
                event.hasReminder -> "Cao"
                event.isAllDay -> "Trung bình"
                else -> "Thấp"
            }

            var curr = eventStart
            while (!curr.isAfter(actualEnd)) {
                if (!curr.isBefore(rangeStart) && !curr.isAfter(rangeEnd)) {
                    if (isDateMatchingRule(curr, event, eventStart)) {
                        val dateStr = curr.format(dateFormatter)
                        val isDone = if (event.repeatRule == null || event.repeatRule == "NONE") {
                            event.isCompleted || completedDateSet.contains(dateStr)
                        } else {
                            completedDateSet.contains(dateStr)
                        }
                        results.add(
                            ComputedTaskOccurrence(
                                id = event.id,
                                title = event.title,
                                boardId = event.boardId,
                                categoryName = catName,
                                categoryEmoji = emoji,
                                colorHex = color,
                                date = dateStr,
                                startTime = event.startTime ?: "08:00",
                                endTime = event.endTime ?: "09:00",
                                isCompleted = isDone,
                                priority = prio
                            )
                        )
                    }
                }

                curr = when (event.repeatRule) {
                    "DAILY" -> curr.plusDays(1)
                    "WEEKLY" -> curr.plusDays(1)
                    "MONTHLY" -> curr.plusMonths(1)
                    else -> {
                        if (curr == eventStart) curr.plusDays(1) else break
                    }
                }
                if ((event.repeatRule == null || event.repeatRule == "NONE") && curr.isAfter(eventStart)) break
            }
        }

        return results
    }

    private fun isDateMatchingRule(date: LocalDate, event: EventEntity, eventStart: LocalDate): Boolean {
        val rule = event.repeatRule ?: "NONE"
        if (rule == "NONE") return date == eventStart
        if (rule == "DAILY") return true
        if (rule == "WEEKLY") {
            val repeatDays = event.repeatDays
            if (repeatDays.isNullOrBlank()) {
                return date.dayOfWeek == eventStart.dayOfWeek
            }
            val shortDay = when (date.dayOfWeek) {
                DayOfWeek.MONDAY -> "MON"
                DayOfWeek.TUESDAY -> "TUE"
                DayOfWeek.WEDNESDAY -> "WED"
                DayOfWeek.THURSDAY -> "THU"
                DayOfWeek.FRIDAY -> "FRI"
                DayOfWeek.SATURDAY -> "SAT"
                DayOfWeek.SUNDAY -> "SUN"
            }
            return repeatDays.contains(shortDay, ignoreCase = true)
        }
        if (rule == "MONTHLY") {
            return date.dayOfMonth == eventStart.dayOfMonth
        }
        return false
    }

    private fun calculateTimeDistribution(tasks: List<ComputedTaskOccurrence>): List<TimeSlotDistribution> {
        var morning = 0
        var afternoon = 0
        var evening = 0
        var night = 0

        tasks.forEach { task ->
            val hour = try {
                task.startTime.split(":").first().toInt()
            } catch (e: Exception) {
                9
            }
            when (hour) {
                in 6..11 -> morning++
                in 12..17 -> afternoon++
                in 18..23 -> evening++
                else -> night++
            }
        }

        val total = (morning + afternoon + evening + night).coerceAtLeast(1)
        var mPct = ((morning.toFloat() / total) * 100).toInt()
        var aPct = ((afternoon.toFloat() / total) * 100).toInt()
        var ePct = ((evening.toFloat() / total) * 100).toInt()
        var nPct = (100 - (mPct + aPct + ePct)).coerceAtLeast(0)

        if (tasks.isEmpty()) {
            mPct = 40
            aPct = 35
            ePct = 20
            nPct = 5
        }

        return listOf(
            TimeSlotDistribution("Sáng", "6h - 12h", mPct, Color(0xFF10B981)),
            TimeSlotDistribution("Chiều", "12h - 18h", aPct, Color(0xFF3B82F6)),
            TimeSlotDistribution("Tối", "18h - 24h", ePct, Color(0xFF8B5CF6)),
            TimeSlotDistribution("Đêm", "0h - 6h", nPct, Color(0xFFF59E0B))
        )
    }

    private fun getPeriodRange(period: PeriodType, anchorDate: LocalDate): Triple<LocalDate, LocalDate, String> {
        return when (period) {
            PeriodType.DAY -> {
                Triple(anchorDate, anchorDate, anchorDate.format(displayDateFormatter))
            }
            PeriodType.WEEK -> {
                val monday = anchorDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val sunday = anchorDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                val rangeStr = "${monday.format(displayDateFormatter)} - ${sunday.format(displayDateFormatter)}"
                Triple(monday, sunday, rangeStr)
            }
            PeriodType.MONTH -> {
                val start = anchorDate.with(TemporalAdjusters.firstDayOfMonth())
                val end = anchorDate.with(TemporalAdjusters.lastDayOfMonth())
                Triple(start, end, "Tháng ${anchorDate.monthValue}/${anchorDate.year}")
            }
        }
    }

    private fun getPreviousPeriodRange(period: PeriodType, anchorDate: LocalDate): Triple<LocalDate, LocalDate, String> {
        return when (period) {
            PeriodType.DAY -> {
                val prev = anchorDate.minusDays(1)
                Triple(prev, prev, prev.format(displayDateFormatter))
            }
            PeriodType.WEEK -> {
                val monday = anchorDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(1)
                val sunday = monday.plusDays(6)
                Triple(monday, sunday, "${monday.format(displayDateFormatter)} - ${sunday.format(displayDateFormatter)}")
            }
            PeriodType.MONTH -> {
                val start = anchorDate.minusMonths(1).with(TemporalAdjusters.firstDayOfMonth())
                val end = start.with(TemporalAdjusters.lastDayOfMonth())
                Triple(start, end, "Tháng ${start.monthValue}/${start.year}")
            }
        }
    }

    private fun parseDate(dateStr: String): LocalDate? {
        return try {
            LocalDate.parse(dateStr, dateFormatter)
        } catch (e: Exception) {
            null
        }
    }

    private fun getVietnameseDayOfWeek(day: DayOfWeek): String {
        return when (day) {
            DayOfWeek.MONDAY -> "Thứ Hai"
            DayOfWeek.TUESDAY -> "Thứ Ba"
            DayOfWeek.WEDNESDAY -> "Thứ Tư"
            DayOfWeek.THURSDAY -> "Thứ Năm"
            DayOfWeek.FRIDAY -> "Thứ Sáu"
            DayOfWeek.SATURDAY -> "Thứ Bảy"
            DayOfWeek.SUNDAY -> "Chủ Nhật"
        }
    }
}
