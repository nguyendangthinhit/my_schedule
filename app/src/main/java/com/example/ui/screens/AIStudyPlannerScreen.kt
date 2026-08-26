package com.example.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.models.Event
import com.example.models.EventCategory
import com.example.viewmodel.ScheduleViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import java.util.UUID

enum class StudyPlannerStep {
    FORM,
    ADJUSTING,
    RESULT
}

enum class TaskDifficulty(val title: String, val color: Color, val bgColor: Color) {
    EASY("Dễ", Color(0xFF10B981), Color(0xFFECFDF5)),
    MEDIUM("Trung bình", Color(0xFFF59E0B), Color(0xFFFEF3C7)),
    HARD("Khó", Color(0xFFEF4444), Color(0xFFFEE2E2))
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
    var currentStep by remember { mutableStateOf(StudyPlannerStep.FORM) }

    // Form inputs state
    var studyGoal by remember {
        mutableStateOf("Ôn thi cuối kỳ môn AI, hoàn thành 3 bài tập lớn và củng cố cấu trúc dữ liệu")
    }

    var taskList by remember {
        mutableStateOf(
            listOf(
                StudyTaskItem(
                    title = "Machine Learning",
                    subtitle = "Ôn thi cuối kỳ",
                    deadline = "10/09/2026",
                    hours = 20,
                    difficulty = TaskDifficulty.HARD,
                    accentColor = Color(0xFF8B5CF6)
                ),
                StudyTaskItem(
                    title = "Deep Learning Project",
                    subtitle = "Bài tập lớn",
                    deadline = "18/09/2026",
                    hours = 15,
                    difficulty = TaskDifficulty.MEDIUM,
                    accentColor = Color(0xFF06B6D4)
                ),
                StudyTaskItem(
                    title = "Data Structures",
                    subtitle = "Ôn tập",
                    deadline = "01/09/2026",
                    hours = 10,
                    difficulty = TaskDifficulty.EASY,
                    accentColor = Color(0xFF10B981)
                )
            )
        )
    }

