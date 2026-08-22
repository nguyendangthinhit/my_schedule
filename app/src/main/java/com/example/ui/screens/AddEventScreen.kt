package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.Event
import com.example.models.EventCategory
import com.example.ui.theme.PrimaryPurple
import com.example.util.DateUtils
import com.example.viewmodel.ScheduleViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventScreen(
    viewModel: ScheduleViewModel,
    onBack: () -> Unit
) {
    val selectedDateFromVm by viewModel.selectedDate.collectAsState()
    val categories by viewModel.categories.collectAsState()
    var eventDate by remember { mutableStateOf(selectedDateFromVm) }
    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedCategory by remember(categories) {
        mutableStateOf(categories.firstOrNull() ?: EventCategory.WORK)
    }
    var startHour by remember { mutableIntStateOf(9) }
    var startMinute by remember { mutableIntStateOf(0) }
    var endHour by remember { mutableIntStateOf(10) }
    var endMinute by remember { mutableIntStateOf(30) }
    var hasReminder by remember { mutableStateOf(true) }
    var reminderOffsetMins by remember { mutableIntStateOf(15) }
    var titleError by remember { mutableStateOf(false) }

    fun saveEvent() {
        if (title.isBlank()) {
            titleError = true
            return
        }
        val startTime = eventDate.atTime(startHour, startMinute)
        val endTime = eventDate.atTime(endHour, endMinute)
        val finalEndTime = if (endTime.isBefore(startTime) || endTime == startTime) {
            startTime.plusHours(1)
        } else {
            endTime
        }

        val newEvent = Event(
            title = title.trim(),
            startTime = startTime,
            endTime = finalEndTime,
            category = selectedCategory,
            isCompleted = false,
            reminderNote = note.trim(),
            hasReminder = hasReminder,
            reminderTimeOffsetMins = reminderOffsetMins
        )
        viewModel.addEvent(newEvent)
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Thêm sự kiện",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { saveEvent() },
                        modifier = Modifier.testTag("btn_save_event")
                    ) {
                        Text("Lưu", color = PrimaryPurple, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF9FAFC)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Tiêu đề sự kiện
            SectionLabel("Tiêu đề sự kiện *")
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    if (it.isNotBlank()) titleError = false
                },
                placeholder = { Text("Ví dụ: Họp dự án, Học tiếng Anh...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .testTag("input_title"),
                shape = RoundedCornerShape(12.dp),
                isError = titleError,
                supportingText = if (titleError) { { Text("Vui lòng nhập tiêu đề sự kiện", color = MaterialTheme.colorScheme.error) } } else null,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    focusedBorderColor = PrimaryPurple
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Chọn Ngày
            SectionLabel("Ngày diễn ra")
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = PrimaryPurple)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = DateUtils.formatFullVietnameseDate(eventDate),
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Chọn Giờ bắt đầu & Giờ kết thúc
            SectionLabel("Khung giờ (Giờ bắt đầu - Giờ kết thúc)")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Start time
                TimeSelectorBox(
                    label = "Bắt đầu",
                    hour = startHour,
                    minute = startMinute,
                    onHourChange = { startHour = it },
                    onMinuteChange = { startMinute = it },
                    modifier = Modifier.weight(1f)
                )

                // End time
                TimeSelectorBox(
                    label = "Kết thúc",
                    hour = endHour,
                    minute = endMinute,
                    onHourChange = { endHour = it },
                    onMinuteChange = { endMinute = it },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Danh mục
            SectionLabel("Danh mục sự kiện")
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories.size) { index ->
                    val category = categories[index]
                    val isSelected = selectedCategory.id == category.id
                    Surface(
                        modifier = Modifier
                            .widthIn(min = 80.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedCategory = category },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) category.bgColor else Color.White,
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) category.color else Color(0xFFE2E8F0)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .background(category.color, CircleShape)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = category.title,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) category.color else Color(0xFF475569),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Nội dung nhắc hẹn / Ghi chú
            SectionLabel("Ghi chú chi tiết")
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                placeholder = { Text("Nhập nội dung ghi chú...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(Color.White, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    focusedBorderColor = PrimaryPurple
                ),
                maxLines = 4
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Reminder options
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = PrimaryPurple)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Bật thông báo nhắc hẹn", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        }
                        Switch(
                            checked = hasReminder,
                            onCheckedChange = { hasReminder = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = PrimaryPurple)
                        )
                    }

                    if (hasReminder) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Thời gian nhắc trước", fontSize = 14.sp, color = Color(0xFF64748B))
                            Text("$reminderOffsetMins phút trước", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = PrimaryPurple)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Bottom Submit Button
            Button(
                onClick = { saveEvent() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Lưu sự kiện vào lịch", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun TimeSelectorBox(
    label: String,
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, fontSize = 12.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = PrimaryPurple, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = String.format("%02d:%02d", hour, minute),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Quick +/- hour buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onHourChange(if (hour > 5) hour - 1 else 23) }
                ) {
                    Text("-1h", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onHourChange(if (hour < 23) hour + 1 else 5) }
                ) {
                    Text("+1h", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onMinuteChange(if (minute == 0) 30 else 0) }
                ) {
                    Text(if (minute == 0) ":30" else ":00", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                }
            }
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF1E293B),
        modifier = Modifier.padding(bottom = 6.dp)
    )
}
