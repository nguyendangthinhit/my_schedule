package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.models.CategoryColorPreset
import com.example.models.EventCategory
import com.example.ui.theme.PrimaryPurple
import com.example.util.LunarCalendarHelper
import com.example.util.NotificationHelper
import com.example.util.ThemeMode
import com.example.viewmodel.ScheduleViewModel

@Composable
fun SettingsScreen(viewModel: ScheduleViewModel) {
    val context = LocalContext.current
    val categories by viewModel.categories.collectAsState()
    val events by viewModel.events.collectAsState()
    val showLunarCalendar by viewModel.showLunarCalendar.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.setNotificationsEnabled(context, true)
            Toast.makeText(context, "Đã bật thông báo sự kiện", Toast.LENGTH_SHORT).show()
        } else {
            viewModel.setNotificationsEnabled(context, false)
            Toast.makeText(context, "Quyền thông báo chưa được cấp", Toast.LENGTH_SHORT).show()
        }
    }

    // Collapsible state for Category Management
    var isCategoriesExpanded by remember { mutableStateOf(false) }

    // Dialog state for adding new category
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryTitle by remember { mutableStateOf("") }
    var selectedColorPreset by remember { mutableStateOf(EventCategory.COLOR_PRESETS.first()) }
    var addCategoryError by remember { mutableStateOf(false) }

    // Dialog state for deleting category
    var categoryToDelete by remember { mutableStateOf<EventCategory?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 60.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = PrimaryPurple)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Cài đặt & Dữ liệu",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Section: Thêm, Xóa danh mục sự kiện (Accordion / Collapsible)
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .testTag("section_categories"),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Clickable Header to Expand / Collapse
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { isCategoriesExpanded = !isCategoriesExpanded }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(PrimaryPurple.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Category,
                                    contentDescription = null,
                                    tint = PrimaryPurple,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Thêm, Xóa danh mục sự kiện",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${categories.size}/5 danh mục đã dùng",
                                    fontSize = 12.sp,
                                    color = if (categories.size >= 5) Color(0xFFEF4444) else Color(0xFF64748B)
                                )
                            }
                        }

                        Icon(
                            imageVector = if (isCategoriesExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isCategoriesExpanded) "Thu gọn" else "Mở rộng",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Content when expanded
                    if (isCategoriesExpanded) {
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))

                        // List of categories with Delete button
                        categories.forEach { category ->
                            val eventsCount = events.count { it.category.id == category.id }
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = category.bgColor,
                                border = androidx.compose.foundation.BorderStroke(1.dp, category.color.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(category.color)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = category.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = Color(0xFF0F172A)
                                            )
                                            Text(
                                                text = "$eventsCount sự kiện",
                                                fontSize = 11.sp,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                    }

                                    // Nút Xóa danh mục
                                    IconButton(
                                        onClick = { categoryToDelete = category },
                                        modifier = Modifier.size(32.dp).testTag("btn_delete_category_${category.id}")
                                    ) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = "Xóa danh mục",
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Nút thêm danh mục mới
                        if (categories.size < 5) {
                            Button(
                                onClick = {
                                    newCategoryTitle = ""
                                    addCategoryError = false
                                    showAddCategoryDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().testTag("btn_open_add_category_dialog")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Thêm danh mục mới (${categories.size}/5)", fontSize = 13.sp)
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFEF2F2),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Đã đạt tối đa 5 danh mục sự kiện.",
                                        fontSize = 12.sp,
                                        color = Color(0xFFDC2626)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Section: Thông báo & Nhắc nhở
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Thông báo & Nhắc nhở", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(12.dp))

                    SettingSwitchRow(
                        icon = Icons.Default.Notifications,
                        title = "Bật thông báo sự kiện",
                        checked = notificationsEnabled,
                        onCheckedChange = { isChecked ->
                            if (isChecked) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    val hasPermission = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    ) == PackageManager.PERMISSION_GRANTED

                                    if (hasPermission) {
                                        viewModel.setNotificationsEnabled(context, true)
                                        Toast.makeText(context, "Đã bật thông báo sự kiện", Toast.LENGTH_SHORT).show()
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                } else {
                                    viewModel.setNotificationsEnabled(context, true)
                                    Toast.makeText(context, "Đã bật thông báo sự kiện", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                viewModel.setNotificationsEnabled(context, false)
                                Toast.makeText(context, "Đã tắt thông báo sự kiện", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 10.dp))

                    SettingSwitchRow(
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        title = "Âm thanh nhắc nhở",
                        checked = soundEnabled,
                        onCheckedChange = { isChecked ->
                            viewModel.setSoundEnabled(context, isChecked)
                            Toast.makeText(
                                context,
                                if (isChecked) "Đã bật âm thanh chuông nhắc nhở" else "Đã tắt âm thanh (chế độ im lặng)",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Nút Thử thông báo & chuông báo ngay
                    OutlinedButton(
                        onClick = {
                            if (!notificationsEnabled) {
                                Toast.makeText(context, "Vui lòng bật thông báo sự kiện trước khi thử nghiệm", Toast.LENGTH_SHORT).show()
                            } else {
                                NotificationHelper.showNotification(
                                    context = context,
                                    id = 7777,
                                    title = "🔔 Nhắc nhở sự kiện lịch trình",
                                    message = "Sự kiện sắp diễn ra! Âm thanh chuông báo và thông báo đang hoạt động rất tốt.",
                                    isTest = true
                                )
                                Toast.makeText(context, "Đã gửi thông báo thử nghiệm lên bảng thông báo!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = PrimaryPurple
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryPurple.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("btn_test_notification")
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Thử nghiệm thông báo & chuông báo ngay", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Section: Giao diện & Ngôn ngữ
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Giao diện & Hiển thị", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(12.dp))

                    SettingSwitchRow(
                        icon = Icons.Default.NightsStay,
                        title = "Hiển thị lịch Âm (Lịch Âm Dương)",
                        checked = showLunarCalendar,
                        onCheckedChange = { viewModel.setShowLunarCalendar(it) }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 12.dp))

                    // Chế độ Sáng / Tối / Theo hệ thống
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when (themeMode) {
                                    ThemeMode.LIGHT -> Icons.Default.LightMode
                                    ThemeMode.DARK -> Icons.Default.DarkMode
                                    ThemeMode.SYSTEM -> Icons.Default.SettingsBrightness
                                },
                                contentDescription = null,
                                tint = PrimaryPurple,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Chế độ giao diện (Sáng / Tối)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = themeMode.subtitle,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 3 thẻ chọn: Sáng / Tối / Theo hệ thống
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ThemeOptionCard(
                                title = "Sáng",
                                icon = Icons.Default.LightMode,
                                isSelected = themeMode == ThemeMode.LIGHT,
                                modifier = Modifier.weight(1f),
                                testTag = "theme_option_light",
                                onClick = {
                                    viewModel.setThemeMode(context, ThemeMode.LIGHT)
                                    Toast.makeText(context, "Đã chuyển sang Chế độ Sáng", Toast.LENGTH_SHORT).show()
                                }
                            )
                            ThemeOptionCard(
                                title = "Tối",
                                icon = Icons.Default.DarkMode,
                                isSelected = themeMode == ThemeMode.DARK,
                                modifier = Modifier.weight(1f),
                                testTag = "theme_option_dark",
                                onClick = {
                                    viewModel.setThemeMode(context, ThemeMode.DARK)
                                    Toast.makeText(context, "Đã chuyển sang Chế độ Tối", Toast.LENGTH_SHORT).show()
                                }
                            )
                            ThemeOptionCard(
                                title = "Hệ thống",
                                icon = Icons.Default.SettingsBrightness,
                                isSelected = themeMode == ThemeMode.SYSTEM,
                                modifier = Modifier.weight(1f),
                                testTag = "theme_option_system",
                                onClick = {
                                    viewModel.setThemeMode(context, ThemeMode.SYSTEM)
                                    Toast.makeText(context, "Đã đặt theo Giao diện hệ thống", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Ngôn ngữ", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Text("Tiếng Việt", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = PrimaryPurple)
                    }
                }
            }
        }
    }

    // Dialog: Thêm danh mục mới
    if (showAddCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = {
                Text("Thêm danh mục sự kiện", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface)
            },
            text = {
                Column {
                    Text(
                        text = "Nhập tên loại sự kiện và chọn màu đại diện:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = newCategoryTitle,
                        onValueChange = {
                            newCategoryTitle = it
                            if (it.isNotBlank()) addCategoryError = false
                        },
                        label = { Text("Tên danh mục *") },
                        placeholder = { Text("Ví dụ: Gia đình, Sức khỏe, Du lịch...") },
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp
                        ),
                        isError = addCategoryError,
                        supportingText = if (addCategoryError) { { Text("Vui lòng nhập tên danh mục", color = MaterialTheme.colorScheme.error) } } else null,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            cursorColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("input_category_title")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Chọn màu đại diện:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(EventCategory.COLOR_PRESETS) { preset ->
                            val isSelected = preset.color == selectedColorPreset.color
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(preset.color)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColorPreset = preset },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Preview
                    Text("Xem trước thẻ:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = selectedColorPreset.bgColor,
                        border = androidx.compose.foundation.BorderStroke(1.dp, selectedColorPreset.color),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(selectedColorPreset.color, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = newCategoryTitle.ifBlank { "Tên danh mục mới" },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = selectedColorPreset.color
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCategoryTitle.isBlank()) {
                            addCategoryError = true
                            return@Button
                        }
                        viewModel.addCategory(
                            title = newCategoryTitle.trim(),
                            color = selectedColorPreset.color,
                            bgColor = selectedColorPreset.bgColor
                        )
                        showAddCategoryDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("btn_confirm_add_category")
                ) {
                    Text("Thêm danh mục")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text("Hủy", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Dialog: Xác nhận Xóa danh mục
    categoryToDelete?.let { cat ->
        val count = events.count { it.category.id == cat.id }
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text("Xác nhận xóa danh mục", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Text(
                    text = "Bạn có chắc muốn xóa danh mục \"${cat.title}\"? " +
                            if (count > 0) "Tất cả $count sự kiện thuộc danh mục này cũng sẽ bị xóa." else "",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCategory(cat.id)
                        categoryToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("btn_confirm_delete_category")
                ) {
                    Text("Xóa", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text("Hủy", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
fun SettingSwitchRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color(0xFF64748B),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = PrimaryPurple,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFCBD5E1)
            )
        )
    }
}

@Composable
fun ThemeOptionCard(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    testTag: String,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) PrimaryPurple else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val bgColor = if (isSelected) PrimaryPurple.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    val iconColor = if (isSelected) PrimaryPurple else MaterialTheme.colorScheme.onSurfaceVariant
    val textColor = if (isSelected) PrimaryPurple else MaterialTheme.colorScheme.onSurface

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor),
        modifier = modifier
            .height(76.dp)
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = textColor
            )
        }
    }
}

