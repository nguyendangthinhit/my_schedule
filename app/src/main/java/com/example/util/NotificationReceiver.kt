package com.example.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notifId = intent.getIntExtra("EXTRA_NOTIF_INT_ID", (System.currentTimeMillis() % 100000).toInt())
        val title = intent.getStringExtra("EXTRA_TITLE") ?: "Nhắc nhở sự kiện"
        val message = intent.getStringExtra("EXTRA_MESSAGE") ?: "Đã đến giờ sự kiện theo lịch trình của bạn."

        NotificationHelper.showNotification(
            context = context,
            id = notifId,
            title = title,
            message = message,
            isTest = false
        )
    }
}
