package com.example.zoria.integration

/**
 * Architectural integration point for future WhatsApp, YouTube, and TikTok integrations.
 * Ready for implementation in subsequent stages.
 */
interface ExternalMediaBridge {
    suspend fun openWhatsAppChat(phoneNumber: String, prefilledText: String? = null): Boolean
    suspend fun searchAndPlayYouTube(query: String): Boolean
    suspend fun openTikTokTrend(tag: String): Boolean
}