    var selectedTimeSlots by remember {
        mutableStateOf(setOf("Sáng", "Tối", "Cuối tuần"))
    }

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<StudyTaskItem?>(null) }
    var taskToDelete by remember { mutableStateOf<StudyTaskItem?>(null) }

    // Intercept back button when inside inner steps
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
                            Toast.makeText(context, "Vui lòng thêm ít nhất 1 môn / nhiệm vụ", Toast.LENGTH_SHORT).show()
                        } else {
                            currentStep = StudyPlannerStep.ADJUSTING
                        }
                    },
                    onBack = onBack
                )
            }

            StudyPlannerStep.ADJUSTING -> {
                AIStudyPlannerAdjustingView(
                    onBack = { currentStep = StudyPlannerStep.FORM },
                    onCompleted = { currentStep = StudyPlannerStep.RESULT }
                )
            }

            StudyPlannerStep.RESULT -> {
                AIStudyPlannerResultView(
                    onBack = { currentStep = StudyPlannerStep.FORM },
                    taskList = taskList,
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
// 1. SCREEN 1: AI Study Planner Form View
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
                        text = "AI Study Planner",
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
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Button(
                        onClick = onGeneratePlan,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(27.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2563EB)
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Tạo kế hoạch với AI",
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Mục tiêu học tập
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "1. Mục tiêu học tập",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = studyGoal,
                    onValueChange = onStudyGoalChange,
                    placeholder = {
                        Text(
                            text = "Mục tiêu của bạn là gì?\nVí dụ: Ôn thi cuối kỳ môn AI, hoàn thành 3 bài tập lớn, học thêm Python...",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 90.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = Color(0xFF3B82F6),
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
                    Text(
                        text = "2. Danh sách môn / nhiệm vụ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    FilledTonalButton(
                        onClick = onAddTaskClick,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Thêm",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            if (taskList.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
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
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Chưa có môn học nào được thêm",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = onAddTaskClick,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Thêm môn đầu tiên")
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
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            // 3. Thời gian rảnh
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "3. Thời gian rảnh",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Chọn thời gian bạn rảnh trong tuần",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

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
                            color = if (isSelected) Color(0xFFEFF6FF) else MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFF3B82F6) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
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
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) Color(0xFF1E40AF) else MaterialTheme.colorScheme.onSurface
                                )
                                Icon(
                                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (isSelected) Color(0xFF2563EB) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
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
                    Text(
                        text = item.subtitle,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Difficulty Badge
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
                        text = "Hạn: ${item.deadline}",
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
                        text = "${item.hours}h",
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
                    // Edit Button
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Chỉnh sửa",
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Delete Button
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
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
// 2. SCREEN 2: Điều chỉnh kế hoạch (AI Loading / Adjusting Screen)
// -------------------------------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AIStudyPlannerAdjustingView(
    onBack: () -> Unit,
    onCompleted: () -> Unit
) {
    var stepProgress by remember { mutableIntStateOf(1) }

    // Progressive animation timer
    LaunchedEffect(Unit) {
        delay(700)
        stepProgress = 2
        delay(800)
        stepProgress = 3
        delay(900)
        stepProgress = 4
        delay(600)
        onCompleted()
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
                        text = "Điều chỉnh kế hoạch",
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
                        .size(160.dp)
                        .offset(y = floatOffset.dp)
                        .scale(pulseScale),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer aura glow
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF8B5CF6).copy(alpha = 0.25f),
                                        Color(0xFF3B82F6).copy(alpha = 0.15f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // Robot head / body container
                    Surface(
                        modifier = Modifier.size(100.dp),
                        shape = RoundedCornerShape(32.dp),
                        color = Color.White,
                        shadowElevation = 10.dp,
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
                                    .size(width = 68.dp, height = 44.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color(0xFF0F172A)),
                                contentAlignment = Alignment.Center
                            ) {
                                // Glowing eyes
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(width = 12.dp, height = 16.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF38BDF8))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(width = 12.dp, height = 16.dp)
                                            .clip(RoundedCornerShape(6.dp))
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
                            .offset(x = (-10).dp, y = 10.dp)
                    )
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier
                            .size(20.dp)
                            .align(Alignment.BottomStart)
                            .offset(x = 10.dp, y = (-10).dp)
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "AI đang điều chỉnh kế hoạch",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "cho bạn...",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(36.dp))

                // Steps Progress List
                val steps = listOf(
                    "Phân tích tiến độ học tập",
                    "Đánh giá thời gian rảnh",
                    "Tối ưu lịch học",
                    "Cập nhật kế hoạch"
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
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
                                fontSize = 15.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                color = if (isDone || isCurrent) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
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
                                    color = Color(0xFF3B82F6)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), CircleShape)
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
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFF0F7FF)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "AI sẽ giúp bạn học hiệu quả hơn mỗi ngày!",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1E40AF)
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
    scheduleViewModel: ScheduleViewModel?,
    onApplied: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Tuần này, 1: Toàn bộ
    var selectedDayIndex by remember { mutableIntStateOf(1) } // 0: T2 (17) -> 1: T3 (18)

    val daysOfWeek = listOf(
        Pair("T2", 17),
        Pair("T3", 18),
        Pair("T4", 19),
        Pair("T5", 20),
        Pair("T6", 21),
        Pair("T7", 22),
        Pair("CN", 23)
    )

    // Schedule map per day index to allow editing, deleting, and adding events
    val scheduleByDay = remember {
        mutableStateMapOf<Int, List<PlanTimelineItem>>(
            0 to listOf(
                PlanTimelineItem(
                    time = "08:00",
                    title = "Machine Learning",
                    subtitle = "Lý thuyết Gradient Descent",
                    accentColor = Color(0xFF8B5CF6)
                ),
                PlanTimelineItem(
                    time = "13:30",
                    title = "Data Structures",
                    subtitle = "Cây nhị phân và đồ thị",
                    accentColor = Color(0xFF10B981)
                ),
                PlanTimelineItem(
                    time = "19:00",
                    title = "Deep Learning Project",
                    subtitle = "Chuẩn bị Dataset",
                    accentColor = Color(0xFF06B6D4)
                )
            ),
            1 to listOf(
                PlanTimelineItem(
                    time = "08:00",
                    title = "Đại số tuyến tính",
                    subtitle = "(Trên lớp)",
                    accentColor = Color(0xFF3B82F6)
                ),
                PlanTimelineItem(
                    time = "10:00",
                    title = "Nghỉ trưa",
                    subtitle = "",
                    accentColor = Color(0xFF94A3B8),
                    isBreak = true
                ),
                PlanTimelineItem(
                    time = "13:00",
                    title = "Data Structures",
                    subtitle = "Ôn tập chương 3",
                    accentColor = Color(0xFF10B981)
                ),
                PlanTimelineItem(
                    time = "19:00",
                    title = "Machine Learning",
                    subtitle = "Ôn thi cuối kỳ",
                    accentColor = Color(0xFF8B5CF6)
                ),
                PlanTimelineItem(
                    time = "21:30",
                    title = "Deep Learning Project",
                    subtitle = "Làm phần 2",
                    accentColor = Color(0xFF06B6D4)
                )
            ),
            2 to listOf(
                PlanTimelineItem(
                    time = "08:30",
                    title = "Machine Learning",
                    subtitle = "Thực hành PyTorch",
                    accentColor = Color(0xFF8B5CF6)
                ),
                PlanTimelineItem(
                    time = "14:00",
                    title = "Deep Learning Project",
                    subtitle = "Huấn luyện mô hình CNN",
                    accentColor = Color(0xFF06B6D4)
                ),
                PlanTimelineItem(
                    time = "19:30",
                    title = "Data Structures",
                    subtitle = "Luyện tập bài tập LeetCode",
                    accentColor = Color(0xFF10B981)
                )
            ),
            3 to listOf(
                PlanTimelineItem(
                    time = "09:00",
                    title = "Data Structures",
                    subtitle = "Ôn tập Đồ thị (Graph)",
                    accentColor = Color(0xFF10B981)
                ),
                PlanTimelineItem(
                    time = "14:00",
                    title = "Nghỉ ngơi & Thể thao",
                    subtitle = "Chạy bộ",
                    accentColor = Color(0xFF94A3B8),
                    isBreak = true
                ),
                PlanTimelineItem(
                    time = "19:00",
                    title = "Deep Learning Project",
                    subtitle = "Viết báo cáo kỹ thuật",
                    accentColor = Color(0xFF06B6D4)
                )
            ),
            4 to listOf(
                PlanTimelineItem(
                    time = "08:00",
                    title = "Machine Learning",
                    subtitle = "Ôn tập trắc nghiệm",
                    accentColor = Color(0xFF8B5CF6)
                ),
                PlanTimelineItem(
                    time = "15:00",
                    title = "Đại số tuyến tính",
                    subtitle = "Làm bài tập ma trận",
                    accentColor = Color(0xFF3B82F6)
                ),
                PlanTimelineItem(
                    time = "20:00",
                    title = "Tổng kết tuần",
                    subtitle = "Đánh giá tiến độ học",
                    accentColor = Color(0xFF10B981)
                )
            ),
            5 to listOf(
                PlanTimelineItem(
                    time = "09:00",
                    title = "Machine Learning",
                    subtitle = "Giải đề thi các năm trước",
                    accentColor = Color(0xFF8B5CF6)
                ),
                PlanTimelineItem(
                    time = "14:30",
                    title = "Deep Learning Project",
                    subtitle = "Kiểm thử mô hình",
                    accentColor = Color(0xFF06B6D4)
                )
            ),
            6 to listOf(
                PlanTimelineItem(
                    time = "09:30",
                    title = "Ôn tập tổng hợp",
                    subtitle = "Xem lại toàn bộ kiến thức",
                    accentColor = Color(0xFF3B82F6)
                ),
                PlanTimelineItem(
                    time = "15:00",
                    title = "Thư giãn & Chuẩn bị tuần mới",
                    subtitle = "",
                    accentColor = Color(0xFF94A3B8),
                    isBreak = true
                )
            )
        )
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
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Button(
                        onClick = {
                            if (scheduleViewModel != null) {
                                coroutineScope.launch {
                                    val now = LocalDate.now()
                                    val todayStartOfWeek = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

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
                                                    reminderNote = "Lịch học tự động tạo bởi AI Study Planner",
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
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
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
                    .padding(vertical = 6.dp),
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
                            color = if (selectedTab == 0) Color.White else Color.Transparent,
                            shadowElevation = if (selectedTab == 0) 2.dp else 0.dp
                        ) {
                            Text(
                                text = "Tuần này",
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 0) Color(0xFF1E293B) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { selectedTab = 1 },
                            shape = RoundedCornerShape(16.dp),
                            color = if (selectedTab == 1) Color.White else Color.Transparent,
                            shadowElevation = if (selectedTab == 1) 2.dp else 0.dp
                        ) {
                            Text(
                                text = "Toàn bộ",
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 1) Color(0xFF1E293B) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Week Range Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⟨  17 - 23 Tháng 8, 2026  ⟩",
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

            Spacer(modifier = Modifier.height(10.dp))

            // Days Strip: T2 (17) -> CN (23)
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
                            color = if (isSelected) Color(0xFF2563EB) else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color(0xFF2563EB) else Color.Transparent),
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
                            text = "Không có lịch trình trong ngày này",
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
                    contentPadding = PaddingValues(bottom = 20.dp)
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
                                tint = Color(0xFF2563EB),
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
    var deadline by remember { mutableStateOf(initialTask?.deadline ?: "15/09/2026") }
    var hours by remember { mutableStateOf(initialTask?.hours?.toString() ?: "10") }
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
                    label = { Text("Tên môn / nhiệm vụ") },
                    placeholder = { Text("VD: Machine Learning") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = subtitle,
                    onValueChange = { subtitle = it },
                    label = { Text("Ghi chú / mục tiêu") },
                    placeholder = { Text("VD: Ôn thi cuối kỳ") },
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
                    text = "Mức độ ưu tiên:",
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
                                fontSize = 12.sp,
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
                                subtitle = subtitle.trim().ifBlank { "Nhiệm vụ học tập" },
                                deadline = deadline.trim(),
                                hours = hours.toIntOrNull() ?: 10,
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
                                title.contains("Data", ignoreCase = true) -> Color(0xFF10B981)
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
