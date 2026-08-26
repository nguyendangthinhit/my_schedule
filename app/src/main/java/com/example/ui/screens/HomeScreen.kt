package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.Event
import com.example.models.EventCategory
import com.example.ui.theme.PrimaryPurple
import com.example.ui.theme.getThemedBgColor
import com.example.ui.theme.getThemedBorderColor
import com.example.util.DateUtils
import com.example.util.LunarCalendarHelper
import com.example.viewmodel.ScheduleViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    viewModel: ScheduleViewModel,
    onNavigateToAddEvent: () -> Unit = {},
    onNavigateToEditEvent: (Int) -> Unit = {}
) {
    val allEvents by viewModel.events.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val showLunarCalendar by viewModel.showLunarCalendar.collectAsState()
    val weatherMap by viewModel.weatherMap.collectAsState()
    var selectedCategoryFilter by remember { mutableStateOf<EventCategory?>(null) }

    val today = LocalDate.now()
    val coroutineScope = rememberCoroutineScope()
    val weekListState = rememberLazyListState()

    val dayEvents = remember(allEvents, selectedDate) {
        allEvents
            .filter { it.date == selectedDate }
            .sortedBy { it.startTime }
    }

    val filteredEvents = remember(dayEvents, selectedCategoryFilter) {
        if (selectedCategoryFilter == null) {
            dayEvents
        } else {
            dayEvents.filter { it.category.id == selectedCategoryFilter?.id }
        }
    }

    val completedCount = dayEvents.count { it.isCompleted }
    val totalCount = dayEvents.size

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
        ) {
            // Top full date, Lunar date and greeting
            item {
                HeaderDateSection(
                    selectedDate = selectedDate,
                    showLunarCalendar = showLunarCalendar,
                    weatherMap = weatherMap,
                    onGoToToday = {
                        viewModel.setSelectedDate(today)
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Horizontal mini calendar picker with Lunar Day and Weather
            item {
                WeekDateCarousel(
                    selectedDate = selectedDate,
                    events = allEvents,
                    showLunarCalendar = showLunarCalendar,
                    weatherMap = weatherMap,
                    listState = weekListState,
                    onSelectDate = { viewModel.setSelectedDate(it) }
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Progress / Summary Card
            item {
                TodayProgressCard(
                    completed = completedCount,
                    total = totalCount,
                    selectedDate = selectedDate
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Category Filter Chips
            item {
                CategoryFilterBar(
                    selectedCategory = selectedCategoryFilter,
                    categories = categories,
                    dayEvents = dayEvents,
                    onCategorySelect = { selectedCategoryFilter = it }
                )
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Section Title
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Việc trong ngày (${filteredEvents.size})",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (selectedCategoryFilter != null) {
                        TextButton(onClick = { selectedCategoryFilter = null }) {
                            Text("Hiện tất cả", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // List of Events with Checkboxes
            if (filteredEvents.isEmpty()) {
                item {
                    EmptyTasksCard(
                        isToday = selectedDate == today
                    )
                }
            } else {
                items(filteredEvents, key = { it.id }) { event ->
                    TaskEventItem(
                        event = event,
                        onToggleCompletion = { viewModel.toggleEventCompletion(event) },
                        onDelete = { viewModel.deleteEvent(event.id) },
                        onEdit = { onNavigateToEditEvent(event.id) }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            // Special Events / Notes Card
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SpecialNoticeCard(selectedDate = selectedDate)
            }
        }

        // Floating Action Button at the bottom (Add '+')
        FloatingActionButton(
            onClick = {
                viewModel.setSelectedDate(selectedDate)
                onNavigateToAddEvent()
            },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
                .size(58.dp)
                .testTag("btn_home_add_event"),
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Thêm sự kiện", modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
fun HeaderDateSection(
    selectedDate: LocalDate,
    showLunarCalendar: Boolean = true,
    weatherMap: Map<LocalDate, com.example.data.entities.WeatherForecastEntity> = emptyMap(),
    onGoToToday: () -> Unit = {}
) {
    val lunarDate = remember(selectedDate) { LunarCalendarHelper.convertSolarToLunar(selectedDate) }
    val isToday = selectedDate == LocalDate.now()
    val weather = weatherMap[selectedDate]

    val dayOfWeekStr = DateUtils.formatDayOfWeekDot(selectedDate.dayOfWeek)
    val lunarStr = if (showLunarCalendar) "ÂL ${lunarDate.day} Th${lunarDate.month}${if (lunarDate.isLeap) " (nhuận)" else ""}" else ""

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Hàng 1: 24  T.2  ÂL 12 Th7
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = selectedDate.dayOfMonth.toString(),
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
                    if (!isToday) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier
                                .padding(bottom = 3.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onGoToToday() }
                                .testTag("btn_home_back_to_today")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    Icons.Default.Today,
                                    contentDescription = "Về hôm nay",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Về hôm nay",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Hàng 2: ⛅  31°/26°C (như trong ảnh mẫu)
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

        // Banner Ngày Lễ nếu có
        if (showLunarCalendar && lunarDate.holidayName != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFEF4444).copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Celebration,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = lunarDate.holidayName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF4444)
                    )
                }
            }
        }
    }
}

@Composable
fun WeekDateCarousel(
    selectedDate: LocalDate,
    events: List<Event> = emptyList(),
    showLunarCalendar: Boolean = true,
    weatherMap: Map<LocalDate, com.example.data.entities.WeatherForecastEntity> = emptyMap(),
    listState: androidx.compose.foundation.lazy.LazyListState = rememberLazyListState(),
    onSelectDate: (LocalDate) -> Unit
) {
    val today = LocalDate.now()
    val startDay = remember(today) { today.minusDays(7) }
    val daysList = remember(startDay) { (0..28).map { startDay.plusDays(it.toLong()) } }

    LaunchedEffect(selectedDate) {
        val index = daysList.indexOf(selectedDate)
        if (index >= 0) {
            // Scroll so the selected date is visible / centered
            val targetIndex = (index - 2).coerceAtLeast(0)
            listState.animateScrollToItem(targetIndex)
        }
    }

    LazyRow(
        state = listState,
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(daysList, key = { it.toString() }) { date ->
            val isSelected = date == selectedDate
            val isToday = date == today
            val dayName = DateUtils.getVietnameseDayOfWeek(date.dayOfWeek, short = true)
            val lunarDate = remember(date) { LunarCalendarHelper.convertSolarToLunar(date) }
            val hasHoliday = lunarDate.holidayName != null || events.any { it.date == date && it.category.id == EventCategory.HOLIDAY.id }
            val dateWeather = weatherMap[date]

            val borderStroke = when {
                hasHoliday -> androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFEF4444))
                !isSelected && isToday -> androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                !isSelected -> androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                else -> null
            }

            Surface(
                modifier = Modifier
                    .width(62.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSelectDate(date) }
                    .testTag("date_chip_${date}"),
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(14.dp),
                border = borderStroke,
                shadowElevation = if (isSelected) 3.dp else 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = dayName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f) else if (hasHoliday) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    // Con số ngày dương lịch + Emoji thời tiết ngay bên phải
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = date.dayOfMonth.toString(),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else if (hasHoliday) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface
                        )
                        if (dateWeather != null) {
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                 text = dateWeather.weatherEmoji,
                                 fontSize = 9.5.sp
                            )
                        }
                    }
                    if (showLunarCalendar) {
                        Spacer(modifier = Modifier.height(2.dp))
                        // Hiển thị ngày Âm lịch bên dưới (ví dụ: 11 hoặc 1/8)
                        Text(
                            text = lunarDate.formatShort(),
                            fontSize = 10.sp,
                            fontWeight = if (lunarDate.isImportantLunarDay || hasHoliday) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color(0xFFFEF08A) else if (hasHoliday) Color(0xFFEF4444) else if (lunarDate.isImportantLunarDay) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TodayProgressCard(
    completed: Int,
    total: Int,
    selectedDate: LocalDate
) {
    val progress = if (total > 0) completed.toFloat() / total.toFloat() else 0f
    val percent = (progress * 100).toInt()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(54.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeWidth = 5.dp
                )
                Text(
                    text = "$percent%",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (selectedDate == LocalDate.now()) "Tiến độ hôm nay" else "Tiến độ ngày ${selectedDate.dayOfMonth}/${selectedDate.monthValue}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (total == 0) "Chưa có sự kiện nào được lên lịch" else "Đã hoàn thành $completed trên tổng số $total sự kiện",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CategoryFilterBar(
    selectedCategory: EventCategory?,
    categories: List<EventCategory>,
    dayEvents: List<Event>,
    onCategorySelect: (EventCategory?) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onCategorySelect(null) },
                label = { Text("Tất cả (${dayEvents.size})", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selectedCategory == null,
                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    selectedBorderColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(20.dp)
            )
        }
        items(categories) { category ->
            val count = dayEvents.count { it.category.id == category.id }
            val isSelected = selectedCategory?.id == category.id
            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelect(if (isSelected) null else category) },
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(category.color, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("${category.title} ($count)", fontSize = 12.sp)
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = category.color,
                    selectedLabelColor = Color.White,
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    selectedBorderColor = category.color
                ),
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@Composable
fun TaskEventItem(
    event: Event,
    onToggleCompletion: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onEdit() },
        shape = RoundedCornerShape(14.dp),
        color = event.category.getThemedBgColor(),
        border = androidx.compose.foundation.BorderStroke(1.dp, event.category.getThemedBorderColor())
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(42.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(event.category.color)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = event.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (event.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = event.category.color
                    ) {
                        Text(
                            text = event.category.title,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = event.category.color,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = DateUtils.formatTimeRange(event.startTime, event.endTime),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    if (event.reminderNote.isNotBlank()) {
                        Spacer(modifier = Modifier.width(10.dp))
                        Icon(
                            Icons.Default.Notes,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onToggleCompletion,
                modifier = Modifier.testTag("check_event_${event.id}")
            ) {
                Icon(
                    if (event.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = "Hoàn thành",
                    tint = if (event.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyTasksCard(
    isToday: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        shadowElevation = 0.5.dp
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.EventAvailable,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = if (isToday) "Hôm nay chưa có sự kiện nào" else "Không có sự kiện trong ngày này",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Lên kế hoạch và theo dõi các mục tiêu hàng ngày của bạn",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun SpecialNoticeCard(selectedDate: LocalDate = LocalDate.now()) {
    val lunarDate = remember(selectedDate) { LunarCalendarHelper.convertSolarToLunar(selectedDate) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(MaterialTheme.colorScheme.surface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Thông tin ngày: ${lunarDate.canChiDay} (Tháng ${lunarDate.canChiMonth})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (lunarDate.holidayName != null) "Dịp lễ: ${lunarDate.holidayName}" else "Năm ${lunarDate.canChiYear} • Chúc bạn một ngày làm việc hiệu quả!",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
