package com.example.fortuna_android.classification

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.media.Image
import androidx.test.core.app.ApplicationProvider
import com.example.fortuna_android.render.YuvToRgbConverter
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadow.api.Shadow
import java.nio.ByteBuffer

/**
 * Shadow class to stub out RenderScript in tests
 */
@Implements(android.renderscript.RenderScript::class)
class ShadowRenderScriptForObjectDetector {
    companion object {
        @JvmStatic
        @Implementation
        fun create(context: android.content.Context): android.renderscript.RenderScript {
            return mockk(relaxed = true)
        }
    }
}

@Implements(android.renderscript.ScriptIntrinsicYuvToRGB::class)
class ShadowScriptIntrinsicYuvToRGB {
    companion object {
        @JvmStatic
        @Implementation
        fun create(rs: android.renderscript.RenderScript, e: android.renderscript.Element): android.renderscript.ScriptIntrinsicYuvToRGB {
            return mockk(relaxed = true)
        }
    }
}

@Implements(android.renderscript.Element::class)
class ShadowElement {
    companion object {
        @JvmStatic
        @Implementation
        fun U8_4(rs: android.renderscript.RenderScript): android.renderscript.Element {
            return mockk(relaxed = true)
        }

        @JvmStatic
        @Implementation
        fun YUV(rs: android.renderscript.RenderScript): android.renderscript.Element {
            return mockk(relaxed = true)
        }
    }
}

@Implements(android.renderscript.Allocation::class)
class ShadowAllocation {
    companion object {
        @JvmStatic
        @Implementation
        fun createSized(rs: android.renderscript.RenderScript, e: android.renderscript.Element, count: Int): android.renderscript.Allocation {
            return mockk(relaxed = true)
        }

        @JvmStatic
        @Implementation
        fun createFromBitmap(rs: android.renderscript.RenderScript, b: Bitmap): android.renderscript.Allocation {
            return mockk(relaxed = true)
        }
    }
}

@Implements(android.renderscript.Type.Builder::class)
class ShadowTypeBuilder

private open class TestableObjectDetector(context: Context) : ObjectDetector(context) {
    var analyzeCallCount = 0
    var lastImageRotation = 0
    var shouldReturnResults = true

    override suspend fun analyze(image: Image, imageRotation: Int): List<DetectedObjectResult> {
        analyzeCallCount++
        lastImageRotation = imageRotation

        return if (shouldReturnResults) {
            listOf(
                DetectedObjectResult(0.9f, "test_object", centerCoordinate = 100 to 200),
                DetectedObjectResult(0.85f, "another_object", centerCoordinate = 300 to 400)
            )
        } else {
            emptyList()
        }
    }
}

