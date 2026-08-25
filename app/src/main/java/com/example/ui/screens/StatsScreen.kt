package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.DashboardRepository
import com.example.viewmodel.AIWeeklyReviewViewModel
import com.example.viewmodel.AIWeeklyReviewViewModelFactory
import com.example.viewmodel.DashboardViewModel
import com.example.viewmodel.DashboardViewModelFactory
import com.example.viewmodel.ScheduleViewModel

private enum class DashboardViewType {
    OVERVIEW,
    CATEGORY_DETAIL,
    AI_WEEKLY_REVIEW
}

@Composable
fun StatsScreen(
    viewModel: ScheduleViewModel,
    dashboardViewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModelFactory(
            DashboardRepository(
                context = LocalContext.current.applicationContext,
                scheduleDao = AppDatabase.getDatabase(LocalContext.current.applicationContext).scheduleDao()
            )
        )
    )
) {
    val context = LocalContext.current.applicationContext
    val reviewViewModel: AIWeeklyReviewViewModel = viewModel(
        factory = AIWeeklyReviewViewModelFactory(
            DashboardRepository(
                context = context,
                scheduleDao = AppDatabase.getDatabase(context).scheduleDao()
            )
        )
    )

    var currentView by remember { mutableStateOf(DashboardViewType.OVERVIEW) }

    // Intercept hardware back button when viewing category details or AI review
    BackHandler(enabled = currentView != DashboardViewType.OVERVIEW) {
        currentView = DashboardViewType.OVERVIEW
    }

    AnimatedContent(
        targetState = currentView,
        transitionSpec = {
            if (targetState == DashboardViewType.CATEGORY_DETAIL || targetState == DashboardViewType.AI_WEEKLY_REVIEW) {
                slideInHorizontally { width -> width } + fadeIn() togetherWith
                        slideOutHorizontally { width -> -width } + fadeOut()
            } else {
                slideInHorizontally { width -> -width } + fadeIn() togetherWith
                        slideOutHorizontally { width -> width } + fadeOut()
            }
        },
        label = "DashboardScreenTransition",
        modifier = Modifier.fillMaxSize()
    ) { targetView ->
        when (targetView) {
            DashboardViewType.OVERVIEW -> {
                DashboardOverviewScreen(
                    viewModel = dashboardViewModel,
                    onNavigateToCategoryDetail = { category ->
                        dashboardViewModel.selectCategory(category)
                        currentView = DashboardViewType.CATEGORY_DETAIL
                    },
                    onNavigateToAIWeeklyReview = {
                        reviewViewModel.loadWeeklyReview()
                        currentView = DashboardViewType.AI_WEEKLY_REVIEW
                    }
                )
            }
            DashboardViewType.CATEGORY_DETAIL -> {
                CategoryDetailScreen(
                    viewModel = dashboardViewModel,
                    onBack = {
                        currentView = DashboardViewType.OVERVIEW
                    }
                )
            }
            DashboardViewType.AI_WEEKLY_REVIEW -> {
                AIWeeklyReviewScreen(
                    reviewViewModel = reviewViewModel,
                    scheduleViewModel = viewModel,
                    onBack = {
                        currentView = DashboardViewType.OVERVIEW
                    }
                )
            }
        }
    }
}

