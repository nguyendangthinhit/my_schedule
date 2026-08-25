package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.Event
import com.example.models.EventCategory
import com.example.models.dashboard.AIHabitPatterns
import com.example.models.dashboard.AISuggestedGoal
import com.example.models.dashboard.AIWeeklyReviewData
import com.example.viewmodel.AIWeeklyReviewUiState
import com.example.viewmodel.AIWeeklyReviewViewModel
import com.example.viewmodel.ScheduleViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIWeeklyReviewScreen(
    reviewViewModel: AIWeeklyReviewViewModel,
    scheduleViewModel: ScheduleViewModel,
    onBack: () -> Unit
) {
    val uiState by reviewViewModel.uiState.collectAsState()
    val isRefreshing by reviewViewModel.isRefreshing.collectAsState()
    val interactionMessages by reviewViewModel.interactionMessages.collectAsState()
    val isAskingAI by reviewViewModel.isAskingAI.collectAsState()
    val addedGoalIds by reviewViewModel.addedGoalIds.collectAsState()

    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var inputPrompt by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "AI phân tích tuần này",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { reviewViewModel.loadWeeklyReview(forceAiRefresh = true) }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Làm mới phân tích",
                            tint = Color(0xFF6366F1)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        when (val state = uiState) {
            is AIWeeklyReviewUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = Color(0xFF6366F1),
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "AI đang đọc và phân tích lịch trình tuần của bạn...",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            is AIWeeklyReviewUiState.Error, is AIWeeklyReviewUiState.Success -> {
                val data = if (state is AIWeeklyReviewUiState.Success) state.data else (state as AIWeeklyReviewUiState.Error).fallbackData ?: return@Scaffold

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 48.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header Avatar AI
                    item {
                        AIHeaderSection()
                    }

                    // 1. Tóm tắt tuần (Card Trắng)
                    item {
                        SummaryCard(summary = data.summary)
                    }

                    // 2. Điểm mạnh (Card Xanh Lá)
                    item {
                        StrengthsCard(strengths = data.strengths)
                    }

                    // 3. Cần cải thiện (Card Cam)
                    item {
                        ImprovementsCard(improvements = data.improvements)
                    }

                    // 4. Mẫu hình & Thói quen (Card Tím)
                    item {
                        HabitsCard(habits = data.habits)
                    }

                    // 5. Gợi ý cho tuần tới (Card Xanh Xám / Tím)
                    item {
                        RecommendationsCard(recommendations = data.recommendations)
                    }

                    // 6. Mục tiêu đề xuất (Actionable Goals)
                    item {
                        SuggestedGoalsCard(
                            goals = data.suggestedGoals,
                            addedGoalIds = addedGoalIds,
                            onAddGoal = { goal ->
                                // Thêm sự kiện vào ScheduleViewModel
                                coroutineScope.launch {
                                    val nextMonday = LocalDate.now().plusWeeks(1).with(java.time.DayOfWeek.MONDAY)
                                    val newEvent = Event(
                                        id = 0,
                                        title = goal.title,
                                        reminderNote = "Mục tiêu được AI đề xuất cho tuần tới để tối ưu hiệu suất.",
                                        category = if (goal.iconType == "FITNESS") EventCategory.PLAY else EventCategory.STUDY,
                                        startTime = LocalDateTime.of(nextMonday, LocalTime.of(8, 0)),
                                        endTime = LocalDateTime.of(nextMonday, LocalTime.of(9, 0)),
                                        isCompleted = false,
                                        hasReminder = true
                                    )
                                    scheduleViewModel.addEvent(newEvent)
                                    reviewViewModel.markGoalAdded(goal.id)
                                    Toast.makeText(context, "Đã thêm '${goal.title}' vào lịch trình tuần tới!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }

                    // 7. Hộp tương tác / Chat hỏi đáp tiếp theo với AI
                    item {
                        AskAISection(
                            inputPrompt = inputPrompt,
                            onInputChange = { inputPrompt = it },
                            isLoading = isAskingAI,
                            onSend = {
                                if (inputPrompt.isNotBlank()) {
                                    val text = inputPrompt
                                    inputPrompt = ""
                                    keyboardController?.hide()
                                    reviewViewModel.askAIQuestion(text)
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(listState.layoutInfo.totalItemsCount)
                                    }
                                }
                            }
                        )
                    }

                    // Danh sách tin nhắn hỏi đáp mở rộng nếu có
                    if (interactionMessages.isNotEmpty()) {
                        item {
                            Text(
                                text = "Phản hồi phân tích bổ sung từ AI:",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        items(interactionMessages, key = { it.id }) { msg ->
                            InteractionMessageBubble(msg = msg)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 1. AI Avatar & Title Header
 */
@Composable
private fun AIHeaderSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar robot tròn tím
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(Color(0xFFEDE9FE)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF6366F1)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = "AI Robot",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "My_schedule AI",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4F46E5)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFFEEF2FF)
            ) {
                Text(
                    text = "Beta",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6366F1),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Đây là phân tích và gợi ý được tạo bởi AI\ndựa trên dữ liệu lịch trình của bạn.",
            fontSize = 12.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 17.sp
        )
    }
}

/**
 * 2. Tóm tắt tuần
 */
@Composable
private fun SummaryCard(summary: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        shadowElevation = 0.5.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Tóm tắt tuần",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = summary,
                fontSize = 13.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
        }
    }
}

/**
 * 3. Điểm mạnh (Xanh lá)
 */
@Composable
private fun StrengthsCard(strengths: List<String>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF0FDF4),
        border = BorderStroke(1.dp, Color(0xFFDCFCE7))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Điểm mạnh",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF15803D)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                strengths.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier
                                .size(18.dp)
                                .offset(y = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = item,
                            fontSize = 13.5.sp,
                            color = Color(0xFF166534),
                            lineHeight = 19.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 4. Cần cải thiện (Cam / Đỏ)
 */
@Composable
private fun ImprovementsCard(improvements: List<String>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFFFF7ED),
        border = BorderStroke(1.dp, Color(0xFFFFEDD5))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Cần cải thiện",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFC2410C)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                improvements.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .offset(y = 2.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEA580C)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PriorityHigh,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = item,
                            fontSize = 13.5.sp,
                            color = Color(0xFF9A3412),
                            lineHeight = 19.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 5. Mẫu hình & Thói quen (Tím)
 */
@Composable
private fun HabitsCard(habits: AIHabitPatterns) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF5F3FF),
        border = BorderStroke(1.dp, Color(0xFFEDE9FE))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Mẫu hình & Thói quen",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6D28D9)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                HabitRowItem(
                    icon = Icons.Default.Stars,
                    label = "Ngày hiệu quả nhất:",
                    value = habits.mostProductiveDay,
                    iconBg = Color(0xFF7C3AED)
                )
                HabitRowItem(
                    icon = Icons.Default.Stars,
                    label = "Ngày kém hiệu quả nhất:",
                    value = habits.leastProductiveDay,
                    iconBg = Color(0xFF7C3AED)
                )
                HabitRowItem(
                    icon = Icons.Default.AccessTime,
                    label = "Khung giờ hiệu quả nhất:",
                    value = habits.mostProductiveTime,
                    iconBg = Color(0xFF6366F1)
                )
                HabitRowItem(
                    icon = Icons.Default.AccessTime,
                    label = "Khung giờ kém hiệu quả:",
                    value = habits.leastProductiveTime,
                    iconBg = Color(0xFF6366F1)
                )
            }
        }
    }
}

