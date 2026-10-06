package com.example.zoria.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream

class AudioOutputManager(private val context: Context) {

    companion object {
        private const val TAG = "AudioOutputManager"
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var mediaPlayer: MediaPlayer? = null

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private var onCompletionCallback: (() -> Unit)? = null

    // Audio Focus Request (Android O+)
    private var audioFocusRequest: AudioFocusRequest? = null

    /**
     * Plays genuine Gemini Native Audio decoded from base64 string.
     * Manages Android audio focus so background music is ducked/paused cleanly.
     */
    fun playGeminiAudio(
        audioBase64: String,
        onPlaybackStarted: () -> Unit = {},
        onPlaybackCompleted: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        stopPlayback()

        try {
            val audioBytes = Base64.decode(audioBase64, Base64.DEFAULT)
            if (audioBytes == null || audioBytes.isEmpty()) {
                onError("خالی آڈیو ڈیٹا موصول ہوا۔")
                return
            }

            requestAudioFocus()

            // Temporary file in private app cache
            val tempFile = File.createTempFile("zoria_voice_", ".mp3", context.cacheDir)
            FileOutputStream(tempFile).use { fos ->
                fos.write(audioBytes)
            }

            this.onCompletionCallback = onPlaybackCompleted

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_ASSISTANT)
                        .build()
                )
                setDataSource(tempFile.absolutePath)
                setOnPreparedListener { mp ->
                    mp.start()
                    _isSpeaking.value = true
                    onPlaybackStarted()
                    Log.d(TAG, "ZORIA native audio playback started.")
                }
                setOnCompletionListener {
                    _isSpeaking.value = false
                    tempFile.delete()
                    abandonAudioFocus()
                    Log.d(TAG, "ZORIA native audio playback completed.")
                    this@AudioOutputManager.onCompletionCallback?.invoke()
                }
                setOnErrorListener { _, what, extra ->
                    _isSpeaking.value = false
                    tempFile.delete()
                    abandonAudioFocus()
                    val msg = "آڈیو چلانے میں خرابی پیش آئی (Code $what, $extra)"
                    Log.e(TAG, msg)
                    onError(msg)
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            _isSpeaking.value = false
            abandonAudioFocus()
            Log.e(TAG, "Failed playing Gemini audio", e)
            onError("آڈیو چلانے میں دشواری: ${e.message}")
        }
    }

    private fun requestAudioFocus() {
        if (audioManager == null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setOnAudioFocusChangeListener { focusChange ->
                    if (focusChange == AudioManager.AUDIOFOCUS_LOSS || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                        stopPlayback()
                    }
                }
                .build()
            audioFocusRequest = req
            audioManager.requestAudioFocus(req)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
        }
    }

    private fun abandonAudioFocus() {
        if (audioManager == null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }

    /**
     * Clear and immediate STOP control.
     * Halts audio playback immediately and releases system audio focus.
     */
    fun stopPlayback() {
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.reset()
                player.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping playback", e)
        } finally {
            mediaPlayer = null
            _isSpeaking.value = false
            onCompletionCallback = null
            abandonAudioFocus()
        }
    }

    fun release() {
        stopPlayback()
    }
}
