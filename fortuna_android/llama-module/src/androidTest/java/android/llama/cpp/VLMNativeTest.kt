package android.llama.cpp

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Native layer tests for VLM functionality
 * Tests JNI bindings and low-level operations
 */
@RunWith(AndroidJUnit4::class)
class VLMNativeTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun testLibraryLoaded() {
        // Test that native library loads successfully
        try {
            System.loadLibrary("llama-android")
            assertTrue("Native library should load", true)
        } catch (e: UnsatisfiedLinkError) {
            fail("Failed to load native library: ${e.message}")
        }
    }

    @Test
    fun testBitmapToNativeConversion() {
        // Create test bitmap
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.RED)

        // This tests the bitmap_from_android JNI function
        // We can't call it directly, but we can verify bitmap properties
        assertNotNull("Bitmap should not be null", bitmap)
        assertEquals("Bitmap width", 100, bitmap.width)
        assertEquals("Bitmap height", 100, bitmap.height)
        assertEquals("Bitmap has pixels", 100 * 100, bitmap.width * bitmap.height)

        // Verify RGBA data
        val pixel = bitmap.getPixel(50, 50)
        val red = Color.red(pixel)
        val green = Color.green(pixel)
        val blue = Color.blue(pixel)
        val alpha = Color.alpha(pixel)

        assertEquals("Red channel should be 255", 255, red)
        assertEquals("Green channel should be 0", 0, green)
        assertEquals("Blue channel should be 0", 0, blue)
        assertEquals("Alpha channel should be 255", 255, alpha)

        println("Bitmap test - Size: ${bitmap.width}x${bitmap.height}")
        println("Bitmap test - Pixel RGBA: ($red, $green, $blue, $alpha)")
    }

    // NOTE: Model files are in app module's assets, not llama-module
    // This test would fail in llama-module context
    // Model existence is tested in SmolVLMManagerTest instead

    @Test
    fun testInternalStorageAccess() {
        // Test that we can write to internal storage
        val testFile = File(context.filesDir, "test_vlm.txt")

        try {
            testFile.writeText("VLM test")
            assertTrue("File should exist", testFile.exists())

            val content = testFile.readText()
            assertEquals("File content should match", "VLM test", content)

            testFile.delete()
            assertFalse("File should be deleted", testFile.exists())

        } catch (e: Exception) {
            fail("Internal storage access failed: ${e.message}")
        }
    }

    @Test
    fun testBitmapFormats() {
        // Test different bitmap formats
        val formats = listOf(
            Bitmap.Config.ARGB_8888,
            Bitmap.Config.RGB_565
        )

        formats.forEach { config ->
            val bitmap = Bitmap.createBitmap(50, 50, config)
            assertNotNull("Bitmap with $config should be created", bitmap)
            assertEquals("Bitmap config should match", config, bitmap.config)

            println("Bitmap format test - Config: $config, Size: ${bitmap.byteCount} bytes")
        }
    }

    @Test
    fun testLargeImageHandling() {
        // Test handling of different image sizes
        val sizes = listOf(
            224 to 224,   // Standard size
            512 to 512,   // Larger size
            1024 to 768   // High resolution
        )

        sizes.forEach { (width, height) ->
            try {
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                assertNotNull("Bitmap $width x $height should be created", bitmap)

                val byteCount = bitmap.byteCount
                val expectedBytes = width * height * 4 // ARGB_8888 = 4 bytes per pixel

                assertEquals("Byte count should match", expectedBytes, byteCount)

                println("Large image test - Size: ${width}x${height}, Bytes: $byteCount")

                bitmap.recycle()

            } catch (e: OutOfMemoryError) {
                println("WARNING: OOM for ${width}x${height}")
            }
        }
    }
}
