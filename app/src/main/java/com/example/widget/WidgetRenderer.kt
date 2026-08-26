package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.util.ThemeHelper

enum class WidgetSizeCategory {
    MICRO_GON,          // 2x1 (Việc hôm nay - Gọn)
    SMALL_VUONG,        // 2x2 (Việc hôm nay - Vuông)
    COMPACT_NGANG       // 4x1, 4x2 (Việc hôm nay - Ngang)
}

object WidgetRenderer {

    fun resolveCategory(minWidthDp: Int, minHeightDp: Int): WidgetSizeCategory {
        if (minWidthDp <= 0 || minHeightDp <= 0) {
            return WidgetSizeCategory.COMPACT_NGANG
        }
        val isWidth4 = minWidthDp >= 240
        val isHeight1 = minHeightDp < 90

        return when {
            // 2x1 -> Việc hôm nay - Gọn
            minWidthDp < 180 && isHeight1 -> WidgetSizeCategory.MICRO_GON

            // 4x1+ -> Việc hôm nay - Ngang
            isWidth4 -> WidgetSizeCategory.COMPACT_NGANG

            // Chiều rộng 2 hoặc 3 (2x2, 2x3, 3x1, 3x2, 3x3) -> Việc hôm nay - Vuông
            else -> WidgetSizeCategory.SMALL_VUONG
        }
    }

    fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, widgetId: Int, data: WidgetDisplayData) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val viewMap = mapOf(
                SizeF(120f, 40f) to buildMicroViews(context, data),    // 2x1 -> Gọn
                SizeF(140f, 110f) to buildSmallViews(context, data),   // 2x2 -> Vuông
                SizeF(260f, 40f) to buildCompactViews(context, data)   // 4x1 -> Ngang
            )
            val responsiveViews = RemoteViews(viewMap)
            appWidgetManager.updateAppWidget(widgetId, responsiveViews)
        } else {
            val options = appWidgetManager.getAppWidgetOptions(widgetId)
            val minWidth = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0) ?: 0
            val minHeight = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0) ?: 0
            val category = resolveCategory(minWidth, minHeight)

            val views = when (category) {
                WidgetSizeCategory.MICRO_GON -> buildMicroViews(context, data)
                WidgetSizeCategory.COMPACT_NGANG -> buildCompactViews(context, data)
                WidgetSizeCategory.SMALL_VUONG -> buildSmallViews(context, data)
            }
            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }

    // 1. VIỆC HÔM NAY - GỌN (2x1)
    fun buildMicroViews(context: Context, data: WidgetDisplayData): RemoteViews {
        val isDark = ThemeHelper.isDarkMode(context)
        val layoutId = if (isDark) R.layout.widget_micro_2x1_dark else R.layout.widget_micro_2x1
        val views = RemoteViews(context.packageName, layoutId)

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

    // 2. VIỆC HÔM NAY - VUÔNG (Width 2 hoặc 3: 2x2, 3x2, etc.)
    fun buildSmallViews(context: Context, data: WidgetDisplayData): RemoteViews {
        val isDark = ThemeHelper.isDarkMode(context)
        val layoutId = if (isDark) R.layout.widget_small_2x2_dark else R.layout.widget_small_2x2
        val views = RemoteViews(context.packageName, layoutId)

        // Date and Weather
        views.setTextViewText(R.id.tv_small_date, data.fullDate)
        views.setTextViewText(R.id.tv_small_weather, data.weatherFull)

        // Open App
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context, 300, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_small_root, openAppPendingIntent)
        views.setOnClickPendingIntent(R.id.btn_small_view_all, openAppPendingIntent)

        // Quick Add Button
        val addIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("action", "ADD_EVENT")
        }
        val addPendingIntent = PendingIntent.getActivity(
            context, 301, addIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_small_add, addPendingIntent)

        // Thẻ Tiếp theo
        if (data.upcomingEvent != null) {
            views.setViewVisibility(R.id.layout_small_next, View.VISIBLE)
            views.setTextViewText(R.id.tv_small_next_title, data.upcomingEvent.title)
            views.setTextViewText(R.id.tv_small_next_category, data.upcomingEvent.categoryTitle)
            views.setTextViewText(R.id.tv_small_next_time, data.upcomingEvent.timeRange)

            val viewEventIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("action", "VIEW_EVENT")
                putExtra("event_id", data.upcomingEvent.id)
            }
            val viewEventPendingIntent = PendingIntent.getActivity(
                context, 302, viewEventIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.layout_small_next, viewEventPendingIntent)
        } else {
            views.setViewVisibility(R.id.layout_small_next, View.GONE)
        }

        // Task list (2 items)
        val taskLayouts = listOf(
            R.id.layout_small_task_1 to Triple(R.id.iv_small_check_1, R.id.tv_small_task_1, R.id.iv_small_dot_1),
            R.id.layout_small_task_2 to Triple(R.id.iv_small_check_2, R.id.tv_small_task_2, R.id.iv_small_dot_2)
        )

        if (data.tasks.isEmpty() && data.upcomingEvent == null) {
            views.setViewVisibility(R.id.tv_small_empty, View.VISIBLE)
            views.setViewVisibility(R.id.layout_small_tasks_container, View.GONE)
        } else {
            views.setViewVisibility(R.id.tv_small_empty, View.GONE)
            views.setViewVisibility(R.id.layout_small_tasks_container, View.VISIBLE)

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
                        if (isDark) R.drawable.ic_widget_check_square_empty_dark else R.drawable.ic_widget_check_square_empty
                    }
                    views.setImageViewResource(checkId, checkIcon)

                    val toggleIntent = Intent(context, ScheduleWidgetActionReceiver::class.java).apply {
                        action = ScheduleWidgetActionReceiver.ACTION_TOGGLE_TASK
                        putExtra(ScheduleWidgetActionReceiver.EXTRA_EVENT_ID, task.id)
                        putExtra(ScheduleWidgetActionReceiver.EXTRA_CURRENT_STATUS, task.isCompleted)
                    }
                    val togglePendingIntent = PendingIntent.getBroadcast(
                        context, 3000 + i, toggleIntent,
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
        views.setTextViewText(R.id.tv_small_footer, data.footerCountOnly)

        return views
    }

    // 3. VIỆC HÔM NAY - NGANG (4x1)
    fun buildCompactViews(context: Context, data: WidgetDisplayData): RemoteViews {
        val isDark = ThemeHelper.isDarkMode(context)
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
                    if (isDark) R.drawable.ic_widget_check_square_empty_dark else R.drawable.ic_widget_check_square_empty
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
