package com.example.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context

object WidgetUpdateHelper {

    fun updateAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)

        // Small Widget (Việc hôm nay - Vuông)
        val smallComponent = ComponentName(context, SmallScheduleWidgetProvider::class.java)
        val smallIds = appWidgetManager.getAppWidgetIds(smallComponent)
        if (smallIds.isNotEmpty()) {
            SmallScheduleWidgetProvider.updateAllWidgets(context, appWidgetManager, smallIds)
        }

        // Compact Widget (Việc hôm nay - Ngang)
        val compactComponent = ComponentName(context, CompactScheduleWidgetProvider::class.java)
        val compactIds = appWidgetManager.getAppWidgetIds(compactComponent)
        if (compactIds.isNotEmpty()) {
            CompactScheduleWidgetProvider.updateAllWidgets(context, appWidgetManager, compactIds)
        }

        // Micro Widget (Việc hôm nay - Gọn)
        val microComponent = ComponentName(context, MicroScheduleWidgetProvider::class.java)
        val microIds = appWidgetManager.getAppWidgetIds(microComponent)
        if (microIds.isNotEmpty()) {
            MicroScheduleWidgetProvider.updateAllWidgets(context, appWidgetManager, microIds)
        }
    }
}
