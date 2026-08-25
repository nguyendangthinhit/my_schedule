package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.Event
import com.example.models.EventCategory
import com.example.network.ApiSyncResult
import com.example.ui.theme.PrimaryPurple
import com.example.ui.theme.getThemedBgColor
import com.example.ui.theme.getThemedBorderColor
import com.example.util.DateUtils
import com.example.util.LunarCalendarHelper
import com.example.viewmodel.ScheduleViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

enum class CalendarViewMode(val title: String) {
    DAY("Ngày"),
    WEEK("Tuần"),
    MONTH("Tháng")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: ScheduleViewModel,
    onNavigateToAddEvent: () -> Unit,
    onNavigateToEditEvent: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val allEvents by viewModel.events.collectAsState()
    val showLunarCalendar by viewModel.showLunarCalendar.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncResult by viewModel.syncResult.collectAsState()
    val weatherMap by viewModel.weatherMap.collectAsState()

    var currentViewMode by remember { mutableStateOf(CalendarViewMode.MONTH) }
    var activeDate by remember { mutableStateOf(LocalDate.now()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedEventToDelete by remember { mutableStateOf<Event?>(null) }
    var showApiSyncDialog by remember { mutableStateOf(false) }
    var showDayDetailsDialog by remember { mutableStateOf(false) }
    var dialogSelectedDate by remember { mutableStateOf(LocalDate.now()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp)
        ) {
            // Top Bar: Title & 3 View Mode Toggle (Ngày, Tuần, Tháng)
            CalendarTopHeader(
                currentMode = currentViewMode,
                onModeSelected = { currentViewMode = it }
            )

            // Date Navigation Header with Previous / Next controls
            CalendarDateNavHeader(
                viewMode = currentViewMode,
                activeDate = activeDate,
                showLunarCalendar = showLunarCalendar,
                weatherMap = weatherMap,
                onPrevious = {
                    activeDate = when (currentViewMode) {
                        CalendarViewMode.DAY -> activeDate.minusDays(1)
                        CalendarViewMode.WEEK -> activeDate.minusWeeks(1)
                        CalendarViewMode.MONTH -> activeDate.minusMonths(1)
                    }
                },
                onNext = {
                    activeDate = when (currentViewMode) {
                        CalendarViewMode.DAY -> activeDate.plusDays(1)
                        CalendarViewMode.WEEK -> activeDate.plusWeeks(1)
                        CalendarViewMode.MONTH -> activeDate.plusMonths(1)
                    }
                },
                onToday = { activeDate = LocalDate.now() }
            )

            // View Content depending on chosen mode
            when (currentViewMode) {
                CalendarViewMode.DAY -> {
                    DayScheduleView(
                        activeDate = activeDate,
                        events = allEvents,
                        showLunarCalendar = showLunarCalendar,
                        weatherMap = weatherMap,
                        onSelectDate = { activeDate = it },
                        onEventClick = { selectedEventToDelete = it; showDeleteDialog = true },
                        onToggleCompletion = { viewModel.toggleEventCompletion(it) }
                    )
                }
                CalendarViewMode.WEEK -> {
                    WeekScheduleView(
                        activeDate = activeDate,
                        events = allEvents,
                        showLunarCalendar = showLunarCalendar,
                        weatherMap = weatherMap,
                        onSelectDate = { activeDate = it },
                        onEventClick = { selectedEventToDelete = it; showDeleteDialog = true },
                        onToggleCompletion = { viewModel.toggleEventCompletion(it) }
                    )
                }
                CalendarViewMode.MONTH -> {
                    MonthScheduleView(
                        activeDate = activeDate,
                        events = allEvents,
                        showLunarCalendar = showLunarCalendar,
                        weatherMap = weatherMap,
                        onSelectDate = { selectedDate ->
                            if (activeDate == selectedDate) {
                                dialogSelectedDate = selectedDate
                                showDayDetailsDialog = true
                            } else {
                                activeDate = selectedDate
                            }
                        },
                        onDateDoubleClick = { selectedDate ->
                            activeDate = selectedDate
                            dialogSelectedDate = selectedDate
                            showDayDetailsDialog = true
                        },
                        onPreviousMonth = {
                            activeDate = activeDate.minusMonths(1)
                        },
                        onNextMonth = {
                            activeDate = activeDate.plusMonths(1)
                        },
                        onEventClick = { selectedEventToDelete = it; showDeleteDialog = true },
                        onToggleCompletion = { viewModel.toggleEventCompletion(it) }
                    )
                }
            }
        }

        // Floating Action Button at the bottom (Add '+')
        FloatingActionButton(
            onClick = {
                viewModel.setSelectedDate(activeDate)
                onNavigateToAddEvent()
            },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
                .size(58.dp)
                .testTag("btn_add_event"),
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Thêm sự kiện", modifier = Modifier.size(28.dp))
        }

        // Delete Confirmation Dialog
        if (showDeleteDialog) {
            val dayEvents = allEvents.filter { it.date == activeDate }
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                containerColor = MaterialTheme.colorScheme.surface,
                title = { Text("Quản lý sự kiện", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                text = {
                    Column {
                        if (dayEvents.isEmpty()) {
                            Text("Không có sự kiện nào trong ngày ${activeDate.dayOfMonth}/${activeDate.monthValue} để quản lý.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Text("Chọn sự kiện bạn muốn chỉnh sửa hoặc xóa:", color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(10.dp))
                            LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                                items(dayEvents) { evt ->
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        color = evt.category.getThemedBgColor(),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, evt.category.getThemedBorderColor()),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(evt.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                                Text(
                                                    DateUtils.formatTimeRange(evt.startTime, evt.endTime),
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(
                                                    onClick = {
                                                        showDeleteDialog = false
                                                        onNavigateToEditEvent(evt.id)
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Edit,
                                                        contentDescription = "Edit",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(4.dp))
                                                IconButton(
                                                    onClick = {
                                                        viewModel.deleteEvent(evt.id)
                                                        if (dayEvents.size <= 1) showDeleteDialog = false
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Delete,
                                                        contentDescription = "Delete",
                                                        tint = Color(0xFFEF4444),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    if (dayEvents.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                viewModel.deleteEvents(dayEvents.map { it.id })
                                showDeleteDialog = false
                            }
                        ) {
                            Text("Xóa tất cả trong ngày", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Đóng", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }

        // API Inspection & Sync Dialog
        if (showApiSyncDialog) {
            AlertDialog(
                onDismissRequest = { showApiSyncDialog = false },
                containerColor = MaterialTheme.colorScheme.surface,
                icon = {
                    Icon(Icons.Default.CloudSync, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                },
                title = {
                    Text("Đồng bộ & Bóc tách Ngày Lễ API", fontWeight = FontWeight.Bold, fontSize = 17.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface)
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "Năm cần đồng bộ: ${activeDate.year}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        if (isSyncing) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 20.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Đang gọi API & bóc tách dữ liệu...", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            val result = syncResult
                            if (result is ApiSyncResult.Success) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = "Nguồn dữ liệu: ${result.source}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Trạng thái: Đã lưu ${result.holidays.size} ngày lễ vào Room Database (bảng 'holidays')",
                                            fontSize = 11.sp,
                                            color = Color(0xFF059669),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Dữ liệu sau khi bóc tách & đưa vào lịch:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Spacer(modifier = Modifier.height(4.dp))

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF0F172A),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = result.rawDataSummary,
                                        fontSize = 11.sp,
                                        color = Color(0xFF38BDF8),
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.syncHolidaysFromApi(context, activeDate.year)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tải lại API")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showApiSyncDialog = false }) {
                        Text("Xong", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }

        // Popup dialog chi tiết các sự kiện trong ngày khi click vào ô ngày ở giao diện tháng
        if (showDayDetailsDialog) {
            DayEventsPopupDialog(
                date = dialogSelectedDate,
                events = allEvents.filter { it.date == dialogSelectedDate },
                showLunarCalendar = showLunarCalendar,
                weather = weatherMap[dialogSelectedDate],
                onDismiss = { showDayDetailsDialog = false },
                onAddNewEvent = {
                    viewModel.setSelectedDate(dialogSelectedDate)
                    showDayDetailsDialog = false
                    onNavigateToAddEvent()
                },
                onEditEvent = { eventId ->
                    showDayDetailsDialog = false
                    onNavigateToEditEvent(eventId)
                },
                onDeleteEvent = { eventId ->
                    val evt = allEvents.find { it.id == eventId }
                    if (evt != null) {
                        selectedEventToDelete = evt
                        showDayDetailsDialog = false
                        showDeleteDialog = true
                    }
                },
                onToggleCompletion = { viewModel.toggleEventCompletion(it) }
            )
        }
    }
}

@Composable
fun CalendarTopHeader(
    currentMode: CalendarViewMode,
    onModeSelected: (CalendarViewMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.DateRange,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Lịch trình",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // 3 mục tùy chọn hiển thị: Ngày, Tuần, Tháng
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.padding(2.dp)
        ) {
            Row(modifier = Modifier.padding(3.dp)) {
                CalendarViewMode.entries.forEach { mode ->
                    val isSelected = currentMode == mode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { onModeSelected(mode) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("mode_${mode.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mode.title,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarDateNavHeader(
    viewMode: CalendarViewMode,
    activeDate: LocalDate,
    showLunarCalendar: Boolean = true,
    weatherMap: Map<LocalDate, com.example.data.entities.WeatherForecastEntity> = emptyMap(),
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit
) {
    val isToday = activeDate == LocalDate.now()
    val dayWeather = if (viewMode == CalendarViewMode.DAY) weatherMap[activeDate] else null

    val titleText = when (viewMode) {
        CalendarViewMode.DAY -> DateUtils.formatShortVietnameseDate(activeDate)
        CalendarViewMode.WEEK -> {
            val monday = activeDate.minusDays((activeDate.dayOfWeek.value - 1).toLong())
            val sunday = monday.plusDays(6)
            "Tuần ${monday.dayOfMonth}/${monday.monthValue} - ${sunday.dayOfMonth}/${sunday.monthValue} (${activeDate.year})"
        }
        CalendarViewMode.MONTH -> "Tháng ${activeDate.monthValue} năm ${activeDate.year}"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        shadowElevation = 0.5.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 4.dp, vertical = 4.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPrevious,
                modifier = Modifier
                    .size(34.dp)
                    .testTag("btn_prev_nav")
            ) {
                Icon(
                    Icons.Default.ChevronLeft,
                    contentDescription = "Previous",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = titleText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (dayWeather != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "${dayWeather.weatherEmoji} ${dayWeather.tempAvg}°C",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp)
                        )
                    }
                }
                if (isToday) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.18f),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF10B981).copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = "Hôm nay",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onToday() }
                            .testTag("btn_back_to_today")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                Icons.Default.Today,
                                contentDescription = "Về hôm nay",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "Về hôm nay",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            IconButton(
                onClick = onNext,
                modifier = Modifier
                    .size(34.dp)
                    .testTag("btn_next_nav")
            ) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Next",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Data class representing an event positioned within the timeline grid.
 */
data class PositionedTimelineEvent(
    val event: Event,
    val topDp: androidx.compose.ui.unit.Dp,
    val heightDp: androidx.compose.ui.unit.Dp,
    val colIndex: Int,
    val totalCols: Int
)

/**
 * Calculates exact vertical offset (topDp), height proportional to event duration (heightDp),
 * and assigns column indices to elegantly lay out overlapping events side-by-side.
 */
fun calculatePositionedTimelineEvents(
    events: List<Event>,
    startHour: Int = 5,
    endHour: Int = 24,
    hourHeightDp: androidx.compose.ui.unit.Dp,
    minHeightDp: androidx.compose.ui.unit.Dp = 20.dp
): List<PositionedTimelineEvent> {
    if (events.isEmpty()) return emptyList()

    // Sort by start minute ascending, then by duration descending
    val sorted = events.sortedWith(
        compareBy<Event> { it.startTime.hour * 60 + it.startTime.minute }
            .thenByDescending {
                (it.endTime.hour * 60 + it.endTime.minute) - (it.startTime.hour * 60 + it.startTime.minute)
            }
    )

    // Cluster overlapping events together
    val clusters = mutableListOf<MutableList<Event>>()
    var currentCluster = mutableListOf<Event>()
    var currentClusterEndMin = -1

    for (event in sorted) {
        val sMin = event.startTime.hour * 60 + event.startTime.minute
        val rawEMin = event.endTime.hour * 60 + event.endTime.minute
        val eMin = if (rawEMin <= sMin) sMin + 30 else rawEMin

        if (currentCluster.isEmpty()) {
            currentCluster.add(event)
            currentClusterEndMin = eMin
        } else {
            if (sMin < currentClusterEndMin) {
                // Overlaps with current cluster
                currentCluster.add(event)
                currentClusterEndMin = maxOf(currentClusterEndMin, eMin)
            } else {
                // End of current cluster, start new
                clusters.add(currentCluster)
                currentCluster = mutableListOf(event)
                currentClusterEndMin = eMin
            }
        }
    }
    if (currentCluster.isNotEmpty()) {
        clusters.add(currentCluster)
    }

    val result = mutableListOf<PositionedTimelineEvent>()

    for (cluster in clusters) {
        // Assign columns to avoid collisions within the cluster
        val colEndTimes = mutableListOf<Int>()
        val eventColMap = mutableMapOf<Event, Int>()

        for (event in cluster) {
            val sMin = event.startTime.hour * 60 + event.startTime.minute
            val rawEMin = event.endTime.hour * 60 + event.endTime.minute
            val eMin = if (rawEMin <= sMin) sMin + 30 else rawEMin

            var assignedCol = -1
            for (i in colEndTimes.indices) {
                if (colEndTimes[i] <= sMin) {
                    assignedCol = i
                    colEndTimes[i] = eMin
                    break
                }
            }
            if (assignedCol == -1) {
                assignedCol = colEndTimes.size
                colEndTimes.add(eMin)
            }
            eventColMap[event] = assignedCol
        }

        val totalCols = maxOf(1, colEndTimes.size)

        for (event in cluster) {
            val sMin = event.startTime.hour * 60 + event.startTime.minute
            val rawEMin = event.endTime.hour * 60 + event.endTime.minute
            val eMin = if (rawEMin <= sMin) sMin + 30 else rawEMin

            val startOffsetMins = (sMin - startHour * 60).coerceAtLeast(0)
            val endOffsetMins = (eMin - startHour * 60).coerceIn(startOffsetMins + 15, (endHour - startHour) * 60)
            val durationMins = maxOf(15, endOffsetMins - startOffsetMins)

            val top = (startOffsetMins.toFloat() / 60f) * hourHeightDp.value
            val calcHeight = (durationMins.toFloat() / 60f) * hourHeightDp.value
            val height = maxOf(minHeightDp.value, calcHeight)

            val col = eventColMap[event] ?: 0

            result.add(
                PositionedTimelineEvent(
                    event = event,
                    topDp = top.dp,
                    heightDp = height.dp,
                    colIndex = col,
                    totalCols = totalCols
                )
            )
        }
    }

    return result
}

/**
 * 1. GIAO DIỆN NGÀY:
 * Hiển thị lịch theo trục giờ từ 5 giờ sáng đến 24h đêm.
 * Các sự kiện chiếm diện tích chiều cao tương ứng với tỉ lệ thời gian (1 tiếng = 64dp, 2 tiếng = 128dp,...).
 * Hỗ trợ chuyển ngày bằng ViewPager (HorizontalPager) lướt trái phải mượt mà.
 */
@Composable
fun DayScheduleContent(
    activeDate: LocalDate,
    events: List<Event>,
    showLunarCalendar: Boolean = true,
    weatherMap: Map<LocalDate, com.example.data.entities.WeatherForecastEntity> = emptyMap(),
    onEventClick: (Event) -> Unit,
    onToggleCompletion: (Event) -> Unit
) {
    val listState = rememberLazyListState()
    val isToday = activeDate == LocalDate.now()
    val currentHour = LocalDateTime.now().hour
    val lunarDate = remember(activeDate) { LunarCalendarHelper.convertSolarToLunar(activeDate) }
    val weather = weatherMap[activeDate]

    val dayHourHeight = 64.dp
    val totalTimelineHours = 19 // 5h to 24h (5, 6, ..., 23, 24)
    val totalTimelineHeight = dayHourHeight * totalTimelineHours
    val positionedEvents = remember(events) {
        calculatePositionedTimelineEvents(
            events = events,
            startHour = 5,
            endHour = 24,
            hourHeightDp = dayHourHeight,
            minHeightDp = 28.dp
        )
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        // Header Thông Tin Ngày & Thời Tiết
        item {
            val dayOfWeekStr = DateUtils.formatDayOfWeekDot(activeDate.dayOfWeek)
            val lunarStr = if (showLunarCalendar) "ÂL ${lunarDate.day} Th${lunarDate.month}${if (lunarDate.isLeap) " (nhuận)" else ""}" else ""

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Row(
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = activeDate.dayOfMonth.toString(),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 34.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = dayOfWeekStr,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                        if (showLunarCalendar) {
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = lunarStr,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                        if (isToday) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.18f),
                                modifier = Modifier.padding(bottom = 3.dp)
                            ) {
                                Text(
                                    text = "Hôm nay",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981),
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    // Hàng 2: Thời tiết ⛅ 31°/26°C
                    if (weather != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = weather.weatherEmoji,
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${weather.tempMax}°/${weather.tempMin}°C",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (isToday && weather.remainingDayForecast != null) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "• ${weather.remainingDayForecast}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Banner Ngày Lễ nổi bật (nếu ngày đó là ngày lễ)
        if (lunarDate.holidayName != null) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFEF4444).copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(Color(0xFFEF4444).copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Celebration, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "🎉 ${lunarDate.holidayName}",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEF4444)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFEF4444)
                                ) {
                                    Text(
                                        text = "Ngày lễ",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Ngày lễ ${if (lunarDate.isLunarHoliday) "Âm lịch" else "Dương lịch"} • ${lunarDate.formatFull()} (${lunarDate.canChiDay})",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Lunar info card on top of Day view (only shown if showLunarCalendar is true)
        if (showLunarCalendar) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF59E0B).copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFF59E0B).copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.NightsStay, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Âm lịch: ${lunarDate.formatFull()}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF59E0B)
                            )
                            Text(
                                text = "Ngày: ${lunarDate.canChiDay} • Tháng: ${lunarDate.canChiMonth}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Timeline Trục Thời Gian với các Event chiếm diện tích theo tỉ lệ thời lượng
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 0.5.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(totalTimelineHeight + 36.dp)
                        .padding(horizontal = 8.dp, vertical = 12.dp)
                ) {
                    // Cột Nhãn Giờ bên trái (05:00 đến 24:00)
                    Box(
                        modifier = Modifier
                            .width(46.dp)
                            .fillMaxHeight()
                    ) {
                        for (i in 0..totalTimelineHours) {
                            val hour = 5 + i
                            val timeLabel = String.format("%02d:00", hour)
                            val topOffset = (i * 64).dp
                            val isCurrentHourMatch = isToday && hour == currentHour

                            Row(
                                modifier = Modifier
                                    .offset(y = topOffset - 7.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = timeLabel,
                                    fontSize = 11.sp,
                                    fontWeight = if (isCurrentHourMatch) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isCurrentHourMatch) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (isCurrentHourMatch) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Khu vực Lưới Thời Gian & Các Thẻ Sự Kiện chiếm tỉ lệ
                    BoxWithConstraints(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        val availableWidth = maxWidth

                        // Đường kẻ ngang phân cách từng giờ
                        for (i in 0..totalTimelineHours) {
                            val topOffset = (i * 64).dp
                            val hour = 5 + i
                            val isCurrentHourMatch = isToday && hour == currentHour

                            HorizontalDivider(
                                modifier = Modifier.offset(y = topOffset),
                                color = if (isCurrentHourMatch) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                thickness = if (isCurrentHourMatch) 1.5.dp else 1.dp
                            )
                        }

                        // Đường kẻ phụ nhẹ nửa tiếng (:30)
                        for (i in 0 until totalTimelineHours) {
                            val topOffset = (i * 64 + 32).dp
                            HorizontalDivider(
                                modifier = Modifier.offset(y = topOffset),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                                thickness = 0.5.dp
                            )
                        }

                        // Đường chỉ báo giờ hiện tại nếu đang xem ngày hôm nay
                        if (isToday) {
                            val now = LocalDateTime.now()
                            val currentMinutesFrom5 = (now.hour * 60 + now.minute) - 5 * 60
                            if (currentMinutesFrom5 in 0..(totalTimelineHours * 60)) {
                                val currentTop = ((currentMinutesFrom5.toFloat() / 60f) * 64f).dp
                                Box(
                                    modifier = Modifier
                                        .offset(y = currentTop - 4.dp)
                                        .fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(Color(0xFFEF4444), CircleShape)
                                        )
                                        HorizontalDivider(
                                            color = Color(0xFFEF4444),
                                            thickness = 1.5.dp,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }

                        // Hiển thị các sự kiện theo đúng tọa độ thời gian và diện tích tỉ lệ
                        positionedEvents.forEach { pos ->
                            val colWidth = availableWidth / pos.totalCols
                            val xOffset = colWidth * pos.colIndex

                            DayTimelineEventCard(
                                positionedEvent = pos,
                                width = colWidth,
                                onClick = { onEventClick(pos.event) },
                                onToggleCompletion = { onToggleCompletion(pos.event) },
                                modifier = Modifier
                                    .offset(x = xOffset, y = pos.topDp)
                                    .width(colWidth)
                                    .height(pos.heightDp)
                                    .padding(horizontal = 2.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DayScheduleView(
    activeDate: LocalDate,
    events: List<Event>,
    showLunarCalendar: Boolean = true,
    weatherMap: Map<LocalDate, com.example.data.entities.WeatherForecastEntity> = emptyMap(),
    onSelectDate: (LocalDate) -> Unit,
    onEventClick: (Event) -> Unit,
    onToggleCompletion: (Event) -> Unit
) {
    val baseDayDate = remember { LocalDate.of(2020, 1, 1) }
    val pageCount = 73000 // ~200 years of days
    fun pageToDate(page: Int): LocalDate = baseDayDate.plusDays(page.toLong())
    fun dateToPage(date: LocalDate): Int = java.time.temporal.ChronoUnit.DAYS.between(baseDayDate, date).toInt()

    val initialPage = remember { dateToPage(activeDate).coerceIn(0, pageCount - 1) }
    val pagerState = rememberPagerState(initialPage = initialPage) { pageCount }

    // Synchronize pager when activeDate changes externally
    LaunchedEffect(activeDate) {
        val targetPage = dateToPage(activeDate).coerceIn(0, pageCount - 1)
        if (pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    // Synchronize activeDate when user swipes pages in HorizontalPager
    LaunchedEffect(pagerState.currentPage) {
        val newDate = pageToDate(pagerState.currentPage)
        if (newDate != activeDate) {
            onSelectDate(newDate)
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        val pageDate = pageToDate(page)
        val dayEvents = remember(pageDate, events) {
            events.filter { it.date == pageDate }
        }
        DayScheduleContent(
            activeDate = pageDate,
            events = dayEvents,
            showLunarCalendar = showLunarCalendar,
            weatherMap = weatherMap,
            onEventClick = onEventClick,
            onToggleCompletion = onToggleCompletion
        )
    }
}

/**
 * Thẻ hiển thị sự kiện trong trục thời gian ngày với diện tích tỉ lệ theo thời lượng.
 */
@Composable
fun DayTimelineEventCard(
    positionedEvent: PositionedTimelineEvent,
    width: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    onToggleCompletion: () -> Unit,
    modifier: Modifier = Modifier
) {
    val event = positionedEvent.event
    val height = positionedEvent.heightDp
    val isShort = height < 44.dp
    val isTall = height >= 68.dp

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = event.category.getThemedBgColor(),
        border = androidx.compose.foundation.BorderStroke(1.dp, event.category.getThemedBorderColor())
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (isShort) 6.dp else 8.dp, vertical = if (isShort) 2.dp else 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thanh màu danh mục bên trái
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(event.category.color)
            )
            Spacer(modifier = Modifier.width(6.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = if (isShort) Arrangement.Center else Arrangement.SpaceBetween
            ) {
                // Tiêu đề và huy hiệu danh mục
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = event.title,
                        fontSize = if (isShort) 12.sp else 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = if (isTall) 2 else 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (!isShort && width > 130.dp) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = event.category.color.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = event.category.title,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = event.category.color,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp)
                            )
                        }
                    }
                }

                // Thông tin khung giờ
                if (!isShort || height >= 32.dp) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 1.dp)
                    ) {
                        Icon(
                            Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = event.category.color,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = DateUtils.formatTimeRange(event.startTime, event.endTime),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Chi tiết ghi chú khi thẻ dài hơn 1 tiếng
                if (isTall && event.reminderNote.isNotBlank()) {
                    Text(
                        text = "📝 ${event.reminderNote}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            // Nút đánh dấu hoàn thành
            if (width > 90.dp) {
                IconButton(
                    onClick = onToggleCompletion,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        if (event.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = "Hoàn thành",
                        tint = if (event.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DayEventCard(
    event: Event,
    onClick: () -> Unit,
    onToggleCompletion: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = event.category.getThemedBgColor(),
        border = androidx.compose.foundation.BorderStroke(1.dp, event.category.getThemedBorderColor())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(38.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(event.category.color)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = event.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = event.category.color.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = event.category.title,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = event.category.color,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = event.category.color,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = DateUtils.formatTimeRange(event.startTime, event.endTime),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            IconButton(onClick = onToggleCompletion) {
                Icon(
                    if (event.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = "Hoàn thành",
                    tint = if (event.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

/**
 * 2. GIAO DIỆN TUẦN:
 * Hiển thị lịch theo 7 ngày trong tuần với danh sách các ngày từ thứ 2 đến CN làm tên cột gắn liền với Lịch tuần theo giờ.
 * Các sự kiện trong các ngày của tuần chiếm diện tích chiều cao tương ứng với tỉ lệ thời lượng (kéo dài > 1 tiếng sẽ mở rộng tương ứng).
 * Hỗ trợ chuyển tuần bằng ViewPager (HorizontalPager) lướt trái phải mượt mà.
 */
@Composable
fun WeekCalendarGridCard(
    monday: LocalDate,
    activeDate: LocalDate,
    events: List<Event>,
    showLunarCalendar: Boolean,
    weatherMap: Map<LocalDate, com.example.data.entities.WeatherForecastEntity> = emptyMap(),
    onSelectDate: (LocalDate) -> Unit,
    onEventClick: (Event) -> Unit
) {
    val weekDays = remember(monday) {
        (0..6).map { monday.plusDays(it.toLong()) }
    }
    val today = LocalDate.now()
    val weekHourHeight = 48.dp
    val totalHours = 19 // 5h to 24h
    val totalGridHeight = weekHourHeight * totalHours

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Row: Cột Giờ + 7 cột Ngày từ Thứ 2 đến CN gắn liền với bảng giờ
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(vertical = 6.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cột nhãn giờ bên trái
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .padding(horizontal = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Giờ",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 7 cột ngày Thứ 2 đến CN
                weekDays.forEach { date ->
                    val isSelected = date == activeDate
                    val isDateToday = date == today
                    val shortDay = DateUtils.getVietnameseDayOfWeek(date.dayOfWeek, short = true)
                    val lunar = remember(date) { LunarCalendarHelper.convertSolarToLunar(date) }
                    val eventsOnThisDay = events.filter { it.date == date }
                    val hasHoliday = lunar.holidayName != null || eventsOnThisDay.any { it.category.id == EventCategory.HOLIDAY.id }
                    val colWeather = weatherMap[date]

                    val borderModifier = if (hasHoliday) {
                        Modifier.border(1.5.dp, Color(0xFFEF4444), RoundedCornerShape(8.dp))
                    } else if (isDateToday && !isSelected) {
                        Modifier.border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    } else {
                        Modifier
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .then(borderModifier)
                            .clickable { onSelectDate(date) }
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else if (isDateToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                else Color.Transparent
                            )
                            .padding(vertical = 4.dp, horizontal = 1.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = shortDay,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else if (hasHoliday) Color(0xFFEF4444) else if (isDateToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = date.dayOfMonth.toString(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else if (hasHoliday) Color(0xFFEF4444) else if (isDateToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            if (colWeather != null) {
                                Spacer(modifier = Modifier.width(1.5.dp))
                                Text(
                                    text = colWeather.weatherEmoji,
                                    fontSize = 8.sp
                                )
                            }
                        }
                        if (showLunarCalendar) {
                            Text(
                                text = lunar.formatShort(),
                                fontSize = 8.5.sp,
                                fontWeight = if (lunar.isImportantLunarDay || hasHoliday) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color(0xFFFEF08A) else if (hasHoliday) Color(0xFFEF4444) else if (lunar.isImportantLunarDay) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), thickness = 1.dp)

            // Nội dung bảng giờ tuần (5:00 đến 24:00) với tỉ lệ thời gian tương ứng
            val weekScrollState = rememberScrollState()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(weekScrollState)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(totalGridHeight + 20.dp)
                        .padding(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    // Cột giờ bên trái (5:00 đến 24:00)
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .fillMaxHeight()
                    ) {
                        for (i in 0..totalHours) {
                            val hour = 5 + i
                            val timeLabel = String.format("%02d:00", hour)
                            val topOffset = (i * 48).dp
                            Text(
                                text = timeLabel,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .offset(y = topOffset - 6.dp)
                                    .width(36.dp)
                            )
                        }
                    }

                    // 7 cột tương ứng 7 ngày trong tuần
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        weekDays.forEach { date ->
                            val isSelected = date == activeDate
                            val isDateToday = date == today
                            val eventsForThisDay = remember(date, events) {
                                events.filter { it.date == date }
                            }
                            val positionedEvents = remember(eventsForThisDay) {
                                calculatePositionedTimelineEvents(
                                    events = eventsForThisDay,
                                    startHour = 5,
                                    endHour = 24,
                                    hourHeightDp = weekHourHeight,
                                    minHeightDp = 18.dp
                                )
                            }

                            BoxWithConstraints(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                        else if (isDateToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)
                                        else Color.Transparent
                                    )
                                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                                    .clickable { onSelectDate(date) }
                            ) {
                                val colWidth = maxWidth

                                // Kẻ các đường ngang chia giờ
                                for (i in 0..totalHours) {
                                    val topOffset = (i * 48).dp
                                    HorizontalDivider(
                                        modifier = Modifier.offset(y = topOffset),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f),
                                        thickness = 0.5.dp
                                    )
                                }

                                // Chỉ báo giờ hiện tại nếu là hôm nay
                                if (isDateToday) {
                                    val now = LocalDateTime.now()
                                    val currentMinutesFrom5 = (now.hour * 60 + now.minute) - 5 * 60
                                    if (currentMinutesFrom5 in 0..(totalHours * 60)) {
                                        val currentTop = ((currentMinutesFrom5.toFloat() / 60f) * 48f).dp
                                        HorizontalDivider(
                                            modifier = Modifier.offset(y = currentTop),
                                            color = Color(0xFFEF4444),
                                            thickness = 1.5.dp
                                        )
                                    }
                                }

                                // Các sự kiện chiếm diện tích tương ứng với tỉ lệ thời lượng
                                positionedEvents.forEach { pos ->
                                    val eventWidth = colWidth / pos.totalCols
                                    val xOffset = eventWidth * pos.colIndex

                                    Surface(
                                        modifier = Modifier
                                            .offset(x = xOffset, y = pos.topDp)
                                            .width(eventWidth)
                                            .height(pos.heightDp)
                                            .padding(1.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .clickable { onEventClick(pos.event) },
                                        color = pos.event.category.getThemedBgColor(),
                                        shape = RoundedCornerShape(4.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, pos.event.category.getThemedBorderColor())
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 2.dp, vertical = 1.5.dp)
                                        ) {
                                            Text(
                                                text = pos.event.title,
                                                fontSize = 8.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = pos.event.category.color,
                                                maxLines = if (pos.heightDp >= 60.dp) 3 else if (pos.heightDp >= 32.dp) 2 else 1,
                                                overflow = TextOverflow.Ellipsis,
                                                lineHeight = 9.5.sp
                                            )
                                            if (pos.heightDp >= 34.dp) {
                                                Text(
                                                    text = DateUtils.formatTimeRange(pos.event.startTime, pos.event.endTime),
                                                    fontSize = 7.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = pos.event.category.color.copy(alpha = 0.85f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    lineHeight = 8.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeekScheduleView(
    activeDate: LocalDate,
    events: List<Event>,
    showLunarCalendar: Boolean = true,
    weatherMap: Map<LocalDate, com.example.data.entities.WeatherForecastEntity> = emptyMap(),
    onSelectDate: (LocalDate) -> Unit,
    onEventClick: (Event) -> Unit,
    onToggleCompletion: (Event) -> Unit
) {
    val baseWeekMonday = remember { LocalDate.of(2020, 1, 6) }
    val pageCount = 10400 // ~200 years of weeks
    fun pageToMonday(page: Int): LocalDate = baseWeekMonday.plusWeeks(page.toLong())
    fun mondayToPage(monday: LocalDate): Int = java.time.temporal.ChronoUnit.WEEKS.between(baseWeekMonday, monday).toInt()

    val currentMonday = remember(activeDate) {
        activeDate.minusDays((activeDate.dayOfWeek.value - 1).toLong())
    }
    val initialPage = remember { mondayToPage(currentMonday).coerceIn(0, pageCount - 1) }
    val pagerState = rememberPagerState(initialPage = initialPage) { pageCount }

    // Synchronize pager when activeDate changes externally (e.g. from top nav buttons or Today)
    LaunchedEffect(activeDate) {
        val monday = activeDate.minusDays((activeDate.dayOfWeek.value - 1).toLong())
        val targetPage = mondayToPage(monday).coerceIn(0, pageCount - 1)
        if (pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    // Synchronize activeDate when user swipes pages in HorizontalPager
    LaunchedEffect(pagerState.currentPage) {
        val newMonday = pageToMonday(pagerState.currentPage)
        val curMonday = activeDate.minusDays((activeDate.dayOfWeek.value - 1).toLong())
        if (newMonday != curMonday) {
            val dayOffset = activeDate.dayOfWeek.value - 1
            val newDate = newMonday.plusDays(dayOffset.toLong())
            onSelectDate(newDate)
        }
    }

    val today = LocalDate.now()
    val isSelectedToday = activeDate == today
    val selectedLunar = remember(activeDate) { LunarCalendarHelper.convertSolarToLunar(activeDate) }
    val selectedWeather = weatherMap[activeDate]
    val dayEvents = remember(activeDate, events) {
        events.filter { it.date == activeDate }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        // Week Calendar ViewPager (HorizontalPager)
        item {
            HorizontalPager(
                state = pagerState,
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                val monday = pageToMonday(page)
                WeekCalendarGridCard(
                    monday = monday,
                    activeDate = activeDate,
                    events = events,
                    showLunarCalendar = showLunarCalendar,
                    weatherMap = weatherMap,
                    onSelectDate = onSelectDate,
                    onEventClick = onEventClick
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Section: Thông tin Ngày Lễ & Sự kiện của ngày được chọn trong tuần (theo bố cục mẫu)
        item {
            val dayOfWeekStr = DateUtils.formatDayOfWeekDot(activeDate.dayOfWeek)
            val lunarStr = if (showLunarCalendar) "ÂL ${selectedLunar.day} Th${selectedLunar.month}${if (selectedLunar.isLeap) " (nhuận)" else ""}" else ""

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        // Hàng 1: 24  T.2  ÂL 12 Th7
                        Row(
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = activeDate.dayOfMonth.toString(),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 34.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = dayOfWeekStr,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                            if (showLunarCalendar) {
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = lunarStr,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                            }
                            if (isSelectedToday) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF10B981).copy(alpha = 0.18f),
                                    modifier = Modifier.padding(bottom = 3.dp)
                                ) {
                                    Text(
                                        text = "Hôm nay",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF10B981),
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        // Hàng 2: ⛅  31°/26°C
                        if (selectedWeather != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedWeather.weatherEmoji,
                                    fontSize = 18.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${selectedWeather.tempMax}°/${selectedWeather.tempMin}°C",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (isSelectedToday && selectedWeather.remainingDayForecast != null) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "• ${selectedWeather.remainingDayForecast}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "${dayEvents.size} việc",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Card Thông tin Ngày Lễ (nếu ngày đang chọn có ngày lễ)
        val holidayTitle = selectedLunar.holidayName ?: dayEvents.firstOrNull { it.category.id == EventCategory.HOLIDAY.id }?.title
        if (holidayTitle != null) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFEF4444).copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFEF4444).copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(Color(0xFFEF4444).copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Celebration, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "🎉 $holidayTitle",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEF4444)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFEF4444)
                                ) {
                                    Text(
                                        text = "Ngày lễ",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Ngày lễ ${if (selectedLunar.isLunarHoliday) "Âm lịch" else "Dương lịch"} • ${selectedLunar.formatFull()} (${selectedLunar.canChiDay}, tháng ${selectedLunar.canChiMonth})",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Danh sách sự kiện của ngày được chọn
        if (dayEvents.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = if (holidayTitle != null) "Không có lịch trình nào khác được lên lịch trong ngày lễ này" else "Không có sự kiện nào được lên lịch cho ngày này",
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(dayEvents) { event ->
                DayEventCard(
                    event = event,
                    onClick = { onEventClick(event) },
                    onToggleCompletion = { onToggleCompletion(event) }
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

data class MonthEventItem(
    val title: String,
    val bgColor: Color,
    val textColor: Color,
    val borderColor: Color
)

@Composable
fun MonthEventBar(
    item: MonthEventItem,
    isCurrentMonth: Boolean
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(14.dp),
        shape = RoundedCornerShape(2.5.dp),
        color = if (isCurrentMonth) item.bgColor else item.bgColor.copy(alpha = 0.35f),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            if (isCurrentMonth) item.borderColor else item.borderColor.copy(alpha = 0.3f)
        )
    ) {
        Text(
            text = item.title,
            fontSize = 7.5.sp,
            fontWeight = FontWeight.Medium,
            color = if (isCurrentMonth) item.textColor else item.textColor.copy(alpha = 0.45f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 9.sp,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 2.dp, vertical = 0.5.dp)
        )
    }
}

/**
 * 3. GIAO DIỆN THÁNG:
 * Hiển thị lịch tháng theo 7 ngày trong tuần theo cột và từng tuần trong tháng theo hàng
 * kèm Âm Lịch, Ngày Lễ và các sự kiện được đánh dấu thanh màu.
 * Hỗ trợ chuyển tháng bằng ViewPager (HorizontalPager) lướt mượt mà.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MonthCalendarGridCard(
    yearMonth: YearMonth,
    activeDate: LocalDate,
    events: List<Event>,
    showLunarCalendar: Boolean,
    weatherMap: Map<LocalDate, com.example.data.entities.WeatherForecastEntity> = emptyMap(),
    onSelectDate: (LocalDate) -> Unit,
    onDateDoubleClick: (LocalDate) -> Unit = {}
) {
    val firstDayOfMonth = remember(yearMonth) { yearMonth.atDay(1) }
    val daysInMonth = remember(yearMonth) { yearMonth.lengthOfMonth() }
    val dayOfWeekOffset = remember(firstDayOfMonth) { firstDayOfMonth.dayOfWeek.value - 1 } // 0 for Mon, 6 for Sun
    val today = LocalDate.now()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Weekday headers (T.2 -> CN)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("T.2", "T.3", "T.4", "T.5", "T.6", "T.7", "CN").forEach { dayName ->
                    Text(
                        text = dayName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (dayName == "CN") Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(4.dp))

            // Month days matrix (fixed 6 rows x 7 days = 42 cells for consistent height across all months)
            val rows = 6

            for (row in 0 until rows) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(88.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (col in 0..6) {
                        val cellIndex = row * 7 + col
                        val dayOffset = cellIndex - dayOfWeekOffset
                        val isCurrentMonth = dayOffset in 0 until daysInMonth
                        val cellDate = if (dayOffset < 0) {
                            firstDayOfMonth.minusDays((-dayOffset).toLong())
                        } else if (dayOffset >= daysInMonth) {
                            firstDayOfMonth.plusDays(dayOffset.toLong())
                        } else {
                            yearMonth.atDay(dayOffset + 1)
                        }

                        val isSelected = cellDate == activeDate
                        val isCellToday = cellDate == today
                        val cellEvents = events.filter { it.date == cellDate }
                        val lunar = remember(cellDate) { LunarCalendarHelper.convertSolarToLunar(cellDate) }
                        val hasHoliday = (showLunarCalendar && lunar.holidayName != null) || cellEvents.any { it.category.id == EventCategory.HOLIDAY.id }
                        val cellWeather = weatherMap[cellDate]

                        val themedHolidayBg = Color(0xFFEF4444).copy(alpha = 0.15f)
                        val themedHolidayBorder = Color(0xFFEF4444).copy(alpha = 0.4f)
                        val themedHolidayText = Color(0xFFEF4444)
                        val surfaceColor = MaterialTheme.colorScheme.surface
                        val isDarkTheme = (0.299f * surfaceColor.red + 0.587f * surfaceColor.green + 0.114f * surfaceColor.blue) < 0.5f

                        val displayItems = remember(cellEvents, lunar.holidayName, showLunarCalendar, isDarkTheme) {
                            val list = mutableListOf<MonthEventItem>()
                            if (showLunarCalendar && lunar.holidayName != null) {
                                list.add(
                                    MonthEventItem(
                                        title = lunar.holidayName,
                                        bgColor = themedHolidayBg,
                                        textColor = themedHolidayText,
                                        borderColor = themedHolidayBorder
                                    )
                                )
                            }
                            cellEvents.forEach { evt ->
                                if (lunar.holidayName == null || evt.title != lunar.holidayName) {
                                    list.add(
                                        MonthEventItem(
                                            title = evt.title,
                                            bgColor = evt.category.getThemedBgColor(isDarkTheme),
                                            textColor = evt.category.color,
                                            borderColor = evt.category.getThemedBorderColor(isDarkTheme)
                                        )
                                    )
                                }
                            }
                            list
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(1.5.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .combinedClickable(
                                    onClick = { onSelectDate(cellDate) },
                                    onDoubleClick = {
                                        onDateDoubleClick(cellDate)
                                    }
                                )
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    else if (isCellToday) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    else if (hasHoliday && isCurrentMonth) Color(0xFFEF4444).copy(alpha = 0.08f)
                                    else Color.Transparent
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else if (isCellToday) 1.dp else 0.5.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else if (isCellToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else if (hasHoliday && isCurrentMonth) Color(0xFFEF4444).copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 2.dp, vertical = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // 1. Solar Day + Emoji Thời Tiết (chỉ hiển thị emoji bên phải ngày dương lịch)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = cellDate.dayOfMonth.toString(),
                                        fontSize = 13.5.sp,
                                        fontWeight = if (isCellToday || isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (!isCurrentMonth) MaterialTheme.colorScheme.outlineVariant
                                        else if (isCellToday) MaterialTheme.colorScheme.primary
                                        else if (col == 6) Color(0xFFEF4444)
                                        else MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center
                                    )
                                    if (cellWeather != null) {
                                        Spacer(modifier = Modifier.width(1.5.dp))
                                        Text(
                                            text = cellWeather.weatherEmoji,
                                            fontSize = 8.5.sp
                                        )
                                    }
                                }

                                // 2. Lunar Day (nằm NGAY DƯỚI ngày dương, chữ nhỏ, màu nhạt nhẹ nhàng)
                                if (showLunarCalendar) {
                                    Text(
                                        text = lunar.formatShort(),
                                        fontSize = 8.5.sp,
                                        fontWeight = if (isCurrentMonth && (lunar.isImportantLunarDay || lunar.holidayName != null)) FontWeight.Medium else FontWeight.Normal,
                                        color = if (!isCurrentMonth) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        else if (lunar.holidayName != null) Color(0xFFEF4444).copy(alpha = 0.85f)
                                        else if (lunar.isImportantLunarDay) Color(0xFFF59E0B)
                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        textAlign = TextAlign.Center,
                                        maxLines = 1
                                    )
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                // 3. Event Color Bars (tối đa 2 dòng, nếu > 2 thì dòng 2 là "...")
                                if (displayItems.isNotEmpty()) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(1.5.dp)
                                    ) {
                                        if (displayItems.size == 1) {
                                            MonthEventBar(item = displayItems[0], isCurrentMonth = isCurrentMonth)
                                        } else if (displayItems.size == 2) {
                                            MonthEventBar(item = displayItems[0], isCurrentMonth = isCurrentMonth)
                                            MonthEventBar(item = displayItems[1], isCurrentMonth = isCurrentMonth)
                                        } else {
                                            MonthEventBar(item = displayItems[0], isCurrentMonth = isCurrentMonth)
                                            Text(
                                                text = "...",
                                                fontSize = 8.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isCurrentMonth) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outlineVariant,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (row < rows - 1) {
                    Spacer(modifier = Modifier.height(2.dp))
                }
            }
        }
    }
}

@Composable
fun MonthScheduleView(
    activeDate: LocalDate,
    events: List<Event>,
    showLunarCalendar: Boolean = true,
    weatherMap: Map<LocalDate, com.example.data.entities.WeatherForecastEntity> = emptyMap(),
    onSelectDate: (LocalDate) -> Unit,
    onDateDoubleClick: (LocalDate) -> Unit = {},
    onPreviousMonth: () -> Unit = {},
    onNextMonth: () -> Unit = {},
    onEventClick: (Event) -> Unit,
    onToggleCompletion: (Event) -> Unit
) {
    val pageCount = 2400
    fun pageToYearMonth(page: Int): YearMonth = YearMonth.of(2020, 1).plusMonths(page.toLong())
    fun yearMonthToPage(ym: YearMonth): Int = (ym.year - 2020) * 12 + (ym.monthValue - 1)

    val currentYearMonth = remember(activeDate) { YearMonth.from(activeDate) }
    val initialPage = remember { yearMonthToPage(currentYearMonth).coerceIn(0, pageCount - 1) }
    val pagerState = rememberPagerState(initialPage = initialPage) { pageCount }

    // Synchronize pager when activeDate changes externally (e.g. from top nav button or Today)
    LaunchedEffect(activeDate) {
        val targetPage = yearMonthToPage(YearMonth.from(activeDate)).coerceIn(0, pageCount - 1)
        if (pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    // Synchronize activeDate when user swipes pages in HorizontalPager
    LaunchedEffect(pagerState.currentPage) {
        val newYearMonth = pageToYearMonth(pagerState.currentPage)
        val currentActiveYM = YearMonth.from(activeDate)
        if (newYearMonth != currentActiveYM) {
            val newDay = activeDate.dayOfMonth.coerceAtMost(newYearMonth.lengthOfMonth())
            val newDate = newYearMonth.atDay(newDay)
            onSelectDate(newDate)
        }
    }

    val today = LocalDate.now()
    val isSelectedToday = activeDate == today
    val selectedWeather = weatherMap[activeDate]

    val dayEvents = remember(activeDate, events) {
        events.filter { it.date == activeDate }
    }
    val selectedLunar = remember(activeDate) { LunarCalendarHelper.convertSolarToLunar(activeDate) }
    val holidayTitle = selectedLunar.holidayName ?: dayEvents.firstOrNull { it.category.id == EventCategory.HOLIDAY.id }?.title

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        // Month Calendar ViewPager (HorizontalPager)
        item {
            HorizontalPager(
                state = pagerState,
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                val ym = pageToYearMonth(page)
                MonthCalendarGridCard(
                    yearMonth = ym,
                    activeDate = activeDate,
                    events = events,
                    showLunarCalendar = showLunarCalendar,
                    weatherMap = weatherMap,
                    onSelectDate = onSelectDate,
                    onDateDoubleClick = onDateDoubleClick
                )
            }
        }

        // Details Header of Events on the Selected Month Date (theo bố cục mẫu)
        item {
            Spacer(modifier = Modifier.height(12.dp))
            val dayOfWeekStr = DateUtils.formatDayOfWeekDot(activeDate.dayOfWeek)
            val lunarStr = if (showLunarCalendar) "ÂL ${selectedLunar.day} Th${selectedLunar.month}${if (selectedLunar.isLeap) " (nhuận)" else ""}" else ""

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        // Hàng 1: 24  T.2  ÂL 12 Th7
                        Row(
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = activeDate.dayOfMonth.toString(),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 34.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = dayOfWeekStr,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                            if (showLunarCalendar) {
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = lunarStr,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                            }
                            if (isSelectedToday) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF10B981).copy(alpha = 0.18f),
                                    modifier = Modifier.padding(bottom = 3.dp)
                                ) {
                                    Text(
                                        text = "Hôm nay",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF10B981),
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        // Hàng 2: ⛅  31°/26°C
                        if (selectedWeather != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedWeather.weatherEmoji,
                                    fontSize = 18.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${selectedWeather.tempMax}°/${selectedWeather.tempMin}°C",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (isSelectedToday && selectedWeather.remainingDayForecast != null) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "• ${selectedWeather.remainingDayForecast}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "${dayEvents.size} việc",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Thẻ thông tin Ngày Lễ nổi bật khi chọn trúng một ngày lễ trong tháng
        if (holidayTitle != null) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFEF4444).copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFEF4444).copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Celebration,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "🎉 $holidayTitle",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEF4444)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFEF4444)
                                ) {
                                    Text(
                                        text = "Ngày lễ",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Ngày lễ ${if (selectedLunar.isLunarHoliday) "Âm lịch" else "Dương lịch"} • ${selectedLunar.formatFull()} (${selectedLunar.canChiDay}, tháng ${selectedLunar.canChiMonth})",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Danh sách các sự kiện được lên lịch trong ngày
        if (dayEvents.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = if (holidayTitle != null) "Không có lịch trình nào khác được lên lịch trong ngày lễ này" else "Không có hoạt động nào trong ngày này",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(dayEvents) { event ->
                DayEventCard(
                    event = event,
                    onClick = { onEventClick(event) },
                    onToggleCompletion = { onToggleCompletion(event) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

/**
 * Popup hiển thị danh sách sự kiện chi tiết khi bấm vào một ô ngày trong Lịch Tháng
 */
@Composable
fun DayEventsPopupDialog(
    date: LocalDate,
    events: List<Event>,
    showLunarCalendar: Boolean,
    weather: com.example.data.entities.WeatherForecastEntity? = null,
    onDismiss: () -> Unit,
    onAddNewEvent: () -> Unit,
    onEditEvent: (Int) -> Unit,
    onDeleteEvent: (Int) -> Unit,
    onToggleCompletion: (Event) -> Unit
) {
    val lunar = remember(date) { LunarCalendarHelper.convertSolarToLunar(date) }
    val isToday = date == LocalDate.now()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${DateUtils.getVietnameseDayOfWeek(date.dayOfWeek)}, ${date.dayOfMonth}/${date.monthValue}/${date.year}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (isToday) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF10B981).copy(alpha = 0.18f)
                                ) {
                                    Text(
                                        text = "Hôm nay",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF10B981),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        if (showLunarCalendar) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Âm lịch: ${lunar.formatFull()} (${lunar.canChiDay})",
                                fontSize = 11.5.sp,
                                color = Color(0xFFF59E0B),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                }

                if (lunar.holidayName != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFEF4444).copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Celebration,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "🎉 ${lunar.holidayName} (Ngày lễ)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEF4444)
                            )
                        }
                    }
                }

                // Thông tin thời tiết của ngày được chọn
                if (weather != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = weather.weatherEmoji,
                                        fontSize = 24.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = weather.conditionDescription,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Nhiệt độ TB: ${weather.tempAvg}°C",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "🌡️ ${weather.tempMin}° - ${weather.tempMax}°C",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            if (!weather.remainingDayForecast.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                Spacer(modifier = Modifier.height(5.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "🌤️ ${weather.remainingDayForecast}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (events.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.EventBusy,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Không có sự kiện nào trong ngày này",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Danh sách sự kiện (${events.size}):",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(events) { evt ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = evt.category.getThemedBgColor(),
                                border = androidx.compose.foundation.BorderStroke(1.dp, evt.category.getThemedBorderColor()),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = evt.category.color
                                        ) {
                                            Text(
                                                text = evt.category.title,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { onEditEvent(evt.id) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Edit,
                                                    contentDescription = "Sửa",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(4.dp))
                                            IconButton(
                                                onClick = { onDeleteEvent(evt.id) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Xóa",
                                                    tint = Color(0xFFEF4444),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = evt.title,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = DateUtils.formatTimeRange(evt.startTime, evt.endTime),
                                            fontSize = 11.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    if (evt.reminderNote.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = evt.reminderNote,
                                            fontSize = 11.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onAddNewEvent,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Thêm sự kiện", fontSize = 12.5.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Đóng", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.5.sp)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    )
}
