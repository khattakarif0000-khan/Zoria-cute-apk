package com.example.zoria.model

import java.util.UUID

enum class MessageSender {
    USER,
    ZORIA
}

/**
 * Represents a conversation turn in ZORIA.
 */
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val language: String = "Urdu",
    val audioBase64: String? = null,
    val isSimplifiedRepeat: Boolean = false,
    val voiceUsed: String? = null
)
