package com.example.util

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.MainActivity
import com.example.MyApplication
import com.example.ui.theme.MyScheduleTheme
import kotlinx.coroutines.launch

/**
 * Màn hình nổi cho sự kiện quan trọng: bật sáng màn hình khóa (showWhenLocked/turnScreenOn trong manifest)
 * và cho người dùng xác nhận đã xem, đánh dấu hoàn thành hoặc mở app.
 */
class ImportantEventActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, 0)
        val eventId = intent.getStringExtra(EXTRA_EVENT_ID)
        val date = intent.getStringExtra(EXTRA_DATE)
        val time = intent.getStringExtra(EXTRA_TIME)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Sự kiện quan trọng"
        val message = intent.getStringExtra(EXTRA_MESSAGE).orEmpty()

        setContent {
            MyScheduleTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(28.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("SỰ KIỆN QUAN TRỌNG", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(16.dp))
                        Text(title, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                        if (!time.isNullOrBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text("Lúc $time", fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        if (message.isNotBlank()) {
                            Spacer(Modifier.height(12.dp))
                            Text(message, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(36.dp))
                        Button(onClick = { dismiss(notifId) }, modifier = Modifier.fillMaxWidth()) { Text("Đã xem") }
                        if (eventId != null && date != null) {
                            Spacer(Modifier.height(10.dp))
                            OutlinedButton(onClick = { complete(eventId, date, notifId) }, modifier = Modifier.fillMaxWidth()) {
                                Text("Hoàn thành")
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(onClick = { openApp(notifId) }, modifier = Modifier.fillMaxWidth()) { Text("Mở ứng dụng") }
                    }
                }
            }
        }
    }

    private fun dismiss(notifId: Int) {
        cancelNotification(notifId)
        finish()
    }

    private fun complete(eventId: String, date: String, notifId: Int) {
        lifecycleScope.launch {
            try {
                (applicationContext as MyApplication).repository.toggleEventCompletion(eventId, date)
                NotificationHelper.cancelEventReminder(applicationContext, eventId)
                com.example.widget.WidgetUpdateHelper.updateAllWidgets(applicationContext)
            } catch (_: Exception) {}
            dismiss(notifId)
        }
    }

    private fun openApp(notifId: Int) {
        cancelNotification(notifId)
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        finish()
    }

    private fun cancelNotification(notifId: Int) {
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(notifId)
    }

    companion object {
        const val EXTRA_NOTIF_ID = "important_notif_id"
        const val EXTRA_EVENT_ID = "important_event_id"
        const val EXTRA_TITLE = "important_title"
        const val EXTRA_MESSAGE = "important_message"
        const val EXTRA_DATE = "important_date"
        const val EXTRA_TIME = "important_time"
    }
}
