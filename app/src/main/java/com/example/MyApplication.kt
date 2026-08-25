package com.example

import android.app.Application
import com.example.data.AppDatabase
import com.example.data.EventRepository
import com.example.util.NotificationHelper

class MyApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { EventRepository(database.scheduleDao()) }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannels(this)
    }
}
