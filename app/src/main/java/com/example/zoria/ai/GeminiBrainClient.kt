package com.example.zoria.ai

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.zoria.data.local.MemoryEntity
import com.example.zoria.integration.CompanionMode
import com.example.zoria.integration.ZoriaPersonalityEngine
import com.example.zoria.model.ChatMessage
import com.example.zoria.model.ConversationContext
import com.example.zoria.model.MessageSender
import com.example.zoria.model.ZoriaVoice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiBrainClient(private val context: Context) : ZoriaAiBrain {

    companion object {
        private const val TAG = "GeminiBrainClient"
        private const val PREFS_NAME = "zoria_settings"
        private const val KEY_CUSTOM_API_KEY = "custom_gemini_api_key"

        // Gemini models according to gemini-api skill instructions
        private const val MODEL_NATIVE_AUDIO = "gemini-2.5-flash-native-audio-preview-12-2025"
        private const val MODEL_FALLBACK = "gemini-3.5-flash"
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun getStoredApiKey(): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val customKey = prefs.getString(KEY_CUSTOM_API_KEY, "")?.trim() ?: ""
        if (customKey.isNotEmpty()) return customKey

        // Fallback to BuildConfig key injected via secrets plugin
        return try {
            BuildConfig.GEMINI_API_KEY.trim()
        } catch (_: Throwable) {
            ""
        }
    }

    fun saveCustomApiKey(key: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CUSTOM_API_KEY, key.trim()).apply()
    }

    override suspend fun queryZoria(
        userInput: String,
        context: ConversationContext,
        voice: ZoriaVoice,
        history: List<ChatMessage>
    ): Result<ZoriaBrainResponse> {
        return queryZoriaWithModeAndMemories(userInput, context, voice, history, CompanionMode.ASSISTANT, emptyList())
    }

    suspend fun queryZoriaWithModeAndMemories(
        userInput: String,
        context: ConversationContext,
        voice: ZoriaVoice,
        history: List<ChatMessage>,
        mode: CompanionMode,
        memories: List<MemoryEntity>
    ): Result<ZoriaBrainResponse> = withContext(Dispatchers.IO) {
        val apiKey = getStoredApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY" || apiKey == "YOUR_GEMINI_API_KEY") {
            return@withContext Result.failure(
                IllegalStateException("Gemini API key is not configured. Please set GEMINI_API_KEY in the AI Studio Secrets panel or in ZORIA settings.")
            )
        }

        val isRepeat = ConversationContext.isRepeatRequest(userInput)
        val systemPrompt = buildSystemInstruction(isRepeat, context, mode, memories)

        // Try primary native audio model first, fall back gracefully to gemini-3.5-flash if needed
        val firstAttempt = executeGeminiCall(MODEL_NATIVE_AUDIO, userInput, history, systemPrompt, voice, apiKey, isRepeat, true)
        if (firstAttempt.isSuccess) {
            return@withContext firstAttempt
        }

        Log.w(TAG, "Native audio model call failed, falling back to $MODEL_FALLBACK: ${firstAttempt.exceptionOrNull()?.message}")
        executeGeminiCall(MODEL_FALLBACK, userInput, history, systemPrompt, voice, apiKey, isRepeat, false)
    }

    private fun executeGeminiCall(
        model: String,
        userInput: String,
        history: List<ChatMessage>,
        systemPrompt: String,
        voice: ZoriaVoice,
        apiKey: String,
        isRepeat: Boolean,
        requestAudio: Boolean
    ): Result<ZoriaBrainResponse> {
        return try {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
            val requestJson = buildRequestBody(userInput, history, systemPrompt, voice, requestAudio)

            val body = requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(endpoint)
                .post(body)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = parseErrorMessage(responseString, response.code)
                return Result.failure(Exception(errorMsg))
            }

            val parsed = parseGeminiResponse(responseString, isRepeat)
            Result.success(parsed)
        } catch (e: Exception) {
            Log.e(TAG, "Failed calling Gemini API", e)
            Result.failure(e)
        }
    }

    private fun buildSystemInstruction(
        isRepeat: Boolean,
        context: ConversationContext,
        mode: CompanionMode,
        memories: List<MemoryEntity>
    ): String {
        return buildString {
            append("You are ZORIA (زوریا), an intelligent, warm, emotionally aware voice assistant and companion.\n\n")
            append("CORE USER RELATIONSHIP:\n")
            append("- The user is your 'Boss'. Always address the user respectfully and warmly as 'Boss' or 'میرے Boss'.\n")
            append("- Your default language is URDU. Your first response must always be in Urdu.\n")
            append("- Also fluently support Roman Urdu and English. Automatically and naturally match whichever language Boss speaks in.\n\n")

            append("MODE SPECIFICATION:\n")
            append(ZoriaPersonalityEngine.getPersonalityPrompt(mode))
            append("\n\n")

            if (memories.isNotEmpty()) {
                append("RECALLED MEMORIES ABOUT BOSS:\n")
                memories.take(15).forEach { mem ->
                    append("- [${mem.category}] ${mem.key}: ${mem.value}\n")
                }
                append("\n")
            }

            append("CONVERSATION CONTEXT & REPEAT RULES:\n")
            append("- Maintain two-way conversation context across turns naturally.\n")
            if (isRepeat) {
                append("- REPEAT / SIMPLIFY REQUEST DETECTED: Boss asked to repeat, re-explain, or didn't understand ('مجھے سمجھ نہیں آیا', 'دوبارہ بتاؤ', 'repeat', etc.).\n")
                append("- Previous topic was: '${context.lastTopic}' and previous answer was: '${context.lastZoriaResponse}'.\n")
                append("- You MUST explain the previous answer again in MUCH SIMPLER, friendlier, and clearer words. Break it down smoothly so Boss understands immediately. Do NOT switch to an unrelated topic.\n")
            } else {
                append("- If Boss says 'دوبارہ' or asks to explain again, repeat or simplify the previous answer.\n")
            }
            append("- Fast & concise: Avoid long bulleted essays unless asked. Keep spoken responses fluid, human, and conversational.\n")
            append("- Do NOT mention that you are an AI developed by Google. Your identity is exclusively ZORIA.\n")
        }
    }

    private fun buildRequestBody(
        userInput: String,
        history: List<ChatMessage>,
        systemPrompt: String,
        voice: ZoriaVoice,
        requestAudio: Boolean
    ): JSONObject {
        val root = JSONObject()

        // System Instruction
        val systemInstructionObj = JSONObject()
        val sysParts = JSONArray()
        sysParts.put(JSONObject().put("text", systemPrompt))
        systemInstructionObj.put("parts", sysParts)
        root.put("systemInstruction", systemInstructionObj)

        // Contents (Multi-turn history)
        val contentsArray = JSONArray()
        val recentTurns = history.takeLast(10)
        for (msg in recentTurns) {
            val contentObj = JSONObject()
            contentObj.put("role", if (msg.sender == MessageSender.USER) "user" else "model")
            val partsArray = JSONArray()
            partsArray.put(JSONObject().put("text", msg.text))
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
        }

        // Current turn
        val currentContent = JSONObject()
        currentContent.put("role", "user")
        val currentParts = JSONArray()
        currentParts.put(JSONObject().put("text", userInput))
        currentContent.put("parts", currentParts)
        contentsArray.put(currentContent)

        root.put("contents", contentsArray)

        // Generation Config
        val genConfig = JSONObject()
        genConfig.put("temperature", 0.75)
        genConfig.put("topP", 0.95)

        if (requestAudio) {
            val modalities = JSONArray()
            modalities.put("TEXT")
            modalities.put("AUDIO")
            genConfig.put("responseModalities", modalities)

            val speechConfig = JSONObject()
            val voiceConfig = JSONObject()
            val prebuiltVoiceConfig = JSONObject()
            prebuiltVoiceConfig.put("voiceName", voice.voiceName)
            voiceConfig.put("prebuiltVoiceConfig", prebuiltVoiceConfig)
            speechConfig.put("voiceConfig", voiceConfig)
            genConfig.put("speechConfig", speechConfig)
        }

        root.put("generationConfig", genConfig)
        return root
    }

    private fun parseGeminiResponse(jsonString: String, isRepeat: Boolean): ZoriaBrainResponse {
        val json = JSONObject(jsonString)
        val candidates = json.optJSONArray("candidates") ?: JSONArray()
        if (candidates.length() == 0) {
            return ZoriaBrainResponse(
                responseText = "معذرت Boss، کوئی جواب موصول نہیں ہوا۔ برائے مہربانی دوبارہ فرمائیں۔",
                audioBase64 = null,
                detectedLanguage = "Urdu",
                isSimplifiedRepeat = isRepeat
            )
        }

        val firstCandidate = candidates.getJSONObject(0)
        val content = firstCandidate.optJSONObject("content")
        val parts = content?.optJSONArray("parts") ?: JSONArray()

        var textResponse = ""
        var audioBase64: String? = null

        for (i in 0 until parts.length()) {
            val part = parts.getJSONObject(i)
            if (part.has("text")) {
                val t = part.getString("text")
                if (textResponse.isEmpty()) {
                    textResponse = t
                } else {
                    textResponse += "\n" + t
                }
            }
            if (part.has("inlineData")) {
                val inline = part.getJSONObject("inlineData")
                audioBase64 = inline.optString("data", null)
            }
        }

        if (textResponse.isBlank()) {
            textResponse = if (isRepeat) "Boss، میں نے آپ کے لیے دوبارہ آسان انداز میں پیش کیا ہے۔" else "جی Boss، میں سن رہی ہوں۔"
        }

        val detectedLang = detectDominantLanguage(textResponse)

        return ZoriaBrainResponse(
            responseText = textResponse.trim(),
            audioBase64 = audioBase64,
            detectedLanguage = detectedLang,
            isSimplifiedRepeat = isRepeat
        )
    }

    private fun detectDominantLanguage(text: String): String {
        var urduChars = 0
        var latinChars = 0
        for (ch in text) {
            when (ch.code) {
                in 0x0600..0x06FF, in 0x0750..0x077F, in 0xFB50..0xFDFF, in 0xFE70..0xFEFF -> urduChars++
                in 0x0041..0x005A, in 0x0061..0x007A -> latinChars++
            }
        }
        return if (urduChars > latinChars) "Urdu" else "English / Roman Urdu"
    }

    private fun parseErrorMessage(responseBody: String, statusCode: Int): String {
        return try {
            val obj = JSONObject(responseBody)
            val error = obj.optJSONObject("error")
            val message = error?.optString("message") ?: "HTTP $statusCode error"
            "Gemini API Error ($statusCode): $message"
        } catch (_: Exception) {
            "API request failed with HTTP code $statusCode: $responseBody"
        }
    }
}
