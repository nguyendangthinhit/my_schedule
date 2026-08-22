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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.CategoryColorPreset
import com.example.models.EventCategory
import com.example.ui.theme.PrimaryPurple
import com.example.viewmodel.ScheduleViewModel

@Composable
fun SettingsScreen(viewModel: ScheduleViewModel) {
    val categories by viewModel.categories.collectAsState()
    val events by viewModel.events.collectAsState()

    var notificationsEnabled by remember { mutableStateOf(true) }
    var soundEnabled by remember { mutableStateOf(true) }
    var darkThemeEnabled by remember { mutableStateOf(false) }

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
            .background(Color(0xFFF9FAFC))
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
                    text = "Cài đặt",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
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
                color = Color.White,
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
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "${categories.size} danh mục hiện có",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
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

                    // Collapsible Content (Đổ xuống danh sách khi bấm vào)
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isCategoriesExpanded,
                        enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                        exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
                    ) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Danh sách loại sự kiện:",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF334155)
                                )

                                // Nút Thêm danh mục
                                FilledTonalButton(
                                    onClick = {
                                        newCategoryTitle = ""
                                        selectedColorPreset = EventCategory.COLOR_PRESETS.first()
                                        addCategoryError = false
                                        showAddCategoryDialog = true
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = PrimaryPurple.copy(alpha = 0.12f),
                                        contentColor = PrimaryPurple
                                    ),
                                    modifier = Modifier.testTag("btn_open_add_category")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Thêm", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Text(
                                text = "Khi xóa một danh mục, tất cả các sự kiện thuộc danh mục đó cũng sẽ được xóa khỏi lịch.",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8),
                                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
                            )

                            if (categories.isEmpty()) {
                                Text(
                                    text = "Chưa có danh mục nào. Hãy nhấn nút Thêm ở trên để tạo danh mục mới.",
                                    fontSize = 13.sp,
                                    color = Color(0xFF94A3B8),
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    categories.forEach { category ->
                                        val count = events.count { it.category.id == category.id }
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = category.bgColor.copy(alpha = 0.7f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, category.color.copy(alpha = 0.3f)),
                                            modifier = Modifier.fillMaxWidth().testTag("category_item_${category.id}")
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(14.dp)
                                                            .background(category.color, CircleShape)
                                                    )
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Column {
                                                        Text(
                                                            text = category.title,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 14.sp,
                                                            color = Color(0xFF1E293B)
                                                        )
                                                        Text(
                                                            text = "$count sự kiện liên kết",
                                                            fontSize = 11.sp,
                                                            color = Color(0xFF64748B)
                                                        )
                                                    }
                                                }

                                                // Nút xóa danh mục
                                                IconButton(
                                                    onClick = { categoryToDelete = category },
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .testTag("btn_delete_category_${category.id}")
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
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Thông báo & Nhắc nhở", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    SettingSwitchRow(
                        icon = Icons.Default.Notifications,
                        title = "Bật thông báo",
                        checked = notificationsEnabled,
                        onCheckedChange = { notificationsEnabled = it }
                    )

                    HorizontalDivider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 10.dp))

                    SettingSwitchRow(
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        title = "Âm thanh nhắc nhở",
                        checked = soundEnabled,
                        onCheckedChange = { soundEnabled = it }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Section: Giao diện & Ngôn ngữ
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Giao diện & Ngôn ngữ", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    SettingSwitchRow(
                        icon = Icons.Default.DarkMode,
                        title = "Chế độ tối (Dark Mode)",
                        checked = darkThemeEnabled,
                        onCheckedChange = { darkThemeEnabled = it }
                    )

                    HorizontalDivider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Ngôn ngữ", fontSize = 14.sp, color = Color(0xFF1E293B))
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
                Text("Thêm danh mục sự kiện", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            },
            text = {
                Column {
                    Text(
                        text = "Nhập tên loại sự kiện và chọn màu đại diện:",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
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
                        isError = addCategoryError,
                        supportingText = if (addCategoryError) { { Text("Vui lòng nhập tên danh mục", color = MaterialTheme.colorScheme.error) } } else null,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("input_category_title")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Chọn màu đại diện:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
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
                                        color = if (isSelected) Color.Black else Color.Transparent,
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
                    Text("Xem trước thẻ:", fontSize = 12.sp, color = Color(0xFF64748B))
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
                                text = newCategoryTitle.ifBlank { "Tên danh mục mẫu" },
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
                        } else {
                            viewModel.addCategory(
                                title = newCategoryTitle.trim(),
                                color = selectedColorPreset.color,
                                bgColor = selectedColorPreset.bgColor
                            )
                            showAddCategoryDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                    modifier = Modifier.testTag("btn_confirm_add_category")
                ) {
                    Text("Thêm danh mục")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    // Dialog: Xác nhận Xóa danh mục và xóa luôn tất cả các event thuộc danh mục đó
    categoryToDelete?.let { cat ->
        val associatedEventCount = events.count { it.category.id == cat.id }
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = {
                Text("Xác nhận xóa danh mục", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        text = "Bạn có chắc chắn muốn xóa danh mục \"${cat.title}\"?",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEF2F2),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Lưu ý: Tất cả $associatedEventCount sự kiện thuộc danh mục này sẽ bị xóa khỏi lịch hoàn toàn!",
                                fontSize = 12.sp,
                                color = Color(0xFF991B1B)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCategory(cat.id)
                        categoryToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    modifier = Modifier.testTag("btn_confirm_delete_category")
                ) {
                    Text("Xóa danh mục & sự kiện")
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text("Hủy")
                }
            }
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
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(title, fontSize = 14.sp, color = Color(0xFF1E293B))
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = PrimaryPurple)
        )
    }
}
