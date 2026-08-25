package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.ScheduleDao
import com.example.data.entities.*

import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        BoardEntity::class,
        EventEntity::class,
        ReminderEntity::class,
        EventCompletionEntity::class,
        HolidayEntity::class,
        WeatherForecastEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scheduleDao(): ScheduleDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "schedule_database"
                )
                .fallbackToDestructiveMigration(true)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        db.execSQL("INSERT INTO boards (name, colorHex, isVisible, sortOrder) VALUES ('Công việc', '#4285F4', 1, 0)")
                        db.execSQL("INSERT INTO boards (name, colorHex, isVisible, sortOrder) VALUES ('Học tập', '#34A853', 1, 1)")
                        db.execSQL("INSERT INTO boards (name, colorHex, isVisible, sortOrder) VALUES ('Đi chơi', '#E91E63', 1, 2)")
                        SampleDataSeeder.seedDatabaseRaw(db)
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
