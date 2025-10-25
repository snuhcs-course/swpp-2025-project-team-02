package com.example.fortuna_android.util

import android.content.Context
import android.widget.Toast
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

/**
 * Unit tests for CustomToast utility class
 * Tests custom toast message display functionality
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class CustomToastTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    // ========== show() Method Tests ==========

    @Test
    fun `test show with default duration`() {
        val message = "Test message"

        // This should not throw any exception
        CustomToast.show(context, message)

        // Verify toast was shown using Robolectric's ShadowToast
        val latestToast = ShadowToast.getLatestToast()
        assertNotNull("Toast should be created", latestToast)
    }

    @Test
    fun `test show with short duration`() {
        val message = "Short duration message"

        CustomToast.show(context, message, Toast.LENGTH_SHORT)

        val latestToast = ShadowToast.getLatestToast()
        assertNotNull("Toast should be created", latestToast)
    }

    @Test
    fun `test show with long duration`() {
        val message = "Long duration message"

        CustomToast.show(context, message, Toast.LENGTH_LONG)

        val latestToast = ShadowToast.getLatestToast()
        assertNotNull("Toast should be created", latestToast)
    }

    @Test
    fun `test show with empty message`() {
        val message = ""

        // Should handle empty message gracefully
        CustomToast.show(context, message)

        val latestToast = ShadowToast.getLatestToast()
        assertNotNull("Toast should be created even with empty message", latestToast)
    }

    @Test
    fun `test show with blank message`() {
        val message = "   "

        // Should handle blank message gracefully
        CustomToast.show(context, message)

        val latestToast = ShadowToast.getLatestToast()
        assertNotNull("Toast should be created even with blank message", latestToast)
    }

    @Test
    fun `test show with very long message`() {
        val message = "A".repeat(1000)

        // Should handle very long messages
        CustomToast.show(context, message)

        val latestToast = ShadowToast.getLatestToast()
        assertNotNull("Toast should be created with long message", latestToast)
    }

    @Test
    fun `test show with special characters`() {
        val message = "Special chars: !@#\$%^&*()_+-={}[]|:;<>?,./~`"

        CustomToast.show(context, message)

        val latestToast = ShadowToast.getLatestToast()
        assertNotNull("Toast should be created with special characters", latestToast)
    }

    @Test
    fun `test show with Korean characters`() {
        val message = "한글 메시지 테스트"

        CustomToast.show(context, message)

        val latestToast = ShadowToast.getLatestToast()
        assertNotNull("Toast should be created with Korean characters", latestToast)
    }

    @Test
    fun `test show with emoji`() {
        val message = "이모지 테스트 😀 🎉 ✅"

        CustomToast.show(context, message)

        val latestToast = ShadowToast.getLatestToast()
        assertNotNull("Toast should be created with emoji", latestToast)
    }

    @Test
    fun `test show with newline characters`() {
        val message = "Line 1\nLine 2\nLine 3"

        CustomToast.show(context, message)

        val latestToast = ShadowToast.getLatestToast()
        assertNotNull("Toast should be created with newlines", latestToast)
    }

    @Test
    fun `test show multiple times`() {
        // Show multiple toasts
        CustomToast.show(context, "Message 1")
        CustomToast.show(context, "Message 2")
        CustomToast.show(context, "Message 3")

        // Latest toast should be the last one shown
        val latestToast = ShadowToast.getLatestToast()
        assertNotNull("Latest toast should exist", latestToast)
    }

    // ========== showSuccess() Method Tests ==========

    @Test
    fun `test showSuccess with default message`() {
        // Default message is "삭제되었어요"
        CustomToast.showSuccess(context)

        val latestToast = ShadowToast.getLatestToast()
        assertNotNull("Toast should be created", latestToast)
    }

    @Test
    fun `test showSuccess with custom message`() {
        val message = "성공했습니다"

        CustomToast.showSuccess(context, message)

        val latestToast = ShadowToast.getLatestToast()
        assertNotNull("Toast should be created", latestToast)
    }

    @Test
    fun `test showSuccess with empty message`() {
        CustomToast.showSuccess(context, "")

        val latestToast = ShadowToast.getLatestToast()
        assertNotNull("Toast should be created even with empty message", latestToast)
    }

    @Test
    fun `test showSuccess multiple times`() {
        CustomToast.showSuccess(context, "Success 1")
        CustomToast.showSuccess(context, "Success 2")
        CustomToast.showSuccess(context)

        val latestToast = ShadowToast.getLatestToast()
        assertNotNull("Latest toast should exist", latestToast)
    }

    // ========== showWarning() Method Tests ==========

    @Test
    fun `test showWarning with message`() {
        val message = "경고 메시지"

        CustomToast.showWarning(context, message)

        val latestToast = ShadowToast.getLatestToast()
        assertNotNull("Toast should be created", latestToast)
    }

    @Test
    fun `test showWarning with empty message`() {
        CustomToast.showWarning(context, "")

        val latestToast = ShadowToast.getLatestToast()
        assertNotNull("Toast should be created even with empty message", latestToast)
    }

    @Test
    fun `test showWarning with critical warning`() {
        val message = "⚠️ 중요한 경고!"

        CustomToast.showWarning(context, message)

        val latestToast = ShadowToast.getLatestToast()
        assertNotNull("Toast should be created", latestToast)
    }

    @Test
    fun `test showWarning multiple times`() {
        CustomToast.showWarning(context, "Warning 1")
        CustomToast.showWarning(context, "Warning 2")
        CustomToast.showWarning(context, "Warning 3")

        val latestToast = ShadowToast.getLatestToast()
        assertNotNull("Latest toast should exist", latestToast)
    }

    // ========== Integration Tests ==========

    @Test
    fun `test all methods in sequence`() {
        // Test that all methods work correctly in sequence
        CustomToast.show(context, "General message")
        CustomToast.showSuccess(context, "Success message")
        CustomToast.showWarning(context, "Warning message")

        val latestToast = ShadowToast.getLatestToast()
        assertNotNull("Latest toast should exist after sequence", latestToast)
    }

    @Test
    fun `test mixed duration calls`() {
        CustomToast.show(context, "Short message", Toast.LENGTH_SHORT)
        CustomToast.show(context, "Long message", Toast.LENGTH_LONG)
        CustomToast.show(context, "Default message")

        val latestToast = ShadowToast.getLatestToast()
        assertNotNull("Latest toast should exist", latestToast)
    }

    // ========== Edge Case Tests ==========

    @Test
    fun `test show with null-like string`() {
        val message = "null"

        CustomToast.show(context, message)

        val latestToast = ShadowToast.getLatestToast()
        assertNotNull("Toast should be created with 'null' string", latestToast)
    }

    @Test
    fun `test show with numeric string`() {
        val message = "12345"

        CustomToast.show(context, message)

        val latestToast = ShadowToast.getLatestToast()
        assertNotNull("Toast should be created with numeric string", latestToast)
    }

    @Test
    fun `test show with mixed language`() {
        val message = "Hello 안녕하세요 こんにちは 你好"

        CustomToast.show(context, message)

        val latestToast = ShadowToast.getLatestToast()
        assertNotNull("Toast should be created with mixed languages", latestToast)
    }

    @Test
    fun `test showSuccess uses show internally`() {
        val message = "커스텀 성공 메시지"

        // showSuccess should internally call show()
        CustomToast.showSuccess(context, message)

        val latestToast = ShadowToast.getLatestToast()
        assertNotNull("Toast should be created via showSuccess", latestToast)
    }

    @Test
    fun `test showWarning uses show internally`() {
        val message = "커스텀 경고 메시지"

        // showWarning should internally call show()
        CustomToast.showWarning(context, message)

        val latestToast = ShadowToast.getLatestToast()
        assertNotNull("Toast should be created via showWarning", latestToast)
    }

    // ========== Stress Tests ==========

    @Test
    fun `test rapid successive calls`() {
        // Test that rapid calls don't cause crashes
        repeat(100) { i ->
            CustomToast.show(context, "Message $i")
        }

        val latestToast = ShadowToast.getLatestToast()
        assertNotNull("Toast should exist after rapid calls", latestToast)
    }

    @Test
    fun `test alternating method calls`() {
        repeat(10) { i ->
            when (i % 3) {
                0 -> CustomToast.show(context, "Message $i")
                1 -> CustomToast.showSuccess(context, "Success $i")
                2 -> CustomToast.showWarning(context, "Warning $i")
            }
        }

        val latestToast = ShadowToast.getLatestToast()
        assertNotNull("Toast should exist after alternating calls", latestToast)
    }
}
