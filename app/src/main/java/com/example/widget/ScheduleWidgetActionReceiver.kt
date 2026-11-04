package com.example.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.MainActivity
import com.example.MyApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ScheduleWidgetActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TOGGLE_TASK = "com.example.widget.ACTION_TOGGLE_TASK"
        const val ACTION_ADD_EVENT = "com.example.widget.ACTION_ADD_EVENT"
        const val ACTION_VIEW_EVENT = "com.example.widget.ACTION_VIEW_EVENT"
        const val EXTRA_EVENT_ID = "extra_event_id"
        const val EXTRA_CURRENT_STATUS = "extra_current_status"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_TOGGLE_TASK -> {
                val eventId = intent.getStringExtra(EXTRA_EVENT_ID) ?: ""
                if (eventId.isNotBlank() && !eventId.startsWith("holiday_")) {
                    val app = context.applicationContext as MyApplication
                    val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                    CoroutineScope(Dispatchers.IO).launch {
                        app.repository.toggleEventCompletion(eventId, todayStr)
                        WidgetUpdateHelper.updateAllWidgets(context)
                    }
                }
            }
            ACTION_ADD_EVENT -> {
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("NAVIGATE_TO", "add_event")
                }
                context.startActivity(launchIntent)
            }
            ACTION_VIEW_EVENT -> {
                val eventId = intent.getStringExtra(EXTRA_EVENT_ID) ?: ""
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    if (eventId.isNotBlank() && !eventId.startsWith("holiday_")) {
                        putExtra("EDIT_EVENT_ID", eventId)
                    }
                }
                context.startActivity(launchIntent)
            }
        }
    }
}
