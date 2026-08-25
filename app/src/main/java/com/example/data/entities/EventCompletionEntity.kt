package com.example.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "event_completions",
    foreignKeys = [
        ForeignKey(
            entity = EventEntity::class,
            parentColumns = ["id"],
            childColumns = ["event_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class EventCompletionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "event_id") val eventId: Int,
    @ColumnInfo(name = "occurrence_date") val occurrenceDate: String, // ISO "yyyy-MM-dd"
    @ColumnInfo(name = "is_completed") val isCompleted: Boolean = true,
    @ColumnInfo(name = "completed_at") val completedAt: Long = System.currentTimeMillis()
)
