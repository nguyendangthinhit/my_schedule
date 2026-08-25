package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LargeScheduleWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateAllWidgets(context, appWidgetManager, appWidgetIds)
    }

    companion object {
        fun updateAllWidgets(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
            val pendingResult = CoroutineScope(Dispatchers.Main).launch {
                val data = WidgetDataHelper.loadWidgetData(context)
                for (widgetId in appWidgetIds) {
                    val views = buildRemoteViews(context, data)
                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            }
        }

        private fun buildRemoteViews(context: Context, data: WidgetDisplayData): RemoteViews {
            val isDark = com.example.util.ThemeHelper.isDarkMode(context)
            val layoutId = if (isDark) R.layout.widget_large_4x2_dark else R.layout.widget_large_4x2
            val views = RemoteViews(context.packageName, layoutId)

            // Date and Weather
            views.setTextViewText(R.id.tv_large_date, data.fullDate)
            views.setTextViewText(R.id.tv_large_weather, data.weatherFull)

            // Open App on root click & view all
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context, 100, openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_large_root, openAppPendingIntent)
            views.setOnClickPendingIntent(R.id.btn_large_view_all, openAppPendingIntent)

            // Quick Add Button
            val addIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("action", "ADD_EVENT")
            }
            val addPendingIntent = PendingIntent.getActivity(
                context, 101, addIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_large_add, addPendingIntent)

            // Upcoming event (Thẻ "Tiếp theo")
            if (data.upcomingEvent != null) {
                views.setViewVisibility(R.id.layout_large_next, View.VISIBLE)
                views.setTextViewText(R.id.tv_large_next_title, data.upcomingEvent.title)
                views.setTextViewText(R.id.tv_large_next_category, data.upcomingEvent.categoryTitle)
                views.setTextViewText(R.id.tv_large_next_time, data.upcomingEvent.timeRange)

                val viewEventIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("action", "VIEW_EVENT")
                    putExtra("event_id", data.upcomingEvent.id)
                }
                val viewEventPendingIntent = PendingIntent.getActivity(
                    context, 102, viewEventIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.layout_large_next, viewEventPendingIntent)
            } else {
                views.setViewVisibility(R.id.layout_large_next, View.GONE)
            }

            // Task list (Up to 4 tasks)
            val taskLayouts = listOf(
                R.id.layout_large_task_1 to Triple(R.id.iv_large_check_1, R.id.tv_large_task_1, Pair(R.id.tv_large_time_1, R.id.iv_large_dot_1)),
                R.id.layout_large_task_2 to Triple(R.id.iv_large_check_2, R.id.tv_large_task_2, Pair(R.id.tv_large_time_2, R.id.iv_large_dot_2)),
                R.id.layout_large_task_3 to Triple(R.id.iv_large_check_3, R.id.tv_large_task_3, Pair(R.id.tv_large_time_3, R.id.iv_large_dot_3)),
                R.id.layout_large_task_4 to Triple(R.id.iv_large_check_4, R.id.tv_large_task_4, Pair(R.id.tv_large_time_4, R.id.iv_large_dot_4))
            )

            if (data.tasks.isEmpty() && data.upcomingEvent == null) {
                views.setViewVisibility(R.id.tv_large_empty, View.VISIBLE)
                views.setViewVisibility(R.id.layout_large_tasks_container, View.GONE)
            } else {
                views.setViewVisibility(R.id.tv_large_empty, View.GONE)
                views.setViewVisibility(R.id.layout_large_tasks_container, View.VISIBLE)

                for (i in 0 until 4) {
                    val (layoutId, components) = taskLayouts[i]
                    val (checkId, titleId, rightGroup) = components
                    val (timeId, dotId) = rightGroup

                    if (i < data.tasks.size) {
                        val task = data.tasks[i]
                        views.setViewVisibility(layoutId, View.VISIBLE)
                        views.setTextViewText(titleId, task.title)
                        views.setTextViewText(timeId, task.timeRange)
                        views.setImageViewResource(dotId, task.dotRes)

                        val checkIcon = if (task.isCompleted) {
                            R.drawable.ic_widget_check_square_checked
                        } else {
                            R.drawable.ic_widget_check_square_empty
                        }
                        views.setImageViewResource(checkId, checkIcon)

                        // Toggle completion pending intent
                        val toggleIntent = Intent(context, ScheduleWidgetActionReceiver::class.java).apply {
                            action = ScheduleWidgetActionReceiver.ACTION_TOGGLE_TASK
                            putExtra(ScheduleWidgetActionReceiver.EXTRA_EVENT_ID, task.id)
                            putExtra(ScheduleWidgetActionReceiver.EXTRA_CURRENT_STATUS, task.isCompleted)
                        }
                        val togglePendingIntent = PendingIntent.getBroadcast(
                            context, 1000 + i, toggleIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(checkId, togglePendingIntent)
                        views.setOnClickPendingIntent(layoutId, togglePendingIntent)
                    } else {
                        views.setViewVisibility(layoutId, View.GONE)
                    }
                }
            }

            // Footer
            views.setTextViewText(R.id.tv_large_footer, data.footerText)

            return views
        }
    }
}
