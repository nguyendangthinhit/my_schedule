package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.models.EventCategory
import com.example.models.ai.AISuggestedScheduleItem
import com.example.viewmodel.AIChatViewModel
import com.example.viewmodel.AIChatViewModelFactory
import com.example.viewmodel.ChatMessage
import com.example.viewmodel.MessageRole
import com.example.viewmodel.ScheduleViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIScreen(
    scheduleViewModel: ScheduleViewModel? = null,
    aiViewModel: AIChatViewModel = viewModel(factory = AIChatViewModelFactory())
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()

    val messages by aiViewModel.messages.collectAsStateWithLifecycle()
    val isLoading by aiViewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by aiViewModel.errorMessage.collectAsStateWithLifecycle()
    val customApiKey by aiViewModel.customApiKey.collectAsStateWithLifecycle()

    val availableCategories by (scheduleViewModel?.categories?.collectAsStateWithLifecycle() ?: remember {
        mutableStateOf(EventCategory.DEFAULT_CATEGORIES)
    })

    var inputText by remember { mutableStateOf("") }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var tempApiKeyInput by remember { mutableStateOf(customApiKey) }
    var showStudyPlanner by remember { mutableStateOf(false) }

    if (showStudyPlanner) {
        AIStudyPlannerScreen(
            onBack = { showStudyPlanner = false },
            scheduleViewModel = scheduleViewModel
        )
        return
    }

    // Prepare schedule context if scheduleViewModel is available
    val todayEvents = scheduleViewModel?.events?.collectAsStateWithLifecycle()?.value?.filter {
        try {
            it.startTime.toLocalDate() == LocalDate.now()
        } catch (e: Exception) {
            false
        }
    } ?: emptyList()

    val scheduleSummary = remember(todayEvents) {
        if (todayEvents.isEmpty()) {
            "Hôm nay (${LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}) người dùng chưa có sự kiện nào trong lịch."
        } else {
            val list = todayEvents.joinToString("; ") {
                "${it.title} (${it.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))} - ${it.endTime.format(DateTimeFormatter.ofPattern("HH:mm"))}, danh mục: ${it.category.title}, trạng thái: ${if (it.isCompleted) "Đã xong" else "Chưa xong"})"
            }
            "Lịch hôm nay (${LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}): $list"
        }
    }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val effectiveKey = aiViewModel.getEffectiveApiKey()
    val hasApiKey = effectiveKey.isNotBlank()

    val quickPrompts = listOf(
        "⚡ Lập thời gian biểu hôm nay & điền vào lịch",
        "⏱️ Gợi ý cách phân bổ thời gian học tập & làm việc",
        "🎯 Lên lịch 3 việc quan trọng nhất hôm nay",
        "🏃 Đề xuất lịch tập gym & nghỉ ngơi hôm nay",
        "💡 Lập lịch trình ngày mai"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
    ) {
        // Top Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.tertiary.takeIf { it != MaterialTheme.colorScheme.primary } ?: Color(0xFF8B5CF6)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.SmartToy,
                                contentDescription = "AI Assistant",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "Trợ lý AI Lịch Trình",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isLoading) "Đang suy nghĩ & phân bổ lịch..." else "Gợi ý & tự động điền lịch trình",
                                fontSize = 11.sp,
                                color = if (isLoading) MaterialTheme.colorScheme.primary else Color(0xFF10B981)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // AI Study Planner button
                        IconButton(
                            onClick = { showStudyPlanner = true },
                            modifier = Modifier.testTag("btn_open_study_planner")
                        ) {
                            Icon(
                                Icons.Default.School,
                                contentDescription = "AI Study Planner",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // API Key status/config button
                        IconButton(
                            onClick = {
                                tempApiKeyInput = customApiKey
                                showApiKeyDialog = true
                            },
                            modifier = Modifier.testTag("btn_ai_api_key")
                        ) {
                            Icon(
                                Icons.Default.Key,
                                contentDescription = "Cấu hình API Key",
                                tint = if (hasApiKey) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }

                        // Clear chat button
                        IconButton(
                            onClick = { aiViewModel.clearHistory() },
                            modifier = Modifier.testTag("btn_clear_chat")
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Làm mới hội thoại",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
            // Missing API Key warning banner
            if (!hasApiKey) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Chưa có API Key. Bạn có thể nhập mã để thử nghiệm chat ngay.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        TextButton(
                            onClick = {
                                tempApiKeyInput = customApiKey
                                showApiKeyDialog = true
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text("Nhập mã", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Message list
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    ChatMessageItem(
                        message = message,
                        onCopyText = { text ->
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("AI Message", text)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Đã sao chép phản hồi", Toast.LENGTH_SHORT).show()
                        },
                        onRetry = {
                            if (message.isError) {
                                val lastUserMsg = messages.findLast { it.role == MessageRole.USER }
                                if (lastUserMsg != null) {
                                    aiViewModel.sendMessage(lastUserMsg.text, scheduleSummary)
                                }
                            }
                        },
                        onToggleItemSelection = { itemId ->
                            aiViewModel.toggleItemSelection(message.id, itemId)
                        },
                        onToggleSelectAll = { selectAll ->
                            aiViewModel.toggleSelectAll(message.id, selectAll)
                        },
                        onAddSelectedToSchedule = { selectedItems ->
                            if (scheduleViewModel != null && selectedItems.isNotEmpty()) {
                                val eventsToAdd = selectedItems.map { it.toEvent(availableCategories) }
                                scheduleViewModel.addEvents(eventsToAdd, context)
                                aiViewModel.markItemsAsAdded(message.id, selectedItems.map { it.id }.toSet())
                                Toast.makeText(
                                    context,
                                    "🎉 Đã thêm ${selectedItems.size} sự kiện vào lịch trình thành công!",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    )
                }
            }

            // Suggested prompt chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(quickPrompts) { prompt ->
                    SuggestionChip(
                        onClick = {
                            if (prompt.contains("học tập & làm việc", ignoreCase = true) || prompt.contains("phân bổ thời gian", ignoreCase = true)) {
                                showStudyPlanner = true
                            } else if (!isLoading) {
                                aiViewModel.sendMessage(prompt, scheduleSummary)
                            }
                        },
                        label = {
                            Text(
                                text = prompt,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = if (prompt.contains("học tập & làm việc", ignoreCase = true)) {
                                Color(0xFFEFF6FF)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            },
                            labelColor = if (prompt.contains("học tập & làm việc", ignoreCase = true)) {
                                Color(0xFF1D4ED8)
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        ),
                        border = SuggestionChipDefaults.suggestionChipBorder(
                            enabled = true,
                            borderColor = if (prompt.contains("học tập & làm việc", ignoreCase = true)) {
                                Color(0xFF3B82F6)
                            } else {
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            }
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }

            // Bottom Input bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                "Nhập yêu cầu gợi ý hoặc phân bổ lịch trình...",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_ai_chat"),
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (inputText.isNotBlank() && !isLoading) {
                                    val text = inputText
                                    inputText = ""
                                    focusManager.clearFocus()
                                    aiViewModel.sendMessage(text, scheduleSummary)
                                }
                            }
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    val canSend = inputText.isNotBlank() && !isLoading
                    IconButton(
                        onClick = {
                            if (canSend) {
                                val text = inputText
                                inputText = ""
                                focusManager.clearFocus()
                                aiViewModel.sendMessage(text, scheduleSummary)
                            }
                        },
                        enabled = canSend,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                if (canSend) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .testTag("btn_send_ai_chat")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Gửi",
                                tint = if (canSend) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

    // Dialog configuration API Key
    if (showApiKeyDialog) {
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            icon = {
                Icon(
                    Icons.Default.Key,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text("Cấu hình Gemini API Key", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Vui lòng sử dụng Gemini API Key chính thức từ Google AI Studio (bắt đầu bằng 'AIzaSy...').\n\n📌 Bạn có thể lấy khóa miễn phí tại: https://aistudio.google.com/app/apikey",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = tempApiKeyInput,
                        onValueChange = { tempApiKeyInput = it },
                        label = { Text("Gemini API Key") },
                        placeholder = { Text("AIzaSy...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_api_key_dialog"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val key = tempApiKeyInput.trim()
                        aiViewModel.setCustomApiKey(key)
                        com.example.util.AiConfigHelper.saveApiKey(context, key)
                        showApiKeyDialog = false
                        Toast.makeText(context, "Đã lưu Gemini API Key thành công", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Lưu & Áp dụng")
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }
}

@Composable
fun ChatMessageItem(
    message: ChatMessage,
    onCopyText: (String) -> Unit,
    onRetry: () -> Unit,
    onToggleItemSelection: (String) -> Unit,
    onToggleSelectAll: (Boolean) -> Unit,
    onAddSelectedToSchedule: (List<AISuggestedScheduleItem>) -> Unit
) {
    val isUser = message.role == MessageRole.USER

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                Color(0xFF8B5CF6)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = if (message.suggestedItems.isNotEmpty()) 340.dp else 310.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                color = when {
                    isUser -> MaterialTheme.colorScheme.primary
                    message.isError -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                },
                border = if (!isUser && !message.isError) {
                    androidx.compose.foundation.BorderStroke(
                        0.5.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )
                } else null,
                modifier = Modifier.testTag("chat_bubble_${if (isUser) "user" else "ai"}")
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    if (message.isLoading) {
                        TypingIndicatorDots()
                    } else {
                        Text(
                            text = message.text,
                            fontSize = 14.sp,
                            color = when {
                                isUser -> Color.White
                                message.isError -> MaterialTheme.colorScheme.onErrorContainer
                                else -> MaterialTheme.colorScheme.onSurface
                            },
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // Actionable Suggestions Card if AI provided structured schedule items
            if (!isUser && !message.isLoading && message.suggestedItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                AISuggestedScheduleCard(
                    suggestedItems = message.suggestedItems,
                    onToggleItem = onToggleItemSelection,
                    onToggleSelectAll = onToggleSelectAll,
                    onAddSelected = onAddSelectedToSchedule
                )
            }

            // Action buttons for AI message (Copy, Retry)
            if (!isUser && !message.isLoading) {
                Row(
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (message.isError) {
                        TextButton(
                            onClick = onRetry,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Thử lại", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        IconButton(
                            onClick = { onCopyText(message.text) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Sao chép",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "User",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun AISuggestedScheduleCard(
    suggestedItems: List<AISuggestedScheduleItem>,
    onToggleItem: (String) -> Unit,
    onToggleSelectAll: (Boolean) -> Unit,
    onAddSelected: (List<AISuggestedScheduleItem>) -> Unit
) {
    val unaddedItems = suggestedItems.filter { !it.isAdded }
    val selectedUnaddedItems = unaddedItems.filter { it.isSelected }
    val allSelected = unaddedItems.isNotEmpty() && selectedUnaddedItems.size == unaddedItems.size
    val allAdded = suggestedItems.all { it.isAdded }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_ai_suggested_schedule")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Lịch trình đề xuất từ AI",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${suggestedItems.size} mục • Tích chọn để AI tự điền vào lịch",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (!allAdded && unaddedItems.isNotEmpty()) {
                    TextButton(
                        onClick = { onToggleSelectAll(!allSelected) },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = if (allSelected) "Bỏ chọn" else "Chọn hết",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            // Items List
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                suggestedItems.forEach { item ->
                    SuggestedScheduleItemRow(
                        item = item,
                        onToggle = { onToggleItem(item.id) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Button
            if (allAdded) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Tất cả đã được thêm vào lịch trình",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF047857)
                        )
                    }
                }
            } else {
                Button(
                    onClick = { onAddSelected(selectedUnaddedItems) },
                    enabled = selectedUnaddedItems.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .testTag("btn_add_ai_schedule_to_calendar"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Icon(
                        Icons.Default.AddCircleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (selectedUnaddedItems.isNotEmpty()) {
                            "AI thêm vào lịch trình (${selectedUnaddedItems.size} mục)"
                        } else {
                            "Chọn ít nhất 1 mục để thêm"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun SuggestedScheduleItemRow(
    item: AISuggestedScheduleItem,
    onToggle: () -> Unit
) {
    val categoryBadge = when (item.categoryType.uppercase()) {
        "STUDY" -> Triple("Học tập", Color(0xFF10B981), Color(0xFFECFDF5))
        "PLAY", "ENTERTAINMENT", "EXERCISE", "FITNESS" -> Triple("Giải trí/Thể thao", Color(0xFF06B6D4), Color(0xFFECFEFF))
        "MEETING" -> Triple("Cuộc họp", Color(0xFFF59E0B), Color(0xFFFEF3C7))
        else -> Triple("Công việc", Color(0xFF3B82F6), Color(0xFFEFF6FF))
    }

    val isToday = item.date == LocalDate.now()
    val dateLabel = if (isToday) {
        "Hôm nay"
    } else if (item.date == LocalDate.now().plusDays(1)) {
        "Ngày mai"
    } else {
        item.date.format(DateTimeFormatter.ofPattern("dd/MM"))
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (item.isAdded) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        } else if (item.isSelected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
        },
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            if (item.isAdded) Color.Transparent
            else if (item.isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !item.isAdded) { onToggle() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (item.isAdded) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Đã thêm",
                    tint = Color(0xFF10B981),
                    modifier = Modifier
                        .size(24.dp)
                        .padding(2.dp)
                )
            } else {
                Checkbox(
                    checked = item.isSelected,
                    onCheckedChange = { onToggle() },
                    modifier = Modifier.size(28.dp),
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (item.isAdded) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (item.isAdded) TextDecoration.None else null,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = categoryBadge.third
                    ) {
                        Text(
                            text = categoryBadge.first,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = categoryBadge.second,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Time
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${item.startTime.format(DateTimeFormatter.ofPattern("HH:mm"))} - ${item.endTime.format(DateTimeFormatter.ofPattern("HH:mm"))}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Date chip
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = dateLabel,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }

                    if (item.reminderNote.isNotBlank()) {
                        Text(
                            text = "• ${item.reminderNote}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TypingIndicatorDots() {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    val dot1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )
    val dot2 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )
    val dot3 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp)
    ) {
        Text(
            text = "Gemini đang phân tích & lên lịch",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.width(4.dp))
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = dot1))
        )
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = dot2))
        )
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = dot3))
        )
    }
}
