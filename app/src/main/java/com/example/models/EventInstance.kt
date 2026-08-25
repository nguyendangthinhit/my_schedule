package com.example.models

import com.example.data.entities.BoardEntity
import com.example.data.entities.EventEntity

data class EventInstance(
    val event: EventEntity,
    val board: BoardEntity?,
    val date: String, // ISO "yyyy-MM-dd"
    val isCompleted: Boolean
)
