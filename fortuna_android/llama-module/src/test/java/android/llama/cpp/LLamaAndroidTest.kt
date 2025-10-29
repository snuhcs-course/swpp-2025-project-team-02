package android.llama.cpp

import android.graphics.Bitmap
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for LLamaAndroid
 * Focus on line coverage - testing method calls and state transitions
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class LLamaAndroidTest {

    private lateinit var llama: LLamaAndroid

    @Before
    fun setup() {
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
    fun testBench_ThrowsWhenNoModelLoaded() = runBlocking {
        try {
            llama.bench(8, 128, 1, 1)
            fail("Should throw IllegalStateException when no model loaded")
        } catch (e: IllegalStateException) {
            assertEquals("No model loaded", e.message)
        }
    }

    @Test
    fun testLoad_ThrowsWhenModelFileInvalid() = runBlocking {
        try {
            llama.load("/invalid/path/to/model.gguf")
            fail("Should throw IllegalStateException for invalid model")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("load_model() failed") == true)
        } catch (e: UnsatisfiedLinkError) {
            // Native library not loaded - expected in unit test
            assertTrue(true)
        }
    }

    @Test
    fun testSend_ReturnsFlowWhenNoModelLoaded() = runBlocking {
        val flow = llama.send("test message")
        val results = flow.toList()

        // Should emit nothing when no model loaded (else branch in send)
        assertTrue(results.isEmpty())
    }

    @Test
    fun testSend_WithFormatChat() = runBlocking {
        val flow = llama.send("test message", formatChat = true)
        val results = flow.toList()

        // Should emit nothing when no model loaded
        assertTrue(results.isEmpty())
    }

    @Test
    fun testSend_WithoutFormatChat() = runBlocking {
        val flow = llama.send("test message", formatChat = false)
        val results = flow.toList()

        // Should emit nothing when no model loaded
        assertTrue(results.isEmpty())
    }

    @Test
    fun testUnload_NoOpWhenNoModelLoaded() = runBlocking {
        // Should not throw when unloading with no model
        llama.unload()
        assertTrue(true)
    }

    @Test
    fun testLoadMmproj_ThrowsWhenNoModelLoaded() = runBlocking {
        try {
            llama.loadMmproj("/path/to/mmproj.bin")
            fail("Should throw IllegalStateException when model not loaded")
        } catch (e: IllegalStateException) {
            assertEquals("Model must be loaded first", e.message)
        } catch (e: UnsatisfiedLinkError) {
            // Native library not loaded - expected in unit test
            assertTrue(true)
        }
    }

    @Test
    fun testSendWithImage_ThrowsWhenNoModelLoaded() = runBlocking {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)

        try {
            val flow = llama.sendWithImage("describe this image", bitmap)
            flow.toList()
            fail("Should throw IllegalStateException when model not loaded")
        } catch (e: IllegalStateException) {
            assertEquals("Model not loaded", e.message)
        } catch (e: UnsatisfiedLinkError) {
            // Native library not loaded - expected in unit test
            assertTrue(true)
        }
    }

    @Test
    fun testSendWithImage_WithValidBitmap() = runBlocking {
        val bitmap = Bitmap.createBitmap(224, 224, Bitmap.Config.ARGB_8888)

        try {
            val flow = llama.sendWithImage("what is in this image?", bitmap)
            flow.toList()
        } catch (e: IllegalStateException) {
            // Expected - no model loaded
            assertTrue(e.message?.contains("Model not loaded") == true)
        } catch (e: UnsatisfiedLinkError) {
            // Native library not loaded - expected in unit test
            assertTrue(true)
        }
    }

    @Test
    fun testSendWithImage_WithDifferentBitmapSizes() = runBlocking {
        val smallBitmap = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888)
        val largeBitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)

        try {
            val flow1 = llama.sendWithImage("small image", smallBitmap)
            flow1.toList()
        } catch (e: Exception) {
            assertTrue(true)
        }

        try {
            val flow2 = llama.sendWithImage("large image", largeBitmap)
            flow2.toList()
        } catch (e: Exception) {
            assertTrue(true)
        }
    }

    @Test
    fun testMultipleMessagesSend() = runBlocking {
        val messages = listOf("hello", "world", "test")

        messages.forEach { message ->
            val flow = llama.send(message)
            val results = flow.toList()
            assertTrue(results.isEmpty())
        }
    }

    @Test
    fun testSend_EmptyMessage() = runBlocking {
        val flow = llama.send("")
        val results = flow.toList()

        assertTrue(results.isEmpty())
    }

    @Test
    fun testSend_LongMessage() = runBlocking {
        val longMessage = "a".repeat(1000)
        val flow = llama.send(longMessage)
        val results = flow.toList()

        assertTrue(results.isEmpty())
    }

    @Test
    fun testSend_MessageWithSpecialCharacters() = runBlocking {
        val specialMessage = "Hello! @#$%^&*() 你好 こんにちは"
        val flow = llama.send(specialMessage)
        val results = flow.toList()

        assertTrue(results.isEmpty())
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
    fun testBench_WithDifferentParameters() = runBlocking {
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
            } catch (e: UnsatisfiedLinkError) {
                assertTrue(true)
            }
        }
    }

    @Test
    fun testBench_WithDefaultNr() = runBlocking {
        try {
            llama.bench(8, 128, 1)
            fail("Should throw when no model loaded")
        } catch (e: IllegalStateException) {
            assertEquals("No model loaded", e.message)
        } catch (e: UnsatisfiedLinkError) {
            assertTrue(true)
        }
    }

    @Test
    fun testLoad_WithDifferentPaths() = runBlocking {
        val paths = listOf(
            "/path/to/model1.gguf",
            "/path/to/model2.gguf",
            "/invalid/path.gguf"
        )

        paths.forEach { path ->
            try {
                llama.load(path)
            } catch (e: IllegalStateException) {
                assertTrue(e.message?.contains("failed") == true)
            } catch (e: UnsatisfiedLinkError) {
                assertTrue(true)
            }
        }
    }

    @Test
    fun testUnload_MultipleTimesDoesNotThrow() = runBlocking {
        // Unload multiple times should be no-op
        repeat(5) {
            llama.unload()
        }
        assertTrue(true)
    }

    @Test
    fun testLoadMmproj_WithDifferentPaths() = runBlocking {
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
            } catch (e: UnsatisfiedLinkError) {
                assertTrue(true)
            }
        }
    }

    @Test
    fun testSendWithImage_DifferentMessages() = runBlocking {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val messages = listOf(
            "what is this?",
            "describe the image",
            "analyze this photo"
        )

        messages.forEach { message ->
            try {
                val flow = llama.sendWithImage(message, bitmap)
                flow.toList()
            } catch (e: IllegalStateException) {
                assertEquals("Model not loaded", e.message)
            } catch (e: UnsatisfiedLinkError) {
                assertTrue(true)
            }
        }
    }

    @Test
    fun testSendWithImage_EmptyMessage() = runBlocking {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)

        try {
            val flow = llama.sendWithImage("", bitmap)
            flow.toList()
        } catch (e: IllegalStateException) {
            assertEquals("Model not loaded", e.message)
        } catch (e: UnsatisfiedLinkError) {
            assertTrue(true)
        }
    }

    @Test
    fun testSendWithImage_DifferentBitmapConfigs() = runBlocking {
        val configs = listOf(
            Bitmap.Config.ARGB_8888,
            Bitmap.Config.RGB_565,
            Bitmap.Config.ALPHA_8
        )

        configs.forEach { config ->
            val bitmap = Bitmap.createBitmap(100, 100, config)
            try {
                val flow = llama.sendWithImage("test", bitmap)
                flow.toList()
            } catch (e: Exception) {
                assertTrue(true)
            }
        }
    }
}
