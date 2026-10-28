package com.example.ui.screens

import android.Manifest
import android.content.ComponentName
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
    val showWeather by viewModel.showWeather.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()

    var showEnableGpsDialog by remember { mutableStateOf(false) }
    var showLocationPermissionDialog by remember { mutableStateOf(false) }
    var showApiKeySettingsDialog by remember { mutableStateOf(false) }
    var tempApiKeySettingsInput by remember { mutableStateOf("") }
    var settingsProvider by remember { mutableStateOf(com.example.util.AiConfigHelper.getSelectedProvider(context)) }
    var settingsModel by remember { mutableStateOf(com.example.util.AiConfigHelper.getSelectedModel(context)) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (isGranted) {
            if (!com.example.util.WeatherHelper.isLocationServiceEnabled(context)) {
                showEnableGpsDialog = true
            } else {
                viewModel.setShowWeather(context, true)
                Toast.makeText(context, "Đã bật hiển thị thời tiết", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Cần cấp quyền vị trí để cập nhật thời tiết địa phương", Toast.LENGTH_LONG).show()
            showLocationPermissionDialog = true
        }
    }

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
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)
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
                                    text = "${categories.size}/20 danh mục đã dùng",
                                    fontSize = 12.sp,
                                    color = if (categories.size >= 20) Color(0xFFEF4444) else Color(0xFF64748B)
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

                        // List of categories with Delete button or Locked badge
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
                                                .border(1.dp, Color(0xFFCBD5E1), CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = category.title,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = Color(0xFF0F172A)
                                                )
                                                if (category.isLocked) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Icon(
                                                        Icons.Default.Lock,
                                                        contentDescription = "Đã khóa",
                                                        tint = Color(0xFFEF4444),
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = if (category.isLocked) "Danh mục hệ thống (Tự động)" else "$eventsCount sự kiện",
                                                fontSize = 11.sp,
                                                color = if (category.isLocked) Color(0xFFEF4444) else Color(0xFF64748B)
                                            )
                                        }
                                    }

                                    if (category.isLocked) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFFEF2F2),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.Lock,
                                                    contentDescription = null,
                                                    tint = Color(0xFFEF4444),
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Đã khóa",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFEF4444)
                                                )
                                            }
                                        }
                                    } else {
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
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Nút thêm danh mục mới
                        if (categories.size < 20) {
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
                                Text("Thêm danh mục mới (${categories.size}/20)", fontSize = 13.sp)
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
                                        text = "Đã đạt tối đa 20 danh mục sự kiện.",
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
                        subtitle = "Xem ngày âm lịch trên các chế độ xem lịch",
                        checked = showLunarCalendar,
                        onCheckedChange = { viewModel.setShowLunarCalendar(it) }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 10.dp))

                    SettingSwitchRow(
                        icon = Icons.Default.WbSunny,
                        title = "Hiển thị thông tin thời tiết",
                        subtitle = "Dự báo thời tiết & nhiệt độ theo vị trí thiết bị",
                        checked = showWeather,
                        onCheckedChange = { isChecked ->
                            if (isChecked) {
                                if (!com.example.util.WeatherHelper.hasLocationPermission(context)) {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                } else if (!com.example.util.WeatherHelper.isLocationServiceEnabled(context)) {
                                    showEnableGpsDialog = true
                                } else {
                                    viewModel.setShowWeather(context, true)
                                    Toast.makeText(context, "Đã bật hiển thị thời tiết", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                viewModel.setShowWeather(context, false)
                                Toast.makeText(context, "Đã tắt hiển thị thời tiết", Toast.LENGTH_SHORT).show()
                            }
                        }
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
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Section: Tiện ích Màn hình chính (Widget)
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .testTag("section_widgets"),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(PrimaryPurple.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Widgets,
                                contentDescription = null,
                                tint = PrimaryPurple,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Tiện ích Màn hình chính (Widget)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Ghim Widget ra Home Screen Android để tương tác nhanh",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Chọn mẫu Widget muốn ghim ra Màn hình chính:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val pinWidgetAction = { providerClass: Class<*> ->
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context)
                                val myProvider = ComponentName(context, providerClass)
                                if (appWidgetManager.isRequestPinAppWidgetSupported) {
                                    appWidgetManager.requestPinAppWidget(myProvider, null, null)
                                    Toast.makeText(context, "Đã mở hộp thoại ghim Widget ra Màn hình chính", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Thiết bị không hỗ trợ ghim tự động. Hãy nhấn giữ Màn hình chính -> Widgets.", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                Toast.makeText(context, "Hãy nhấn giữ Màn hình chính Android -> Widgets -> Chọn ứng dụng.", Toast.LENGTH_LONG).show()
                            }
                        }

                        OutlinedButton(
                            onClick = { pinWidgetAction(com.example.widget.SmallScheduleWidgetProvider::class.java) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text("📌 Vuông (2x2)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(
                            onClick = { pinWidgetAction(com.example.widget.CompactScheduleWidgetProvider::class.java) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text("📌 Ngang (4x2)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(
                            onClick = { pinWidgetAction(com.example.widget.MicroScheduleWidgetProvider::class.java) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text("📌 Gọn (2x1)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Section: Cấu hình Trợ lý AI (Multi-Key Rotation & Groq LPU)
        item {
            val currentProvider = remember(showApiKeySettingsDialog) {
                com.example.util.AiConfigHelper.getSelectedProvider(context)
            }
            val currentModel = remember(showApiKeySettingsDialog) {
                com.example.util.AiConfigHelper.getSelectedModel(context)
            }
            val geminiKeysCount = remember(showApiKeySettingsDialog) {
                com.example.util.AiConfigHelper.getAllConfiguredKeys(context).size
            }
            val groqKeysCount = remember(showApiKeySettingsDialog) {
                com.example.util.AiConfigHelper.getAllConfiguredGroqKeys(context).size
            }
            val currentModelName = remember(currentModel) {
                com.example.util.AiConfigHelper.AVAILABLE_MODELS.find { it.id == currentModel }?.displayName ?: currentModel
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .testTag("section_ai_keys"),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (currentProvider == com.example.util.AiProvider.GROQ) Color(0xFF10B981).copy(alpha = 0.15f)
                                        else PrimaryPurple.copy(alpha = 0.12f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = currentProvider.iconBadge,
                                    fontSize = 18.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Cấu hình AI & API Keys (Gemini & Groq)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Hỗ trợ Groq LPU (chống cao tải) & Google Gemini",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (currentProvider == com.example.util.AiProvider.GROQ) Color(0xFFECFDF5) else Color(0xFFEFF6FF)
                        ) {
                            Text(
                                text = if (currentProvider == com.example.util.AiProvider.GROQ) "⚡ Groq Active" else "🌐 Gemini Active",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (currentProvider == com.example.util.AiProvider.GROQ) Color(0xFF047857) else Color(0xFF1D4ED8),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Model & Key Status Summary
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Mô hình đang chạy:", fontSize = 12.sp, color = Color(0xFF64748B))
                            Text(
                                text = currentModelName,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Khóa API đã lưu:", fontSize = 12.sp, color = Color(0xFF64748B))
                            Text(
                                text = "Gemini: $geminiKeysCount key  •  Groq: $groqKeysCount key",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            settingsProvider = com.example.util.AiConfigHelper.getSelectedProvider(context)
                            settingsModel = com.example.util.AiConfigHelper.getSelectedModel(context)
                            tempApiKeySettingsInput = if (settingsProvider == com.example.util.AiProvider.GROQ) {
                                com.example.util.AiConfigHelper.getRawSavedGroqKeys(context)
                            } else {
                                com.example.util.AiConfigHelper.getRawSavedKeys(context)
                            }
                            showApiKeySettingsDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("btn_configure_api_keys")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cài đặt Mô hình & Nhập API Key (Gemini / Groq)", fontSize = 12.5.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Chọn màu đại diện:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            text = selectedColorPreset.name,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = selectedColorPreset.contrastTextColor
                        )
                    }
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
                                        color = when {
                                            isSelected -> MaterialTheme.colorScheme.onSurface
                                            preset.isLight -> Color(0xFFCBD5E1)
                                            else -> Color(0xFFE2E8F0)
                                        },
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColorPreset = preset },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = if (preset.isLight) Color(0xFF1E293B) else Color.White,
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
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selectedColorPreset.isLight) Color(0xFFCBD5E1) else selectedColorPreset.color
                        ),
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
                                    .border(1.dp, Color(0xFFCBD5E1), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = newCategoryTitle.ifBlank { "Tên danh mục mới" },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = selectedColorPreset.contrastTextColor
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
        if (cat.isLocked) {
            categoryToDelete = null
            return@let
        }
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

    // Dialog: Yêu cầu bật dịch vụ định vị (GPS) trên thiết bị
    if (showEnableGpsDialog) {
        AlertDialog(
            onDismissRequest = {
                viewModel.setShowWeather(context, true)
                showEnableGpsDialog = false
            },
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF59E0B).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.LocationOff,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(26.dp)
                    )
                }
            },
            title = {
                Text("Yêu cầu bật định vị (GPS)", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface)
            },
            text = {
                Text(
                    "Thiết bị của bạn đang tắt dịch vụ định vị (GPS). Vui lòng bật định vị trong Cài đặt của thiết bị để ứng dụng cập nhật dự báo thời tiết chính xác cho khu vực của bạn.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEnableGpsDialog = false
                        viewModel.setShowWeather(context, true)
                        com.example.util.WeatherHelper.openLocationSettings(context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Mở Cài đặt vị trí")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showEnableGpsDialog = false
                        viewModel.setShowWeather(context, true)
                    }
                ) {
                    Text("Để sau", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Dialog: Yêu cầu cấp quyền vị trí
    if (showLocationPermissionDialog) {
        AlertDialog(
            onDismissRequest = {
                showLocationPermissionDialog = false
            },
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(PrimaryPurple.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Place,
                        contentDescription = null,
                        tint = PrimaryPurple,
                        modifier = Modifier.size(26.dp)
                    )
                }
            },
            title = {
                Text("Cấp quyền truy cập vị trí", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface)
            },
            text = {
                Text(
                    "Để hiển thị thông tin thời tiết địa phương và nhiệt độ theo giờ chính xác, ứng dụng cần quyền truy cập vị trí của thiết bị.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLocationPermissionDialog = false
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Cấp quyền")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showLocationPermissionDialog = false
                    }
                ) {
                    Text("Để sau", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Dialog: Cấu hình AI & Quản lý API Keys (Gemini & Groq Cloud)
    if (showApiKeySettingsDialog) {
        val configuredKeyInfos = remember(showApiKeySettingsDialog, settingsProvider) {
            com.example.util.AiConfigHelper.getApiKeyInfoList(context, settingsProvider)
        }

        AlertDialog(
            onDismissRequest = { showApiKeySettingsDialog = false },
            icon = {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            if (settingsProvider == com.example.util.AiProvider.GROQ) Color(0xFF10B981).copy(alpha = 0.15f)
                            else PrimaryPurple.copy(alpha = 0.12f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = settingsProvider.iconBadge,
                        fontSize = 22.sp
                    )
                }
            },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Chọn mô hình & Quản lý API Key",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.5.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Hỗ trợ Groq LPU (Chống cao tải) & Google Gemini",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 450.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Provider Tabs
                    item {
                        Text(
                            text = "1. Nền tảng AI (Provider):",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            com.example.util.AiProvider.values().forEach { prov ->
                                val isSelected = settingsProvider == prov
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            settingsProvider = prov
                                            settingsModel = if (prov == com.example.util.AiProvider.GROQ) {
                                                com.example.network.GroqApiService.MODEL_LLAMA_3_3_70B
                                            } else {
                                                com.example.network.GeminiApiService.MODEL_GEMINI_3_5_FLASH_LITE
                                            }
                                            tempApiKeySettingsInput = if (prov == com.example.util.AiProvider.GROQ) {
                                                com.example.util.AiConfigHelper.getRawSavedGroqKeys(context)
                                            } else {
                                                com.example.util.AiConfigHelper.getRawSavedKeys(context)
                                            }
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) {
                                        if (prov == com.example.util.AiProvider.GROQ) Color(0xFFECFDF5) else Color(0xFFEFF6FF)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                    },
                                    border = androidx.compose.foundation.BorderStroke(
                                        if (isSelected) 1.5.dp else 0.5.dp,
                                        if (isSelected) {
                                            if (prov == com.example.util.AiProvider.GROQ) Color(0xFF10B981) else Color(0xFF3B82F6)
                                        } else {
                                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                        }
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "${prov.iconBadge} ${if (prov == com.example.util.AiProvider.GROQ) "Groq Cloud" else "Google Gemini"}",
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) {
                                                if (prov == com.example.util.AiProvider.GROQ) Color(0xFF047857) else Color(0xFF1D4ED8)
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (prov == com.example.util.AiProvider.GROQ) "Siêu tốc, chống nghẽn" else "Đa năng, thông minh",
                                            fontSize = 9.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 2. Model Selection
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "2. Chọn mô hình (${if (settingsProvider == com.example.util.AiProvider.GROQ) "Groq" else "Gemini"}):",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        val modelsForProvider = com.example.util.AiConfigHelper.AVAILABLE_MODELS.filter { it.provider == settingsProvider }
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            modelsForProvider.forEach { modelInfo ->
                                val isSelected = settingsModel == modelInfo.id
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { settingsModel = modelInfo.id },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                    },
                                    border = androidx.compose.foundation.BorderStroke(
                                        if (isSelected) 1.2.dp else 0.5.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { settingsModel = modelInfo.id },
                                            modifier = Modifier.size(20.dp),
                                            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = modelInfo.displayName,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                if (modelInfo.isRecommended) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = Color(0xFF10B981).copy(alpha = 0.15f)
                                                    ) {
                                                        Text(
                                                            text = "Khuyên dùng",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF047857),
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = modelInfo.description,
                                                fontSize = 10.5.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                lineHeight = 14.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. API Key Status & Input
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "3. Khóa API (${if (settingsProvider == com.example.util.AiProvider.GROQ) "Groq Key" else "Gemini Key"}):",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        if (configuredKeyInfos.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                configuredKeyInfos.forEachIndexed { idx, info ->
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (info.isRateLimited) Color(0xFFFEF2F2) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Key #${idx + 1}: ${info.maskedKey}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = if (info.isRateLimited) "🔴 Rate Limit" else "🟢 Sẵn sàng",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (info.isRateLimited) Color(0xFFDC2626) else Color(0xFF059669)
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        OutlinedTextField(
                            value = tempApiKeySettingsInput,
                            onValueChange = { tempApiKeySettingsInput = it },
                            label = {
                                Text(if (settingsProvider == com.example.util.AiProvider.GROQ) "Nhập Groq API Key (gsk_...)" else "Nhập Gemini API Key (AIzaSy...)")
                            },
                            placeholder = {
                                Text(if (settingsProvider == com.example.util.AiProvider.GROQ) "gsk_abcd1234...\n(Hỗ trợ nhiều key mỗi dòng 1 key)" else "AIzaSy...\n(Hỗ trợ nhiều key)")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 85.dp)
                                .testTag("input_api_keys_settings"),
                            singleLine = false,
                            maxLines = 4,
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Thẻ hiển thị Model Info & Cơ chế xử lý sự cố chi tiết
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Chi tiết Model & Cơ chế tự động khi có sự cố:",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                // Gemini Card
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFEFF6FF),
                                    border = androidx.compose.foundation.BorderStroke(
                                        if (settingsProvider == com.example.util.AiProvider.GEMINI) 1.2.dp else 0.5.dp,
                                        if (settingsProvider == com.example.util.AiProvider.GEMINI) Color(0xFF3B82F6) else Color(0xFFBFDBFE)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "🌐 Google Gemini",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1D4ED8)
                                            )
                                            if (settingsProvider == com.example.util.AiProvider.GEMINI) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = Color(0xFF3B82F6).copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        text = "Đang chọn",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF1D4ED8),
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = "• Mặc định: gemini-3.5-flash-lite (Gemini 3.5 Flash Lite)",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF1E3A8A)
                                        )
                                        Text(
                                            text = "• Khi có sự cố (404 chưa mở model): Tự động chuyển tiếp sang gemini-3.1-flash-lite-preview",
                                            fontSize = 10.sp,
                                            color = Color(0xFF1E40AF),
                                            lineHeight = 13.sp
                                        )
                                        Text(
                                            text = "• Khi có sự cố (503 cao tải / 429 quá lượt): Tự động chuyển tiếp tức thì sang Groq Llama 3.3 70B (nếu có Groq Key) hoặc đổi sang Gemini 3.1 Flash Lite",
                                            fontSize = 10.sp,
                                            color = Color(0xFF1E40AF),
                                            lineHeight = 13.sp
                                        )
                                        Text(
                                            text = "👉 Nhận API Key tại: aistudio.google.com",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1D4ED8)
                                        )
                                    }
                                }

                                // Groq Card
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFF0FDF4),
                                    border = androidx.compose.foundation.BorderStroke(
                                        if (settingsProvider == com.example.util.AiProvider.GROQ) 1.2.dp else 0.5.dp,
                                        if (settingsProvider == com.example.util.AiProvider.GROQ) Color(0xFF10B981) else Color(0xFF86EFAC)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "⚡ Groq LPU (Grod)",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF047857)
                                            )
                                            if (settingsProvider == com.example.util.AiProvider.GROQ) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = Color(0xFF10B981).copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        text = "Đang chọn",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF047857),
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = "• Mặc định: Groq Llama 3.3 70B Versatile (llama-3.3-70b-versatile)",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF065F46)
                                        )
                                        Text(
                                            text = "• Khi có sự cố (Rate limit / Quá lượt): Tự động xoay vòng danh sách Key dự phòng (Key Rotation) hoặc đổi sang Llama 3.1 8B Instant",
                                            fontSize = 10.sp,
                                            color = Color(0xFF166534),
                                            lineHeight = 13.sp
                                        )
                                        Text(
                                            text = "• Ưu điểm: Tốc độ LPU 300-800 tok/s, miễn phí, không bao giờ bị nghẽn 'Server Overloaded'",
                                            fontSize = 10.sp,
                                            color = Color(0xFF166534),
                                            lineHeight = 13.sp
                                        )
                                        Text(
                                            text = "👉 Nhận Groq Key miễn phí tại: console.groq.com",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF15803D)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val input = tempApiKeySettingsInput.trim()
                        if (input.isNotBlank()) {
                            if (settingsProvider == com.example.util.AiProvider.GROQ) {
                                com.example.util.AiConfigHelper.saveRawGroqKeysInput(context, input)
                            } else {
                                com.example.util.AiConfigHelper.saveRawKeysInput(context, input)
                            }
                        }
                        com.example.util.AiConfigHelper.setSelectedProvider(context, settingsProvider)
                        com.example.util.AiConfigHelper.setSelectedModel(context, settingsModel)
                        showApiKeySettingsDialog = false

                        val providerName = if (settingsProvider == com.example.util.AiProvider.GROQ) "Groq Cloud" else "Google Gemini"
                        val modelName = com.example.util.AiConfigHelper.AVAILABLE_MODELS.find { it.id == settingsModel }?.displayName ?: settingsModel
                        Toast.makeText(
                            context,
                            "Đã áp dụng $providerName ($modelName)!",
                            Toast.LENGTH_LONG
                        ).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Lưu & Áp dụng")
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeySettingsDialog = false }) {
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
    subtitle: String? = null,
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f).padding(end = 8.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color(0xFF64748B),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
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

