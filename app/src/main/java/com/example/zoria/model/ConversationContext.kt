package com.example.zoria.model

/**
 * Tracks multi-turn conversational context so ZORIA never treats
 * repeat or clarification requests as disconnected conversations.
 */
data class ConversationContext(
    val lastTopic: String = "",
    val lastUserQuery: String = "",
    val lastZoriaResponse: String = "",
    val preferredLanguage: String = "Urdu",
    val repeatCount: Int = 0
) {
    companion object {
        private val REPEAT_PATTERNS_URDU = listOf(
            "مجھے سمجھ نہیں آیا",
            "سمجھ نہیں آیا",
            "دوبارہ بتاؤ",
            "دوبارہ بتائیں",
            "پھر سے سمجھاؤ",
            "پھر سے بتاؤ",
            "ایک بار اور بتاؤ",
            "ایک بار پھر بتائیں",
            "آسان لفظوں میں بتاؤ",
            "آسان الفاظ میں سمجھاؤ",
            "سمجھ نہیں سکی",
            "سمجھ نہیں سکا",
            "واپس بتاؤ"
        )

        private val REPEAT_PATTERNS_ROMAN_URDU = listOf(
            "samajh nahi aya",
            "samjh nahi aya",
            "dobara batao",
            "phir se batao",
            "phir se samjhao",
            "aik baar aur batao",
            "ek bar phir batao",
            "asan alfaz me batao",
            "asan lafzon me samjhao"
        )

        private val REPEAT_PATTERNS_ENGLISH = listOf(
            "repeat",
            "repeat that",
            "say that again",
            "i did not understand",
            "i didn't understand",
            "explain again",
            "can you repeat",
            "simplify that",
            "in simpler words",
            "pardon",
            "once more"
        )

        /**
         * Checks if the user is asking to repeat or re-explain the previous response.
         */
        fun isRepeatRequest(input: String): Boolean {
            val normalized = input.trim().lowercase()
            if (REPEAT_PATTERNS_URDU.any { normalized.contains(it) }) return true
            if (REPEAT_PATTERNS_ROMAN_URDU.any { normalized.contains(it) }) return true
            if (REPEAT_PATTERNS_ENGLISH.any { normalized.contains(it) }) return true
            return false
        }
    }
}
