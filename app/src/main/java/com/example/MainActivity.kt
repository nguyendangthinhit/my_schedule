package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.navigation.AppNavigation
import com.example.ui.theme.MyScheduleTheme
import com.example.util.ThemeMode
import com.example.viewmodel.ScheduleViewModel
import com.example.viewmodel.ScheduleViewModelFactory

class MainActivity : ComponentActivity() {
  private var navigateToExtra by androidx.compose.runtime.mutableStateOf<String?>(null)
  private var editEventIdExtra by androidx.compose.runtime.mutableStateOf<Int?>(null)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    handleIntent(intent)
    enableEdgeToEdge()
    setContent {
      val repository = (applicationContext as MyApplication).repository
      val scheduleViewModel: ScheduleViewModel = viewModel(factory = ScheduleViewModelFactory(repository))

      LaunchedEffect(Unit) {
        scheduleViewModel.initThemeSettings(this@MainActivity)
        scheduleViewModel.initNotificationSettings(this@MainActivity)
      }

      val themeMode by scheduleViewModel.themeMode.collectAsState()
      val isDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
      }

      MyScheduleTheme(darkTheme = isDark) {
        AppNavigation(
          viewModel = scheduleViewModel,
          navigateTo = navigateToExtra,
          editEventId = editEventIdExtra
        )
      }
    }
  }

  override fun onNewIntent(intent: android.content.Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleIntent(intent)
  }

  private fun handleIntent(intent: android.content.Intent?) {
    if (intent != null) {
      val nav = intent.getStringExtra("NAVIGATE_TO")
      val action = intent.getStringExtra("action")
      val eventId = if (intent.hasExtra("EDIT_EVENT_ID")) {
        intent.getIntExtra("EDIT_EVENT_ID", -1)
      } else {
        intent.getIntExtra("event_id", -1)
      }

      if (action == "ADD_EVENT" || nav == "add_event") {
        navigateToExtra = "add_event"
      } else {
        navigateToExtra = nav
      }

      editEventIdExtra = if (eventId != -1) eventId else null
    }
  }
}