@Composable
private fun HabitRowItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    iconBg: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "$label ",
            fontSize = 13.5.sp,
            color = Color(0xFF4C1D95)
        )
        Text(
            text = value,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4C1D95)
        )
    }
}

/**
 * 6. Gợi ý cho tuần tới
 */
@Composable
private fun RecommendationsCard(recommendations: List<String>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Gợi ý cho tuần tới",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                recommendations.forEachIndexed { index, text ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .offset(y = 1.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF6366F1)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = text,
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 19.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 7. Mục tiêu đề xuất (Actionable Goals)
 */
@Composable
private fun SuggestedGoalsCard(
    goals: List<AISuggestedGoal>,
    addedGoalIds: Set<String>,
    onAddGoal: (AISuggestedGoal) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Mục tiêu đề xuất",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            goals.forEach { goal ->
                val isAdded = addedGoalIds.contains(goal.id)

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    shadowElevation = 0.5.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (goal.iconType == "FITNESS") Color(0xFFDCFCE7) else Color(0xFFFFE4E6)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (goal.iconType == "FITNESS") Icons.Default.FitnessCenter else Icons.Default.TrackChanges,
                                    contentDescription = null,
                                    tint = if (goal.iconType == "FITNESS") Color(0xFF16A34A) else Color(0xFFE11D48),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = goal.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = goal.timeFrame,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        OutlinedButton(
                            onClick = { if (!isAdded) onAddGoal(goal) },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isAdded) Color(0xFF10B981) else Color(0xFF6366F1).copy(alpha = 0.6f)
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isAdded) Color(0xFFECFDF5) else Color.Transparent,
                                contentColor = if (isAdded) Color(0xFF059669) else Color(0xFF6366F1)
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            if (isAdded) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Đã thêm", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            } else {
                                Text(text = "Thêm vào lịch", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 8. Hộp tương tác Chat hỏi đáp với AI
 */
@Composable
private fun AskAISection(
    inputPrompt: String,
    onInputChange: (String) -> Unit,
    isLoading: Boolean,
    onSend: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF6366F1)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF6366F1), Color(0xFF4F46E5))
                    )
                )
                .padding(16.dp)
        ) {
            Text(
                text = "Bạn muốn AI phân tích điều gì tiếp theo?",
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = Color.White
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = inputPrompt,
                        onValueChange = onInputChange,
                        placeholder = {
                            Text(
                                text = "Ví dụ: Phân tích tháng này của tôi...",
                                fontSize = 13.sp,
                                color = Color.Gray.copy(alpha = 0.8f)
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color(0xFF1E293B),
                            unfocusedTextColor = Color(0xFF1E293B)
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { onSend() }),
                        modifier = Modifier.weight(1f)
                    )

                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color(0xFF6366F1),
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    if (inputPrompt.isNotBlank()) Color(0xFF6366F1) else Color(0xFFC7D2FE)
                                )
                                .clickable(enabled = inputPrompt.isNotBlank()) { onSend() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Gửi",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InteractionMessageBubble(msg: com.example.models.dashboard.AIInteractionMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = if (msg.isUser) 14.dp else 4.dp,
                bottomEnd = if (msg.isUser) 4.dp else 14.dp
            ),
            color = if (msg.isUser) Color(0xFF6366F1) else Color(0xFFF1F5F9),
            border = if (msg.isUser) null else BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.widthIn(max = 310.dp)
        ) {
            Text(
                text = msg.text,
                fontSize = 13.5.sp,
                color = if (msg.isUser) Color.White else Color(0xFF1E293B),
                lineHeight = 19.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }
    }
}
