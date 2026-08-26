package com.example.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.Event
import com.example.models.EventCategory
import com.example.network.GeminiApiService
import com.example.network.GeminiContent
import com.example.network.GeminiGenerationConfig
import com.example.network.GeminiPart
import com.example.network.GeminiRequest
import com.example.util.AiConfigHelper
import com.example.viewmodel.ScheduleViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.UUID

enum class StudyPlannerStep {
    FORM,
    ADJUSTING,
    RESULT
}

enum class TaskDifficulty(val title: String, val color: Color, val bgColor: Color) {
    EASY("Dễ (Thấp)", Color(0xFF10B981), Color(0xFFECFDF5)),
    MEDIUM("Trung bình", Color(0xFFF59E0B), Color(0xFFFEF3C7)),
    HARD("Khó (Cao)", Color(0xFFEF4444), Color(0xFFFEE2E2))
}

data class StudyTaskItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val subtitle: String,
    val deadline: String,
    val hours: Int,
    val difficulty: TaskDifficulty,
    val accentColor: Color
)

data class PlanTimelineItem(
    val id: String = UUID.randomUUID().toString(),
    val time: String,
    val title: String,
    val subtitle: String,
    val accentColor: Color,
    val category: EventCategory = EventCategory.STUDY,
    val isBreak: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIStudyPlannerScreen(
    onBack: () -> Unit,
    scheduleViewModel: ScheduleViewModel? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var currentStep by remember { mutableStateOf(StudyPlannerStep.FORM) }

    // Observe existing schedule events to prevent overlapping
    val existingEvents by (scheduleViewModel?.events?.collectAsStateWithLifecycle() ?: remember {
        mutableStateOf(emptyList<Event>())
    })
    val now = remember { LocalDate.now() }
    val todayStartOfWeek = remember(now) { now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) }

    // Form inputs state - Starts empty as requested
    var studyGoal by remember { mutableStateOf("") }
    var taskList by remember { mutableStateOf(emptyList<StudyTaskItem>()) }
    var selectedTimeSlots by remember {
        mutableStateOf(setOf("Sáng", "Chiều", "Tối", "Cuối tuần"))
    }

    // Generated schedule holder
    var generatedSchedule by remember {
        mutableStateOf<Map<Int, List<PlanTimelineItem>>>(emptyMap())
    }

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<StudyTaskItem?>(null) }
    var taskToDelete by remember { mutableStateOf<StudyTaskItem?>(null) }

    // Intercept back button inside inner steps
    BackHandler {
        when (currentStep) {
            StudyPlannerStep.FORM -> onBack()
            StudyPlannerStep.ADJUSTING -> currentStep = StudyPlannerStep.FORM
            StudyPlannerStep.RESULT -> currentStep = StudyPlannerStep.FORM
        }
    }

    AnimatedContent(
        targetState = currentStep,
        transitionSpec = {
            if (targetState.ordinal > initialState.ordinal) {
                slideInHorizontally { width -> width } + fadeIn() togetherWith
                        slideOutHorizontally { width -> -width } + fadeOut()
            } else {
                slideInHorizontally { width -> -width } + fadeIn() togetherWith
                        slideOutHorizontally { width -> width } + fadeOut()
            }
        },
        label = "study_planner_transition"
    ) { step ->
        when (step) {
            StudyPlannerStep.FORM -> {
                AIStudyPlannerFormView(
                    studyGoal = studyGoal,
                    onStudyGoalChange = { studyGoal = it },
                    taskList = taskList,
                    onAddTaskClick = { showAddTaskDialog = true },
                    onEditTask = { item -> taskToEdit = item },
                    onDeleteTask = { item -> taskToDelete = item },
                    selectedTimeSlots = selectedTimeSlots,
                    onToggleTimeSlot = { slot ->
                        selectedTimeSlots = if (selectedTimeSlots.contains(slot)) {
                            selectedTimeSlots - slot
                        } else {
                            selectedTimeSlots + slot
                        }
                    },
                    onGeneratePlan = {
                        if (taskList.isEmpty()) {
                            Toast.makeText(context, "Vui lòng thêm ít nhất 1 môn / nhiệm vụ để AI lên lịch", Toast.LENGTH_SHORT).show()
                        } else {
                            currentStep = StudyPlannerStep.ADJUSTING
                        }
                    },
                    onBack = onBack
                )
            }

            StudyPlannerStep.ADJUSTING -> {
                AIStudyPlannerAdjustingView(
                    studyGoal = studyGoal,
                    taskList = taskList,
                    selectedTimeSlots = selectedTimeSlots,
                    existingEvents = existingEvents,
                    todayStartOfWeek = todayStartOfWeek,
                    onBack = { currentStep = StudyPlannerStep.FORM },
                    onCompleted = { resultPlan ->
                        generatedSchedule = resultPlan
                        currentStep = StudyPlannerStep.RESULT
                    }
                )
            }

            StudyPlannerStep.RESULT -> {
                AIStudyPlannerResultView(
                    onBack = { currentStep = StudyPlannerStep.FORM },
                    taskList = taskList,
                    initialSchedule = generatedSchedule,
                    scheduleViewModel = scheduleViewModel,
                    onApplied = {
                        Toast.makeText(
                            context,
                            "🎉 Đã thêm toàn bộ kế hoạch vào Lịch biểu thành công!",
                            Toast.LENGTH_LONG
                        ).show()
                        onBack()
                    }
                )
            }
        }
    }

    // Add Task Dialog
    if (showAddTaskDialog) {
        TaskUpsertDialog(
            initialTask = null,
            onDismiss = { showAddTaskDialog = false },
            onConfirm = { newItem ->
                taskList = taskList + newItem
                showAddTaskDialog = false
                Toast.makeText(context, "Đã thêm môn học mới", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Edit Task Dialog
    taskToEdit?.let { itemToEdit ->
        TaskUpsertDialog(
            initialTask = itemToEdit,
            onDismiss = { taskToEdit = null },
            onConfirm = { updatedItem ->
                taskList = taskList.map { if (it.id == updatedItem.id) updatedItem else it }
                taskToEdit = null
                Toast.makeText(context, "Đã cập nhật môn học", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Delete Task Confirmation Dialog
    taskToDelete?.let { itemToDelete ->
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            title = {
                Text(
                    text = "Xóa môn / nhiệm vụ?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Text("Bạn có chắc chắn muốn xóa môn \"${itemToDelete.title}\" khỏi kế hoạch học tập không?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        taskList = taskList.filter { it.id != itemToDelete.id }
                        taskToDelete = null
                        Toast.makeText(context, "Đã xóa môn học", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Xóa", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { taskToDelete = null }) {
                    Text("Hủy")
                }
            }
        )
    }
}

// -------------------------------------------------------------------------------------------------
// 1. SCREEN 1: AI Planner Form View
// -------------------------------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AIStudyPlannerFormView(
    studyGoal: String,
    onStudyGoalChange: (String) -> Unit,
    taskList: List<StudyTaskItem>,
    onAddTaskClick: () -> Unit,
    onEditTask: (StudyTaskItem) -> Unit,
    onDeleteTask: (StudyTaskItem) -> Unit,
    selectedTimeSlots: Set<String>,
    onToggleTimeSlot: (String) -> Unit,
    onGeneratePlan: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "AI Planner",
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Button(
                        onClick = onGeneratePlan,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_generate_ai_plan"),
                        shape = RoundedCornerShape(25.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Tạo kế hoạch với AI",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
                // 1. Mục tiêu học tập
                item {
                    Text(
                        text = "1. Mục tiêu học tập",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = studyGoal,
                        onValueChange = onStudyGoalChange,
                        placeholder = {
                            Text(
                                text = "Mục tiêu của bạn là gì? (Tùy chọn)\nVí dụ: Ôn thi cuối kỳ, hoàn thành bài tập lớn nhóm 3, củng cố kiến thức...",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 86.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                        )
                    )
                }

                // 2. Danh sách môn / nhiệm vụ
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "2. Danh sách môn / nhiệm vụ (${taskList.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "AI sẽ đọc tên, ghi chú, hạn chót, số giờ và độ ưu tiên để xếp lịch",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        FilledTonalButton(
                            onClick = onAddTaskClick,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("btn_add_study_task")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Thêm",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                if (taskList.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onAddTaskClick() },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.School,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Chưa có môn học / nhiệm vụ nào",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Nhấn để thêm môn học đầu tiên kèm hạn chót và số giờ",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = onAddTaskClick,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Thêm môn học")
                                }
                            }
                        }
                    }
                } else {
                    items(taskList, key = { it.id }) { item ->
                        StudyTaskCard(
                            item = item,
                            onEdit = { onEditTask(item) },
                            onDelete = { onDeleteTask(item) }
                        )
                    }
                }

                // 3. Thời gian rảnh
                item {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "3. Khung giờ rảnh trong tuần",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "AI sẽ chỉ xếp lịch vào những khung giờ bạn rảnh",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val slots = listOf(
                        "Sáng" to "Buổi sáng (08:00 - 11:30)",
                        "Chiều" to "Buổi chiều (13:30 - 17:00)",
                        "Tối" to "Buổi tối (19:00 - 22:30)",
                        "Cuối tuần" to "Thứ Bảy & Chủ Nhật"
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        slots.forEach { (key, label) ->
                            val isSelected = selectedTimeSlots.contains(key)
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onToggleTimeSlot(key) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Icon(
                                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
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

@Composable
private fun StudyTaskCard(
    item: StudyTaskItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onEdit() },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon circle
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(item.accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.School,
                        contentDescription = null,
                        tint = item.accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.subtitle.isNotBlank()) {
                        Text(
                            text = item.subtitle,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Difficulty / Priority Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = item.difficulty.bgColor
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(item.difficulty.color)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = item.difficulty.title,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = item.difficulty.color
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                thickness = 0.8.dp
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Bottom row: Deadline info + Edit & Delete Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "Hạn: ${item.deadline.ifBlank { "Chưa đặt" }}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "•",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "${item.hours} giờ",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Action Buttons: Edit & Delete
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Chỉnh sửa",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Xóa",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// 2. SCREEN 2: Điều chỉnh kế hoạch (AI Loading & Real Gemini Generation Screen)
// -------------------------------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AIStudyPlannerAdjustingView(
    studyGoal: String,
    taskList: List<StudyTaskItem>,
    selectedTimeSlots: Set<String>,
    existingEvents: List<Event>,
    todayStartOfWeek: LocalDate,
    onBack: () -> Unit,
    onCompleted: (Map<Int, List<PlanTimelineItem>>) -> Unit
) {
    var stepProgress by remember { mutableIntStateOf(1) }
    val geminiService = remember { GeminiApiService.create() }

    // Execute real AI scheduling logic in background while showing animated progress
    LaunchedEffect(Unit) {
        val animationJob = launch {
            delay(500)
            stepProgress = 2
            delay(600)
            stepProgress = 3
            delay(700)
            stepProgress = 4
        }

        val resultPlan = withContext(Dispatchers.IO) {
            generateStudyPlan(
                geminiService = geminiService,
                studyGoal = studyGoal,
                taskList = taskList,
                selectedTimeSlots = selectedTimeSlots,
                existingEvents = existingEvents,
                todayStartOfWeek = todayStartOfWeek
            )
        }

        animationJob.join()
        delay(300)
        onCompleted(resultPlan)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "robot_anim")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floating"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "AI đang xử lý kế hoạch",
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // 3D Glowing Robot Character Avatar
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .offset(y = floatOffset.dp)
                        .scale(pulseScale),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer aura glow
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                        Color(0xFF8B5CF6).copy(alpha = 0.15f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // Robot head / body container
                    Surface(
                        modifier = Modifier.size(96.dp),
                        shape = RoundedCornerShape(30.dp),
                        color = Color.White,
                        shadowElevation = 8.dp,
                        border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFE2E8F0))
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFFF8FAFC), Color(0xFFE2E8F0))
                                    )
                                )
                        ) {
                            // Visor / Screen Face
                            Box(
                                modifier = Modifier
                                    .size(width = 64.dp, height = 40.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF0F172A)),
                                contentAlignment = Alignment.Center
                            ) {
                                // Glowing eyes
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(width = 10.dp, height = 14.dp)
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(Color(0xFF38BDF8))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(width = 10.dp, height = 14.dp)
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(Color(0xFF38BDF8))
                                    )
                                }
                            }
                        }
                    }

                    // Floating sparkles around robot
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFFA855F7),
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = (-8).dp, y = 8.dp)
                    )
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier
                            .size(20.dp)
                            .align(Alignment.BottomStart)
                            .offset(x = 8.dp, y = (-8).dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "AI đang đọc chi tiết nhiệm vụ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "và kiểm tra lịch trống để tránh trùng...",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Steps Progress List
                val steps = listOf(
                    "Đọc tên môn, mục tiêu, hạn chót & mức ưu tiên",
                    "Kiểm tra các sự kiện đã có để tránh hoàn toàn trùng giờ",
                    "Phân tích khung giờ rảnh (${selectedTimeSlots.joinToString(", ")})",
                    "Tối ưu hóa các phiên học & hoàn thiện kế hoạch"
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    steps.forEachIndexed { index, stepTitle ->
                        val stepIndex = index + 1
                        val isDone = stepProgress > stepIndex
                        val isCurrent = stepProgress == stepIndex

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stepTitle,
                                fontSize = 14.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                color = if (isDone || isCurrent) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.weight(1f)
                            )

                            if (isDone) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFD1FAE5)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Hoàn thành",
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else if (isCurrent) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), CircleShape)
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Banner
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "AI sẽ tự động né các sự kiện đã có để bạn không bị trùng lịch!",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// 3. SCREEN 3: Kế hoạch của bạn (Timetable Schedule Result Screen)
// -------------------------------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AIStudyPlannerResultView(
    onBack: () -> Unit,
    taskList: List<StudyTaskItem>,
    initialSchedule: Map<Int, List<PlanTimelineItem>>,
    scheduleViewModel: ScheduleViewModel?,
    onApplied: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Tuần này, 1: Toàn bộ

    val now = remember { LocalDate.now() }
    val todayStartOfWeek = remember(now) { now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) }
    val todayDayOfWeekIndex = remember(now) {
        // Monday = 0, Tuesday = 1, ..., Sunday = 6
        (now.dayOfWeek.value - 1).coerceIn(0, 6)
    }

    var selectedDayIndex by remember { mutableIntStateOf(todayDayOfWeekIndex) }

    val daysOfWeek = remember(todayStartOfWeek) {
        listOf(
            Pair("T2", todayStartOfWeek.dayOfMonth),
            Pair("T3", todayStartOfWeek.plusDays(1).dayOfMonth),
            Pair("T4", todayStartOfWeek.plusDays(2).dayOfMonth),
            Pair("T5", todayStartOfWeek.plusDays(3).dayOfMonth),
            Pair("T6", todayStartOfWeek.plusDays(4).dayOfMonth),
            Pair("T7", todayStartOfWeek.plusDays(5).dayOfMonth),
            Pair("CN", todayStartOfWeek.plusDays(6).dayOfMonth)
        )
    }

    val weekDateRangeTitle = remember(todayStartOfWeek) {
        val endOfWeek = todayStartOfWeek.plusDays(6)
        "⟨  ${todayStartOfWeek.dayOfMonth} - ${endOfWeek.dayOfMonth} Tháng ${todayStartOfWeek.monthValue}, ${todayStartOfWeek.year}  ⟩"
    }

    // Mutable schedule map per day index to allow editing, deleting, and adding events
    val scheduleByDay = remember(initialSchedule) {
        mutableStateMapOf<Int, List<PlanTimelineItem>>().apply {
            putAll(initialSchedule)
        }
    }

    var timelineItemToEdit by remember { mutableStateOf<Pair<Int, PlanTimelineItem>?>(null) }
    var showAddTimelineDialog by remember { mutableStateOf(false) }

    val currentDayItems = scheduleByDay[selectedDayIndex] ?: emptyList()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Kế hoạch của bạn",
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Button(
                        onClick = {
                            if (scheduleViewModel != null) {
                                coroutineScope.launch {
                                    val eventsToInsert = mutableListOf<Event>()
                                    scheduleByDay.forEach { (dayIdx, items) ->
                                        val targetDate = todayStartOfWeek.plusDays(dayIdx.toLong())
                                        items.filter { !it.isBreak }.forEach { timelineItem ->
                                            val parts = timelineItem.time.split(":")
                                            val h = parts.getOrNull(0)?.toIntOrNull() ?: 8
                                            val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
                                            val startDt = LocalDateTime.of(targetDate, LocalTime.of(h, m))
                                            val endDt = startDt.plusHours(2)

                                            val fullTitle = if (timelineItem.subtitle.isNotBlank()) {
                                                "${timelineItem.title}: ${timelineItem.subtitle}"
                                            } else {
                                                timelineItem.title
                                            }

                                            eventsToInsert.add(
                                                Event(
                                                    title = fullTitle,
                                                    startTime = startDt,
                                                    endTime = endDt,
                                                    category = timelineItem.category,
                                                    isCompleted = false,
                                                    reminderNote = "Lịch học tự động tạo bởi AI Planner",
                                                    hasReminder = true,
                                                    reminderTimeOffsetMins = 15
                                                )
                                            )
                                        }
                                    }

                                    scheduleViewModel.addEvents(eventsToInsert, context)
                                    onApplied()
                                }
                            } else {
                                onApplied()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(25.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Áp dụng vào Lịch biểu",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Segmented Tabs: "Tuần này" / "Toàn bộ"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Row(modifier = Modifier.padding(4.dp)) {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { selectedTab = 0 },
                            shape = RoundedCornerShape(16.dp),
                            color = if (selectedTab == 0) MaterialTheme.colorScheme.surface else Color.Transparent,
                            shadowElevation = if (selectedTab == 0) 2.dp else 0.dp
                        ) {
                            Text(
                                text = "Tuần này",
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { selectedTab = 1 },
                            shape = RoundedCornerShape(16.dp),
                            color = if (selectedTab == 1) MaterialTheme.colorScheme.surface else Color.Transparent,
                            shadowElevation = if (selectedTab == 1) 2.dp else 0.dp
                        ) {
                            Text(
                                text = "Toàn bộ",
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Week Range Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = weekDateRangeTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )

                TextButton(
                    onClick = { showAddTimelineDialog = true },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Thêm sự kiện", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Days Strip: T2 -> CN
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                daysOfWeek.forEachIndexed { idx, (dayName, dateNum) ->
                    val isSelected = selectedDayIndex == idx
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { selectedDayIndex = idx }
                            .padding(vertical = 4.dp, horizontal = 2.dp)
                    ) {
                        Text(
                            text = dayName,
                            fontSize = 12.sp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dateNum.toString(),
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Schedule Timeline List
            if (currentDayItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.EventBusy,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Không có lịch học trong ngày này",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showAddTimelineDialog = true }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Thêm sự kiện")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    items(currentDayItems, key = { it.id }) { item ->
                        TimelineItemRow(
                            item = item,
                            onEdit = {
                                timelineItemToEdit = Pair(selectedDayIndex, item)
                            },
                            onDelete = {
                                val updatedList = currentDayItems.filter { it.id != item.id }
                                scheduleByDay[selectedDayIndex] = updatedList
                                Toast.makeText(context, "Đã xóa sự kiện", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }

    // Add Timeline Event Dialog
    if (showAddTimelineDialog) {
        TimelineItemUpsertDialog(
            initialItem = null,
            dayLabel = daysOfWeek.getOrNull(selectedDayIndex)?.first ?: "ngày chọn",
            onDismiss = { showAddTimelineDialog = false },
            onConfirm = { newItem ->
                val current = scheduleByDay[selectedDayIndex] ?: emptyList()
                scheduleByDay[selectedDayIndex] = (current + newItem).sortedBy { it.time }
                showAddTimelineDialog = false
                Toast.makeText(context, "Đã thêm sự kiện vào kế hoạch", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Edit Timeline Event Dialog
    timelineItemToEdit?.let { (dayIdx, itemToEdit) ->
        TimelineItemUpsertDialog(
            initialItem = itemToEdit,
            dayLabel = daysOfWeek.getOrNull(dayIdx)?.first ?: "ngày chọn",
            onDismiss = { timelineItemToEdit = null },
            onConfirm = { updatedItem ->
                val current = scheduleByDay[dayIdx] ?: emptyList()
                scheduleByDay[dayIdx] = current.map { if (it.id == updatedItem.id) updatedItem else it }.sortedBy { it.time }
                timelineItemToEdit = null
                Toast.makeText(context, "Đã cập nhật sự kiện", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun TimelineItemRow(
    item: PlanTimelineItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Time label
        Text(
            text = item.time,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .width(46.dp)
                .padding(top = 12.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Card item
        if (item.isBreak) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onEdit() },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Coffee,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.title,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = "Sửa",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "Xóa",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        } else {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onEdit() },
                shape = RoundedCornerShape(14.dp),
                color = item.accentColor.copy(alpha = 0.08f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    item.accentColor.copy(alpha = 0.25f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left vertical indicator bar
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(36.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(item.accentColor)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (item.subtitle.isNotBlank()) {
                            Text(
                                text = item.subtitle,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Action buttons
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Edit,
                                contentDescription = "Chỉnh sửa",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = "Xóa",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// 4. DIALOGS: Add/Edit Task Dialog & Add/Edit Timeline Item Dialog
// -------------------------------------------------------------------------------------------------
@Composable
private fun TaskUpsertDialog(
    initialTask: StudyTaskItem?,
    onDismiss: () -> Unit,
    onConfirm: (StudyTaskItem) -> Unit
) {
    val isEdit = initialTask != null
    var title by remember { mutableStateOf(initialTask?.title ?: "") }
    var subtitle by remember { mutableStateOf(initialTask?.subtitle ?: "") }
    var deadline by remember { mutableStateOf(initialTask?.deadline ?: "") }
    var hours by remember { mutableStateOf(initialTask?.hours?.toString() ?: "8") }
    var difficulty by remember { mutableStateOf(initialTask?.difficulty ?: TaskDifficulty.MEDIUM) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEdit) "Chỉnh sửa môn / nhiệm vụ" else "Thêm môn / nhiệm vụ",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tên môn / nhiệm vụ *") },
                    placeholder = { Text("VD: Machine Learning, Toán cao cấp...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = subtitle,
                    onValueChange = { subtitle = it },
                    label = { Text("Ghi chú / mục tiêu chi tiết") },
                    placeholder = { Text("VD: Ôn thi chương 1-4, làm bài tập nhóm...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = deadline,
                        onValueChange = { deadline = it },
                        label = { Text("Hạn chót") },
                        placeholder = { Text("15/09/2026") },
                        modifier = Modifier.weight(1.1f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = hours,
                        onValueChange = { hours = it },
                        label = { Text("Số giờ") },
                        modifier = Modifier.weight(0.9f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }

                Text(
                    text = "Mức độ ưu tiên / độ khó:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TaskDifficulty.values().forEach { diff ->
                        val isSelected = difficulty == diff
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { difficulty = diff },
                            color = if (isSelected) diff.bgColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, diff.color) else null,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = diff.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) diff.color else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val accentColor = when (difficulty) {
                            TaskDifficulty.HARD -> Color(0xFF8B5CF6)
                            TaskDifficulty.MEDIUM -> Color(0xFF06B6D4)
                            TaskDifficulty.EASY -> Color(0xFF10B981)
                        }
                        onConfirm(
                            StudyTaskItem(
                                id = initialTask?.id ?: UUID.randomUUID().toString(),
                                title = title.trim(),
                                subtitle = subtitle.trim(),
                                deadline = deadline.trim(),
                                hours = hours.toIntOrNull() ?: 8,
                                difficulty = difficulty,
                                accentColor = accentColor
                            )
                        )
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text(if (isEdit) "Lưu thay đổi" else "Thêm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

@Composable
private fun TimelineItemUpsertDialog(
    initialItem: PlanTimelineItem?,
    dayLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (PlanTimelineItem) -> Unit
) {
    val isEdit = initialItem != null
    var time by remember { mutableStateOf(initialItem?.time ?: "08:00") }
    var title by remember { mutableStateOf(initialItem?.title ?: "") }
    var subtitle by remember { mutableStateOf(initialItem?.subtitle ?: "") }
    var isBreak by remember { mutableStateOf(initialItem?.isBreak ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEdit) "Sửa sự kiện ($dayLabel)" else "Thêm sự kiện ($dayLabel)",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    label = { Text("Thời gian (HH:mm)") },
                    placeholder = { Text("08:00") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tên môn / sự kiện") },
                    placeholder = { Text("VD: Machine Learning") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = subtitle,
                    onValueChange = { subtitle = it },
                    label = { Text("Chi tiết / nội dung") },
                    placeholder = { Text("VD: Ôn thi chương 1-3") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { isBreak = !isBreak }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = isBreak,
                        onCheckedChange = { isBreak = it }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Đánh dấu là giờ nghỉ ngơi / giải lao", fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val accentColor = if (isBreak) {
                            Color(0xFF94A3B8)
                        } else {
                            when {
                                title.contains("Machine", ignoreCase = true) || title.contains("AI", ignoreCase = true) -> Color(0xFF8B5CF6)
                                title.contains("Deep", ignoreCase = true) -> Color(0xFF06B6D4)
                                title.contains("Data", ignoreCase = true) || title.contains("Toán", ignoreCase = true) -> Color(0xFF10B981)
                                else -> Color(0xFF3B82F6)
                            }
                        }
                        onConfirm(
                            PlanTimelineItem(
                                id = initialItem?.id ?: UUID.randomUUID().toString(),
                                time = time.trim().ifBlank { "08:00" },
                                title = title.trim(),
                                subtitle = subtitle.trim(),
                                accentColor = accentColor,
                                category = if (isBreak) EventCategory.PLAY else EventCategory.STUDY,
                                isBreak = isBreak
                            )
                        )
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text(if (isEdit) "Lưu thay đổi" else "Thêm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

// -------------------------------------------------------------------------------------------------
// 5. REAL AI SCHEDULING LOGIC WITH GEMINI AND INTELLIGENT RULE-BASED FALLBACK (CONFLICT AVOIDANCE)
// -------------------------------------------------------------------------------------------------
private suspend fun generateStudyPlan(
    geminiService: GeminiApiService,
    studyGoal: String,
    taskList: List<StudyTaskItem>,
    selectedTimeSlots: Set<String>,
    existingEvents: List<Event>,
    todayStartOfWeek: LocalDate
): Map<Int, List<PlanTimelineItem>> {
    val apiKey = AiConfigHelper.getEffectiveApiKey()

    if (apiKey.isNotBlank() && taskList.isNotEmpty()) {
        try {
            val taskListSummary = taskList.mapIndexed { idx, t ->
                "${idx + 1}. Môn/Nhiệm vụ: \"${t.title}\", Ghi chú/Mục tiêu: \"${t.subtitle.ifBlank { "Không có" }}\", Hạn chót: \"${t.deadline.ifBlank { "Không có" }}\", Tổng số giờ cần học: ${t.hours} giờ, Mức độ ưu tiên/Độ khó: ${t.difficulty.title}"
            }.joinToString("\n")

            val timeFormat = DateTimeFormatter.ofPattern("HH:mm")
            val dateFormat = DateTimeFormatter.ofPattern("dd/MM")

            val existingEventsSummary = (0..6).joinToString("\n") { dayIdx ->
                val date = todayStartOfWeek.plusDays(dayIdx.toLong())
                val dayName = when (dayIdx) {
                    0 -> "Thứ 2"
                    1 -> "Thứ 3"
                    2 -> "Thứ 4"
                    3 -> "Thứ 5"
                    4 -> "Thứ 6"
                    5 -> "Thứ 7"
                    else -> "Chủ Nhật"
                }
                val eventsOnDay = existingEvents.filter { it.startTime.toLocalDate() == date }
                if (eventsOnDay.isEmpty()) {
                    "- $dayName (dayIndex $dayIdx, ngày ${date.format(dateFormat)}): Trống cả ngày (chưa có sự kiện nào)."
                } else {
                    val evList = eventsOnDay.joinToString(", ") { e ->
                        "\"${e.title}\" (${e.startTime.format(timeFormat)} - ${e.endTime.format(timeFormat)})"
                    }
                    "- $dayName (dayIndex $dayIdx, ngày ${date.format(dateFormat)}): ĐÃ CÓ SỰ KIỆN: [$evList] -> YÊU CẦU: TUYỆT ĐỐI KHÔNG XẾP LỊCH TRÙNG CÁC GIỜ NÀY!"
                }
            }

            val prompt = buildString {
                append("Bạn là chuyên gia lập kế hoạch học tập thông minh (AI Planner).\n")
                append("Hãy đọc kỹ từng môn học/nhiệm vụ dưới đây để phân bổ lịch trình học tập cho cả tuần (Thứ 2 đến Chủ Nhật, tương ứng dayIndex từ 0 đến 6):\n\n")
                append("=== THÔNG TIN NGƯỜI DÙNG CUNG CẤP ===\n")
                if (studyGoal.isNotBlank()) {
                    append("- Mục tiêu chung: $studyGoal\n")
                }
                append("- Các khung giờ rảnh được chọn: ${selectedTimeSlots.joinToString(", ")}\n")
                append("  (Tham khảo: Sáng: 08:00 - 11:30, Chiều: 13:30 - 17:00, Tối: 19:00 - 22:30, Cuối tuần: T7 & CN)\n")
                append("- Danh sách môn học / nhiệm vụ cụ thể cần lên lịch:\n")
                append(taskListSummary)
                append("\n\n")
                append("=== DANH SÁCH SỰ KIỆN ĐÃ CÓ TRONG LỊCH BIỂU (QUAN TRỌNG - TRÁNH TRÙNG LỊCH) ===\n")
                append(existingEventsSummary)
                append("\n\n")
                append("=== NGUYÊN TẮC XẾP LỊCH BẰNG AI ===\n")
                append("1. TUYỆT ĐỐI KHÔNG TẠO SỰ KIỆN TRÙNG LỊCH: Nếu một ngày nào đó trong tuần đã có sự kiện đã lên lịch, KHÔNG ĐƯỢC xếp môn học hay giờ giải lao trùng vào khoảng thời gian đó. Hãy né khung giờ đã có sự kiện ra (xếp trước hoặc sau sự kiện đó ít nhất 15 phút, hoặc chọn khung giờ trống khác trong ngày, hoặc xếp vào ngày khác).\n")
                append("2. Đọc đúng tên từng môn học và ghi chú/mục tiêu của môn đó.\n")
                append("3. Phân bổ các môn có độ ưu tiên cao (Khó) hoặc hạn chót gần vào các khung giờ tập trung tốt và nhiều buổi hơn.\n")
                append("4. Chia nhỏ số giờ cần học của từng môn thành các buổi học hợp lý (khoảng 1.5 - 2 giờ mỗi buổi) rải đều trong tuần.\n")
                append("5. Phần 'subtitle' của từng sự kiện phải ghi rõ nội dung/mục tiêu của buổi học đó (ví dụ: 'Ôn tập lý thuyết', 'Làm bài tập chương 2', 'Luyện đề trắc nghiệm', dựa theo ghi chú của môn).\n")
                append("6. Thêm các buổi nghỉ ngơi/giải lao ngắn (isBreak = true, category = 'PLAY').\n")
                append("7. Chỉ xếp lịch vào các buổi/khung giờ người dùng rảnh (${selectedTimeSlots.joinToString(", ")}).\n")
                append("8. Bắt buộc xuất kết quả dưới dạng JSON Array duy nhất theo mẫu sau, không bọc văn bản thừa:\n")
                append("[\n")
                append("  {\n")
                append("    \"dayIndex\": 0,\n")
                append("    \"time\": \"08:30\",\n")
                append("    \"title\": \"Tên môn học\",\n")
                append("    \"subtitle\": \"Nội dung buổi học cụ thể\",\n")
                append("    \"isBreak\": false,\n")
                append("    \"category\": \"STUDY\"\n")
                append("  }\n")
                append("]\n")
                append("Ghi chú: dayIndex là số nguyên từ 0 (Thứ 2) đến 6 (Chủ Nhật). time là chuỗi 'HH:mm'.")
            }

            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        role = "user",
                        parts = listOf(GeminiPart(text = prompt))
                    )
                ),
                generationConfig = GeminiGenerationConfig(
                    temperature = 0.4f,
                    topP = 0.95f,
                    topK = 40
                )
            )

            val response = geminiService.generateContent(
                model = GeminiApiService.MODEL_GEMINI_DEFAULT,
                apiKey = apiKey,
                request = request
            )

            val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            Log.d("AIPlanner", "Gemini response: $responseText")

            val parsedPlan = parseGeminiPlanJson(responseText, taskList)
            if (parsedPlan.isNotEmpty()) {
                // Post-process with deterministic conflict avoidance against existing calendar events
                return resolveAndAvoidConflicts(parsedPlan, existingEvents, todayStartOfWeek, selectedTimeSlots)
            }
        } catch (e: Exception) {
            Log.e("AIPlanner", "Error calling Gemini for study plan", e)
        }
    }

    // Fallback: Intelligent rule-based scheduler based on exact user tasks, priority, deadline, and free slots
    return generateFallbackPlan(taskList, selectedTimeSlots, existingEvents, todayStartOfWeek)
}

private fun parseGeminiPlanJson(
    jsonText: String,
    taskList: List<StudyTaskItem>
): Map<Int, List<PlanTimelineItem>> {
    val result = mutableMapOf<Int, MutableList<PlanTimelineItem>>()

    try {
        // Extract json array substring
        val startIdx = jsonText.indexOf('[')
        val endIdx = jsonText.lastIndexOf(']')
        if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
            val jsonArrayStr = jsonText.substring(startIdx, endIdx + 1)
            val jsonArray = JSONArray(jsonArrayStr)

            // Map task titles to colors
            val taskColorMap = taskList.associate { it.title.trim().lowercase() to it.accentColor }

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val dayIndex = obj.optInt("dayIndex", 0).coerceIn(0, 6)
                val time = obj.optString("time", "08:00")
                val title = obj.optString("title", "Môn học")
                val subtitle = obj.optString("subtitle", "")
                val isBreak = obj.optBoolean("isBreak", false)
                val categoryStr = obj.optString("category", "STUDY")

                val accentColor = if (isBreak) {
                    Color(0xFF94A3B8)
                } else {
                    taskColorMap[title.trim().lowercase()]
                        ?: taskList.find { title.contains(it.title, ignoreCase = true) }?.accentColor
                        ?: when {
                            title.contains("Machine", ignoreCase = true) || title.contains("AI", ignoreCase = true) -> Color(0xFF8B5CF6)
                            title.contains("Deep", ignoreCase = true) -> Color(0xFF06B6D4)
                            else -> Color(0xFF3B82F6)
                        }
                }

                val item = PlanTimelineItem(
                    id = UUID.randomUUID().toString(),
                    time = time,
                    title = title,
                    subtitle = subtitle,
                    accentColor = accentColor,
                    category = if (isBreak) EventCategory.PLAY else EventCategory.STUDY,
                    isBreak = isBreak
                )

                result.getOrPut(dayIndex) { mutableListOf() }.add(item)
            }

            // Sort each day by time
            return result.mapValues { (_, items) -> items.sortedBy { it.time } }
        }
    } catch (e: Exception) {
        Log.e("AIPlanner", "Failed to parse Gemini json", e)
    }

    return emptyMap()
}

/**
 * Checks all generated timeline items against existing calendar events for the week,
 * shifting or reassigning items to non-conflicting free time slots to prevent any overlap.
 */
private fun resolveAndAvoidConflicts(
    rawPlan: Map<Int, List<PlanTimelineItem>>,
    existingEvents: List<Event>,
    todayStartOfWeek: LocalDate,
    selectedTimeSlots: Set<String>
): Map<Int, List<PlanTimelineItem>> {
    val result = mutableMapOf<Int, MutableList<PlanTimelineItem>>()

    val hasMorning = selectedTimeSlots.contains("Sáng")
    val hasAfternoon = selectedTimeSlots.contains("Chiều")
    val hasEvening = selectedTimeSlots.contains("Tối")
    val hasWeekend = selectedTimeSlots.contains("Cuối tuần")

    val allowedDays = if (hasWeekend) (0..6).toSet() else (0..4).toSet()

    // Flatten all items to schedule
    val itemsToPlace = mutableListOf<Pair<Int, PlanTimelineItem>>()
    rawPlan.forEach { (dayIdx, items) ->
        items.forEach { item ->
            itemsToPlace.add(Pair(dayIdx, item))
        }
    }

    val placedRangesByDay = mutableMapOf<Int, MutableList<Pair<LocalTime, LocalTime>>>()

    for ((preferredDayIdx, item) in itemsToPlace) {
        val durationMins = if (item.isBreak) 20 else 90

        // Preferred day first, then other allowed days
        val dayCandidates = listOf(preferredDayIdx).filter { allowedDays.contains(it) } +
                allowedDays.filter { it != preferredDayIdx }

        var placed = false

        for (dayIdx in dayCandidates) {
            val date = todayStartOfWeek.plusDays(dayIdx.toLong())
            val existingOnDay = existingEvents.filter { it.startTime.toLocalDate() == date }
            val placedRanges = placedRangesByDay.getOrPut(dayIdx) { mutableListOf() }

            // If this is the preferred day, check if the original item.time works without conflict
            val origTime = parseLocalTime(item.time)
            if (dayIdx == preferredDayIdx && origTime != null) {
                val origEnd = origTime.plusMinutes(durationMins.toLong())
                val hasConflictWithExisting = existingOnDay.any { ev ->
                    origTime.isBefore(ev.endTime.toLocalTime()) && origEnd.isAfter(ev.startTime.toLocalTime())
                }
                val hasConflictWithPlaced = placedRanges.any { (pStart, pEnd) ->
                    origTime.isBefore(pEnd) && origEnd.isAfter(pStart)
                }

                if (!hasConflictWithExisting && !hasConflictWithPlaced) {
                    placedRanges.add(Pair(origTime, origEnd))
                    result.getOrPut(dayIdx) { mutableListOf() }.add(item)
                    placed = true
                    break
                }
            }

            // Find a free slot candidate on dayIdx that avoids existing events and already placed items
            val candidateTimes = getCandidateStartTimes(hasMorning, hasAfternoon, hasEvening)
            for (cTime in candidateTimes) {
                val cEnd = cTime.plusMinutes(durationMins.toLong())
                val hasConflictWithExisting = existingOnDay.any { ev ->
                    cTime.isBefore(ev.endTime.toLocalTime()) && cEnd.isAfter(ev.startTime.toLocalTime())
                }
                val hasConflictWithPlaced = placedRanges.any { (pStart, pEnd) ->
                    cTime.isBefore(pEnd) && cEnd.isAfter(pStart)
                }

                if (!hasConflictWithExisting && !hasConflictWithPlaced) {
                    val formattedTime = String.format("%02d:%02d", cTime.hour, cTime.minute)
                    placedRanges.add(Pair(cTime, cEnd))
                    result.getOrPut(dayIdx) { mutableListOf() }.add(item.copy(time = formattedTime))
                    placed = true
                    break
                }
            }

            if (placed) break
        }

        // If no non-conflicting slot was found, place into preferred day to not drop the task
        if (!placed) {
            result.getOrPut(preferredDayIdx) { mutableListOf() }.add(item)
        }
    }

    return result.mapValues { (_, items) -> items.sortedBy { it.time } }
}

private fun parseLocalTime(timeStr: String): LocalTime? {
    return try {
        val parts = timeStr.trim().split(":")
        val h = parts[0].toInt()
        val m = if (parts.size > 1) parts[1].toInt() else 0
        LocalTime.of(h, m)
    } catch (e: Exception) {
        null
    }
}

private fun getCandidateStartTimes(
    hasMorning: Boolean,
    hasAfternoon: Boolean,
    hasEvening: Boolean
): List<LocalTime> {
    val list = mutableListOf<LocalTime>()
    if (hasMorning) {
        list.add(LocalTime.of(7, 30))
        list.add(LocalTime.of(8, 0))
        list.add(LocalTime.of(8, 30))
        list.add(LocalTime.of(9, 0))
        list.add(LocalTime.of(9, 30))
        list.add(LocalTime.of(10, 0))
        list.add(LocalTime.of(10, 30))
    }
    if (hasAfternoon) {
        list.add(LocalTime.of(13, 30))
        list.add(LocalTime.of(14, 0))
        list.add(LocalTime.of(14, 30))
        list.add(LocalTime.of(15, 0))
        list.add(LocalTime.of(15, 30))
        list.add(LocalTime.of(16, 0))
    }
    if (hasEvening) {
        list.add(LocalTime.of(18, 30))
        list.add(LocalTime.of(19, 0))
        list.add(LocalTime.of(19, 30))
        list.add(LocalTime.of(20, 0))
        list.add(LocalTime.of(20, 30))
        list.add(LocalTime.of(21, 0))
    }
    return list
}

private fun generateFallbackPlan(
    taskList: List<StudyTaskItem>,
    selectedTimeSlots: Set<String>,
    existingEvents: List<Event>,
    todayStartOfWeek: LocalDate
): Map<Int, List<PlanTimelineItem>> {
    if (taskList.isEmpty()) return emptyMap()

    val result = mutableMapOf<Int, MutableList<PlanTimelineItem>>()

    // Sort tasks by priority (HARD first, then MEDIUM, then EASY)
    val sortedTasks = taskList.sortedByDescending {
        when (it.difficulty) {
            TaskDifficulty.HARD -> 3
            TaskDifficulty.MEDIUM -> 2
            TaskDifficulty.EASY -> 1
        }
    }

    val hasMorning = selectedTimeSlots.contains("Sáng")
    val hasAfternoon = selectedTimeSlots.contains("Chiều")
    val hasEvening = selectedTimeSlots.contains("Tối")
    val hasWeekend = selectedTimeSlots.contains("Cuối tuần")

    val daysToSchedule = if (hasWeekend) (0..6).toList() else (0..4).toList()

    var taskPointer = 0
    val placedRangesByDay = mutableMapOf<Int, MutableList<Pair<LocalTime, LocalTime>>>()

    daysToSchedule.forEach { dayIdx ->
        val date = todayStartOfWeek.plusDays(dayIdx.toLong())
        val existingOnDay = existingEvents.filter { it.startTime.toLocalDate() == date }
        val placedRanges = placedRangesByDay.getOrPut(dayIdx) { mutableListOf() }
        val dayItems = mutableListOf<PlanTimelineItem>()

        fun findFreeSlot(preferredTimes: List<LocalTime>, durationMins: Int): LocalTime? {
            for (pTime in preferredTimes) {
                val pEnd = pTime.plusMinutes(durationMins.toLong())
                val conflictWithExisting = existingOnDay.any { ev ->
                    pTime.isBefore(ev.endTime.toLocalTime()) && pEnd.isAfter(ev.startTime.toLocalTime())
                }
                val conflictWithPlaced = placedRanges.any { (s, e) ->
                    pTime.isBefore(e) && pEnd.isAfter(s)
                }
                if (!conflictWithExisting && !conflictWithPlaced) {
                    return pTime
                }
            }
            return null
        }

        if (hasMorning) {
            val task = sortedTasks[taskPointer % sortedTasks.size]
            taskPointer++
            val subTopic = if (task.subtitle.isNotBlank()) task.subtitle else "Học phần trọng tâm"

            val morningCandidates = listOf(
                LocalTime.of(8, 30),
                LocalTime.of(8, 0),
                LocalTime.of(9, 0),
                LocalTime.of(7, 30),
                LocalTime.of(9, 30),
                LocalTime.of(10, 0)
            )
            val freeStart = findFreeSlot(morningCandidates, 90)
            if (freeStart != null) {
                val freeEnd = freeStart.plusMinutes(90)
                placedRanges.add(Pair(freeStart, freeEnd))
                dayItems.add(
                    PlanTimelineItem(
                        time = String.format("%02d:%02d", freeStart.hour, freeStart.minute),
                        title = task.title,
                        subtitle = subTopic,
                        accentColor = task.accentColor,
                        category = EventCategory.STUDY
                    )
                )

                // Add short break 5-15 mins after study if not overlapping
                val breakStart = freeEnd.plusMinutes(5)
                val breakEnd = breakStart.plusMinutes(15)
                val breakConflictWithExisting = existingOnDay.any { ev ->
                    breakStart.isBefore(ev.endTime.toLocalTime()) && breakEnd.isAfter(ev.startTime.toLocalTime())
                }
                if (!breakConflictWithExisting && breakStart.isBefore(LocalTime.of(12, 0))) {
                    placedRanges.add(Pair(breakStart, breakEnd))
                    dayItems.add(
                        PlanTimelineItem(
                            time = String.format("%02d:%02d", breakStart.hour, breakStart.minute),
                            title = "Nghỉ giải lao",
                            subtitle = "Thư giãn 15 phút",
                            accentColor = Color(0xFF94A3B8),
                            category = EventCategory.PLAY,
                            isBreak = true
                        )
                    )
                }
            }
        }

        if (hasAfternoon && (dayIdx % 2 == 0 || !hasMorning)) {
            val task = sortedTasks[taskPointer % sortedTasks.size]
            taskPointer++
            val subTopic = if (task.subtitle.isNotBlank()) "Thực hành: ${task.subtitle}" else "Làm bài tập & thực hành"

            val afternoonCandidates = listOf(
                LocalTime.of(14, 0),
                LocalTime.of(13, 30),
                LocalTime.of(14, 30),
                LocalTime.of(15, 0),
                LocalTime.of(15, 30),
                LocalTime.of(16, 0)
            )
            val freeStart = findFreeSlot(afternoonCandidates, 90)
            if (freeStart != null) {
                placedRanges.add(Pair(freeStart, freeStart.plusMinutes(90)))
                dayItems.add(
                    PlanTimelineItem(
                        time = String.format("%02d:%02d", freeStart.hour, freeStart.minute),
                        title = task.title,
                        subtitle = subTopic,
                        accentColor = task.accentColor,
                        category = EventCategory.STUDY
                    )
                )
            }
        }

        if (hasEvening) {
            val task = sortedTasks[taskPointer % sortedTasks.size]
            taskPointer++
            val subTopic = if (task.deadline.isNotBlank()) "Ôn tập theo hạn chót (${task.deadline})" else "Ôn tập & tổng kết"

            val eveningCandidates = listOf(
                LocalTime.of(19, 30),
                LocalTime.of(19, 0),
                LocalTime.of(20, 0),
                LocalTime.of(18, 30),
                LocalTime.of(20, 30),
                LocalTime.of(21, 0)
            )
            val freeStart = findFreeSlot(eveningCandidates, 90)
            if (freeStart != null) {
                placedRanges.add(Pair(freeStart, freeStart.plusMinutes(90)))
                dayItems.add(
                    PlanTimelineItem(
                        time = String.format("%02d:%02d", freeStart.hour, freeStart.minute),
                        title = task.title,
                        subtitle = subTopic,
                        accentColor = task.accentColor,
                        category = EventCategory.STUDY
                    )
                )
            }
        }

        if (dayItems.isNotEmpty()) {
            result[dayIdx] = dayItems.sortedBy { it.time }.toMutableList()
        }
    }

    return result
}
