package com.example.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "holidays")
data class HolidayEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String, // ISO "yyyy-MM-dd"
    val name: String,
    val source: String? = null,
    @ColumnInfo(name = "is_lunar_based") val isLunarBased: Boolean = false,
    @ColumnInfo(name = "last_synced_at") val lastSyncedAt: Long = System.currentTimeMillis()
)
