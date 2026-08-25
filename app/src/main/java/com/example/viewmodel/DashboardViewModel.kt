package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.DashboardRepository
import com.example.data.entities.BoardEntity
import com.example.data.entities.EventCompletionEntity
import com.example.data.entities.EventEntity
import com.example.data.entities.HolidayEntity
import com.example.models.dashboard.*
import kotlinx.coroutines.flow.*
import java.time.LocalDate

sealed interface DashboardOverviewUiState {
    object Loading : DashboardOverviewUiState
    data class Success(val data: DashboardOverviewData) : DashboardOverviewUiState
    data class Error(val message: String) : DashboardOverviewUiState
}

sealed interface CategoryDetailUiState {
    object Loading : CategoryDetailUiState
    data class Success(val data: CategoryDetailData) : CategoryDetailUiState
    data class Error(val message: String) : CategoryDetailUiState
}

private data class DashboardDbData(
    val boards: List<BoardEntity>,
    val events: List<EventEntity>,
    val completions: List<EventCompletionEntity>,
    val holidays: List<HolidayEntity>
)

class DashboardViewModel(
    private val repository: DashboardRepository
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(PeriodType.WEEK)
    val selectedPeriod: StateFlow<PeriodType> = _selectedPeriod.asStateFlow()

    private val _anchorDate = MutableStateFlow(LocalDate.now())
    val anchorDate: StateFlow<LocalDate> = _anchorDate.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<Int>(1)
    val selectedCategoryId: StateFlow<Int> = _selectedCategoryId.asStateFlow()

    private val dbDataFlow: Flow<DashboardDbData> = combine(
        repository.allBoards,
        repository.allEvents,
        repository.allCompletions,
        repository.allHolidays
    ) { boards, events, completions, holidays ->
        DashboardDbData(boards, events, completions, holidays)
    }

    val categories: StateFlow<List<DashboardCategory>> = repository.categoriesFlow
        .onEach { cats ->
            if (cats.isNotEmpty() && cats.none { it.id == _selectedCategoryId.value }) {
                _selectedCategoryId.value = cats.first().id
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val overviewState: StateFlow<DashboardOverviewUiState> = combine(
        dbDataFlow,
        _selectedPeriod,
        _anchorDate
    ) { dbData, period, anchorDate ->
        try {
            val (data, _) = repository.calculateOverview(
                boards = dbData.boards,
                events = dbData.events,
                completions = dbData.completions,
                holidays = dbData.holidays,
                period = period,
                anchorDate = anchorDate
            )
            DashboardOverviewUiState.Success(data)
        } catch (e: Exception) {
            DashboardOverviewUiState.Error(e.message ?: "Lỗi tải dữ liệu thống kê")
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, DashboardOverviewUiState.Loading)

    val categoryDetailState: StateFlow<CategoryDetailUiState> = combine(
        dbDataFlow,
        _selectedCategoryId,
        _anchorDate
    ) { dbData, catId, anchorDate ->
        try {
            val validCatId = if (dbData.boards.any { it.id == catId }) {
                catId
            } else {
                dbData.boards.firstOrNull()?.id ?: 1
            }
            val data = repository.calculateCategoryDetail(
                boards = dbData.boards,
                events = dbData.events,
                completions = dbData.completions,
                holidays = dbData.holidays,
                categoryId = validCatId,
                anchorDate = anchorDate
            )
            CategoryDetailUiState.Success(data)
        } catch (e: Exception) {
            CategoryDetailUiState.Error(e.message ?: "Lỗi tải chi tiết chủ đề")
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, CategoryDetailUiState.Loading)

    fun setPeriod(period: PeriodType) {
        _selectedPeriod.value = period
    }

    fun selectCategory(category: DashboardCategory) {
        _selectedCategoryId.value = category.id
    }

    fun selectCategoryId(categoryId: Int) {
        _selectedCategoryId.value = categoryId
    }

    fun previousPeriod() {
        val current = _anchorDate.value
        _anchorDate.value = when (_selectedPeriod.value) {
            PeriodType.DAY -> current.minusDays(1)
            PeriodType.WEEK -> current.minusWeeks(1)
            PeriodType.MONTH -> current.minusMonths(1)
        }
    }

    fun nextPeriod() {
        val current = _anchorDate.value
        _anchorDate.value = when (_selectedPeriod.value) {
            PeriodType.DAY -> current.plusDays(1)
            PeriodType.WEEK -> current.plusWeeks(1)
            PeriodType.MONTH -> current.plusMonths(1)
        }
    }
}

class DashboardViewModelFactory(
    private val repository: DashboardRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            return DashboardViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
