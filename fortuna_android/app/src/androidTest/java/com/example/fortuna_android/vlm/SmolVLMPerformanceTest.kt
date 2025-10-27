package com.example.fortuna_android.vlm

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.system.measureTimeMillis

/**
 * Performance test for VLM image processing
 * Measures timing for different image sizes and scaling operations
 */
@RunWith(AndroidJUnit4::class)
class SmolVLMPerformanceTest {

    private lateinit var vlmManager: SmolVLMManager
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setup() = runBlocking {
        vlmManager = SmolVLMManager.getInstance(context)

        // Load model once for all tests
        println("⏳ Loading VLM model...")
        val loadTime = measureTimeMillis {
            withTimeout(60000) {
                vlmManager.initialize()
            }
        }
        println("✅ Model loaded in ${loadTime}ms")
        assertTrue("Model should be loaded", vlmManager.isLoaded())
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
    fun testBitmapScalingPerformance() {
        println("\n=== Bitmap Scaling Performance Test ===")

        val sizes = listOf(
            1920 to 1080,  // Full HD (before optimization)
            640 to 480,    // Optimized capture size
            336 to 336     // VLM target size
        )

        sizes.forEach { (width, height) ->
            val bitmap = createTestBitmap(width, height, Color.BLUE)

            // Measure scaling time
            var scaledBitmap: Bitmap? = null
            val scaleTime = measureTimeMillis {
                scaledBitmap = optimizeImageForVLM(bitmap, 336)
            }

            println("📐 ${width}x${height} → ${scaledBitmap!!.width}x${scaledBitmap!!.height}: ${scaleTime}ms")

            // Clean up
            bitmap.recycle()
            if (scaledBitmap != bitmap) {
                scaledBitmap?.recycle()
            }
        }
    }

    @Test
    fun testVLMInferencePerformance_SmallImage() = runBlocking {
        println("\n=== VLM Inference Performance (640x480) ===")

        val bitmap = createTestBitmap(640, 480, Color.RED)
        val prompt = "What color is this?"

        var optimizedBitmap: Bitmap? = null
        val optimizeTime = measureTimeMillis {
            optimizedBitmap = optimizeImageForVLM(bitmap, 336)
        }
        println("🖼️  Image optimization: ${optimizeTime}ms")

        val tokens = mutableListOf<String>()
        val inferenceTime = measureTimeMillis {
            withTimeout(45000) {
                vlmManager.analyzeImage(optimizedBitmap!!, prompt)
                    .take(20)
                    .collect { token ->
                        tokens.add(token)
                    }
            }
        }

        println("🧠 VLM inference (20 tokens): ${inferenceTime}ms")
        println("⚡ Total time: ${optimizeTime + inferenceTime}ms")
        println("📝 Response: ${tokens.joinToString("")}")

        assertTrue("Should generate tokens", tokens.isNotEmpty())

        bitmap.recycle()
        if (optimizedBitmap != bitmap) {
            optimizedBitmap?.recycle()
        }
    }

    @Test
    fun testVLMInferencePerformance_LargeImage() = runBlocking {
        println("\n=== VLM Inference Performance (1920x1080) ===")

        val bitmap = createTestBitmap(1920, 1080, Color.GREEN)
        val prompt = "What color is this?"

        var optimizedBitmap: Bitmap? = null
        val optimizeTime = measureTimeMillis {
            optimizedBitmap = optimizeImageForVLM(bitmap, 336)
        }
        println("🖼️  Image optimization: ${optimizeTime}ms")

        val tokens = mutableListOf<String>()
        val inferenceTime = measureTimeMillis {
            withTimeout(45000) {
                vlmManager.analyzeImage(optimizedBitmap!!, prompt)
                    .take(20)
                    .collect { token ->
                        tokens.add(token)
                    }
            }
        }

        println("🧠 VLM inference (20 tokens): ${inferenceTime}ms")
        println("⚡ Total time: ${optimizeTime + inferenceTime}ms")
        println("📝 Response: ${tokens.joinToString("")}")

        assertTrue("Should generate tokens", tokens.isNotEmpty())

        bitmap.recycle()
        if (optimizedBitmap != bitmap) {
            optimizedBitmap?.recycle()
        }
    }

    @Test
    fun testVLMInferencePerformance_OptimalImage() = runBlocking {
        println("\n=== VLM Inference Performance (336x336 - No Scaling) ===")

        val bitmap = createTestBitmap(336, 336, Color.YELLOW)
        val prompt = "What color is this?"

        var optimizedBitmap: Bitmap? = null
        val optimizeTime = measureTimeMillis {
            optimizedBitmap = optimizeImageForVLM(bitmap, 336)
        }
        println("🖼️  Image optimization: ${optimizeTime}ms (should be ~0ms)")

        val tokens = mutableListOf<String>()
        val inferenceTime = measureTimeMillis {
            withTimeout(45000) {
                vlmManager.analyzeImage(optimizedBitmap!!, prompt)
                    .take(20)
                    .collect { token ->
                        tokens.add(token)
                    }
            }
        }

        println("🧠 VLM inference (20 tokens): ${inferenceTime}ms")
        println("⚡ Total time: ${optimizeTime + inferenceTime}ms")
        println("📝 Response: ${tokens.joinToString("")}")

        assertTrue("Should generate tokens", tokens.isNotEmpty())
        assertTrue("Should skip scaling", optimizedBitmap == bitmap)

        bitmap.recycle()
    }

    @Test
    fun testMemoryUsage() {
        println("\n=== Memory Usage Test ===")

        val sizes = listOf(
            1920 to 1080,
            640 to 480,
            336 to 336
        )

        sizes.forEach { (width, height) ->
            val bitmap = createTestBitmap(width, height, Color.CYAN)
            val byteCount = bitmap.byteCount
            val megabytes = byteCount / 1024.0 / 1024.0

            println("📦 ${width}x${height}: ${byteCount} bytes (${String.format("%.2f", megabytes)} MB)")

            bitmap.recycle()
        }
    }

    @Test
    fun testPerformance_Before_LargeImage() = runBlocking {
        println("\n=== Performance Test: BEFORE Optimization (1920x1080) ===")

        val largeBitmap = createTestBitmap(1920, 1080, Color.MAGENTA)
        var totalTime = 0L

        var optimizedBitmap: Bitmap? = null
        val scalingTime = measureTimeMillis {
            optimizedBitmap = optimizeImageForVLM(largeBitmap, 336)
        }
        totalTime += scalingTime
        println("  - Scaling: ${scalingTime}ms")

        val tokens = mutableListOf<String>()
        val inferenceTime = measureTimeMillis {
            withTimeout(45000) {
                vlmManager.analyzeImage(optimizedBitmap!!, "Describe this.")
                    .take(20)
                    .collect { tokens.add(it) }
            }
        }
        totalTime += inferenceTime
        println("  - Inference: ${inferenceTime}ms")
        println("  ⏱️  TOTAL: ${totalTime}ms")
        println("  📝 Response: ${tokens.joinToString("")}")

        // Clean up
        largeBitmap.recycle()
        if (optimizedBitmap != largeBitmap) {
            optimizedBitmap?.recycle()
        }

        assertTrue("Should generate tokens", tokens.isNotEmpty())
    }

    @Test
    fun testPerformance_After_OptimizedImage() = runBlocking {
        println("\n=== Performance Test: AFTER Optimization (640x480) ===")

        val smallBitmap = createTestBitmap(640, 480, Color.MAGENTA)
        var totalTime = 0L

        var optimizedBitmap: Bitmap? = null
        val scalingTime = measureTimeMillis {
            optimizedBitmap = optimizeImageForVLM(smallBitmap, 336)
        }
        totalTime += scalingTime
        println("  - Scaling: ${scalingTime}ms")

        val tokens = mutableListOf<String>()
        val inferenceTime = measureTimeMillis {
            withTimeout(45000) {
                vlmManager.analyzeImage(optimizedBitmap!!, "Describe this.")
                    .take(20)
                    .collect { tokens.add(it) }
            }
        }
        totalTime += inferenceTime
        println("  - Inference: ${inferenceTime}ms")
        println("  ⏱️  TOTAL: ${totalTime}ms")
        println("  📝 Response: ${tokens.joinToString("")}")

        // Clean up
        smallBitmap.recycle()
        if (optimizedBitmap != smallBitmap) {
            optimizedBitmap?.recycle()
        }

        assertTrue("Should generate tokens", tokens.isNotEmpty())
    }

    /**
     * Helper: Create test bitmap
     */
    private fun createTestBitmap(width: Int, height: Int, color: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(color)
        return bitmap
    }

    /**
     * Helper: Optimize image for VLM (same logic as VLMTestActivity)
     */
    private fun optimizeImageForVLM(bitmap: Bitmap, maxSize: Int = 336): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxSize && height <= maxSize) {
            return bitmap
        }

        val scale = minOf(
            maxSize.toFloat() / width,
            maxSize.toFloat() / height
        )

        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}
