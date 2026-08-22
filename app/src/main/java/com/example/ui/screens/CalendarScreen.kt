package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.Event
import com.example.models.EventCategory
import com.example.ui.theme.PrimaryPurple
import com.example.util.DateUtils
import com.example.viewmodel.ScheduleViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

enum class CalendarViewMode(val title: String) {
    DAY("Ngày"),
    WEEK("Tuần"),
    MONTH("Tháng")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: ScheduleViewModel,
    onNavigateToAddEvent: () -> Unit
) {
    val allEvents by viewModel.events.collectAsState()
    var currentViewMode by remember { mutableStateOf(CalendarViewMode.WEEK) } // Mặc định hiển thị lịch tuần
    var activeDate by remember { mutableStateOf(LocalDate.now()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedEventToDelete by remember { mutableStateOf<Event?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFC))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp) // Space for bottom action buttons & bottom nav
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
                        events = allEvents.filter { it.date == activeDate },
                        onEventClick = { selectedEventToDelete = it; showDeleteDialog = true }
                    )
                }
                CalendarViewMode.WEEK -> {
                    WeekScheduleView(
                        activeDate = activeDate,
                        events = allEvents,
                        onSelectDate = { activeDate = it },
                        onEventClick = { selectedEventToDelete = it; showDeleteDialog = true }
                    )
                }
                CalendarViewMode.MONTH -> {
                    MonthScheduleView(
                        activeDate = activeDate,
                        events = allEvents,
                        onSelectDate = { activeDate = it },
                        onEventClick = { selectedEventToDelete = it; showDeleteDialog = true }
                    )
                }
            }
        }

        // 2 Floating Action Buttons at the bottom (Add '+' and Delete 'X')
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Nút xóa event (hình dấu X)
            FloatingActionButton(
                onClick = {
                    val eventsOnDate = allEvents.filter { it.date == activeDate }
                    if (eventsOnDate.isNotEmpty()) {
                        selectedEventToDelete = eventsOnDate.firstOrNull()
                    }
                    showDeleteDialog = true
                },
                containerColor = Color.White,
                contentColor = Color(0xFFEF4444),
                shape = CircleShape,
                modifier = Modifier
                    .size(52.dp)
                    .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                    .testTag("btn_delete_event"),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 3.dp)
            ) {
                Icon(Icons.Default.Clear, contentDescription = "Xóa sự kiện", modifier = Modifier.size(24.dp))
            }

            // Nút thêm event (hình dấu +)
            FloatingActionButton(
                onClick = {
                    viewModel.setSelectedDate(activeDate)
                    onNavigateToAddEvent()
                },
                containerColor = PrimaryPurple,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .size(58.dp)
                    .testTag("btn_add_event"),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Thêm sự kiện", modifier = Modifier.size(28.dp))
            }
        }

        // Delete Confirmation Dialog
        if (showDeleteDialog) {
            val dayEvents = allEvents.filter { it.date == activeDate }
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Quản lý & Xóa sự kiện", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        if (dayEvents.isEmpty()) {
                            Text("Không có sự kiện nào trong ngày ${activeDate.dayOfMonth}/${activeDate.monthValue} để xóa.")
                        } else {
                            Text("Chọn sự kiện bạn muốn xóa:")
                            Spacer(modifier = Modifier.height(10.dp))
                            LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                                items(dayEvents) { evt ->
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                viewModel.deleteEvent(evt.id)
                                                showDeleteDialog = false
                                            },
                                        color = evt.category.bgColor,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(evt.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text(
                                                    DateUtils.formatTimeRange(evt.startTime, evt.endTime),
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF64748B)
                                                )
                                            }
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
                        Text("Đóng")
                    }
                }
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
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.DateRange,
                contentDescription = null,
                tint = PrimaryPurple,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Lịch hoạt động",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
        }

        // 3 mục tùy chọn hiển thị: Ngày, Tuần, Tháng (ở góc trên bên phải)
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFFE2E8F0).copy(alpha = 0.7f),
            modifier = Modifier.padding(2.dp)
        ) {
            Row(modifier = Modifier.padding(3.dp)) {
                CalendarViewMode.entries.forEach { mode ->
                    val isSelected = currentMode == mode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) PrimaryPurple else Color.Transparent)
                            .clickable { onModeSelected(mode) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("mode_${mode.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mode.title,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Color(0xFF475569)
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
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit
) {
    val titleText = when (viewMode) {
        CalendarViewMode.DAY -> DateUtils.formatShortVietnameseDate(activeDate)
        CalendarViewMode.WEEK -> {
            val monday = activeDate.minusDays((activeDate.dayOfWeek.value - 1).toLong())
            val sunday = monday.plusDays(6)
            "Tháng ${activeDate.monthValue}, ${activeDate.year} (${monday.dayOfMonth}/${monday.monthValue} - ${sunday.dayOfMonth}/${sunday.monthValue})"
        }
        CalendarViewMode.MONTH -> "Tháng ${activeDate.monthValue} năm ${activeDate.year}"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 0.5.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevious, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous", tint = Color(0xFF334155))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = titleText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    textAlign = TextAlign.Center
                )
                if (activeDate != LocalDate.now()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = PrimaryPurple.copy(alpha = 0.1f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onToday() }
                    ) {
                        Text(
                            text = "Hôm nay",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryPurple,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            IconButton(onClick = onNext, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next", tint = Color(0xFF334155))
            }
        }
    }
}

/**
 * 1. GIAO DIỆN NGÀY:
 * Hiển thị lịch theo giờ từ 5 giờ sáng đến 24h đêm (mỗi 1 giờ) cùng các event đã được lên lịch.
 */
@Composable
fun DayScheduleView(
    activeDate: LocalDate,
    events: List<Event>,
    onEventClick: (Event) -> Unit
) {
    val listState = rememberLazyListState()
    val isToday = activeDate == LocalDate.now()
    val currentHour = LocalDateTime.now().hour

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Hours from 5:00 AM to 24:00 (index 5 to 24)
        items(20) { index ->
            val hour = index + 5 // 5 to 24
            val timeLabel = String.format("%02d:00", hour)
            val eventsInThisHour = events.filter {
                val startH = it.startTime.hour
                val endH = if (it.endTime.minute > 0) it.endTime.hour + 1 else it.endTime.hour
                hour in startH until maxOf(startH + 1, endH)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 68.dp)
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Time Label (5h - 24h)
                Column(
                    modifier = Modifier.width(52.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = timeLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isToday && hour == currentHour) PrimaryPurple else Color(0xFF64748B)
                    )
                    if (isToday && hour == currentHour) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(PrimaryPurple, CircleShape)
                        )
                    }
                }

                // Vertical timeline line + Events container
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                ) {
                    // Divider line for the hour
                    HorizontalDivider(
                        color = if (isToday && hour == currentHour) PrimaryPurple.copy(alpha = 0.4f) else Color(0xFFE2E8F0),
                        thickness = 1.dp,
                        modifier = Modifier.align(Alignment.TopStart)
                    )

                    if (eventsInThisHour.isEmpty()) {
                        Spacer(modifier = Modifier.height(56.dp))
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, bottom = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            eventsInThisHour.forEach { event ->
                                DayEventCard(
                                    event = event,
                                    onClick = { onEventClick(event) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DayEventCard(
    event: Event,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = event.category.bgColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, event.category.color.copy(alpha = 0.4f))
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
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color.White
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
                        color = Color(0xFF475569)
                    )
                    if (event.isCompleted) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "✓ Đã hoàn thành",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF10B981)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 2. GIAO DIỆN TUẦN:
 * Hiển thị lịch theo 7 ngày trong tuần theo cột và từng giờ trong ngày theo hàng cùng các event đã được lên lịch.
 */
@Composable
fun WeekScheduleView(
    activeDate: LocalDate,
    events: List<Event>,
    onSelectDate: (LocalDate) -> Unit,
    onEventClick: (Event) -> Unit
) {
    val monday = remember(activeDate) {
        activeDate.minusDays((activeDate.dayOfWeek.value - 1).toLong())
    }
    val weekDays = remember(monday) {
        (0..6).map { monday.plusDays(it.toLong()) }
    }
    val today = LocalDate.now()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        // Weekday 7-column header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(12.dp))
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.width(42.dp)) // Space above time column
            weekDays.forEach { date ->
                val isSelected = date == activeDate
                val isToday = date == today
                val shortDay = DateUtils.getVietnameseDayOfWeek(date.dayOfWeek, short = true)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelectDate(date) }
                        .background(if (isSelected) PrimaryPurple else if (isToday) PrimaryPurple.copy(alpha = 0.12f) else Color.Transparent)
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = shortDay,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected) Color.White else if (isToday) PrimaryPurple else Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = date.dayOfMonth.toString(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else if (isToday) PrimaryPurple else Color(0xFF0F172A)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Week Timetable Grid (Hours 5 to 23 as rows, 7 days as columns)
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.White, RoundedCornerShape(12.dp))
                .padding(vertical = 8.dp)
        ) {
            items(19) { index ->
                val hour = index + 5 // 5h to 23h
                val timeLabel = String.format("%02d:00", hour)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(62.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // Hour label column
                    Text(
                        text = timeLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier
                            .width(42.dp)
                            .padding(top = 2.dp)
                    )

                    // 7 day columns for this hour
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        weekDays.forEach { date ->
                            val eventsForCell = events.filter { evt ->
                                evt.date == date && evt.startTime.hour == hour
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .border(0.5.dp, Color(0xFFF1F5F9))
                                    .padding(1.dp)
                            ) {
                                if (eventsForCell.isNotEmpty()) {
                                    val event = eventsForCell.first()
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(4.dp))
                                            .clickable { onEventClick(event) },
                                        color = event.category.bgColor,
                                        shape = RoundedCornerShape(4.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, event.category.color)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(2.dp),
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = event.title,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = event.category.color,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                lineHeight = 10.sp
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

/**
 * 3. GIAO DIỆN THÁNG:
 * Hiển thị lịch tháng theo 7 ngày trong tuần theo cột và từng tuần trong tháng theo hàng
 * cùng các event đã được lên lịch (được đánh dấu bằng các thanh màu tương ứng với loại event,
 * nếu không thể hiển thị hết trong ô ngày thì hiển thị 3 chấm).
 */
@Composable
fun MonthScheduleView(
    activeDate: LocalDate,
    events: List<Event>,
    onSelectDate: (LocalDate) -> Unit,
    onEventClick: (Event) -> Unit
) {
    val yearMonth = remember(activeDate) { YearMonth.from(activeDate) }
    val firstDayOfMonth = remember(yearMonth) { yearMonth.atDay(1) }
    val daysInMonth = remember(yearMonth) { yearMonth.lengthOfMonth() }
    val dayOfWeekOffset = remember(firstDayOfMonth) { firstDayOfMonth.dayOfWeek.value - 1 } // 0 for Mon, 6 for Sun
    val today = LocalDate.now()

    val dayEvents = remember(activeDate, events) {
        events.filter { it.date == activeDate }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        // Month Calendar Grid Card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    // Weekday headers (T2 -> CN)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN").forEach { dayName ->
                            Text(
                                text = dayName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (dayName == "CN") Color(0xFFEF4444) else Color(0xFF64748B),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    Spacer(modifier = Modifier.height(4.dp))

                    // Month days matrix (rows of 7 days)
                    val totalCells = ((dayOfWeekOffset + daysInMonth + 6) / 7) * 7
                    val rows = totalCells / 7

                    for (row in 0 until rows) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            for (col in 0..6) {
                                val cellIndex = row * 7 + col
                                val dayNumber = cellIndex - dayOfWeekOffset + 1

                                if (dayNumber in 1..daysInMonth) {
                                    val cellDate = yearMonth.atDay(dayNumber)
                                    val isSelected = cellDate == activeDate
                                    val isToday = cellDate == today
                                    val cellEvents = events.filter { it.date == cellDate }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .padding(2.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { onSelectDate(cellDate) }
                                            .background(
                                                if (isSelected) PrimaryPurple.copy(alpha = 0.15f)
                                                else if (isToday) Color(0xFFF8FAFC)
                                                else Color.Transparent
                                            )
                                            .border(
                                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                                color = if (isSelected) PrimaryPurple else Color(0xFFE2E8F0),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .padding(2.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            // Date Number indicator
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .background(
                                                        if (isToday) PrimaryPurple else Color.Transparent,
                                                        CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = dayNumber.toString(),
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isToday) Color.White else if (col == 6) Color(0xFFEF4444) else Color(0xFF0F172A)
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(2.dp))

                                            // Event indicators: colored bars corresponding to event type
                                            if (cellEvents.isNotEmpty()) {
                                                Column(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                                ) {
                                                    // Display up to 2 colored event pills
                                                    cellEvents.take(2).forEach { evt ->
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(10.dp)
                                                                .background(evt.category.color, RoundedCornerShape(2.dp))
                                                                .padding(horizontal = 2.dp),
                                                            contentAlignment = Alignment.CenterStart
                                                        ) {
                                                            Text(
                                                                text = evt.title,
                                                                fontSize = 7.sp,
                                                                color = Color.White,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }

                                                    // Nếu không thể hiển thị hết trong ô ngày thì hiển thị 3 chấm (...)
                                                    if (cellEvents.size > 2) {
                                                        Text(
                                                            text = "...",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = PrimaryPurple,
                                                            textAlign = TextAlign.Center,
                                                            modifier = Modifier.fillMaxWidth()
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // Empty day slot outside current month
                                    Spacer(modifier = Modifier.weight(1f))
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

        // Details of Events on the Selected Month Date
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Lịch ngày ${activeDate.dayOfMonth}/${activeDate.monthValue} (${dayEvents.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = DateUtils.getVietnameseDayOfWeek(activeDate.dayOfWeek),
                    fontSize = 13.sp,
                    color = PrimaryPurple,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (dayEvents.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White
                ) {
                    Text(
                        text = "Không có hoạt động nào trong ngày này",
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(dayEvents) { event ->
                DayEventCard(
                    event = event,
                    onClick = { onEventClick(event) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
