package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    editEventId: Int? = null,
    navigationTrigger: Long = 0L
) {
    val navController = rememberNavController()
    var mainScreenTab by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    var mainTabTrigger by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(0L) }

    androidx.compose.runtime.LaunchedEffect(navigateTo, editEventId, navigationTrigger) {
        if (navigateTo == "add_event") {
            navController.navigate("add_event")
        } else if (editEventId != null && editEventId != -1) {
            navController.navigate("edit_event/$editEventId")
        } else if (navigateTo == "home") {
            navController.popBackStack("main", inclusive = false)
            mainScreenTab = "home"
            mainTabTrigger = System.currentTimeMillis()
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
                targetTab = mainScreenTab,
                tabTrigger = mainTabTrigger,
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
