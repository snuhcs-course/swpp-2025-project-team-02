package com.example.fortuna_android.tts

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for FortuneTtsManager
 */
class FortuneTtsManagerTest {

    private lateinit var mockAdapter: MockTtsAdapter
    private lateinit var manager: FortuneTtsManager

    /**
     * Simple mock TtsAdapter for testing
     */
    class MockTtsAdapter : TtsAdapter {
        var speakCalled = false
        var stopCalled = false
        var releaseCalled = false
        var lastSpokenText: String? = null
        var playing = false
        var completeListener: (() -> Unit)? = null

        override fun speak(text: String) {
            speakCalled = true
            lastSpokenText = text
            playing = true
        }

        override fun stop() {
            stopCalled = true
            playing = false
        }

        override fun isPlaying(): Boolean = playing

        override fun setOnCompleteListener(listener: () -> Unit) {
            completeListener = listener
        }

        override fun release() {
            releaseCalled = true
            playing = false
        }

        fun simulateComplete() {
            completeListener?.invoke()
            playing = false
        }
    }

    @Before
    fun setUp() {
        mockAdapter = MockTtsAdapter()
        manager = FortuneTtsManager(mockAdapter)
    }

    @Test
    fun `test speak calls adapter speak`() {
        manager.speak("Hello")
        assertTrue(mockAdapter.speakCalled)
        assertEquals("Hello", mockAdapter.lastSpokenText)
    }

    @Test
    fun `test isPlaying returns adapter state`() {
        assertFalse(manager.isPlaying())
        mockAdapter.playing = true
        assertTrue(manager.isPlaying())
    }

    @Test
    fun `test release calls adapter release and stop`() {
        manager.release()
        assertTrue(mockAdapter.stopCalled)
        assertTrue(mockAdapter.releaseCalled)
    }

    @Test
    fun `test setOnCompleteListener delegates to adapter`() {
        var called = false
        manager.setOnCompleteListener { called = true }
        mockAdapter.completeListener?.invoke()
        assertTrue(called)
    }

    @Test
    fun `test stopTts calls adapter stop`() {
        manager.stopTts(0)
        assertTrue(mockAdapter.stopCalled)
    }

    @Test
    fun `test speak with empty string`() {
        manager.speak("")
        assertTrue(mockAdapter.speakCalled)
        assertEquals("", mockAdapter.lastSpokenText)
    }

    @Test
    fun `test speak with long text`() {
        val longText = "A".repeat(1000)
        manager.speak(longText)
        assertTrue(mockAdapter.speakCalled)
        assertEquals(longText, mockAdapter.lastSpokenText)
    }

    @Test
    fun `test multiple speaks`() {
        manager.speak("First")
        manager.speak("Second")
        manager.speak("Third")
        assertEquals("Third", mockAdapter.lastSpokenText)
    }

    @Test
    fun `test onViewDetached when playing`() {
        mockAdapter.playing = true
        manager.onViewDetached(0)
        assertTrue(mockAdapter.stopCalled)
    }

    @Test
    fun `test onViewDetached when not playing`() {
        mockAdapter.playing = false
        manager.onViewDetached(0)
        assertFalse(mockAdapter.stopCalled)
    }

    @Test
    fun `test release stops before releasing`() {
        mockAdapter.playing = true
        manager.release()
        assertTrue(mockAdapter.stopCalled)
        assertTrue(mockAdapter.releaseCalled)
    }

    @Test
    fun `test manager creation with adapter`() {
        val adapter = MockTtsAdapter()
        val mgr = FortuneTtsManager(adapter)
        assertNotNull(mgr)
    }
}
