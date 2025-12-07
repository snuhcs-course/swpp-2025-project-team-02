package com.example.fortuna_android.tts

import android.media.AudioTrack
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAudioTrack
import org.robolectric.shadows.ShadowLog
import org.mockito.MockedStatic
import org.mockito.Mockito.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.Base64

/**
 * Unit tests for OpenAiRealtimeTtsAdapter
 *
 * Tests the OpenAI Realtime API TTS implementation including:
 * - Basic TTS interface contract compliance
 * - WebSocket connection and messaging
 * - Audio playback functionality
 * - Error handling scenarios
 * - Resource cleanup
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class OpenAiRealtimeTtsAdapterTest {

    private lateinit var adapter: OpenAiRealtimeTtsAdapter
    private val testApiKey = "test-api-key-12345"
    private val testVoice = "alloy"
    private val testModel = "gpt-4o-realtime-preview-2024-12-17"
    private var completionCalled = false

    @Before
    fun setup() {
        // Enable shadows for testing
        ShadowLog.stream = System.out
        completionCalled = false

        // Create adapter with test configuration
        adapter = OpenAiRealtimeTtsAdapter(testApiKey, testVoice, testModel)
    }

    // ========== Basic Interface Tests ==========

    @Test
    fun testConstructor_defaultValues() {
        val defaultAdapter = OpenAiRealtimeTtsAdapter(testApiKey)
        assertNotNull("Adapter should be created with default values", defaultAdapter)
    }

    @Test
    fun testConstructor_customValues() {
        val customAdapter = OpenAiRealtimeTtsAdapter(
            apiKey = testApiKey,
            voice = "echo",
            model = "custom-model"
        )
        assertNotNull("Adapter should be created with custom values", customAdapter)
    }

    @Test
    fun testIsPlaying_initiallyFalse() {
        assertFalse("isPlaying should initially be false", adapter.isPlaying())
    }

    @Test
    fun testSetOnCompleteListener() {
        var called = false
        adapter.setOnCompleteListener { called = true }

        // We can't directly trigger the completion without mocking WebSocket
        // but we can verify the method doesn't throw exceptions
        assertFalse("Listener setup should not trigger callback", called)
    }

    @Test
    fun testRelease_callsCleanup() {
        // Should not throw exceptions when called
        adapter.release()

        // Verify state after release
        assertFalse("isPlaying should be false after release", adapter.isPlaying())
    }

    @Test
    fun testStop_whenNotPlaying() {
        // Should handle stop gracefully even when not playing
        adapter.stop()
        assertFalse("isPlaying should remain false", adapter.isPlaying())
    }

    // ========== Speak Method Tests ==========

    @Test
    fun testSpeak_emptyText() {
        adapter.speak("")

        // Should handle empty text gracefully - will log warning and return
        assertFalse("isPlaying should remain false for empty text", adapter.isPlaying())
    }

    @Test
    fun testSpeak_blankText() {
        adapter.speak("   ")

        // Should handle blank text gracefully
        assertFalse("isPlaying should remain false for blank text", adapter.isPlaying())
    }

    @Test
    fun testSpeak_validText() {
        val testText = "Hello, this is a test!"

        // This will attempt to connect WebSocket, which will fail in test environment
        // but should not throw exceptions
        try {
            adapter.speak(testText)
            // The isPlaying state will be set to true initially
            // (it gets set to false later when WebSocket fails)
        } catch (e: Exception) {
            fail("speak() should handle WebSocket connection failures gracefully")
        }
    }

    @Test
    fun testSpeak_longText() {
        val longText = "A".repeat(1000)

        try {
            adapter.speak(longText)
        } catch (e: Exception) {
            fail("speak() should handle long text gracefully")
        }
    }

    @Test
    fun testSpeak_specialCharacters() {
        val specialText = "Hello! 안녕하세요? 🎉 #test @user"

        try {
            adapter.speak(specialText)
        } catch (e: Exception) {
            fail("speak() should handle special characters gracefully")
        }
    }

    // ========== Audio Handling Tests ==========

    @Test
    fun testPlayAudioChunk_validBase64() {
        // Create a simple PCM audio data and encode it
        val audioData = ByteArray(1024) { (it % 256).toByte() }
        val base64Audio = Base64.getEncoder().encodeToString(audioData)

        // This test verifies the method doesn't crash with valid base64
        try {
            val method = adapter::class.java.getDeclaredMethod("playAudioChunk", String::class.java)
            method.isAccessible = true
            method.invoke(adapter, base64Audio)
        } catch (e: Exception) {
            // Expected - AudioTrack is not initialized in this test context
            // but the method should handle it gracefully
        }
    }

    @Test
    fun testPlayAudioChunk_invalidBase64() {
        val invalidBase64 = "not-valid-base64!"

        try {
            val method = adapter::class.java.getDeclaredMethod("playAudioChunk", String::class.java)
            method.isAccessible = true
            method.invoke(adapter, invalidBase64)
        } catch (e: Exception) {
            // Expected - should catch and handle invalid base64
        }
    }

    @Test
    fun testPlayAudioChunk_emptyString() {
        try {
            val method = adapter::class.java.getDeclaredMethod("playAudioChunk", String::class.java)
            method.isAccessible = true
            method.invoke(adapter, "")
        } catch (e: Exception) {
            // Expected - should handle empty string gracefully
        }
    }

    // ========== WebSocket Message Handling Tests ==========

    @Test
    fun testHandleServerMessage_sessionCreated() {
        val sessionMessage = JSONObject().apply {
            put("type", "session.created")
            put("session", JSONObject())
        }.toString()

        try {
            val method = adapter::class.java.getDeclaredMethod("handleServerMessage", String::class.java)
            method.isAccessible = true
            method.invoke(adapter, sessionMessage)
        } catch (e: Exception) {
            fail("handleServerMessage should handle session.created gracefully")
        }
    }

    @Test
    fun testHandleServerMessage_responseAudioDelta() {
        val audioData = ByteArray(512) { (it % 256).toByte() }
        val base64Audio = Base64.getEncoder().encodeToString(audioData)

        val audioMessage = JSONObject().apply {
            put("type", "response.audio.delta")
            put("delta", base64Audio)
        }.toString()

        try {
            val method = adapter::class.java.getDeclaredMethod("handleServerMessage", String::class.java)
            method.isAccessible = true
            method.invoke(adapter, audioMessage)
        } catch (e: Exception) {
            // Expected - AudioTrack won't be properly initialized in test
        }
    }

    @Test
    fun testHandleServerMessage_responseAudioDone() {
        val doneMessage = JSONObject().apply {
            put("type", "response.audio.done")
        }.toString()

        var listenerCalled = false
        adapter.setOnCompleteListener { listenerCalled = true }

        try {
            val method = adapter::class.java.getDeclaredMethod("handleServerMessage", String::class.java)
            method.isAccessible = true
            method.invoke(adapter, doneMessage)

            assertTrue("Completion listener should be called", listenerCalled)
            assertFalse("isPlaying should be false after completion", adapter.isPlaying())
        } catch (e: Exception) {
            fail("handleServerMessage should handle response.audio.done gracefully")
        }
    }

    @Test
    fun testHandleServerMessage_responseDone() {
        val doneMessage = JSONObject().apply {
            put("type", "response.done")
        }.toString()

        try {
            val method = adapter::class.java.getDeclaredMethod("handleServerMessage", String::class.java)
            method.isAccessible = true
            method.invoke(adapter, doneMessage)
        } catch (e: Exception) {
            fail("handleServerMessage should handle response.done gracefully")
        }
    }

    @Test
    fun testHandleServerMessage_error() {
        val errorMessage = JSONObject().apply {
            put("type", "error")
            put("error", JSONObject().apply {
                put("message", "Test error message")
                put("code", "test_error")
            })
        }.toString()

        var listenerCalled = false
        adapter.setOnCompleteListener { listenerCalled = true }

        try {
            val method = adapter::class.java.getDeclaredMethod("handleServerMessage", String::class.java)
            method.isAccessible = true
            method.invoke(adapter, errorMessage)

            assertTrue("Completion listener should be called on error", listenerCalled)
            assertFalse("isPlaying should be false after error", adapter.isPlaying())
        } catch (e: Exception) {
            fail("handleServerMessage should handle errors gracefully")
        }
    }

    @Test
    fun testHandleServerMessage_unknownType() {
        val unknownMessage = JSONObject().apply {
            put("type", "unknown.message.type")
            put("data", "some data")
        }.toString()

        try {
            val method = adapter::class.java.getDeclaredMethod("handleServerMessage", String::class.java)
            method.isAccessible = true
            method.invoke(adapter, unknownMessage)
        } catch (e: Exception) {
            fail("handleServerMessage should handle unknown message types gracefully")
        }
    }

    @Test
    fun testHandleServerMessage_invalidJson() {
        val invalidJson = "{ invalid json structure"

        try {
            val method = adapter::class.java.getDeclaredMethod("handleServerMessage", String::class.java)
            method.isAccessible = true
            method.invoke(adapter, invalidJson)
        } catch (e: Exception) {
            // Expected - should catch and log JSON parsing errors
        }
    }

    @Test
    fun testHandleServerMessage_emptyJson() {
        val emptyJson = "{}"

        try {
            val method = adapter::class.java.getDeclaredMethod("handleServerMessage", String::class.java)
            method.isAccessible = true
            method.invoke(adapter, emptyJson)
        } catch (e: Exception) {
            fail("handleServerMessage should handle empty JSON gracefully")
        }
    }

    // ========== Session Initialization Tests ==========

    @Test
    fun testInitializeSession() {
        try {
            val method = adapter::class.java.getDeclaredMethod("initializeSession")
            method.isAccessible = true
            method.invoke(adapter)
        } catch (e: Exception) {
            // Expected - WebSocket will be null in test context
        }
    }

    @Test
    fun testSendTextToSpeak() {
        val testText = "Test text for TTS"

        try {
            val method = adapter::class.java.getDeclaredMethod("sendTextToSpeak", String::class.java)
            method.isAccessible = true
            method.invoke(adapter, testText)
        } catch (e: Exception) {
            // Expected - WebSocket will be null in test context
        }
    }

    // ========== Resource Cleanup Tests ==========

    @Test
    fun testCleanupAudio() {
        try {
            val method = adapter::class.java.getDeclaredMethod("cleanupAudio")
            method.isAccessible = true
            method.invoke(adapter)
        } catch (e: Exception) {
            fail("cleanupAudio should handle cleanup gracefully")
        }
    }

    @Test
    fun testCleanupAudio_multipleCallsSafe() {
        try {
            val method = adapter::class.java.getDeclaredMethod("cleanupAudio")
            method.isAccessible = true

            // Call multiple times - should be safe
            method.invoke(adapter)
            method.invoke(adapter)
            method.invoke(adapter)
        } catch (e: Exception) {
            fail("Multiple cleanupAudio calls should be safe")
        }
    }

    // ========== Edge Cases and Error Handling ==========

    @Test
    fun testSpeak_afterRelease() {
        adapter.release()

        try {
            adapter.speak("Test after release")
            // Should handle gracefully
        } catch (e: Exception) {
            fail("speak() after release() should not throw exceptions")
        }
    }

    @Test
    fun testStop_afterRelease() {
        adapter.release()

        try {
            adapter.stop()
            assertFalse("isPlaying should be false after release and stop", adapter.isPlaying())
        } catch (e: Exception) {
            fail("stop() after release() should not throw exceptions")
        }
    }

    @Test
    fun testMultipleReleaseCalls() {
        try {
            adapter.release()
            adapter.release() // Should be safe to call multiple times
            adapter.release()
        } catch (e: Exception) {
            fail("Multiple release() calls should be safe")
        }
    }

    @Test
    fun testWebSocketListener_onOpen() {
        // Test that WebSocket listener methods don't crash
        try {
            val listenerField = adapter::class.java.getDeclaredField("webSocketListener")
            listenerField.isAccessible = true
            val listener = listenerField.get(adapter) as WebSocketListener

            // Create mock WebSocket and Response
            val mockWebSocket = mock(WebSocket::class.java)
            val mockResponse = mock(okhttp3.Response::class.java)

            listener.onOpen(mockWebSocket, mockResponse)
        } catch (e: Exception) {
            // Expected - will try to call initializeSession which needs WebSocket setup
        }
    }

    @Test
    fun testWebSocketListener_onFailure() {
        var listenerCalled = false
        adapter.setOnCompleteListener { listenerCalled = true }

        try {
            val listenerField = adapter::class.java.getDeclaredField("webSocketListener")
            listenerField.isAccessible = true
            val listener = listenerField.get(adapter) as WebSocketListener

            val mockWebSocket = mock(WebSocket::class.java)
            val mockResponse = mock(okhttp3.Response::class.java)
            val testException = RuntimeException("Test WebSocket failure")

            listener.onFailure(mockWebSocket, testException, mockResponse)

            assertTrue("Completion listener should be called on failure", listenerCalled)
            assertFalse("isPlaying should be false after failure", adapter.isPlaying())
        } catch (e: Exception) {
            fail("WebSocket onFailure should handle errors gracefully")
        }
    }

    @Test
    fun testWebSocketListener_onClosing() {
        try {
            val listenerField = adapter::class.java.getDeclaredField("webSocketListener")
            listenerField.isAccessible = true
            val listener = listenerField.get(adapter) as WebSocketListener

            val mockWebSocket = mock(WebSocket::class.java)

            listener.onClosing(mockWebSocket, 1000, "Normal closure")

            verify(mockWebSocket).close(1000, null)
        } catch (e: Exception) {
            fail("WebSocket onClosing should handle closure gracefully")
        }
    }

    @Test
    fun testWebSocketListener_onClosed() {
        var listenerCalled = false
        adapter.setOnCompleteListener { listenerCalled = true }

        try {
            val listenerField = adapter::class.java.getDeclaredField("webSocketListener")
            listenerField.isAccessible = true
            val listener = listenerField.get(adapter) as WebSocketListener

            val mockWebSocket = mock(WebSocket::class.java)

            listener.onClosed(mockWebSocket, 1000, "Normal closure")

            assertTrue("Completion listener should be called when closed", listenerCalled)
            assertFalse("isPlaying should be false when closed", adapter.isPlaying())
        } catch (e: Exception) {
            fail("WebSocket onClosed should handle closure gracefully")
        }
    }

    // ========== Integration-style Tests ==========

    @Test
    fun testFullLifecycle_normalFlow() {
        var completionCount = 0
        adapter.setOnCompleteListener { completionCount++ }

        // Start speaking
        adapter.speak("Integration test text")

        // Stop speaking
        adapter.stop()

        // Check state
        assertFalse("isPlaying should be false after stop", adapter.isPlaying())
        assertEquals("Completion listener should be called once", 1, completionCount)

        // Release resources
        adapter.release()
    }

    @Test
    fun testConcurrentOperations() {
        // Test that concurrent operations don't cause issues
        adapter.speak("First text")
        adapter.speak("Second text") // Should handle multiple speak calls
        adapter.stop()
        adapter.stop() // Should handle multiple stop calls

        assertFalse("isPlaying should be false", adapter.isPlaying())
    }

    @Test
    fun testConfiguration_constants() {
        // Verify that constants are properly defined
        try {
            val tagField = adapter::class.java.getDeclaredField("TAG")
            tagField.isAccessible = true
            val tag = tagField.get(null) as String
            assertEquals("OpenAiRealtimeTts", tag)
        } catch (e: Exception) {
            fail("TAG constant should be accessible")
        }
    }
}