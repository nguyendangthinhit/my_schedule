package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.example.util.DateUtils
import com.example.viewmodel.ScheduleViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    viewModel: ScheduleViewModel,
    onNavigateToAddEvent: () -> Unit = {}
) {
    val allEvents by viewModel.events.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val categories by viewModel.categories.collectAsState()
    var selectedCategoryFilter by remember { mutableStateOf<EventCategory?>(null) }

    val today = LocalDate.now()
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFC))
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
    ) {
        // Top full date and greeting
        item {
            HeaderDateSection(selectedDate = selectedDate)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Horizontal 7-day mini calendar picker
        item {
            WeekDateCarousel(
                selectedDate = selectedDate,
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
                    color = Color(0xFF1E293B)
                )
                if (selectedCategoryFilter != null) {
                    TextButton(onClick = { selectedCategoryFilter = null }) {
                        Text("Hiện tất cả", color = PrimaryPurple, fontSize = 13.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // List of Events with Checkboxes
        if (filteredEvents.isEmpty()) {
            item {
                EmptyTasksCard(
                    isToday = selectedDate == today,
                    onAddTask = onNavigateToAddEvent
                )
            }
        } else {
            items(filteredEvents, key = { it.id }) { event ->
                TaskEventItem(
                    event = event,
                    onToggleCompletion = { viewModel.toggleEventCompletion(event.id) },
                    onDelete = { viewModel.deleteEvent(event.id) }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        // Special Events / Notes Card
        item {
            Spacer(modifier = Modifier.height(16.dp))
            SpecialNoticeCard()
        }
    }
}

@Composable
fun HeaderDateSection(selectedDate: LocalDate) {
    val today = LocalDate.now()
    val isToday = selectedDate == today

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Việc trong ngày",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(4.dp))
                // Hiển thị thứ, ngày, tháng, năm hiện tại ở trên cùng
                Text(
                    text = DateUtils.formatFullVietnameseDate(selectedDate),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = PrimaryPurple
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = Color(0xFF64748B)
                    )
                }
            }
        }
    }
}

@Composable
fun WeekDateCarousel(
    selectedDate: LocalDate,
    onSelectDate: (LocalDate) -> Unit
) {
    val today = LocalDate.now()
    // Show 14 days centered around selected date / today
    val startDay = remember(today) { today.minusDays(3) }
    val daysList = remember(startDay) { (0..13).map { startDay.plusDays(it.toLong()) } }

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(daysList) { date ->
            val isSelected = date == selectedDate
            val isToday = date == today
            val dayName = DateUtils.getVietnameseDayOfWeek(date.dayOfWeek, short = true)

            Surface(
                modifier = Modifier
                    .width(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSelectDate(date) }
                    .testTag("date_chip_${date}"),
                color = if (isSelected) PrimaryPurple else Color.White,
                shape = RoundedCornerShape(14.dp),
                border = if (!isSelected && isToday) androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryPurple.copy(alpha = 0.5f)) else null,
                shadowElevation = if (isSelected) 3.dp else 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = dayName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isSelected) Color.White.copy(alpha = 0.85f) else Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = date.dayOfMonth.toString(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else Color(0xFF0F172A)
                    )
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
        color = Color.White,
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
                    color = PrimaryPurple,
                    trackColor = Color(0xFFE2E8F0),
                    strokeWidth = 5.dp
                )
                Text(
                    text = "$percent%",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryPurple
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (selectedDate == LocalDate.now()) "Tiến độ hôm nay" else "Tiến độ ngày ${selectedDate.dayOfMonth}/${selectedDate.monthValue}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Đã hoàn thành $completed trên $total công việc",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B)
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
    val totalCount = dayEvents.size
    val isAllSelected = selectedCategory == null

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // "Tất cả" Card
        item {
            CategoryPill(
                title = "Tất cả",
                count = totalCount,
                color = PrimaryPurple,
                bgColor = PrimaryPurple.copy(alpha = 0.12f),
                isSelected = isAllSelected,
                onClick = { onCategorySelect(null) },
                modifier = Modifier.widthIn(min = 72.dp)
            )
        }

        items(categories, key = { it.id }) { category ->
            val count = dayEvents.count { it.category.id == category.id }
            CategoryPill(
                title = category.title,
                count = count,
                color = category.color,
                bgColor = category.bgColor,
                isSelected = selectedCategory?.id == category.id,
                onClick = {
                    onCategorySelect(if (selectedCategory?.id == category.id) null else category)
                },
                modifier = Modifier.widthIn(min = 72.dp)
            )
        }
    }
}

@Composable
fun CategoryPill(
    title: String,
    count: Int,
    color: Color,
    bgColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = if (isSelected) color else bgColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (isSelected) Color.White else Color(0xFF334155),
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = count.toString(),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else color
            )
        }
    }
}

@Composable
fun TaskEventItem(
    event: Event,
    onToggleCompletion: () -> Unit,
    onDelete: () -> Unit
) {
    val timeText = DateUtils.formatTimeRange(event.startTime, event.endTime)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .testTag("task_item_${event.id}"),
        shape = RoundedCornerShape(14.dp),
        color = event.category.bgColor,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (event.isCompleted) Color(0xFFCBD5E1) else event.category.color.copy(alpha = 0.35f)
        ),
        shadowElevation = 0.5.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 0.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left color bar
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(56.dp)
                    .clip(RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp))
                    .background(if (event.isCompleted) Color(0xFF94A3B8) else event.category.color)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Checkbox tick đã hoàn thành hay chưa
            Checkbox(
                checked = event.isCompleted,
                onCheckedChange = { onToggleCompletion() },
                colors = CheckboxDefaults.colors(
                    checkedColor = event.category.color,
                    uncheckedColor = Color(0xFF64748B),
                    checkmarkColor = Color.White
                ),
                modifier = Modifier.testTag("checkbox_${event.id}")
            )

            Spacer(modifier = Modifier.width(6.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (event.isCompleted) Color(0xFF64748B) else Color(0xFF0F172A),
                    textDecoration = if (event.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Time range
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = timeText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF475569)
                        )
                    }

                    // Category tag
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.White.copy(alpha = 0.9f)
                    ) {
                        Text(
                            text = event.category.title,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = event.category.color,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (event.reminderNote.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "📝 ${event.reminderNote}",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B),
                        maxLines = 1
                    )
                }
            }

            // Status chip or delete icon
            if (event.isCompleted) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFE2E8F0)
                ) {
                    Text(
                        text = "Xong",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF475569),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            } else {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyTasksCard(isToday: Boolean, onAddTask: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.TaskAlt,
                contentDescription = null,
                tint = PrimaryPurple.copy(alpha = 0.5f),
                modifier = Modifier.size(54.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (isToday) "Chưa có việc nào trong hôm nay" else "Không có lịch hoạt động trong ngày này",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF64748B)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAddTask,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Thêm việc mới")
            }
        }
    }
}

@Composable
fun SpecialNoticeCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF1F5F9)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(PrimaryPurple.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = PrimaryPurple,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "Mẹo quản lý thời gian",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Hoàn thành các công việc ưu tiên vào buổi sáng để tối ưu hiệu suất làm việc!",
                    fontSize = 12.sp,
                    color = Color(0xFF475569)
                )
            }
        }
    }
}
