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

class MiniScheduleWidgetProvider : AppWidgetProvider() {

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
            val layoutId = if (isDark) R.layout.widget_mini_2x1_dark else R.layout.widget_mini_2x1
            val views = RemoteViews(context.packageName, layoutId)

            // Date: Thứ, ngày, tháng
            views.setTextViewText(R.id.tv_mini_date, data.microMiniDate)

            // Open App
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context, 500, openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_mini_root, openAppPendingIntent)

            // Quick Add Button
            val addIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("action", "ADD_EVENT")
            }
            val addPendingIntent = PendingIntent.getActivity(
                context, 501, addIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_mini_add, addPendingIntent)

            // Thẻ Tiếp theo
            if (data.upcomingEvent != null) {
                views.setViewVisibility(R.id.layout_mini_next, View.VISIBLE)
                views.setTextViewText(R.id.tv_mini_next_title, data.upcomingEvent.title)
                views.setTextViewText(R.id.tv_mini_next_category, data.upcomingEvent.categoryTitle)
                views.setTextViewText(R.id.tv_mini_next_time, data.upcomingEvent.timeRange)

                val viewEventIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("action", "VIEW_EVENT")
                    putExtra("event_id", data.upcomingEvent.id)
                }
                val viewEventPendingIntent = PendingIntent.getActivity(
                    context, 502, viewEventIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.layout_mini_next, viewEventPendingIntent)
            } else {
                views.setViewVisibility(R.id.layout_mini_next, View.GONE)
            }

            return views
        }
    }
}
