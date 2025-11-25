package com.example.fortuna_android.tts

import android.util.Log
import android.widget.TextView

/**
 * Manages TTS playback for fortune card text sections
 *
 * Handles:
 * - Toggle play/stop when text is clicked
 * - Visual feedback (glow effect) on active TextView
 * - Ensures only one text section speaks at a time
 * - Automatic cleanup on completion
 */
class FortuneTtsManager(
    private val ttsAdapter: TtsAdapter
) {
    companion object {
        private const val TAG = "FortuneTtsManager"
    }

    private var currentPlayingView: TextView? = null
    private var currentPlayingText: String? = null

    /**
     * Handle TextView click for TTS
     * - If already playing this text: stop
     * - If playing different text: stop previous, start new
     * - If not playing: start speaking
     *
     * @param textView The TextView that was clicked
     * @param glowBackground Resource ID for glow drawable (active state)
     * @param normalBackground Resource ID for normal drawable (inactive state)
     */
    fun handleTextClick(
        textView: TextView,
        glowBackground: Int,
        normalBackground: Int
    ) {
        val text = textView.text.toString()

        if (text.isBlank()) {
            Log.w(TAG, "TextView has no text, ignoring click")
            return
        }

        // If this view is already playing, stop it
        if (currentPlayingView == textView && ttsAdapter.isPlaying()) {
            Log.d(TAG, "Stopping TTS for current view")
            stopTts(normalBackground)
            return
        }

        // If another view is playing, stop it first
        if (currentPlayingView != null && ttsAdapter.isPlaying()) {
            Log.d(TAG, "Stopping TTS for previous view")
            stopTts(normalBackground)
        }

        // Start speaking new text
        Log.d(TAG, "Starting TTS for new text: ${text.take(30)}...")
        currentPlayingView = textView
        currentPlayingText = text

        // Apply glow effect
        textView.setBackgroundResource(glowBackground)

        // Set completion listener
        ttsAdapter.setOnCompleteListener {
            Log.d(TAG, "TTS completed, removing glow effect")
            textView.setBackgroundResource(normalBackground)
            currentPlayingView = null
            currentPlayingText = null
        }

        // Start speaking
        ttsAdapter.speak(text)
    }

    /**
     * Stop TTS and remove glow effect from current view
     *
     * @param normalBackground Resource ID for normal drawable
     */
    fun stopTts(normalBackground: Int) {
        ttsAdapter.stop()
        currentPlayingView?.setBackgroundResource(normalBackground)
        currentPlayingView = null
        currentPlayingText = null
    }

    /**
     * Check if any TTS is currently playing
     */
    fun isPlaying(): Boolean {
        return ttsAdapter.isPlaying()
    }

    /**
     * Speak text directly without UI binding
     * @param text The text to speak
     */
    fun speak(text: String) {
        Log.d(TAG, "Speaking text: ${text.take(50)}...")
        ttsAdapter.speak(text)
    }

    /**
     * Set completion listener for TTS
     * @param listener Callback invoked when speaking is done
     */
    fun setOnCompleteListener(listener: () -> Unit) {
        ttsAdapter.setOnCompleteListener(listener)
    }

    /**
     * Release TTS resources
     * Call this when the view is destroyed or no longer needed
     */
    fun release() {
        Log.d(TAG, "Releasing TTS manager")
        ttsAdapter.stop()
        ttsAdapter.release()
        currentPlayingView = null
        currentPlayingText = null
    }

    /**
     * Stop any active TTS when view becomes invisible
     * Call this from onDetachedFromWindow or when view is hidden
     *
     * @param normalBackground Resource ID for normal drawable
     */
    fun onViewDetached(normalBackground: Int) {
        if (isPlaying()) {
            Log.d(TAG, "View detached, stopping TTS")
            stopTts(normalBackground)
        }
    }
}
