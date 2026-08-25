package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MicroScheduleWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateAllWidgets(context, appWidgetManager, appWidgetIds)
    }

    companion object {
        fun updateAllWidgets(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
            CoroutineScope(Dispatchers.Main).launch {
                val data = WidgetDataHelper.loadWidgetData(context)
                for (widgetId in appWidgetIds) {
                    val views = buildRemoteViews(context, data)
                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            }
        }

        private fun buildRemoteViews(context: Context, data: WidgetDisplayData): RemoteViews {
            val isDark = com.example.util.ThemeHelper.isDarkMode(context)
            val layoutId = if (isDark) R.layout.widget_micro_2x1_dark else R.layout.widget_micro_2x1
            val views = RemoteViews(context.packageName, layoutId)

            // Date: Thứ, ngày, tháng
            views.setTextViewText(R.id.tv_micro_date, data.microMiniDate)

            // Open App
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context, 600, openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_micro_root, openAppPendingIntent)

            // Quick Add Button
            val addIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("action", "ADD_EVENT")
            }
            val addPendingIntent = PendingIntent.getActivity(
                context, 601, addIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_micro_add, addPendingIntent)

            // Event info
            if (data.upcomingEvent != null) {
                views.setTextViewText(R.id.tv_micro_title, data.upcomingEvent.title)
                views.setTextViewText(R.id.tv_micro_time, data.upcomingEvent.startTimeOnly)
            } else if (data.tasks.isNotEmpty()) {
                val firstTask = data.tasks.first()
                views.setTextViewText(R.id.tv_micro_title, firstTask.title)
                views.setTextViewText(R.id.tv_micro_time, firstTask.timeRange.split("-").firstOrNull()?.trim() ?: firstTask.timeRange)
            } else {
                views.setTextViewText(R.id.tv_micro_title, "Không có lịch trình")
                views.setTextViewText(R.id.tv_micro_time, "Nhấn + để thêm")
            }

            return views
        }
    }
}
