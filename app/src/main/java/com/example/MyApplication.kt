package com.example

import android.app.Application
import com.example.data.AppDatabase
import com.example.data.EventRepository
import com.example.data.SampleDataSeeder
import com.example.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { EventRepository(database.scheduleDao()) }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannels(this)

        // Seed sample data for August 2026 if not already seeded
        CoroutineScope(Dispatchers.IO).launch {
            SampleDataSeeder.seedSampleDataIfNeeded(this@MyApplication, database.scheduleDao())
        }
    }
}
