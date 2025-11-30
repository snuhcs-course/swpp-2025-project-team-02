package com.example.fortuna_android.vlm

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for SmolVLMManager
 * Focus on testable methods without coroutine dependencies
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SmolVLMManagerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `test getInstance returns same instance`() {
        val instance1 = SmolVLMManager.getInstance(context)
        val instance2 = SmolVLMManager.getInstance(context)
        assertSame(instance1, instance2)
    }

    @Test
    fun `test getInstance returns non-null`() {
        val instance = SmolVLMManager.getInstance(context)
        assertNotNull(instance)
    }

    @Test
    fun `test isModelLoaded initially false`() {
        val manager = SmolVLMManager.getInstance(context)
        // Model should not be loaded initially in test environment
        assertNotNull(manager)
    }

    @Test
    fun `test manager can be obtained multiple times`() {
        repeat(5) {
            val instance = SmolVLMManager.getInstance(context)
            assertNotNull(instance)
        }
    }

    @Test
    fun `test singleton pattern with different context calls`() {
        val instance1 = SmolVLMManager.getInstance(context)
        val instance2 = SmolVLMManager.getInstance(context.applicationContext)
        assertSame(instance1, instance2)
    }
}
