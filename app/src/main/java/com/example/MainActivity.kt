package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.navigation.AppNavigation
import com.example.ui.theme.MyScheduleTheme
import com.example.util.ThemeMode
import com.example.viewmodel.ScheduleViewModel
import com.example.viewmodel.ScheduleViewModelFactory

class MainActivity : ComponentActivity() {
  private var navigateToExtra by androidx.compose.runtime.mutableStateOf<String?>(null)
  private var editEventIdExtra by androidx.compose.runtime.mutableStateOf<Int?>(null)
  private var navigationTrigger by androidx.compose.runtime.mutableStateOf(0L)

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
        scheduleViewModel.initWeatherSettings(this@MainActivity)
      }

      val themeMode by scheduleViewModel.themeMode.collectAsState()
      val isDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
      }

      MyScheduleTheme(darkTheme = isDark) {
        val focusManager = LocalFocusManager.current
        Box(
          modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
              detectTapGestures(
                onTap = {
                  focusManager.clearFocus()
                }
              )
            }
        ) {
          AppNavigation(
            viewModel = scheduleViewModel,
            navigateTo = navigateToExtra,
            editEventId = editEventIdExtra,
            navigationTrigger = navigationTrigger
          )
        }
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
      val action = intent.getStringExtra("action") ?: intent.action
      val dataStr = intent.data?.toString() ?: ""
      val eventId = if (intent.hasExtra("EDIT_EVENT_ID")) {
        intent.getIntExtra("EDIT_EVENT_ID", -1)
      } else {
        intent.getIntExtra("event_id", -1)
      }

      if (action == "ADD_EVENT" || action == "com.example.action.ADD_EVENT" || nav == "add_event" || dataStr.contains("/add")) {
        navigateToExtra = "add_event"
        editEventIdExtra = null
      } else if (action == "OPEN_HOME" || action == "com.example.action.NAVIGATE_HOME" || nav == "home" || dataStr.contains("/home")) {
        navigateToExtra = "home"
        editEventIdExtra = null
      } else if (eventId != -1) {
        editEventIdExtra = eventId
        navigateToExtra = null
      } else {
        navigateToExtra = "home"
        editEventIdExtra = null
      }
      navigationTrigger = System.currentTimeMillis()
    }
  }
}

