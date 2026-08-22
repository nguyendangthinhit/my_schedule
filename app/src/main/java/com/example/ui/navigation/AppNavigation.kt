package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.AddEventScreen
import com.example.ui.screens.MainScreen
import com.example.ui.screens.WelcomeScreen
import com.example.viewmodel.ScheduleViewModel

@Composable
fun AppNavigation(
    scheduleViewModel: ScheduleViewModel = viewModel()
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "welcome") {
        composable("welcome") {
            WelcomeScreen(
                onStart = {
                    navController.navigate("main") {
                        popUpTo("welcome") { inclusive = true }
                    }
                }
            )
        }
        composable("main") {
            MainScreen(
                viewModel = scheduleViewModel,
                onNavigateToAddEvent = {
                    navController.navigate("add_event")
                }
            )
        }
        composable("add_event") {
            AddEventScreen(
                viewModel = scheduleViewModel,
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
