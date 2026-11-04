package com.example.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "event_completions",
    foreignKeys = [
        ForeignKey(
            entity = EventEntity::class,
            parentColumns = ["id"],
            childColumns = ["event_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("event_id"), Index("occurrence_date")]
)
data class EventCompletionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "event_id") val eventId: String,
    @ColumnInfo(name = "occurrence_date") val occurrenceDate: String, // ISO "yyyy-MM-dd"
    @ColumnInfo(name = "is_completed") val isCompleted: Boolean = true,
    @ColumnInfo(name = "completed_at") val completedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    val dirty: Boolean = true
)
