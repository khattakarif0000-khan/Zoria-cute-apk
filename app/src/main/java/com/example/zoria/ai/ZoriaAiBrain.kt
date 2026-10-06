package com.example.zoria.ai

import com.example.zoria.model.ChatMessage
import com.example.zoria.model.ConversationContext
import com.example.zoria.model.ZoriaVoice

/**
 * Result returned by ZORIA's AI Brain containing textual response,
 * real Gemini native audio (if generated), detected language, and metadata.
 */
data class ZoriaBrainResponse(
    val responseText: String,
    val audioBase64: String?,
    val detectedLanguage: String,
    val isSimplifiedRepeat: Boolean
)

/**
 * Clean interface contract for ZORIA's AI intelligence core.
 * Future brain models or multimodal expansions plug cleanly into this interface.
 */
interface ZoriaAiBrain {
    /**
     * Executes a conversational turn with full multi-turn context,
     * maintaining topic continuity and repeat/re-explain awareness.
     */
    suspend fun queryZoria(
        userInput: String,
        context: ConversationContext,
        voice: ZoriaVoice,
        history: List<ChatMessage>
    ): Result<ZoriaBrainResponse>
}
