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

class CompactScheduleWidgetProvider : AppWidgetProvider() {

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
            val layoutId = if (isDark) R.layout.widget_compact_4x1_dark else R.layout.widget_compact_4x1
            val views = RemoteViews(context.packageName, layoutId)

            // Date and Weather
            views.setTextViewText(R.id.tv_compact_date, data.fullDate)
            views.setTextViewText(R.id.tv_compact_weather, data.weatherShort)

            // Open App
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context, 400, openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_compact_root, openAppPendingIntent)

            // Quick Add Button
            val addIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("action", "ADD_EVENT")
            }
            val addPendingIntent = PendingIntent.getActivity(
                context, 401, addIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_compact_add, addPendingIntent)

            // Thẻ Tiếp theo
            if (data.upcomingEvent != null) {
                views.setViewVisibility(R.id.layout_compact_next, View.VISIBLE)
                views.setTextViewText(R.id.tv_compact_next_title, data.upcomingEvent.title)
                views.setTextViewText(R.id.tv_compact_next_category, data.upcomingEvent.categoryTitle)
                views.setTextViewText(R.id.tv_compact_next_time, data.upcomingEvent.timeRange)

                val viewEventIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("action", "VIEW_EVENT")
                    putExtra("event_id", data.upcomingEvent.id)
                }
                val viewEventPendingIntent = PendingIntent.getActivity(
                    context, 402, viewEventIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.layout_compact_next, viewEventPendingIntent)
            } else {
                views.setViewVisibility(R.id.layout_compact_next, View.GONE)
            }

            // Task list (2 items)
            val taskLayouts = listOf(
                R.id.layout_compact_task_1 to Triple(R.id.iv_compact_check_1, R.id.tv_compact_task_1, R.id.iv_compact_dot_1),
                R.id.layout_compact_task_2 to Triple(R.id.iv_compact_check_2, R.id.tv_compact_task_2, R.id.iv_compact_dot_2)
            )

            for (i in 0 until 2) {
                val (layoutId, components) = taskLayouts[i]
                val (checkId, titleId, dotId) = components

                if (i < data.tasks.size) {
                    val task = data.tasks[i]
                    views.setViewVisibility(layoutId, View.VISIBLE)
                    views.setTextViewText(titleId, task.title)
                    views.setImageViewResource(dotId, task.dotRes)

                    val checkIcon = if (task.isCompleted) {
                        R.drawable.ic_widget_check_square_checked
                    } else {
                        R.drawable.ic_widget_check_square_empty
                    }
                    views.setImageViewResource(checkId, checkIcon)

                    val toggleIntent = Intent(context, ScheduleWidgetActionReceiver::class.java).apply {
                        action = ScheduleWidgetActionReceiver.ACTION_TOGGLE_TASK
                        putExtra(ScheduleWidgetActionReceiver.EXTRA_EVENT_ID, task.id)
                        putExtra(ScheduleWidgetActionReceiver.EXTRA_CURRENT_STATUS, task.isCompleted)
                    }
                    val togglePendingIntent = PendingIntent.getBroadcast(
                        context, 4000 + i, toggleIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(checkId, togglePendingIntent)
                    views.setOnClickPendingIntent(layoutId, togglePendingIntent)
                } else {
                    views.setViewVisibility(layoutId, View.GONE)
                }
            }

            return views
        }
    }
}
