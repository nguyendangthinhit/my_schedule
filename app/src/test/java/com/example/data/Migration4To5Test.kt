package com.example.data

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Migration4To5Test {

    private fun createInMemoryDb(): SupportSQLiteDatabase {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(null)
            .callback(object : SupportSQLiteOpenHelper.Callback(4) {
                override fun onCreate(db: SupportSQLiteDatabase) {}
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()
        return FrameworkSQLiteOpenHelperFactory().create(config).writableDatabase
    }

    @Test
    fun migration4To5_addsIsImportantDefaultFalseAndKeepsData() {
        val db = createInMemoryDb()
        db.execSQL("CREATE TABLE events (id TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, has_reminder INTEGER NOT NULL)")
        db.execSQL("INSERT INTO events (id, title, has_reminder) VALUES ('e1', 'Thi cuối kỳ', 1)")

        AppDatabase.MIGRATION_4_5.migrate(db)

        db.query("SELECT title, has_reminder, is_important FROM events WHERE id = 'e1'").use { c ->
            assertEquals(true, c.moveToFirst())
            assertEquals("Thi cuối kỳ", c.getString(0))
            assertEquals(1, c.getInt(1))
            assertEquals(0, c.getInt(2))
        }

        db.execSQL("UPDATE events SET is_important = 1 WHERE id = 'e1'")
        db.query("SELECT is_important FROM events WHERE id = 'e1'").use { c ->
            c.moveToFirst()
            assertEquals(1, c.getInt(0))
        }
        db.close()
    }
}
