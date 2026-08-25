package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import com.example.MyApplication
import com.example.viewmodel.ScheduleViewModel
import com.example.viewmodel.ScheduleViewModelFactory
import com.example.ui.screens.AddEventScreen
import com.example.ui.screens.MainScreen
import com.example.ui.screens.WelcomeScreen

@Composable
fun AppNavigation(
    viewModel: ScheduleViewModel = run {
        val context = LocalContext.current
        val repository = (context.applicationContext as MyApplication).repository
        viewModel(factory = ScheduleViewModelFactory(repository))
    },
    navigateTo: String? = null,
    editEventId: Int? = null
) {
    val navController = rememberNavController()

    androidx.compose.runtime.LaunchedEffect(navigateTo, editEventId) {
        if (navigateTo == "add_event") {
            navController.navigate("add_event")
        } else if (editEventId != null && editEventId != -1) {
            navController.navigate("edit_event/$editEventId")
        }
    }

    NavHost(navController = navController, startDestination = "main") {
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
                viewModel = viewModel,
                onNavigateToAddEvent = {
                    navController.navigate("add_event")
                },
                onNavigateToEditEvent = { eventId ->
                    navController.navigate("edit_event/$eventId")
                }
            )
        }
        composable("add_event") {
            AddEventScreen(
                viewModel = viewModel,
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        composable(
            "edit_event/{eventId}",
            arguments = listOf(navArgument("eventId") { type = NavType.IntType })
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getInt("eventId")
            AddEventScreen(
                viewModel = viewModel,
                eventId = eventId,
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
