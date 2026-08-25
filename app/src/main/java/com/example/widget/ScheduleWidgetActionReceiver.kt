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
                val eventId = intent.getIntExtra(EXTRA_EVENT_ID, -1)
                if (eventId != -1 && eventId < 100000) {
                    val app = context.applicationContext as MyApplication
                    val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                    CoroutineScope(Dispatchers.IO).launch {
                        app.repository.toggleCompletion(eventId, todayStr)
                        // Also update single event isCompleted field directly if needed
                        val event = app.database.scheduleDao().getEventById(eventId)
                        if (event != null) {
                            app.database.scheduleDao().updateEvent(event.copy(isCompleted = !event.isCompleted))
                        }
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
                val eventId = intent.getIntExtra(EXTRA_EVENT_ID, -1)
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    if (eventId != -1 && eventId < 100000) {
                        putExtra("EDIT_EVENT_ID", eventId)
                    }
                }
                context.startActivity(launchIntent)
            }
        }
    }
}
