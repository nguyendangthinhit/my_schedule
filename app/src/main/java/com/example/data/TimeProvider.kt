package com.example.data

/**
 * Pluggable time provider interface to allow deterministic time injection
 * during testing and consistent updatedAt assignment in EventRepository.
 */
interface TimeProvider {
    fun currentTimeMillis(): Long
}

object SystemTimeProvider : TimeProvider {
    override fun currentTimeMillis(): Long = System.currentTimeMillis()
}
