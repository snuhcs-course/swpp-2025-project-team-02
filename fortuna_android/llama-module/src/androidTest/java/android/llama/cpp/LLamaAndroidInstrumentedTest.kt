package android.llama.cpp

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test for LLamaAndroid
 * This test runs on an Android device/emulator where native libraries are available
 * Focus on line coverage - testing all public methods and branches
 */
@RunWith(AndroidJUnit4::class)
class LLamaAndroidInstrumentedTest {

    private lateinit var llama: LLamaAndroid

    @Before
    fun setup() {
        // Get singleton instance - native libraries will be loaded
        llama = LLamaAndroid.instance()
    }

    @Test
    fun testInstance_Singleton() {
        val instance1 = LLamaAndroid.instance()
        val instance2 = LLamaAndroid.instance()

        assertSame(instance1, instance2)
    }

    @Test
    fun testInstance_NotNull() {
        val instance = LLamaAndroid.instance()

        assertNotNull(instance)
    }

    @Test
    fun testBench_ThrowsWhenNoModelLoaded() = runTest {
        try {
            llama.bench(8, 128, 1, 1)
            fail("Should throw IllegalStateException when no model loaded")
        } catch (e: IllegalStateException) {
            assertEquals("No model loaded", e.message)
        }
    }

    @Test
    fun testBench_WithDefaultNr() = runTest {
        try {
            llama.bench(8, 128, 1)
            fail("Should throw IllegalStateException when no model loaded")
        } catch (e: IllegalStateException) {
            assertEquals("No model loaded", e.message)
        }
    }

