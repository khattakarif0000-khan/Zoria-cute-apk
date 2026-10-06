package com.example.zoria.integration

/**
 * Architectural integration point for real-time voice interruption (barge-in).
 * Ready to detect user voice activity while ZORIA is speaking and trigger instant cutoff.
 */
interface VoiceInterruptionHandler {
    fun startBargeInDetection(onInterruptionDetected: () -> Unit)
    fun stopBargeInDetection()
}
