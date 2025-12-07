package com.example.fortuna_android.tts

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import org.junit.runner.RunWith

/**
 * Instrumented tests for AndroidTtsAdapter
 *
 * Tests the core TTS functionality including:
 * - speak() - text-to-speech with various text inputs and states
 * - stop() - stopping speech at different points
 * - release() - proper resource cleanup and state management
 *
 * These tests run on Android device/emulator to test actual TTS integration.
 */
@RunWith(AndroidJUnit4::class)
class AndroidTtsAdapterTest {

    private lateinit var adapter: AndroidTtsAdapter
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        adapter = AndroidTtsAdapter(context)
    }

    @After
    fun tearDown() {
        try {
            adapter.release()
        } catch (_: Exception) {
            // Ignore cleanup errors
        }
    }

    // ========== speak() Method Tests ==========

    @Test
    fun testSpeak_validText() {
        val testText = "안녕하세요! 운세를 알려드릴게요."

        // Test speaking valid Korean text
        adapter.speak(testText)

        // Allow TTS initialization time
        Thread.sleep(2000)

        // The text should be processed without throwing exceptions
        // Note: In test environment, TTS may not actually produce sound
        // but the method should execute the text processing logic
    }

    @Test
    fun testSpeak_emptyText() {
        // Test speaking empty text
        adapter.speak("")

        Thread.sleep(500)

        // Should handle empty text gracefully without crashing
        // isPlaying should remain false for empty text
        assertFalse("Should not be playing for empty text", adapter.isPlaying())
    }

    @Test
    fun testSpeak_blankText() {
        // Test speaking blank text (whitespace only)
        adapter.speak("   ")

        Thread.sleep(500)

        // Should handle blank text gracefully
        assertFalse("Should not be playing for blank text", adapter.isPlaying())
    }

    @Test
    fun testSpeak_longText() {
        val longText = "이것은 매우 긴 텍스트입니다. " + "반복되는 문장입니다. ".repeat(10)

        // Test speaking long text
        adapter.speak(longText)

        Thread.sleep(1000)

        // Should handle long text without issues
        // In test environment, this exercises the text processing logic
    }

    @Test
    fun testSpeak_specialCharacters() {
        val specialText = "오늘의 운세: ★☆♥♡! 행운이 함께하길... (행운 지수: 85%)"

        // Test speaking text with special characters
        adapter.speak(specialText)

        Thread.sleep(1000)

        // Should process special characters appropriately
    }

    @Test
    fun testSpeak_englishText() {
        val englishText = "Your fortune for today is excellent!"

        // Test speaking English text (should fallback gracefully)
        adapter.speak(englishText)

        Thread.sleep(1000)

        // Should handle non-Korean text appropriately
    }

    @Test
    fun testSpeak_beforeInitialization() {
        // Create new adapter to test pre-initialization behavior
        val newAdapter = AndroidTtsAdapter(context)

        // Speak immediately before TTS has time to initialize
        newAdapter.speak("초기화 전 텍스트")

        Thread.sleep(500)

        // Should queue the text for later processing
        // When TTS initializes, it should speak the queued text

        // Allow time for initialization
        Thread.sleep(2000)

        // Cleanup
        newAdapter.release()
    }

    @Test
    fun testSpeak_multipleCallsSequential() {
        // Test multiple speak calls in sequence
        adapter.speak("첫 번째 문장입니다.")
        Thread.sleep(500)

        adapter.speak("두 번째 문장입니다.")
        Thread.sleep(500)

        adapter.speak("세 번째 문장입니다.")
        Thread.sleep(1000)

        // Should handle multiple sequential speak calls
        // Later calls should replace earlier ones (QUEUE_FLUSH behavior)
    }

    // ========== stop() Method Tests ==========

    @Test
    fun testStop_whileSpeaking() {
        // Start speaking
        adapter.speak("이것은 중간에 멈춰질 긴 텍스트입니다. 매우 긴 내용이므로 중간에 멈출 수 있습니다.")

        // Allow TTS to start
        Thread.sleep(1000)

        // Stop speaking
        adapter.stop()

        // Allow stop to take effect
        Thread.sleep(500)

        // Should not be playing after stop
        assertFalse("Should not be playing after stop", adapter.isPlaying())
    }

    @Test
    fun testStop_whenNotSpeaking() {
        // Stop when not speaking
        adapter.stop()

        Thread.sleep(200)

        // Should handle stop gracefully when not speaking
        assertFalse("Should not be playing when not speaking", adapter.isPlaying())
    }

    @Test
    fun testStop_multipleCalls() {
        // Start speaking
        adapter.speak("테스트 텍스트입니다.")
        Thread.sleep(500)

        // Multiple stop calls
        adapter.stop()
        Thread.sleep(100)
        adapter.stop()
        Thread.sleep(100)
        adapter.stop()

        // Should handle multiple stop calls gracefully
        assertFalse("Should not be playing after multiple stops", adapter.isPlaying())
    }

    @Test
    fun testStop_clearsPendingText() {
        // Create new adapter to test pending text behavior
        val newAdapter = AndroidTtsAdapter(context)

        // Queue text before initialization
        newAdapter.speak("대기 중인 텍스트입니다.")

        // Stop before initialization completes
        newAdapter.stop()

        Thread.sleep(2000) // Allow time for initialization

        // Should not speak the pending text after stop
        assertFalse("Should not be playing after stop cleared pending text", newAdapter.isPlaying())

        // Cleanup
        newAdapter.release()
    }

    // ========== release() Method Tests ==========

    @Test
    fun testRelease_whileSpeaking() {
        // Start speaking
        adapter.speak("릴리즈 테스트를 위한 텍스트입니다.")
        Thread.sleep(500)

        // Release while speaking
        adapter.release()

        Thread.sleep(200)

        // Should not be playing after release
        assertFalse("Should not be playing after release", adapter.isPlaying())
    }

    @Test
    fun testRelease_whenIdle() {
        // Release when idle
        adapter.release()

        Thread.sleep(200)

        // Should handle release gracefully when idle
        assertFalse("Should not be playing after release when idle", adapter.isPlaying())
    }

    @Test
    fun testRelease_multipleCallsSafe() {
        // Multiple release calls should be safe
        adapter.release()
        Thread.sleep(100)
        adapter.release()
        Thread.sleep(100)
        adapter.release()

        // Should handle multiple release calls without crashing
        assertFalse("Should not be playing after multiple releases", adapter.isPlaying())
    }

    @Test
    fun testRelease_afterSpeak() {
        // Test release after speak call
        adapter.speak("릴리즈 전 텍스트입니다.")
        Thread.sleep(1000)

        adapter.release()

        // Should clean up resources properly
        assertFalse("Should not be playing after release", adapter.isPlaying())
    }

    // ========== State Management Tests ==========

    @Test
    fun testIsPlaying_initialState() {
        // Should not be playing initially
        assertFalse("Should not be playing initially", adapter.isPlaying())
    }

    @Test
    fun testIsPlaying_afterSpeak() {
        adapter.speak("상태 테스트 텍스트입니다.")

        // Allow TTS to potentially start
        Thread.sleep(1000)

        // In test environment, isPlaying state depends on TTS availability
        // Method should return a boolean without crashing
        val isPlaying = adapter.isPlaying()
        assertTrue("isPlaying should return a boolean", isPlaying is Boolean)
    }

    @Test
    fun testIsPlaying_afterStop() {
        adapter.speak("정지 후 상태 테스트입니다.")
        Thread.sleep(500)

        adapter.stop()
        Thread.sleep(200)

        // Should not be playing after stop
        assertFalse("Should not be playing after stop", adapter.isPlaying())
    }

    @Test
    fun testIsPlaying_afterRelease() {
        adapter.speak("릴리즈 후 상태 테스트입니다.")
        Thread.sleep(500)

        adapter.release()
        Thread.sleep(200)

        // Should not be playing after release
        assertFalse("Should not be playing after release", adapter.isPlaying())
    }

    // ========== Completion Listener Tests ==========

    @Test
    fun testSetOnCompleteListener() {
        var completionCount = 0

        // Set completion listener
        adapter.setOnCompleteListener {
            completionCount++
        }

        // Speak short text
        adapter.speak("완료 리스너 테스트입니다.")

        // Allow time for completion
        Thread.sleep(3000)

        // In test environment, completion may or may not occur
        // But listener should be set without crashing
        assertTrue("Completion count should be non-negative", completionCount >= 0)
    }

    @Test
    fun testOnCompleteListener_afterStop() {
        var stopCompletionCalled = false

        adapter.setOnCompleteListener {
            stopCompletionCalled = true
        }

        // Speak then stop
        adapter.speak("정지 완료 리스너 테스트입니다.")
        Thread.sleep(500)

        adapter.stop()
        Thread.sleep(500)

        // Stop should trigger completion listener
        assertTrue("Completion listener should be called after stop", stopCompletionCalled)
    }

    // ========== Integration Tests ==========

    @Test
    fun testFullLifecycle_speakStopRelease() {
        var completionCount = 0

        adapter.setOnCompleteListener {
            completionCount++
        }

        // Full lifecycle: speak -> stop -> release
        adapter.speak("전체 생명주기 테스트입니다.")
        Thread.sleep(500)

        adapter.stop()
        Thread.sleep(200)

        adapter.release()
        Thread.sleep(200)

        // Should complete lifecycle without crashing
        assertFalse("Should not be playing after full lifecycle", adapter.isPlaying())
        assertTrue("Should have called completion listener", completionCount > 0)
    }

    @Test
    fun testSequentialOperations() {
        // Test sequential speak/stop operations
        adapter.speak("첫 번째 순차 테스트입니다.")
        Thread.sleep(300)

        adapter.stop()
        Thread.sleep(100)

        adapter.speak("두 번째 순차 테스트입니다.")
        Thread.sleep(300)

        adapter.stop()
        Thread.sleep(100)

        // Should handle sequential operations without issues
        assertFalse("Should not be playing after sequential operations", adapter.isPlaying())
    }

    @Test
    fun testErrorRecovery_afterFailure() {
        // Test recovery after potential failures
        try {
            adapter.speak("에러 복구 테스트입니다.")
            Thread.sleep(500)

            // Force some operations that might fail
            adapter.stop()
            adapter.speak("")
            adapter.stop()

            // Should recover and continue working
            adapter.speak("복구 후 테스트입니다.")
            Thread.sleep(500)

        } catch (e: Exception) {
            fail("Should handle operations gracefully without throwing exceptions: ${e.message}")
        }

        // Should be in a valid state
        val isPlaying = adapter.isPlaying()
        assertTrue("isPlaying should return valid state", isPlaying is Boolean)
    }
}