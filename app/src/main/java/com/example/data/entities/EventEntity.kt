package com.example.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "events",
    foreignKeys = [
        ForeignKey(
            entity = BoardEntity::class,
            parentColumns = ["id"],
            childColumns = ["board_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("start_date"), Index("board_id")]
)
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String? = null,
    @ColumnInfo(name = "board_id") val boardId: Int,
    @ColumnInfo(name = "start_date") val startDate: String, // ISO "yyyy-MM-dd"
    @ColumnInfo(name = "start_time") val startTime: String? = null, // "HH:mm"
    @ColumnInfo(name = "end_time") val endTime: String? = null, // "HH:mm"
    @ColumnInfo(name = "is_all_day") val isAllDay: Boolean = false,
    @ColumnInfo(name = "repeat_rule") val repeatRule: String? = "NONE", // NONE, DAILY, WEEKLY, MONTHLY
    @ColumnInfo(name = "repeat_days") val repeatDays: String? = null, // VD "TUE" hoặc "MON,WED,FRI"
    @ColumnInfo(name = "repeat_end_date") val repeatEndDate: String? = null,
    @ColumnInfo(name = "is_completed") val isCompleted: Boolean = false,
    @ColumnInfo(name = "completed_at") val completedAt: Long? = null,
    @ColumnInfo(name = "has_reminder") val hasReminder: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)
