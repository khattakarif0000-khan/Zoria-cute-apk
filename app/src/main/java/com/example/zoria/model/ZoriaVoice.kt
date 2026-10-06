package com.example.zoria.model

/**
 * Prebuilt voices supported by Gemini Native Audio for ZORIA.
 * Kore is the designated primary voice; Aoede is the alternative.
 */
enum class ZoriaVoice(
    val voiceName: String,
    val displayNameUrdu: String,
    val displayNameEnglish: String,
    val description: String
) {
    KORE(
        voiceName = "Kore",
        displayNameUrdu = "کوری (Kore - بنیادی)",
        displayNameEnglish = "Kore (Primary)",
        description = "پروقار، نرم اور قدرتی بنیادی آواز (Warm, balanced and natural)"
    ),
    AOEDE(
        voiceName = "Aoede",
        displayNameUrdu = "ایوڈی (Aoede - متبادل)",
        displayNameEnglish = "Aoede (Alternative)",
        description = "روشن، شائستہ اور خوش الحان متبادل آواز (Bright and melodic)"
    )
}
