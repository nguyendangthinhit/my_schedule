package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.Event
import com.example.models.EventCategory
import com.example.ui.theme.getThemedBgColor
import com.example.ui.theme.getThemedBorderColor
import com.example.util.DateUtils
import com.example.viewmodel.ScheduleViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventScreen(
    viewModel: ScheduleViewModel,
    eventId: Int? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val selectedDateFromVm by viewModel.selectedDate.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val allEvents by viewModel.events.collectAsState()

    val existingEvent = remember(eventId, allEvents) {
        allEvents.find { it.id == eventId }
    }

    var eventDate by remember(existingEvent, selectedDateFromVm) { mutableStateOf(existingEvent?.date ?: selectedDateFromVm) }

    LaunchedEffect(selectedDateFromVm, existingEvent) {
        if (existingEvent == null) {
            eventDate = selectedDateFromVm
        }
    }
    var title by remember { mutableStateOf(existingEvent?.title ?: "") }
    var note by remember { mutableStateOf(existingEvent?.reminderNote ?: "") }
    var selectedCategory by remember(categories, existingEvent) {
        mutableStateOf(existingEvent?.category ?: categories.firstOrNull() ?: EventCategory.WORK)
    }
    var startHour by remember { mutableIntStateOf(existingEvent?.startTime?.hour ?: 9) }
    var startMinute by remember { mutableIntStateOf(existingEvent?.startTime?.minute ?: 0) }
    var endHour by remember { mutableIntStateOf(existingEvent?.endTime?.hour ?: 10) }
    var endMinute by remember { mutableIntStateOf(existingEvent?.endTime?.minute ?: 30) }
    var hasReminder by remember { mutableStateOf(existingEvent?.hasReminder ?: true) }
    var reminderOffsetMins by remember { mutableIntStateOf(existingEvent?.reminderTimeOffsetMins ?: 15) }
    var titleError by remember { mutableStateOf(false) }

    // 7 days of week repeater
    var selectedDaysOfWeek by remember(eventDate) {
        mutableStateOf(setOf(eventDate.dayOfWeek))
    }

    LaunchedEffect(eventDate) {
        if (!selectedDaysOfWeek.contains(eventDate.dayOfWeek)) {
            selectedDaysOfWeek = selectedDaysOfWeek + eventDate.dayOfWeek
        }
    }

    val currentWeekMonday = remember(eventDate) {
        eventDate.minusDays((eventDate.dayOfWeek.value - 1).toLong())
    }

    val weekDaysList = remember(currentWeekMonday) {
        listOf(
            DayOfWeek.MONDAY to "T.2",
            DayOfWeek.TUESDAY to "T.3",
            DayOfWeek.WEDNESDAY to "T.4",
            DayOfWeek.THURSDAY to "T.5",
            DayOfWeek.FRIDAY to "T.6",
            DayOfWeek.SATURDAY to "T.7",
            DayOfWeek.SUNDAY to "CN"
        )
    }

    val today = remember { LocalDate.now() }

    fun showDatePicker() {
        val datePickerDialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                eventDate = LocalDate.of(year, month + 1, dayOfMonth)
            },
            eventDate.year,
            eventDate.monthValue - 1,
            eventDate.dayOfMonth
        )
        datePickerDialog.show()
    }

    fun showTimePicker(isStartTime: Boolean) {
        val initialHour = if (isStartTime) startHour else endHour
        val initialMinute = if (isStartTime) startMinute else endMinute
        val timePickerDialog = TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                if (isStartTime) {
                    startHour = hourOfDay
                    startMinute = minute
                    val currentStart = LocalTime.of(startHour, startMinute)
                    val currentEnd = LocalTime.of(endHour, endMinute)
                    if (currentEnd.isBefore(currentStart) || currentEnd == currentStart) {
                        val newEnd = currentStart.plusHours(1)
                        endHour = newEnd.hour
                        endMinute = newEnd.minute
                    }
                } else {
                    endHour = hourOfDay
                    endMinute = minute
                }
            },
            initialHour,
            initialMinute,
            true
        )
        timePickerDialog.show()
    }

    fun saveEvent() {
        if (title.isBlank()) {
            titleError = true
            Toast.makeText(context, "Vui lòng nhập tiêu đề sự kiện", Toast.LENGTH_SHORT).show()
            return
        }

        if (eventId != null && existingEvent != null) {
            val startTime = eventDate.atTime(startHour, startMinute)
            var endTime = eventDate.atTime(endHour, endMinute)
            if (endTime.isBefore(startTime) || endTime == startTime) {
                endTime = startTime.plusHours(1)
            }
            val updatedEvent = existingEvent.copy(
                title = title.trim(),
                startTime = startTime,
                endTime = endTime,
                category = selectedCategory,
                reminderNote = note.trim(),
                hasReminder = hasReminder,
                reminderTimeOffsetMins = reminderOffsetMins
            )
            viewModel.updateEvent(updatedEvent, context)
            viewModel.setSelectedDate(eventDate)
            Toast.makeText(context, "Đã cập nhật sự kiện thành công!", Toast.LENGTH_SHORT).show()
        } else {
            val targetDays = if (selectedDaysOfWeek.isEmpty()) setOf(eventDate.dayOfWeek) else selectedDaysOfWeek
            for (day in targetDays.sortedBy { it.value }) {
                val targetDate = currentWeekMonday.plusDays((day.value - 1).toLong())
                val itemStartTime = targetDate.atTime(startHour, startMinute)
                var itemEndTime = targetDate.atTime(endHour, endMinute)
                if (itemEndTime.isBefore(itemStartTime) || itemEndTime == itemStartTime) {
                    itemEndTime = itemStartTime.plusHours(1)
                }
                val newEvent = Event(
                    title = title.trim(),
                    startTime = itemStartTime,
                    endTime = itemEndTime,
                    category = selectedCategory,
                    isCompleted = false,
                    reminderNote = note.trim(),
                    hasReminder = hasReminder,
                    reminderTimeOffsetMins = reminderOffsetMins
                )
                viewModel.addEvent(newEvent, context)
            }
            viewModel.setSelectedDate(eventDate)
            if (targetDays.size > 1) {
                Toast.makeText(context, "Đã tạo ${targetDays.size} sự kiện trong tuần thành công!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Đã lưu sự kiện vào lịch!", Toast.LENGTH_SHORT).show()
            }
        }
        onBack()
    }

    val durationText = remember(startHour, startMinute, endHour, endMinute) {
        val startMins = startHour * 60 + startMinute
        var endMins = endHour * 60 + endMinute
        if (endMins <= startMins) endMins += 24 * 60
        val diffMins = endMins - startMins
        val h = diffMins / 60
        val m = diffMins % 60
        if (h > 0 && m > 0) "${h}h ${m}p" else if (h > 0) "${h} giờ" else "${m} phút"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (eventId != null) "Chỉnh sửa sự kiện" else "Thêm sự kiện mới",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                actions = {
                    Button(
                        onClick = { saveEvent() },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("btn_save_event"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Lưu", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .clickable(
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                ) {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .clickable(
                        indication = null,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    ) {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    }
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
            // 1. Tiêu đề sự kiện
            SectionLabel("Tiêu đề sự kiện *")
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    if (it.isNotBlank()) titleError = false
                },
                placeholder = { Text("Ví dụ: Học tiếng Anh, Họp dự án, Đi tập gym...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                ),
                trailingIcon = {
                    if (title.isNotEmpty()) {
                        IconButton(onClick = { title = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_title"),
                shape = RoundedCornerShape(12.dp),
                isError = titleError,
                supportingText = if (titleError) { { Text("Vui lòng nhập tiêu đề sự kiện", color = MaterialTheme.colorScheme.error) } } else null,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 2. Chọn Ngày diễn ra
            SectionLabel("Ngày diễn ra")
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showDatePicker() }
                    .testTag("btn_select_date"),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = DateUtils.formatFullVietnameseDate(eventDate),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (eventDate == today) "Hôm nay (Chạm để đổi ngày)" else "Chạm để mở lịch chọn ngày",
                                fontSize = 12.sp,
                                color = if (eventDate == today) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(Icons.Default.EditCalendar, contentDescription = "Chọn ngày", tint = MaterialTheme.colorScheme.primary)
                }
            }

            // Quick date shortcut chips
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val shortcuts = listOf(
                    "Hôm nay" to today,
                    "Ngày mai" to today.plusDays(1),
                    "Ngày kia" to today.plusDays(2)
                )
                shortcuts.forEach { (label, date) ->
                    val isSelected = eventDate == date
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { eventDate = date },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.padding(vertical = 8.dp),
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 3. Lặp lại trong tuần (7 ngày trong tuần)
            if (eventId == null) {
                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionLabel("Lặp lại trong tuần")
                    if (selectedDaysOfWeek.size > 1) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "${selectedDaysOfWeek.size} ngày được chọn",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "Chọn các ngày trong tuần để tự động lặp lại sự kiện cùng khung giờ:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    weekDaysList.forEach { (dayOfWeek, label) ->
                        val isSelected = selectedDaysOfWeek.contains(dayOfWeek)
                        val dayDate = currentWeekMonday.plusDays((dayOfWeek.value - 1).toLong())

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    val newSet = if (isSelected) {
                                        if (selectedDaysOfWeek.size > 1) selectedDaysOfWeek - dayOfWeek else selectedDaysOfWeek
                                    } else {
                                        selectedDaysOfWeek + dayOfWeek
                                    }
                                    selectedDaysOfWeek = newSet
                                }
                                .testTag("repeat_day_${dayOfWeek.name}"),
                            shape = RoundedCornerShape(10.dp),
                            color = when {
                                isSelected -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.surface
                            },
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else if (dayOfWeek == DayOfWeek.SUNDAY) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${dayDate.dayOfMonth}",
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 4. Khung giờ
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionLabel("Khung giờ")
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "Thời lượng: $durationText",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TimePickerCard(
                    label = "Giờ bắt đầu",
                    hour = startHour,
                    minute = startMinute,
                    onOpenDialog = { showTimePicker(true) },
                    onHourChange = { startHour = it },
                    onMinuteChange = { startMinute = it },
                    modifier = Modifier.weight(1f)
                )

                TimePickerCard(
                    label = "Giờ kết thúc",
                    hour = endHour,
                    minute = endMinute,
                    onOpenDialog = { showTimePicker(false) },
                    onHourChange = { endHour = it },
                    onMinuteChange = { endMinute = it },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 4. Danh mục sự kiện
            SectionLabel("Danh mục sự kiện")
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(categories.size) { index ->
                    val category = categories[index]
                    val isSelected = selectedCategory.id == category.id
                    Surface(
                        modifier = Modifier
                            .widthIn(min = 90.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedCategory = category }
                            .testTag("cat_option_${category.id}"),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) category.getThemedBgColor() else MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) category.color else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(category.color, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = category.title,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) category.color else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 5. Ghi chú chi tiết
            SectionLabel("Ghi chú chi tiết (Tùy chọn)")
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                placeholder = { Text("Nhập địa điểm, nội dung chuẩn bị hoặc ghi chú...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary
                ),
                maxLines = 4
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 6. Nhắc hẹn thông báo
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Thông báo nhắc nhở", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text(if (hasReminder) "Sẽ báo chuông trước giờ" else "Tắt thông báo", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Switch(
                            checked = hasReminder,
                            onCheckedChange = { hasReminder = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    if (hasReminder) {
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Nhắc trước thời gian diễn ra:", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))

                        // Reminder offset chips
                        val reminderOptions = listOf(
                            5 to "5 phút",
                            10 to "10 phút",
                            15 to "15 phút",
                            30 to "30 phút",
                            60 to "1 giờ",
                            1440 to "1 ngày"
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(reminderOptions.size) { i ->
                                val (mins, label) = reminderOptions[i]
                                val isSelected = reminderOffsetMins == mins
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { reminderOffsetMins = mins },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Text(
                                        text = label,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 7. Nút Lưu chính ở đáy
            Button(
                onClick = { saveEvent() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_save_event_bottom"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (eventId != null) "Cập nhật sự kiện" else "Lưu sự kiện vào lịch",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
}

@Composable
fun TimePickerCard(
    label: String,
    hour: Int,
    minute: Int,
    onOpenDialog: () -> Unit,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(6.dp))

            // Main Time Display - Clickable to open Dialog
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onOpenDialog() },
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = String.format("%02d:%02d", hour, minute),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick adjustment buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onHourChange(if (hour > 0) hour - 1 else 23) }
                ) {
                    Text("-1h", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp))
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onHourChange(if (hour < 23) hour + 1 else 0) }
                ) {
                    Text("+1h", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp))
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onMinuteChange(if (minute == 0) 30 else 0) }
                ) {
                    Text(if (minute == 0) ":30" else ":00", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

