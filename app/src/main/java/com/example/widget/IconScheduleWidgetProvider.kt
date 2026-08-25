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

class IconScheduleWidgetProvider : AppWidgetProvider() {

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
            val views = RemoteViews(context.packageName, R.layout.widget_icon_1x1)

            // Open App
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context, 700, openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_icon_root, openAppPendingIntent)

            // Badge count (remaining or total tasks)
            val uncompletedCount = data.totalCount - data.completedCount
            val displayCount = if (uncompletedCount > 0) uncompletedCount else data.totalCount

            if (displayCount > 0) {
                views.setViewVisibility(R.id.tv_icon_badge_count, View.VISIBLE)
                views.setTextViewText(R.id.tv_icon_badge_count, displayCount.toString())
            } else {
                views.setViewVisibility(R.id.tv_icon_badge_count, View.GONE)
            }

            return views
        }
    }
}
