package com.example.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.theme.PrimaryPurple

sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    object Home : BottomNavItem("home", "Việc trong ngày", Icons.Default.Home)
    object Calendar : BottomNavItem("calendar", "Lịch hoạt động", Icons.Default.DateRange)
    object AI : BottomNavItem("ai", "AI", Icons.Default.SmartToy)
    object Stats : BottomNavItem("stats", "Thống kê", Icons.Default.BarChart)
    object Settings : BottomNavItem("settings", "Cài đặt", Icons.Default.Settings)
}

@Composable
fun MainScreen(onNavigateToAddEvent: () -> Unit) {
    val navController = rememberNavController()
    
    Scaffold(
        bottomBar = { BottomNavigationBar(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Home.route) {
                HomeScreen()
            }
            composable(BottomNavItem.Calendar.route) {
                CalendarScreen(onNavigateToAddEvent = onNavigateToAddEvent)
            }
            composable(BottomNavItem.AI.route) {
                // Placeholder
                Text("AI Screen (Coming soon)")
            }
            composable(BottomNavItem.Stats.route) {
                // Placeholder
                Text("Stats Screen (Coming soon)")
            }
            composable(BottomNavItem.Settings.route) {
                // Placeholder
                Text("Settings Screen (Coming soon)")
            }
        }
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
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) },
                selected = currentRoute == item.route,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PrimaryPurple,
                    selectedTextColor = PrimaryPurple,
                    indicatorColor = PrimaryPurple.copy(alpha = 0.1f)
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
