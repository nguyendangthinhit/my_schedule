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
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scheduleDao(): ScheduleDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                insertDefaultBoardsSql(db)
            }
        }

        private fun insertDefaultBoardsSql(db: SupportSQLiteDatabase) {
            db.execSQL("INSERT OR IGNORE INTO boards (id, name, colorHex, isVisible, sortOrder) VALUES (1, 'Công việc', '#3B82F6', 1, 0)")
            db.execSQL("INSERT OR IGNORE INTO boards (id, name, colorHex, isVisible, sortOrder) VALUES (2, 'Học tập', '#10B981', 1, 1)")
            db.execSQL("INSERT OR IGNORE INTO boards (id, name, colorHex, isVisible, sortOrder) VALUES (3, 'Đi chơi', '#06B6D4', 1, 2)")
            db.execSQL("INSERT OR IGNORE INTO boards (id, name, colorHex, isVisible, sortOrder) VALUES (4, 'Cuộc họp', '#F59E0B', 1, 3)")
            db.execSQL("INSERT OR IGNORE INTO boards (id, name, colorHex, isVisible, sortOrder) VALUES (5, 'Ngày lễ', '#EF4444', 1, 4)")
            db.execSQL("INSERT OR IGNORE INTO boards (id, name, colorHex, isVisible, sortOrder) VALUES (6, 'Cá nhân', '#8B5CF6', 1, 5)")
            db.execSQL("INSERT OR IGNORE INTO boards (id, name, colorHex, isVisible, sortOrder) VALUES (7, 'Sức khỏe', '#EC4899', 1, 6)")
            db.execSQL("INSERT OR IGNORE INTO boards (id, name, colorHex, isVisible, sortOrder) VALUES (8, 'Tài chính', '#14B8A6', 1, 7)")
            db.execSQL("INSERT OR IGNORE INTO boards (id, name, colorHex, isVisible, sortOrder) VALUES (9, 'Việc cần làm', '#6366F1', 1, 8)")
            db.execSQL("INSERT OR IGNORE INTO boards (id, name, colorHex, isVisible, sortOrder) VALUES (10, 'Gia đình', '#F97316', 1, 9)")
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "schedule_database"
                )
                .addMigrations(MIGRATION_2_3)
                .fallbackToDestructiveMigration(true)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        insertDefaultBoardsSql(db)
                    }

                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        insertDefaultBoardsSql(db)
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