/**
 * Unit tests for ObjectDetector abstract class
 * Tests the YUV conversion functionality and abstract behavior
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], shadows = [
    ShadowRenderScriptForObjectDetector::class,
    ShadowScriptIntrinsicYuvToRGB::class,
    ShadowElement::class,
    ShadowAllocation::class,
    ShadowTypeBuilder::class
])
class ObjectDetectorTest {

    private lateinit var context: Context
    private lateinit var objectDetector: TestableObjectDetector
    private lateinit var mockImage: Image

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        objectDetector = TestableObjectDetector(context)
        mockImage = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    // ========== Initialization Tests ==========

    @Test
    fun `test ObjectDetector initializes with context`() {
        assertNotNull("ObjectDetector should be initialized", objectDetector)
        assertEquals("Context should match", context, objectDetector.context)
    }

    @Test
    fun `test ObjectDetector has YuvToRgbConverter initialized`() {
        assertNotNull("YuvConverter should be initialized", objectDetector.yuvConverter)
        assertTrue("YuvConverter should be YuvToRgbConverter",
            objectDetector.yuvConverter is YuvToRgbConverter)
    }

    // ========== convertYuv Tests ==========

    @Test
    fun `test convertYuv creates bitmap with correct dimensions`() {
        // Arrange
        val width = 640
        val height = 480
        every { mockImage.width } returns width
        every { mockImage.height } returns height
        every { mockImage.format } returns ImageFormat.YUV_420_888

        // Act
        val bitmap = objectDetector.convertYuv(mockImage)

        // Assert
        assertNotNull("Bitmap should not be null", bitmap)
        assertEquals("Bitmap width should match image width", width, bitmap.width)
        assertEquals("Bitmap height should match image height", height, bitmap.height)
        assertEquals("Bitmap should use ARGB_8888 config",
            Bitmap.Config.ARGB_8888, bitmap.config)
    }

    @Test
    fun `test convertYuv handles different image sizes`() {
        val testSizes = listOf(
            320 to 240,
            640 to 480,
            1280 to 720
        )

        testSizes.forEach { (width, height) ->
            // Arrange
            val mockImg = mockk<Image>(relaxed = true)
            every { mockImg.width } returns width
            every { mockImg.height } returns height
            every { mockImg.format } returns ImageFormat.YUV_420_888

            // Act
            val bitmap = objectDetector.convertYuv(mockImg)

            // Assert
            assertEquals("Bitmap width should be $width", width, bitmap.width)
            assertEquals("Bitmap height should be $height", height, bitmap.height)
        }
    }

    // ========== Abstract analyze() Tests ==========

    @Test
    fun `test analyze returns detection results`() = runBlocking {
        // Arrange
        val imageRotation = 90
        every { mockImage.width } returns 640
        every { mockImage.height } returns 480
        every { mockImage.format } returns ImageFormat.YUV_420_888

        // Act
        val results = objectDetector.analyze(mockImage, imageRotation)

        // Assert
        assertNotNull("Results should not be null", results)
        assertEquals("Should have 2 results", 2, results.size)
        assertEquals("First result confidence should be 0.9", 0.9f, results[0].confidence)
        assertEquals("First result label should be test_object", "test_object", results[0].label)
        assertEquals("First result coordinates should be (100, 200)", 100 to 200, results[0].centerCoordinate)
        assertEquals("Second result confidence should be 0.85", 0.85f, results[1].confidence)
        assertEquals("Analyze should be called once", 1, objectDetector.analyzeCallCount)
        assertEquals("Image rotation should be passed correctly", imageRotation, objectDetector.lastImageRotation)
    }

    @Test
    fun `test analyze returns empty list when no objects detected`() = runBlocking {
        // Arrange
        objectDetector.shouldReturnResults = false
        every { mockImage.width } returns 640
        every { mockImage.height } returns 480
        every { mockImage.format } returns ImageFormat.YUV_420_888

        // Act
        val results = objectDetector.analyze(mockImage, 0)

        // Assert
        assertNotNull("Results should not be null", results)
        assertTrue("Results should be empty", results.isEmpty())
        assertEquals("Analyze should be called once", 1, objectDetector.analyzeCallCount)
    }

    @Test
    fun `test analyze handles different rotation values`() = runBlocking {
        val rotations = listOf(0, 90, 180, 270)

        every { mockImage.width } returns 640
        every { mockImage.height } returns 480
        every { mockImage.format } returns ImageFormat.YUV_420_888

        rotations.forEach { rotation ->
            // Act
            objectDetector.analyze(mockImage, rotation)

            // Assert
            assertEquals("Last rotation should be $rotation",
                rotation, objectDetector.lastImageRotation)
        }
    }

    // ========== Multiple Calls Tests ==========

    @Test
    fun `test multiple analyze calls work correctly`() = runBlocking {
        // Arrange
        every { mockImage.width } returns 640
        every { mockImage.height } returns 480
        every { mockImage.format } returns ImageFormat.YUV_420_888

        // Act
        objectDetector.analyze(mockImage, 0)
        objectDetector.analyze(mockImage, 90)
        objectDetector.analyze(mockImage, 180)

        // Assert
        assertEquals("Should be called 3 times", 3, objectDetector.analyzeCallCount)
    }

    // ========== Context Tests ==========

    @Test
    fun `test ObjectDetector maintains context reference`() {
        assertSame("Context should be the same instance", context, objectDetector.context)
    }

    @Test
    fun `test multiple ObjectDetector instances with same context`() {
        val detector1 = TestableObjectDetector(context)
        val detector2 = TestableObjectDetector(context)

        assertSame("Both detectors should share same context",
            detector1.context, detector2.context)
    }

    // ========== Edge Cases ==========

    @Test
    fun `test convertYuv with small image size`() {
        // Arrange
        val width = 16
        val height = 16
        val mockImg = mockk<Image>(relaxed = true)
        every { mockImg.width } returns width
        every { mockImg.height } returns height
        every { mockImg.format } returns ImageFormat.YUV_420_888

        // Act
        val bitmap = objectDetector.convertYuv(mockImg)

        // Assert
        assertEquals("Bitmap width should be 16", 16, bitmap.width)
        assertEquals("Bitmap height should be 16", 16, bitmap.height)
    }
}
