package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Today
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
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.theme.PrimaryPurple
import com.example.util.NotificationHelper
import com.example.viewmodel.ScheduleViewModel

sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    object Home : BottomNavItem("home", "Việc cần làm", Icons.Default.Today)
    object Calendar : BottomNavItem("calendar", "Lịch trình", Icons.Default.DateRange)
    object AI : BottomNavItem("ai", "AI", Icons.Default.SmartToy)
    object Stats : BottomNavItem("stats", "Thống kê", Icons.Default.BarChart)
    object Settings : BottomNavItem("settings", "Cài đặt", Icons.Default.Settings)
}

@Composable
fun MainScreen(
    viewModel: ScheduleViewModel,
    onNavigateToAddEvent: () -> Unit,
    onNavigateToEditEvent: (Int) -> Unit
) {
    val context = LocalContext.current
    val navController = rememberNavController()

    // Initialize notification settings
    LaunchedEffect(Unit) {
        viewModel.initNotificationSettings(context)
    }

    var showPermissionDialog by remember {
        mutableStateOf(NotificationHelper.isFirstLaunch(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        NotificationHelper.setFirstLaunch(context, false)
        if (isGranted) {
            viewModel.setNotificationsEnabled(context, true)
            viewModel.setSoundEnabled(context, true)
            NotificationHelper.showNotification(
                context = context,
                id = 1001,
                title = "🔔 Đã bật thông báo sự kiện",
                message = "Bạn sẽ nhận được thông báo kèm âm thanh nhắc nhở trước khi diễn ra sự kiện.",
                isTest = true
            )
        } else {
            viewModel.setNotificationsEnabled(context, false)
            viewModel.setSoundEnabled(context, false)
        }
        showPermissionDialog = false
    }

    Scaffold(
        bottomBar = { BottomNavigationBar(navController) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToAddEvent = onNavigateToAddEvent,
                    onNavigateToEditEvent = onNavigateToEditEvent
                )
            }
            composable(BottomNavItem.Calendar.route) {
                CalendarScreen(
                    viewModel = viewModel,
                    onNavigateToAddEvent = onNavigateToAddEvent,
                    onNavigateToEditEvent = onNavigateToEditEvent
                )
            }
            composable(BottomNavItem.AI.route) {
                AIScreen(scheduleViewModel = viewModel)
            }
            composable(BottomNavItem.Stats.route) {
                StatsScreen(viewModel = viewModel)
            }
            composable(BottomNavItem.Settings.route) {
                SettingsScreen(viewModel = viewModel)
            }
        }
    }

    // Dialog yêu cầu quyền thông báo khi mở app lần đầu
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = {
                // Khi dismiss ra ngoài
                NotificationHelper.setFirstLaunch(context, false)
                viewModel.setNotificationsEnabled(context, false)
                viewModel.setSoundEnabled(context, false)
                showPermissionDialog = false
            },
            icon = {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Bật thông báo & nhắc nhở",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Ứng dụng cần quyền thông báo để nhắc nhở các lịch trình, sự kiện quan trọng đúng giờ và phát âm thanh chuông báo cho bạn.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Thông báo đẩy trên thanh trạng thái", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Âm thanh chuông báo nhắc hẹn", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Bạn có thể tùy ý bật/tắt lại bất kỳ lúc nào trong phần Cài đặt.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED

                            if (hasPermission) {
                                NotificationHelper.setFirstLaunch(context, false)
                                viewModel.setNotificationsEnabled(context, true)
                                viewModel.setSoundEnabled(context, true)
                                NotificationHelper.showNotification(
                                    context = context,
                                    id = 1001,
                                    title = "🔔 Đã bật thông báo sự kiện",
                                    message = "Bạn sẽ nhận được thông báo kèm âm thanh nhắc nhở trước khi diễn ra sự kiện.",
                                    isTest = true
                                )
                                showPermissionDialog = false
                            } else {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        } else {
                            NotificationHelper.setFirstLaunch(context, false)
                            viewModel.setNotificationsEnabled(context, true)
                            viewModel.setSoundEnabled(context, true)
                            NotificationHelper.showNotification(
                                context = context,
                                id = 1001,
                                title = "🔔 Đã bật thông báo sự kiện",
                                message = "Bạn sẽ nhận được thông báo kèm âm thanh nhắc nhở trước khi diễn ra sự kiện.",
                                isTest = true
                            )
                            showPermissionDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("btn_accept_notification_permission")
                ) {
                    Text("Đồng ý & Bật thông báo", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        NotificationHelper.setFirstLaunch(context, false)
                        viewModel.setNotificationsEnabled(context, false)
                        viewModel.setSoundEnabled(context, false)
                        showPermissionDialog = false
                    },
                    modifier = Modifier.testTag("btn_deny_notification_permission")
                ) {
                    Text("Không đồng ý", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Calendar,
        BottomNavItem.AI,
        BottomNavItem.Stats,
        BottomNavItem.Settings
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        items.forEach { item ->
            val isSelected = currentRoute == item.route
            NavigationBarItem(
                icon = {
                    Icon(
                        item.icon,
                        contentDescription = item.title,
                        modifier = Modifier.testTag("nav_${item.route}")
                    )
                },
                label = {
                    Text(
                        text = item.title,
                        fontSize = 9.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false
                    )
                },
                selected = isSelected,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ),
                onClick = {
                    navController.navigate(item.route) {
                        navController.graph.startDestinationRoute?.let { route ->
                            popUpTo(route) {
                                saveState = true
                            }
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}
