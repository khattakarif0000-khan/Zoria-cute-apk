package com.example.zoria.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Foundation entity for ZORIA's long-term companion memory.
 * Stores user preferences, name, traits, and facts learned during natural conversations.
 */
@Entity(tableName = "zoria_memories")
data class MemoryEntity(
    @PrimaryKey
    val id: String,
    val key: String,
    val value: String,
    val category: String, // e.g. "USER_INFO", "PREFERENCE", "EMOTION", "COMPANION"
    val timestamp: Long = System.currentTimeMillis()
)
