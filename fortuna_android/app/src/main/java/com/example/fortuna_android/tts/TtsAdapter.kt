package com.example.fortuna_android.tts

/**
 * TTS (Text-to-Speech) adapter interface
 *
 * Provides an abstraction layer for different TTS implementations.
 * This allows easy swapping between Android native TTS, OpenAI TTS,
 * ElevenLabs, or any other TTS service.
 */
interface TtsAdapter {
    /**
     * Speak the given text
     * @param text The text to be spoken
     */
    fun speak(text: String)

    /**
     * Stop speaking immediately
     */
    fun stop()

    /**
     * Check if TTS is currently speaking
     * @return true if speaking, false otherwise
     */
    fun isPlaying(): Boolean

    /**
     * Set a listener to be called when TTS completes speaking
     * @param listener Callback invoked when speaking is done
     */
    fun setOnCompleteListener(listener: () -> Unit)

    /**
     * Release resources used by TTS
     * Should be called when the TTS is no longer needed
     */
    fun release()
}
