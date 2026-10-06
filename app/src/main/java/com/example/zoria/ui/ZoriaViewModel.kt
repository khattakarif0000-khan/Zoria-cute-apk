package com.example.zoria.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.zoria.ai.GeminiBrainClient
import com.example.zoria.data.local.MemoryEntity
import com.example.zoria.data.local.MessageEntity
import com.example.zoria.data.local.ZoriaDatabase
import com.example.zoria.integration.CompanionMode
import com.example.zoria.integration.OfflineCommandEngine
import com.example.zoria.integration.PhoneActionResult
import com.example.zoria.integration.PhoneAppController
import com.example.zoria.model.AssistantState
import com.example.zoria.model.ChatMessage
import com.example.zoria.model.ConversationContext
import com.example.zoria.model.MessageSender
import com.example.zoria.model.ZoriaVoice
import com.example.zoria.service.ZoriaForegroundService
import com.example.zoria.service.ZoriaNotificationListenerService
import com.example.zoria.voice.AudioInputManager
import com.example.zoria.voice.AudioOutputManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class ZoriaViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "ZoriaViewModel"
        const val INITIAL_GREETING_URDU = "السلام علیکم Boss، میں ZORIA ہوں۔ بتائیں، میں آپ کی کیا مدد کر سکتی ہوں؟"
    }

    private val database = ZoriaDatabase.getDatabase(application)
    private val messageDao = database.messageDao()
    private val memoryDao = database.memoryDao()

    private val brainClient = GeminiBrainClient(application)
    private val audioOutput = AudioOutputManager(application)
    val phoneAppController = PhoneAppController(application)
    private val offlineEngine = OfflineCommandEngine(application, phoneAppController)

    private val _assistantState = MutableStateFlow(AssistantState.IDLE)
    val assistantState: StateFlow<AssistantState> = _assistantState.asStateFlow()

    private val _selectedVoice = MutableStateFlow(ZoriaVoice.KORE)
    val selectedVoice: StateFlow<ZoriaVoice> = _selectedVoice.asStateFlow()

    private val _selectedMode = MutableStateFlow(CompanionMode.ASSISTANT)
    val selectedMode: StateFlow<CompanionMode> = _selectedMode.asStateFlow()

    private val _isContinuousConversationEnabled = MutableStateFlow(true)
    val isContinuousConversationEnabled: StateFlow<Boolean> = _isContinuousConversationEnabled.asStateFlow()

    private val _isBackgroundServiceActive = MutableStateFlow(false)
    val isBackgroundServiceActive: StateFlow<Boolean> = _isBackgroundServiceActive.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isMicPermissionGranted = MutableStateFlow(false)
    val isMicPermissionGranted: StateFlow<Boolean> = _isMicPermissionGranted.asStateFlow()

    private val _isNotificationAccessGranted = MutableStateFlow(false)
    val isNotificationAccessGranted: StateFlow<Boolean> = _isNotificationAccessGranted.asStateFlow()

    private val _hasConfiguredApiKey = MutableStateFlow(false)
    val hasConfiguredApiKey: StateFlow<Boolean> = _hasConfiguredApiKey.asStateFlow()

    private val _conversationContext = MutableStateFlow(ConversationContext())
    val conversationContext: StateFlow<ConversationContext> = _conversationContext.asStateFlow()

    // Truthful hardware microphone listener
    private val audioInput = AudioInputManager(
        context = application,
        onSpeechResult = { recognizedText ->
            handleUserSpeechInput(recognizedText)
        },
        onError = { err ->
            _errorMessage.value = err
            _assistantState.value = AssistantState.ERROR
        }
    )

    // Hardware microphone RMS decibels
    val rmsDecibels: StateFlow<Float> = audioInput.rmsDecibels

    // Real-time conversation stream persisted in Room
    val messages: StateFlow<List<ChatMessage>> = messageDao.getAllMessages()
        .map { entities ->
            entities.map { entity ->
                ChatMessage(
                    id = entity.id,
                    sender = if (entity.sender == "USER") MessageSender.USER else MessageSender.ZORIA,
                    text = entity.text,
                    timestamp = entity.timestamp,
                    language = entity.language,
                    audioBase64 = entity.audioBase64,
                    isSimplifiedRepeat = entity.isSimplifiedRepeat,
                    voiceUsed = entity.voiceUsed
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Long-term safe memories stream from Room
    val memories: StateFlow<List<MemoryEntity>> = memoryDao.getAllMemories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        checkApiKeyStatus()
        checkPermissions()
        initializeInitialGreetingIfNeeded()
        observeBackgroundNotifications()
    }

    fun checkApiKeyStatus() {
        val key = brainClient.getStoredApiKey()
        _hasConfiguredApiKey.value = key.isNotEmpty() && key != "MY_GEMINI_API_KEY" && key != "YOUR_GEMINI_API_KEY"
    }

    fun checkPermissions() {
        _isNotificationAccessGranted.value = ZoriaNotificationListenerService.isNotificationAccessGranted(getApplication())
    }

    fun setMicPermissionGranted(granted: Boolean) {
        _isMicPermissionGranted.value = granted
    }

    private fun initializeInitialGreetingIfNeeded() {
        viewModelScope.launch {
            val existing = messageDao.getRecentMessages(1)
            if (existing.isEmpty()) {
                val greetingEntity = MessageEntity(
                    id = UUID.randomUUID().toString(),
                    sender = "ZORIA",
                    text = INITIAL_GREETING_URDU,
                    timestamp = System.currentTimeMillis(),
                    language = "Urdu",
                    audioBase64 = null,
                    isSimplifiedRepeat = false,
                    voiceUsed = ZoriaVoice.KORE.voiceName
                )
                messageDao.insertMessage(greetingEntity)
                _conversationContext.value = ConversationContext(
                    lastTopic = "تعارف اور آغاز",
                    lastZoriaResponse = INITIAL_GREETING_URDU,
                    preferredLanguage = "Urdu"
                )
            }
        }
    }

    private fun observeBackgroundNotifications() {
        viewModelScope.launch {
            ZoriaNotificationListenerService.notificationEvents.collect { event ->
                // Add announcement message to conversation transcript
                val notificationMsg = MessageEntity(
                    id = UUID.randomUUID().toString(),
                    sender = "ZORIA",
                    text = "🔔 " + event.announcementUrdu,
                    timestamp = System.currentTimeMillis(),
                    language = "Urdu",
                    audioBase64 = null,
                    isSimplifiedRepeat = false,
                    voiceUsed = _selectedVoice.value.voiceName
                )
                messageDao.insertMessage(notificationMsg)
            }
        }
    }

    fun toggleContinuousConversation() {
        _isContinuousConversationEnabled.value = !_isContinuousConversationEnabled.value
    }

    fun toggleBackgroundService() {
        val next = !_isBackgroundServiceActive.value
        _isBackgroundServiceActive.value = next
        if (next) {
            ZoriaForegroundService.startService(getApplication())
        } else {
            ZoriaForegroundService.stopService(getApplication())
        }
    }

    fun setMode(mode: CompanionMode) {
        _selectedMode.value = mode
    }

    /**
     * Handles voice interruption (barge-in):
     * If ZORIA is currently speaking, calling this immediately cuts off audio playback
     * and transitions directly to LISTENING.
     */
    fun startListeningWithBargeIn() {
        if (audioOutput.isSpeaking.value || _assistantState.value == AssistantState.SPEAKING) {
            audioOutput.stopPlayback()
        }
        startListening()
    }

    fun startListening() {
        if (!_isMicPermissionGranted.value) {
            _errorMessage.value = "مائیکروفون کی اجازت درکار ہے۔"
            _assistantState.value = AssistantState.ERROR
            return
        }

        audioOutput.stopPlayback()
        _errorMessage.value = null
        _assistantState.value = AssistantState.LISTENING

        val currentLang = _conversationContext.value.preferredLanguage
        val langCode = if (currentLang == "Urdu") "ur-PK" else "en-US"
        audioInput.startListening(langCode)
    }

    fun stopListening() {
        audioInput.stopListening()
        if (_assistantState.value == AssistantState.LISTENING) {
            _assistantState.value = AssistantState.IDLE
        }
    }

    /**
     * Clear STOP control: immediately halts listening, speech recognition, and audio playback.
     */
    fun stopAll() {
        audioInput.cancelListening()
        audioOutput.stopPlayback()
        _assistantState.value = AssistantState.IDLE
        _errorMessage.value = null
    }

    private fun handleUserSpeechInput(text: String) {
        if (text.isBlank()) {
            _assistantState.value = AssistantState.IDLE
            return
        }
        processUserInput(text)
    }

    fun sendTextMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        audioOutput.stopPlayback()
        audioInput.cancelListening()
        processUserInput(trimmed)
    }

    private fun processUserInput(rawInput: String) {
        viewModelScope.launch {
            _errorMessage.value = null
            _assistantState.value = AssistantState.PROCESSING

            val isRepeat = ConversationContext.isRepeatRequest(rawInput)

            // Save user message to Room
            val userMsg = MessageEntity(
                id = UUID.randomUUID().toString(),
                sender = "USER",
                text = rawInput,
                timestamp = System.currentTimeMillis(),
                language = detectLanguage(rawInput),
                audioBase64 = null,
                isSimplifiedRepeat = isRepeat,
                voiceUsed = null
            )
            messageDao.insertMessage(userMsg)

            // Detect and persist user preferences/memories
            inspectAndStoreMemory(rawInput)

            // 1. First, check if this is a phone action command (e.g. WhatsApp, Camera, Flashlight, Volume, Settings)
            val actionResult = phoneAppController.evaluateAndExecuteCommand(rawInput)
            if (actionResult !is PhoneActionResult.NotACommand) {
                handlePhoneActionResult(actionResult, rawInput)
                return@launch
            }

            // 2. Check offline state
            if (!offlineEngine.isNetworkAvailable()) {
                val offlineReply = offlineEngine.processOffline(rawInput)
                saveZoriaReply(offlineReply, null, isRepeat)
                _assistantState.value = AssistantState.IDLE
                return@launch
            }

            // 3. Query Gemini Brain with selected Mode and Memories
            val currentContext = _conversationContext.value
            val currentHistory = messages.value
            val currentMemories = memories.value

            val result = brainClient.queryZoriaWithModeAndMemories(
                userInput = rawInput,
                context = currentContext,
                voice = _selectedVoice.value,
                history = currentHistory,
                mode = _selectedMode.value,
                memories = currentMemories
            )

            result.onSuccess { response ->
                saveZoriaReply(response.responseText, response.audioBase64, response.isSimplifiedRepeat)

                // Update ongoing context
                _conversationContext.value = ConversationContext(
                    lastTopic = if (isRepeat) currentContext.lastTopic else rawInput.take(60),
                    lastUserQuery = rawInput,
                    lastZoriaResponse = response.responseText,
                    preferredLanguage = response.detectedLanguage,
                    repeatCount = if (isRepeat) currentContext.repeatCount + 1 else 0
                )

                // Play genuine Gemini Native Audio
                if (!response.audioBase64.isNullOrEmpty()) {
                    _assistantState.value = AssistantState.SPEAKING
                    audioOutput.playGeminiAudio(
                        audioBase64 = response.audioBase64,
                        onPlaybackStarted = {
                            _assistantState.value = AssistantState.SPEAKING
                        },
                        onPlaybackCompleted = {
                            // Flow: IDLE -> LISTENING -> PROCESSING -> SPEAKING -> LISTENING (if continuous mode)
                            if (_isContinuousConversationEnabled.value) {
                                startListening()
                            } else {
                                _assistantState.value = AssistantState.IDLE
                            }
                        },
                        onError = {
                            if (_isContinuousConversationEnabled.value) {
                                startListening()
                            } else {
                                _assistantState.value = AssistantState.IDLE
                            }
                        }
                    )
                } else {
                    _assistantState.value = AssistantState.IDLE
                }
            }.onFailure { err ->
                Log.e(TAG, "ZORIA brain processing failed", err)
                _errorMessage.value = err.message ?: "نیٹ ورک میں خرابی پیش آئی۔"
                _assistantState.value = AssistantState.ERROR
            }
        }
    }

    private suspend fun handlePhoneActionResult(actionResult: PhoneActionResult, userQuery: String) {
        val replyText = when (actionResult) {
            is PhoneActionResult.Success -> actionResult.messageUrdu
            is PhoneActionResult.Failure -> actionResult.reasonUrdu
            is PhoneActionResult.HandledText -> actionResult.responseText
            PhoneActionResult.NotACommand -> ""
        }

        saveZoriaReply(replyText, null, false)
        _conversationContext.value = _conversationContext.value.copy(
            lastTopic = userQuery,
            lastZoriaResponse = replyText
        )
        _assistantState.value = AssistantState.IDLE
    }

    private suspend fun saveZoriaReply(text: String, audioBase64: String?, isRepeat: Boolean) {
        val zoriaMsg = MessageEntity(
            id = UUID.randomUUID().toString(),
            sender = "ZORIA",
            text = text,
            timestamp = System.currentTimeMillis(),
            language = detectLanguage(text),
            audioBase64 = audioBase64,
            isSimplifiedRepeat = isRepeat,
            voiceUsed = _selectedVoice.value.voiceName
        )
        messageDao.insertMessage(zoriaMsg)
    }

    private fun inspectAndStoreMemory(input: String) {
        val lower = input.lowercase()
        viewModelScope.launch {
            if (lower.contains("mera naam") || lower.contains("my name is") || lower.contains("میرا نام")) {
                val entity = MemoryEntity(
                    id = UUID.randomUUID().toString(),
                    key = "User Name",
                    value = input,
                    category = "USER_INFO"
                )
                memoryDao.insertMemory(entity)
            } else if (lower.contains("pasand") || lower.contains("i like") || lower.contains("مجھے پسند")) {
                val entity = MemoryEntity(
                    id = UUID.randomUUID().toString(),
                    key = "Preference",
                    value = input,
                    category = "PREFERENCE"
                )
                memoryDao.insertMemory(entity)
            }
        }
    }

    fun deleteMemory(id: String) {
        viewModelScope.launch {
            memoryDao.deleteMemory(id)
        }
    }

    fun clearAllMemories() {
        viewModelScope.launch {
            memoryDao.clearAllMemories()
        }
    }

    fun requestRepeatOrSimplify() {
        val currentContext = _conversationContext.value
        if (currentContext.lastZoriaResponse.isBlank()) {
            sendTextMessage("دوبارہ بتائیں")
        } else {
            sendTextMessage("مجھے سمجھ نہیں آیا، آسان لفظوں میں دوبارہ بتاؤ")
        }
    }

    fun setVoice(voice: ZoriaVoice) {
        _selectedVoice.value = voice
    }

    fun saveCustomApiKey(key: String) {
        brainClient.saveCustomApiKey(key)
        checkApiKeyStatus()
        _errorMessage.value = null
        if (_assistantState.value == AssistantState.ERROR) {
            _assistantState.value = AssistantState.IDLE
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            stopAll()
            messageDao.clearAllMessages()
            initializeInitialGreetingIfNeeded()
        }
    }

    private fun detectLanguage(text: String): String {
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

    override fun onCleared() {
        super.onCleared()
        audioInput.destroy()
        audioOutput.release()
    }
}
