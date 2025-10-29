package com.example.fortuna_android.vlm

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test for SmolVLM model
 * Tests model loading, text generation, and image analysis
 */
@RunWith(AndroidJUnit4::class)
class 피ㅡSmolVLMManagerTest {

    private lateinit var vlmManager: SmolVLMManager
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setup() {
        vlmManager = SmolVLMManager.getInstance(context)
    }

    @After
    fun teardown() = runBlocking {
        try {
            vlmManager.unload()
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }

    @Test
    fun testModelLoading() = runBlocking {
        // Test that model loads without exceptions
        try {
            withTimeout(60000) { // 60 second timeout
                vlmManager.initialize()
            }
            assertTrue("Model should be loaded", vlmManager.isLoaded())
        } catch (e: Exception) {
            fail("Model loading failed: ${e.message}")
        }
    }

    @Test
    fun testTextGeneration() = runBlocking {
        // Initialize model
        withTimeout(60000) {
            vlmManager.initialize()
        }

        assertTrue("Model must be loaded", vlmManager.isLoaded())

        // Test simple text generation
        val prompt = "Echo: Test"
        val tokens = mutableListOf<String>()

        withTimeout(30000) { // 30 second timeout
            vlmManager.generateText(prompt)
                .take(10) // Take first 10 tokens
                .collect { token ->
                    tokens.add(token)
                }
        }

        assertTrue("Should generate at least 1 token", tokens.isNotEmpty())
        val response = tokens.joinToString("")
        assertFalse("Response should not be empty", response.isBlank())

        println("Text generation test - Prompt: '$prompt'")
        println("Text generation test - Response: '$response'")
    }

    @Test
    fun testBitmapCreation() {
        // Test creating a simple test bitmap
        val bitmap = createTestBitmap(224, 224)

        assertNotNull("Bitmap should be created", bitmap)
        assertEquals("Bitmap width should be 224", 224, bitmap.width)
        assertEquals("Bitmap height should be 224", 224, bitmap.height)
        assertEquals("Bitmap config should be ARGB_8888", Bitmap.Config.ARGB_8888, bitmap.config)
    }

    @Test
    fun testImageAnalysis() = runBlocking {
        // Initialize model
        withTimeout(60000) {
            vlmManager.initialize()
        }

        assertTrue("Model must be loaded", vlmManager.isLoaded())

        // Create test image (red square)
        val testBitmap = createTestBitmap(224, 224, Color.RED)

        // Test image analysis
        val prompt = "What color is this image?"
        val tokens = mutableListOf<String>()

        try {
            withTimeout(45000) { // 45 second timeout
                vlmManager.analyzeImage(testBitmap, prompt)
                    .take(20) // Take first 20 tokens
                    .collect { token ->
                        tokens.add(token)
                    }
            }

            assertTrue("Should generate at least 1 token for image", tokens.isNotEmpty())
            val response = tokens.joinToString("")
            assertFalse("Image analysis response should not be empty", response.isBlank())

            println("Image analysis test - Prompt: '$prompt'")
            println("Image analysis test - Response: '$response'")

        } catch (e: Exception) {
            fail("Image analysis failed: ${e.message}\n${e.stackTraceToString()}")
        }
    }

    // NOTE: Multiple consecutive inferences are not supported by current VLM implementation
    // Each inference requires a fresh model state (setup/teardown cycle)

    @Test
    fun testModelUnload() = runBlocking {
        // Load model
        withTimeout(60000) {
            vlmManager.initialize()
        }
        assertTrue("Model should be loaded", vlmManager.isLoaded())

        // Unload model
        vlmManager.unload()
        assertFalse("Model should be unloaded", vlmManager.isLoaded())
    }

    /**
     * Helper function to create a test bitmap
     */
    private fun createTestBitmap(width: Int, height: Int, color: Int = Color.BLUE): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(color)
        return bitmap
    }
}
