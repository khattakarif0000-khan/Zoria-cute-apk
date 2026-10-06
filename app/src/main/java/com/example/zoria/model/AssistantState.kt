package com.example.zoria.model

/**
 * Real lifecycle states of the ZORIA voice assistant.
 * No fake animations or simulated states are permitted.
 */
enum class AssistantState(val labelUrdu: String, val labelEnglish: String) {
    IDLE("تیار ہے", "IDLE"),
    LISTENING("سن رہی ہوں…", "LISTENING"),
    PROCESSING("سوچ رہی ہوں…", "PROCESSING"),
    SPEAKING("بول رہی ہوں…", "SPEAKING"),
    ERROR("خرابی", "ERROR")
}
