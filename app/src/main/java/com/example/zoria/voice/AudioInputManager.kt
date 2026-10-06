package com.example.zoria.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class AudioInputManager(
    private val context: Context,
    private val onSpeechResult: (String) -> Unit,
    private val onError: (String) -> Unit
) {

    companion object {
        private const val TAG = "AudioInputManager"
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var isCurrentlyListening = false

    // Real RMS decibels from microphone (typically ranges from -2dB to +10dB)
    private val _rmsDecibels = MutableStateFlow(0f)
    val rmsDecibels: StateFlow<Float> = _rmsDecibels.asStateFlow()

    private val _isListeningFlow = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListeningFlow.asStateFlow()

    init {
        initRecognizer()
    }

    private fun initRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w(TAG, "Speech recognition service is not available on this device.")
            return
        }
        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(createListener())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed creating SpeechRecognizer", e)
        }
    }

    private fun createListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isCurrentlyListening = true
                _isListeningFlow.value = true
                Log.d(TAG, "SpeechRecognizer: Ready for speech")
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "SpeechRecognizer: User started speaking")
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Truthful sound amplitude from hardware microphone
                val clamped = if (rmsdB < 0f) 0f else rmsdB
                _rmsDecibels.value = clamped
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                // In accordance with ZORIA core requirements:
                // RAW MICROPHONE RECORDINGS ARE NEVER SAVED TO STORAGE
            }

            override fun onEndOfSpeech() {
                isCurrentlyListening = false
                _isListeningFlow.value = false
                _rmsDecibels.value = 0f
                Log.d(TAG, "SpeechRecognizer: End of speech detected")
            }

            override fun onError(error: Int) {
                isCurrentlyListening = false
                _isListeningFlow.value = false
                _rmsDecibels.value = 0f
                val errorMessage = getErrorMessage(error)
                Log.e(TAG, "SpeechRecognizer error: $errorMessage (code: $error)")

                // Only report actionable errors, ignore quiet timeouts when user just tapped stop
                if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                    onError(errorMessage)
                }
            }

            override fun onResults(results: Bundle?) {
                isCurrentlyListening = false
                _isListeningFlow.value = false
                _rmsDecibels.value = 0f

                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val recognizedText = matches?.firstOrNull()?.trim() ?: ""
                if (recognizedText.isNotEmpty()) {
                    onSpeechResult(recognizedText)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                // Can be hooked for real-time live preview
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    fun startListening(languagePreference: String = "ur-PK") {
        if (speechRecognizer == null) {
            initRecognizer()
        }
        if (speechRecognizer == null) {
            onError("آواز پہچاننے کی سروس دستیاب نہیں ہے۔ برائے مہربانی ٹیکسٹ کے ذریعے لکھیں۔")
            return
        }

        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languagePreference)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languagePreference)
                // Also support Roman Urdu / English recognition in fallback
                putExtra(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES, arrayListOf("ur-PK", "ur", "en-US"))
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            speechRecognizer?.startListening(intent)
            isCurrentlyListening = true
            _isListeningFlow.value = true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting speech listening", e)
            onError("مائیکروفون شروع کرنے میں دشواری پیش آئی: ${e.message}")
        }
    }

    fun stopListening() {
        try {
            if (isCurrentlyListening) {
                speechRecognizer?.stopListening()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping speech listening", e)
        } finally {
            isCurrentlyListening = false
            _isListeningFlow.value = false
            _rmsDecibels.value = 0f
        }
    }

    fun cancelListening() {
        try {
            speechRecognizer?.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling speech listening", e)
        } finally {
            isCurrentlyListening = false
            _isListeningFlow.value = false
            _rmsDecibels.value = 0f
        }
    }

    fun destroy() {
        stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    private fun getErrorMessage(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "آڈیو ریکارڈنگ میں خرابی پیش آئی۔"
            SpeechRecognizer.ERROR_CLIENT -> "کلائنٹ کی خرابی۔"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "مائیکروفون کی اجازت درکار ہے۔"
            SpeechRecognizer.ERROR_NETWORK -> "انٹرنیٹ کنکشن کی خرابی پیش آئی۔"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "نیٹ ورک کا وقت ختم ہو گیا۔"
            SpeechRecognizer.ERROR_NO_MATCH -> "کوئی آواز سمجھ نہیں آئی۔"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "آڈیو سروس مصروف ہے۔"
            SpeechRecognizer.ERROR_SERVER -> "سرور میں خرابی پیش آئی۔"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "کوئی بات نہیں سنی گئی۔"
            else -> "نامعلوم خرابی پیش آئی (Error $errorCode)"
        }
    }
}
