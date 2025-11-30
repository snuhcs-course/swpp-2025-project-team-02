package com.example.fortuna_android.tts

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for TtsAdapter interface
 *
 * Tests interface contract and mock implementations
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class TtsAdapterTest {

    private lateinit var mockAdapter: MockTtsAdapter
    private var listenerCalled = false

    @Before
    fun setup() {
        mockAdapter = MockTtsAdapter()
        listenerCalled = false
    }

    @Test
    fun testSpeak_callsSpeakMethod() {
        mockAdapter.speak("Test text")

        assertTrue("speak() should have been called", mockAdapter.speakCalled)
        assertEquals("Test text", mockAdapter.lastSpokenText)
    }

    @Test
    fun testSpeak_emptyText() {
        mockAdapter.speak("")

        assertTrue("speak() should accept empty string", mockAdapter.speakCalled)
        assertEquals("", mockAdapter.lastSpokenText)
    }

    @Test
    fun testStop_callsStopMethod() {
        mockAdapter.stop()

        assertTrue("stop() should have been called", mockAdapter.stopCalled)
    }

    @Test
    fun testIsPlaying_returnsFalseByDefault() {
        assertFalse("isPlaying() should return false by default", mockAdapter.isPlaying())
    }

    @Test
    fun testIsPlaying_returnsTrueWhenPlaying() {
        mockAdapter.isCurrentlyPlaying = true

        assertTrue("isPlaying() should return true when playing", mockAdapter.isPlaying())
    }

    @Test
    fun testSetOnCompleteListener_storesListener() {
        var called = false
        mockAdapter.setOnCompleteListener { called = true }

        assertNotNull("Listener should be stored", mockAdapter.completeListener)

        // Invoke the listener
        mockAdapter.completeListener?.invoke()
        assertTrue("Listener should be invoked", called)
    }

    @Test
    fun testRelease_callsReleaseMethod() {
        mockAdapter.release()

        assertTrue("release() should have been called", mockAdapter.releaseCalled)
    }

    @Test
    fun testFullLifecycle() {
        // Set listener
        mockAdapter.setOnCompleteListener { listenerCalled = true }

        // Start speaking
        mockAdapter.speak("Full lifecycle test")
        mockAdapter.isCurrentlyPlaying = true
        assertTrue(mockAdapter.isPlaying())

        // Stop
        mockAdapter.stop()
        mockAdapter.isCurrentlyPlaying = false
        assertFalse(mockAdapter.isPlaying())

        // Release
        mockAdapter.release()
        assertTrue(mockAdapter.releaseCalled)
    }

    /**
     * Mock implementation of TtsAdapter for testing
     */
    private class MockTtsAdapter : TtsAdapter {
        var speakCalled = false
        var stopCalled = false
        var releaseCalled = false
        var lastSpokenText: String? = null
        var isCurrentlyPlaying = false
        var completeListener: (() -> Unit)? = null

        override fun speak(text: String) {
            speakCalled = true
            lastSpokenText = text
        }

        override fun stop() {
            stopCalled = true
            isCurrentlyPlaying = false
            completeListener?.invoke()
        }

        override fun isPlaying(): Boolean {
            return isCurrentlyPlaying
        }

        override fun setOnCompleteListener(listener: () -> Unit) {
            completeListener = listener
        }

        override fun release() {
            releaseCalled = true
        }
    }
}
