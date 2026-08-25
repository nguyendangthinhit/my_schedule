package com.example.data

import android.content.Context
import android.util.Log
import com.example.analytics.AnalyticsEngine
import com.example.data.dao.ScheduleDao
import com.example.data.entities.BoardEntity
import com.example.data.entities.EventCompletionEntity
import com.example.data.entities.EventEntity
import com.example.data.entities.HolidayEntity
import com.example.models.dashboard.*
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

class DashboardRepository(
    private val context: Context,
    private val scheduleDao: ScheduleDao = AppDatabase.getDatabase(context).scheduleDao(),
    private val analyticsEngine: AnalyticsEngine = AnalyticsEngine()
) {
    private val moshi: Moshi = Moshi.Builder().build()
    private val exportAdapter = moshi.adapter(AiDashboardExportData::class.java).indent("  ")
    private val aiExportFileName = "ai_dashboard_analytics.json"

    val allBoards: Flow<List<BoardEntity>> = scheduleDao.getAllBoards()
    val allEvents: Flow<List<EventEntity>> = scheduleDao.getAllEvents()
    val allCompletions: Flow<List<EventCompletionEntity>> = scheduleDao.getAllCompletions()
    val allHolidays: Flow<List<HolidayEntity>> = scheduleDao.getAllHolidays()

    val categoriesFlow: Flow<List<DashboardCategory>> = scheduleDao.getAllBoards().map { boards ->
        if (boards.isEmpty()) {
            listOf(
                DashboardCategory(1, "Công việc", "#4285F4"),
                DashboardCategory(2, "Học tập", "#34A853"),
                DashboardCategory(3, "Đi chơi", "#E91E63")
            )
        } else {
            boards.map { board ->
                DashboardCategory(
                    id = board.id,
                    name = board.name,
                    colorHex = board.colorHex,
                    iconEmoji = DashboardCategory.getEmojiForCategoryName(board.name)
                )
            }
        }
    }

    /**
     * Calculate overview data from raw entities (pure function)
     */
    fun calculateOverview(
        boards: List<BoardEntity>,
        events: List<EventEntity>,
        completions: List<EventCompletionEntity>,
        holidays: List<HolidayEntity>,
        period: PeriodType = PeriodType.WEEK,
        anchorDate: LocalDate = LocalDate.now()
    ): Pair<DashboardOverviewData, AiDashboardExportData> {
        val (overviewData, aiExportData) = analyticsEngine.calculateOverviewFromDatabase(
            boards = boards,
            events = events,
            completions = completions,
            holidays = holidays,
            period = period,
            anchorDate = anchorDate
        )
        // Background save metrics
        try {
            val jsonString = exportAdapter.toJson(aiExportData)
            val file = File(context.filesDir, aiExportFileName)
            file.writeText(jsonString, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e("DashboardRepository", "Failed to save AI metrics", e)
        }
        return Pair(overviewData, aiExportData)
    }

    /**
     * Calculate category detail data from raw entities (pure function)
     */
    fun calculateCategoryDetail(
        boards: List<BoardEntity>,
        events: List<EventEntity>,
        completions: List<EventCompletionEntity>,
        holidays: List<HolidayEntity>,
        categoryId: Int,
        anchorDate: LocalDate = LocalDate.now()
    ): CategoryDetailData {
        return analyticsEngine.calculateCategoryDetailFromDatabase(
            boards = boards,
            events = events,
            completions = completions,
            holidays = holidays,
            categoryId = categoryId,
            anchorDate = anchorDate
        )
    }

    /**
     * Get overview data calculated strictly from real database entities,
     * and persist the AI-ready metrics to a JSON file.
     */
    suspend fun getOverviewData(
        period: PeriodType = PeriodType.WEEK,
        anchorDate: LocalDate = LocalDate.now()
    ): DashboardOverviewData = withContext(Dispatchers.IO) {
        val boards = try { scheduleDao.getAllBoards().first() } catch (e: Exception) { emptyList() }
        val events = try { scheduleDao.getAllEvents().first() } catch (e: Exception) { emptyList() }
        val completions = try { scheduleDao.getAllCompletions().first() } catch (e: Exception) { emptyList() }
        val holidays = try { scheduleDao.getAllHolidays().first() } catch (e: Exception) { emptyList() }

        val (overviewData, _) = calculateOverview(
            boards = boards,
            events = events,
            completions = completions,
            holidays = holidays,
            period = period,
            anchorDate = anchorDate
        )

        overviewData
    }

    /**
     * Get detailed category metrics calculated from database entities
     */
    suspend fun getCategoryDetailData(
        categoryId: Int,
        anchorDate: LocalDate = LocalDate.now()
    ): CategoryDetailData = withContext(Dispatchers.IO) {
        val boards = try { scheduleDao.getAllBoards().first() } catch (e: Exception) { emptyList() }
        val events = try { scheduleDao.getAllEvents().first() } catch (e: Exception) { emptyList() }
        val completions = try { scheduleDao.getAllCompletions().first() } catch (e: Exception) { emptyList() }
        val holidays = try { scheduleDao.getAllHolidays().first() } catch (e: Exception) { emptyList() }

        calculateCategoryDetail(
            boards = boards,
            events = events,
            completions = completions,
            holidays = holidays,
            categoryId = categoryId,
            anchorDate = anchorDate
        )
    }

    /**
     * Fetch all categories directly from database boards
     */
    suspend fun getAllCategories(): List<DashboardCategory> = withContext(Dispatchers.IO) {
        val boards = try { scheduleDao.getAllBoards().first() } catch (e: Exception) { emptyList() }
        if (boards.isEmpty()) {
            listOf(
                DashboardCategory(1, "Công việc", "#4285F4"),
                DashboardCategory(2, "Học tập", "#34A853"),
                DashboardCategory(3, "Đi chơi", "#E91E63")
            )
        } else {
            boards.map { board ->
                DashboardCategory(
                    id = board.id,
                    name = board.name,
                    colorHex = board.colorHex,
                    iconEmoji = DashboardCategory.getEmojiForCategoryName(board.name)
                )
            }
        }
    }

    /**
     * Write calculated AI metrics into JSON file in internal storage
     */
    private suspend fun saveAiMetricsToJson(exportData: AiDashboardExportData) = withContext(Dispatchers.IO) {
        try {
            val jsonString = exportAdapter.toJson(exportData)
            val file = File(context.filesDir, aiExportFileName)
            file.writeText(jsonString, Charsets.UTF_8)
            Log.d("DashboardRepository", "AI Dashboard metrics successfully saved to ${file.absolutePath} (${jsonString.length} chars)")
        } catch (e: Exception) {
            Log.e("DashboardRepository", "Failed to save AI Dashboard metrics to JSON", e)
        }
    }

    /**
     * Read the saved AI dashboard JSON file content
     */
    suspend fun readAiDashboardJson(): String = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, aiExportFileName)
            if (file.exists()) {
                file.readText(Charsets.UTF_8)
            } else {
                ""
            }
        } catch (e: Exception) {
            Log.e("DashboardRepository", "Error reading AI Dashboard JSON file", e)
            ""
        }
    }

    /**
     * Returns the File pointer to the saved JSON file
     */
    fun getAiDashboardJsonFile(): File {
        return File(context.filesDir, aiExportFileName)
    }
}
