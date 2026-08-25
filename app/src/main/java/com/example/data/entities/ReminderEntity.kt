package com.example.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = EventEntity::class,
            parentColumns = ["id"],
            childColumns = ["event_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "event_id") val eventId: Int,
    @ColumnInfo(name = "remind_before_minutes") val remindBeforeMinutes: Int,
    @ColumnInfo(name = "is_triggered") val isTriggered: Boolean = false,
    @ColumnInfo(name = "notification_id") val notificationId: Int? = null
)