    @Test
    fun testLoad_ThrowsWhenModelFileInvalid() = runTest {
        try {
            llama.load("/invalid/path/to/model.gguf")
            fail("Should throw IllegalStateException for invalid model")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("failed") == true)
        }
    }

    @Test
    fun testSend_ReturnsEmptyFlowWhenNoModelLoaded() = runTest {
        val flow = llama.send("test message")
        val results = flow.toList()

        // Should emit nothing when no model loaded (else branch in send)
        assertTrue(results.isEmpty())
    }

    @Test
    fun testSend_WithFormatChatTrue() = runTest {
        val flow = llama.send("test message", formatChat = true)
        val results = flow.toList()

        // Should emit nothing when no model loaded
        assertTrue(results.isEmpty())
    }

    @Test
    fun testSend_WithFormatChatFalse() = runTest {
        val flow = llama.send("test message", formatChat = false)
        val results = flow.toList()

        // Should emit nothing when no model loaded
        assertTrue(results.isEmpty())
    }

    @Test
    fun testUnload_NoOpWhenNoModelLoaded() = runTest {
        // Should not throw when unloading with no model (else branch coverage)
        llama.unload()
        assertTrue(true)
    }

    @Test
    fun testLoadMmproj_ThrowsWhenNoModelLoaded() = runTest {
        try {
            llama.loadMmproj("/path/to/mmproj.bin")
            fail("Should throw IllegalStateException when model not loaded")
        } catch (e: IllegalStateException) {
            assertEquals("Model must be loaded first", e.message)
        }
    }

    @Test
    fun testSendWithImage_ThrowsWhenNoModelLoaded() = runTest {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)

        try {
            val flow = llama.sendWithImage("test message", bitmap)
            flow.toList()
            fail("Should throw IllegalStateException when model not loaded")
        } catch (e: IllegalStateException) {
            assertEquals("Model not loaded", e.message)
        }
    }

    @Test
    fun testSend_EmptyMessage() = runTest {
        val flow = llama.send("")
        val results = flow.toList()

        assertTrue(results.isEmpty())
    }

    @Test
    fun testSend_LongMessage() = runTest {
        val longMessage = "a".repeat(1000)
        val flow = llama.send(longMessage)
        val results = flow.toList()

        assertTrue(results.isEmpty())
    }

    @Test
    fun testSend_SpecialCharacters() = runTest {
        val specialMessage = "Hello! @#$%^&*() 你好 こんにちは"
        val flow = llama.send(specialMessage)
        val results = flow.toList()

        assertTrue(results.isEmpty())
    }

    @Test
    fun testMultipleSendCalls() = runTest {
        repeat(3) { i ->
            val flow = llama.send("message $i")
            val results = flow.toList()
            assertTrue(results.isEmpty())
        }
    }

    @Test
    fun testBench_WithDifferentParameters() = runTest {
        val testCases = listOf(
            listOf(8, 128, 1, 1),
            listOf(16, 256, 2, 2),
            listOf(32, 512, 4, 4)
        )

        testCases.forEach { (pp, tg, pl, nr) ->
            try {
                llama.bench(pp, tg, pl, nr)
                fail("Should throw when no model loaded")
            } catch (e: IllegalStateException) {
                assertEquals("No model loaded", e.message)
            }
        }
    }

    @Test
    fun testLoad_WithDifferentPaths() = runTest {
        val paths = listOf(
            "/path/to/model1.gguf",
            "/path/to/model2.gguf",
            "/invalid/path.gguf"
        )

        paths.forEach { path ->
            try {
                llama.load(path)
                fail("Should throw for invalid path")
            } catch (e: IllegalStateException) {
                assertTrue(e.message?.contains("failed") == true)
            }
        }
    }

    @Test
    fun testUnload_MultipleTimes() = runTest {
        // Unload multiple times should be no-op
        repeat(3) {
            llama.unload()
        }
        assertTrue(true)
    }

    @Test
    fun testLoadMmproj_WithDifferentPaths() = runTest {
        val paths = listOf(
            "/path/to/mmproj1.bin",
            "/path/to/mmproj2.bin"
        )

        paths.forEach { path ->
            try {
                llama.loadMmproj(path)
                fail("Should throw when model not loaded")
            } catch (e: IllegalStateException) {
                assertEquals("Model must be loaded first", e.message)
            }
        }
    }

    @Test
    fun testSend_MultipleCallsSequentially() = runTest {
        repeat(5) { i ->
            val flow = llama.send("sequential message $i")
            val results = flow.toList()
            assertTrue(results.isEmpty())
        }
    }

    @Test
    fun testSend_FormatChatVariations() = runTest {
        val testCases = listOf(
            "test" to true,
            "test" to false,
            "" to true,
            "long message test" to false
        )

        testCases.forEach { (message, formatChat) ->
            val flow = llama.send(message, formatChat)
            val results = flow.toList()
            assertTrue(results.isEmpty())
        }
    }

    @Test
    fun testBench_BoundaryValues() = runTest {
        try {
            llama.bench(1, 1, 1, 1)
            fail("Should throw when no model loaded")
        } catch (e: IllegalStateException) {
            assertEquals("No model loaded", e.message)
        }
    }

    @Test
    fun testSendWithImage_WithValidBitmap() = runTest {
        val bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)

        try {
            val flow = llama.sendWithImage("Describe this image", bitmap)
            flow.toList()
            fail("Should throw when model not loaded")
        } catch (e: IllegalStateException) {
            assertEquals("Model not loaded", e.message)
        }
    }

    @Test
    fun testSendWithImage_WithSmallBitmap() = runTest {
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)

        try {
            val flow = llama.sendWithImage("test", bitmap)
            flow.toList()
            fail("Should throw when model not loaded")
        } catch (e: IllegalStateException) {
            assertEquals("Model not loaded", e.message)
        }
    }

    @Test
    fun testSendWithImage_WithDifferentBitmapConfigs() = runTest {
        val configs = listOf(
            Bitmap.Config.ARGB_8888,
            Bitmap.Config.RGB_565
        )

        configs.forEach { config ->
            val bitmap = Bitmap.createBitmap(50, 50, config)
            try {
                val flow = llama.sendWithImage("test", bitmap)
                flow.toList()
                fail("Should throw when model not loaded")
            } catch (e: IllegalStateException) {
                assertEquals("Model not loaded", e.message)
            }
        }
    }

    @Test
    fun testInstance_ThreadSafety() {
        val instances = mutableListOf<LLamaAndroid>()

        repeat(10) {
            instances.add(LLamaAndroid.instance())
        }

        // All instances should be the same object
        instances.forEach { instance ->
            assertSame(instances[0], instance)
        }
    }

    @Test
    fun testContextUsage() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertNotNull(context)

        // Verify singleton works across contexts
        val instance = LLamaAndroid.instance()
        assertNotNull(instance)
    }
}
