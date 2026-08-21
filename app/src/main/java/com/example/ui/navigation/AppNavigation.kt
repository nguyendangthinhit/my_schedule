package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.AddEventScreen
import com.example.ui.screens.MainScreen
import com.example.ui.screens.WelcomeScreen

@Composable
fun AppNavigation() {
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
                onNavigateToAddEvent = {
                    navController.navigate("add_event")
                }
            )
        }
        composable("add_event") {
            AddEventScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
