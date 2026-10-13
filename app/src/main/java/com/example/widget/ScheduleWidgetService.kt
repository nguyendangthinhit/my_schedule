package com.example.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.R
import com.example.util.ThemeHelper
import kotlinx.coroutines.runBlocking

class ScheduleWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return ScheduleRemoteViewsFactory(this.applicationContext)
    }
}

class ScheduleRemoteViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {
    private var tasks: List<WidgetTaskItem> = emptyList()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        runBlocking {
            try {
                val data = WidgetDataHelper.loadWidgetData(context)
                tasks = data.tasks
            } catch (e: Exception) {
                tasks = emptyList()
            }
        }
    }

    override fun onDestroy() {
        tasks = emptyList()
    }

    override fun getCount(): Int = tasks.size

    override fun getViewAt(position: Int): RemoteViews? {
        if (position < 0 || position >= tasks.size) return null
        val task = tasks[position]
        val isDark = ThemeHelper.isDarkMode(context)
        val layoutId = if (isDark) R.layout.widget_task_item_dark else R.layout.widget_task_item
        val views = RemoteViews(context.packageName, layoutId)

        views.setTextViewText(R.id.tv_task_title, task.title)
        views.setImageViewResource(R.id.iv_task_dot, task.dotRes)

        val checkIcon = if (task.isCompleted) {
            R.drawable.ic_widget_check_square_checked
        } else {
            if (isDark) R.drawable.ic_widget_check_square_empty_dark else R.drawable.ic_widget_check_square_empty
        }
        views.setImageViewResource(R.id.iv_task_check, checkIcon)

        // Fill-in intent for toggling task completion when clicking item or checkbox
        val fillInIntent = Intent().apply {
            putExtra(ScheduleWidgetActionReceiver.EXTRA_EVENT_ID, task.id)
            putExtra(ScheduleWidgetActionReceiver.EXTRA_CURRENT_STATUS, task.isCompleted)
        }
        views.setOnClickFillInIntent(R.id.iv_task_check, fillInIntent)
        views.setOnClickFillInIntent(R.id.widget_task_item_root, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = tasks.getOrNull(position)?.id?.toLong() ?: position.toLong()

    override fun hasStableIds(): Boolean = true
}
