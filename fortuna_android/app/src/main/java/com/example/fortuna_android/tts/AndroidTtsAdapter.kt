package com.example.fortuna_android.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.*

/**
 * Android native TTS implementation
 *
 * Wraps Android's TextToSpeech API to conform to TtsAdapter interface.
 * Provides low-latency, offline TTS with customizable pitch and speech rate.
 */
class AndroidTtsAdapter(
    private val context: Context,
    private val pitch: Float = 1.0f,  // 1.0 = normal, >1.0 = higher, <1.0 = lower
    private val speechRate: Float = 1.0f  // 1.0 = normal, >1.0 = faster, <1.0 = slower
) : TtsAdapter {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var isSpeaking = false
    private var onCompleteListener: (() -> Unit)? = null

    companion object {
        private const val TAG = "AndroidTtsAdapter"
        private const val UTTERANCE_ID = "fortune_tts"
    }

    init {
        initializeTts()
    }

    private fun initializeTts() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.KOREAN)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "Korean language is not supported")
                    isInitialized = false
                } else {
                    // Set pitch and speech rate
                    tts?.setPitch(pitch)
                    tts?.setSpeechRate(speechRate)
                    isInitialized = true
                    Log.d(TAG, "TTS initialized successfully (pitch: $pitch, rate: $speechRate)")
                }
            } else {
                Log.e(TAG, "TTS initialization failed with status: $status")
                isInitialized = false
            }
        }

        // Set up utterance progress listener
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                isSpeaking = true
                Log.d(TAG, "TTS started speaking")
            }

            override fun onDone(utteranceId: String?) {
                isSpeaking = false
                Log.d(TAG, "TTS completed speaking")
                onCompleteListener?.invoke()
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                isSpeaking = false
                Log.e(TAG, "TTS error occurred")
                onCompleteListener?.invoke()
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                isSpeaking = false
                Log.e(TAG, "TTS error occurred with code: $errorCode")
                onCompleteListener?.invoke()
            }
        })
    }

    override fun speak(text: String) {
        if (!isInitialized) {
            Log.w(TAG, "TTS not initialized, cannot speak")
            return
        }

        if (text.isBlank()) {
            Log.w(TAG, "Text is empty, nothing to speak")
            return
        }

        Log.d(TAG, "Speaking text: ${text.take(50)}...")
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
    }

    override fun stop() {
        if (isSpeaking) {
            Log.d(TAG, "Stopping TTS")
            tts?.stop()
            isSpeaking = false
            onCompleteListener?.invoke()
        }
    }

    override fun isPlaying(): Boolean {
        return isSpeaking
    }

    override fun setOnCompleteListener(listener: () -> Unit) {
        onCompleteListener = listener
    }

    override fun release() {
        Log.d(TAG, "Releasing TTS resources")
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
        isSpeaking = false
    }
}
