package com.example.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context

object WidgetUpdateHelper {

    fun updateAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)

        // Large Widget
        val largeComponent = ComponentName(context, LargeScheduleWidgetProvider::class.java)
        val largeIds = appWidgetManager.getAppWidgetIds(largeComponent)
        if (largeIds.isNotEmpty()) {
            LargeScheduleWidgetProvider.updateAllWidgets(context, appWidgetManager, largeIds)
        }

        // Medium Widget
        val mediumComponent = ComponentName(context, MediumScheduleWidgetProvider::class.java)
        val mediumIds = appWidgetManager.getAppWidgetIds(mediumComponent)
        if (mediumIds.isNotEmpty()) {
            MediumScheduleWidgetProvider.updateAllWidgets(context, appWidgetManager, mediumIds)
        }

        // Small Widget
        val smallComponent = ComponentName(context, SmallScheduleWidgetProvider::class.java)
        val smallIds = appWidgetManager.getAppWidgetIds(smallComponent)
        if (smallIds.isNotEmpty()) {
            SmallScheduleWidgetProvider.updateAllWidgets(context, appWidgetManager, smallIds)
        }

        // Compact Widget
        val compactComponent = ComponentName(context, CompactScheduleWidgetProvider::class.java)
        val compactIds = appWidgetManager.getAppWidgetIds(compactComponent)
        if (compactIds.isNotEmpty()) {
            CompactScheduleWidgetProvider.updateAllWidgets(context, appWidgetManager, compactIds)
        }

        // Mini Widget
        val miniComponent = ComponentName(context, MiniScheduleWidgetProvider::class.java)
        val miniIds = appWidgetManager.getAppWidgetIds(miniComponent)
        if (miniIds.isNotEmpty()) {
            MiniScheduleWidgetProvider.updateAllWidgets(context, appWidgetManager, miniIds)
        }

        // Micro Widget
        val microComponent = ComponentName(context, MicroScheduleWidgetProvider::class.java)
        val microIds = appWidgetManager.getAppWidgetIds(microComponent)
        if (microIds.isNotEmpty()) {
            MicroScheduleWidgetProvider.updateAllWidgets(context, appWidgetManager, microIds)
        }

        // Icon Widget
        val iconComponent = ComponentName(context, IconScheduleWidgetProvider::class.java)
        val iconIds = appWidgetManager.getAppWidgetIds(iconComponent)
        if (iconIds.isNotEmpty()) {
            IconScheduleWidgetProvider.updateAllWidgets(context, appWidgetManager, iconIds)
        }
    }
}
