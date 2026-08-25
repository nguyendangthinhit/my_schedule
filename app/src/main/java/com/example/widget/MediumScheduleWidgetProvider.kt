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

class MediumScheduleWidgetProvider : AppWidgetProvider() {

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
            val layoutId = if (isDark) R.layout.widget_medium_4x1_5_dark else R.layout.widget_medium_4x1_5
            val views = RemoteViews(context.packageName, layoutId)

            // Date and Weather
            views.setTextViewText(R.id.tv_medium_date, data.fullDate)
            views.setTextViewText(R.id.tv_medium_weather, data.weatherFull)

            // Open App
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context, 200, openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_medium_root, openAppPendingIntent)
            views.setOnClickPendingIntent(R.id.btn_medium_view_all, openAppPendingIntent)

            // Quick Add Button
            val addIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("action", "ADD_EVENT")
            }
            val addPendingIntent = PendingIntent.getActivity(
                context, 201, addIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_medium_add, addPendingIntent)

            // Thẻ Tiếp theo
            if (data.upcomingEvent != null) {
                views.setViewVisibility(R.id.layout_medium_next, View.VISIBLE)
                views.setTextViewText(R.id.tv_medium_next_title, data.upcomingEvent.title)
                views.setTextViewText(R.id.tv_medium_next_category, data.upcomingEvent.categoryTitle)
                views.setTextViewText(R.id.tv_medium_next_time, data.upcomingEvent.timeRange)

                val viewEventIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("action", "VIEW_EVENT")
                    putExtra("event_id", data.upcomingEvent.id)
                }
                val viewEventPendingIntent = PendingIntent.getActivity(
                    context, 202, viewEventIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.layout_medium_next, viewEventPendingIntent)
            } else {
                views.setViewVisibility(R.id.layout_medium_next, View.GONE)
            }

            // Task list (2 tasks)
            val taskLayouts = listOf(
                R.id.layout_medium_task_1 to Triple(R.id.iv_medium_check_1, R.id.tv_medium_task_1, Pair(R.id.tv_medium_time_1, R.id.iv_medium_dot_1)),
                R.id.layout_medium_task_2 to Triple(R.id.iv_medium_check_2, R.id.tv_medium_task_2, Pair(R.id.tv_medium_time_2, R.id.iv_medium_dot_2))
            )

            if (data.tasks.isEmpty() && data.upcomingEvent == null) {
                views.setViewVisibility(R.id.tv_medium_empty, View.VISIBLE)
                views.setViewVisibility(R.id.layout_medium_tasks_container, View.GONE)
            } else {
                views.setViewVisibility(R.id.tv_medium_empty, View.GONE)
                views.setViewVisibility(R.id.layout_medium_tasks_container, View.VISIBLE)

                for (i in 0 until 2) {
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

                        val toggleIntent = Intent(context, ScheduleWidgetActionReceiver::class.java).apply {
                            action = ScheduleWidgetActionReceiver.ACTION_TOGGLE_TASK
                            putExtra(ScheduleWidgetActionReceiver.EXTRA_EVENT_ID, task.id)
                            putExtra(ScheduleWidgetActionReceiver.EXTRA_CURRENT_STATUS, task.isCompleted)
                        }
                        val togglePendingIntent = PendingIntent.getBroadcast(
                            context, 2000 + i, toggleIntent,
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
            views.setTextViewText(R.id.tv_medium_footer, data.footerText)

            return views
        }
    }
}
