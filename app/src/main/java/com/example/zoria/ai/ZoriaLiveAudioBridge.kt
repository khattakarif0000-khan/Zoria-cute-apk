package com.example.zoria.ai

import com.example.zoria.model.ZoriaVoice

/**
 * Clean architectural bridge prepared for Gemini Native Audio / Gemini Live
 * real-time bidirectional audio streaming (WebSockets / WebRTC) in the next stage.
 *
 * Defines the contract for streaming live microphone PCM frames and receiving
 * live synthesized audio chunks from Gemini Live without buffering whole utterances.
 */
interface ZoriaLiveAudioBridge {
    /**
     * Prepares and connects a real-time duplex session with Gemini Live API.
     */
    suspend fun connectLiveSession(voice: ZoriaVoice, apiKey: String): Result<Unit>

    /**
     * Streams an incoming PCM audio buffer from the microphone to Gemini.
     */
    suspend fun streamAudioChunk(pcmChunk: ByteArray)

    /**
     * Callback invoked whenever Gemini streams back a synthesized audio chunk.
     */
    fun setIncomingAudioListener(listener: (pcmChunk: ByteArray) -> Unit)

    /**
     * Closes the active live audio bridge session immediately.
     */
    fun disconnectLiveSession()

    /**
     * True if the duplex streaming connection is currently active.
     */
    val isLiveConnected: Boolean
}
