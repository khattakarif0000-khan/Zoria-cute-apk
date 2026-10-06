package com.example.zoria.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "zoria_messages")
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val sender: String,
    val text: String,
    val timestamp: Long,
    val language: String,
    val audioBase64: String?,
    val isSimplifiedRepeat: Boolean,
    val voiceUsed: String?
)
