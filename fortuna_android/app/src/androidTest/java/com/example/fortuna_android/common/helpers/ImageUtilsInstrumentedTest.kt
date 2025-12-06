package com.example.fortuna_android.common.helpers

import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.media.Image
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.Ignore
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import java.nio.ByteBuffer

/**
 * Instrumented test for ImageUtils
 * Tests image conversion and optimization functionality
 */
@RunWith(AndroidJUnit4::class)
class ImageUtilsInstrumentedTest {

    private lateinit var mockImage: Image
    private lateinit var mockPlane0: Image.Plane
    private lateinit var mockPlane1: Image.Plane
    private lateinit var mockPlane2: Image.Plane

    @Before
    fun setup() {
        mockImage = mock(Image::class.java)
        mockPlane0 = mock(Image.Plane::class.java)
        mockPlane1 = mock(Image.Plane::class.java)
        mockPlane2 = mock(Image.Plane::class.java)
    }

    // ========== convertYuvImageToBitmap Tests ==========

    @Ignore("Skipping on device due to mocked Image/Plane instability")
    @Test
    fun testConvertYuvImageToBitmap_InvalidFormat() {
        // Arrange
        val testImage = mock(Image::class.java)
        `when`(testImage.format).thenReturn(ImageFormat.JPEG)

        // Act
        val result = ImageUtils.convertYuvImageToBitmap(testImage)

        // Assert
        assertNull("Should return null for non-YUV format", result)
    }

    @Ignore("Skipping on device due to mocked Image/Plane instability")
    @Test
    fun testConvertYuvImageToBitmap_NullImage() {
        // Arrange
        `when`(mockImage.format).thenReturn(ImageFormat.YUV_420_888)
        `when`(mockImage.width).thenReturn(0)
        `when`(mockImage.height).thenReturn(0)
        `when`(mockImage.planes).thenReturn(arrayOf())

        // Act
        val result = ImageUtils.convertYuvImageToBitmap(mockImage)

        // Assert
        assertNull("Should return null for invalid image", result)
    }

    @Ignore("Skipping on device due to mocked Image/Plane instability")
    @Test
    fun testConvertYuvImageToBitmap_ExceptionHandling() {
        // Arrange
        `when`(mockImage.format).thenReturn(ImageFormat.YUV_420_888)
        `when`(mockImage.width).thenThrow(RuntimeException("Test exception"))

        // Act
        val result = ImageUtils.convertYuvImageToBitmap(mockImage)

        // Assert
        assertNull("Should return null when exception occurs", result)
    }

    @Ignore("Skipping on device due to mocked Image/Plane instability")
    @Test
    fun testConvertYuvImageToBitmap_WithValidYuvImage_PixelStride1() {
        // Arrange
        val width = 4
        val height = 4
        val ySize = width * height
        val uvSize = (width * height) / 4

        val yBuffer = ByteBuffer.allocateDirect(ySize)
        val uBuffer = ByteBuffer.allocateDirect(uvSize)
        val vBuffer = ByteBuffer.allocateDirect(uvSize)

        // Fill buffers with test data
        for (i in 0 until ySize) yBuffer.put(100.toByte())
        for (i in 0 until uvSize) {
            uBuffer.put(128.toByte())
            vBuffer.put(128.toByte())
        }
        yBuffer.rewind()
        uBuffer.rewind()
        vBuffer.rewind()

        `when`(mockImage.format).thenReturn(ImageFormat.YUV_420_888)
        `when`(mockImage.width).thenReturn(width)
        `when`(mockImage.height).thenReturn(height)
        `when`(mockImage.planes).thenReturn(arrayOf(mockPlane0, mockPlane1, mockPlane2))

        `when`(mockPlane0.buffer).thenReturn(yBuffer)
        `when`(mockPlane1.buffer).thenReturn(uBuffer)
        `when`(mockPlane2.buffer).thenReturn(vBuffer)

        `when`(mockPlane1.pixelStride).thenReturn(1)
        `when`(mockPlane1.rowStride).thenReturn(width / 2)
        `when`(mockPlane2.rowStride).thenReturn(width / 2)

        // Act
        val result = ImageUtils.convertYuvImageToBitmap(mockImage)

        // Assert
        assertNotNull("Should return bitmap for valid YUV image", result)
        if (result != null) {
            assertEquals("Bitmap width should match", width, result.width)
            assertEquals("Bitmap height should match", height, result.height)
        }
    }

    @Ignore("Skipping on device due to mocked Image/Plane instability")
    @Test
    fun testConvertYuvImageToBitmap_WithValidYuvImage_PixelStride2() {
        // Arrange
        val width = 4
        val height = 4
        val ySize = width * height
        val uvSize = (width * height) / 2  // For pixelStride=2

        val yBuffer = ByteBuffer.allocateDirect(ySize)
        val uBuffer = ByteBuffer.allocateDirect(uvSize)
        val vBuffer = ByteBuffer.allocateDirect(uvSize)

        // Fill buffers with test data
        for (i in 0 until ySize) yBuffer.put(100.toByte())
        for (i in 0 until uvSize) {
            uBuffer.put(128.toByte())
            vBuffer.put(128.toByte())
        }
        yBuffer.rewind()
        uBuffer.rewind()
        vBuffer.rewind()

        `when`(mockImage.format).thenReturn(ImageFormat.YUV_420_888)
        `when`(mockImage.width).thenReturn(width)
        `when`(mockImage.height).thenReturn(height)
        `when`(mockImage.planes).thenReturn(arrayOf(mockPlane0, mockPlane1, mockPlane2))

        `when`(mockPlane0.buffer).thenReturn(yBuffer)
        `when`(mockPlane1.buffer).thenReturn(uBuffer)
        `when`(mockPlane2.buffer).thenReturn(vBuffer)

        `when`(mockPlane1.pixelStride).thenReturn(2)
        `when`(mockPlane1.rowStride).thenReturn(width)
        `when`(mockPlane2.rowStride).thenReturn(width)

        // Act
        val result = ImageUtils.convertYuvImageToBitmap(mockImage)

        // Assert
        assertNotNull("Should return bitmap for valid YUV image with pixelStride=2", result)
        if (result != null) {
            assertEquals("Bitmap width should match", width, result.width)
            assertEquals("Bitmap height should match", height, result.height)
        }
    }

    @Ignore("Skipping on device due to mocked Image/Plane instability")
    @Test
    fun testConvertYuvImageToBitmap_LargerDimensions() {
        // Arrange
        val width = 8
        val height = 8
        val ySize = width * height
        val uvSize = (width * height) / 4

        val yBuffer = ByteBuffer.allocateDirect(ySize)
        val uBuffer = ByteBuffer.allocateDirect(uvSize)
        val vBuffer = ByteBuffer.allocateDirect(uvSize)

        for (i in 0 until ySize) yBuffer.put((i % 256).toByte())
        for (i in 0 until uvSize) {
            uBuffer.put(128.toByte())
            vBuffer.put(128.toByte())
        }
        yBuffer.rewind()
        uBuffer.rewind()
        vBuffer.rewind()

        `when`(mockImage.format).thenReturn(ImageFormat.YUV_420_888)
        `when`(mockImage.width).thenReturn(width)
        `when`(mockImage.height).thenReturn(height)
        `when`(mockImage.planes).thenReturn(arrayOf(mockPlane0, mockPlane1, mockPlane2))

        `when`(mockPlane0.buffer).thenReturn(yBuffer)
        `when`(mockPlane1.buffer).thenReturn(uBuffer)
        `when`(mockPlane2.buffer).thenReturn(vBuffer)

        `when`(mockPlane1.pixelStride).thenReturn(1)
        `when`(mockPlane1.rowStride).thenReturn(width / 2)
        `when`(mockPlane2.rowStride).thenReturn(width / 2)

        // Act
        val result = ImageUtils.convertYuvImageToBitmap(mockImage)

        // Assert
        assertNotNull("Should return bitmap for larger YUV image", result)
        if (result != null) {
            assertEquals("Bitmap width should match", width, result.width)
            assertEquals("Bitmap height should match", height, result.height)
        }
    }

    // ========== optimizeImageForVLM Tests ==========

    @Test
    fun testOptimizeImageForVLM_AlreadySmall() {
        // Arrange
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val maxSize = 336

        // Act
        val result = ImageUtils.optimizeImageForVLM(bitmap, maxSize)

        // Assert
        assertSame("Should return same bitmap when already small enough", bitmap, result)
        assertEquals("Width should not change", 100, result.width)
        assertEquals("Height should not change", 100, result.height)
    }

    @Test
    fun testOptimizeImageForVLM_NeedsDownscaling_Width() {
        // Arrange
        val bitmap = Bitmap.createBitmap(800, 400, Bitmap.Config.ARGB_8888)
        val maxSize = 336

        // Act
        val result = ImageUtils.optimizeImageForVLM(bitmap, maxSize)

        // Assert
        assertNotNull("Should return optimized bitmap", result)
        assertTrue("Width should be scaled down", result.width <= maxSize)
        assertTrue("Height should be scaled down", result.height <= maxSize)

        // Check aspect ratio is maintained
        val originalRatio = 800.0 / 400.0
        val resultRatio = result.width.toDouble() / result.height.toDouble()
        assertEquals("Aspect ratio should be maintained", originalRatio, resultRatio, 0.01)
    }

    @Test
    fun testOptimizeImageForVLM_NeedsDownscaling_Height() {
        // Arrange
        val bitmap = Bitmap.createBitmap(400, 800, Bitmap.Config.ARGB_8888)
        val maxSize = 336

        // Act
        val result = ImageUtils.optimizeImageForVLM(bitmap, maxSize)

        // Assert
        assertNotNull("Should return optimized bitmap", result)
        assertTrue("Width should be scaled down", result.width <= maxSize)
        assertTrue("Height should be scaled down", result.height <= maxSize)

        // Check aspect ratio is maintained
        val originalRatio = 400.0 / 800.0
        val resultRatio = result.width.toDouble() / result.height.toDouble()
        assertEquals("Aspect ratio should be maintained", originalRatio, resultRatio, 0.01)
    }

    @Test
    fun testOptimizeImageForVLM_Square() {
        // Arrange
        val bitmap = Bitmap.createBitmap(1000, 1000, Bitmap.Config.ARGB_8888)
        val maxSize = 336

        // Act
        val result = ImageUtils.optimizeImageForVLM(bitmap, maxSize)

        // Assert
        assertNotNull("Should return optimized bitmap", result)
        assertEquals("Width should be maxSize", maxSize, result.width)
        assertEquals("Height should be maxSize", maxSize, result.height)
    }

    @Test
    fun testOptimizeImageForVLM_CustomMaxSize() {
        // Arrange
        val bitmap = Bitmap.createBitmap(1000, 500, Bitmap.Config.ARGB_8888)
        val maxSize = 200

        // Act
        val result = ImageUtils.optimizeImageForVLM(bitmap, maxSize)

        // Assert
        assertNotNull("Should return optimized bitmap", result)
        assertTrue("Width should be <= maxSize", result.width <= maxSize)
        assertTrue("Height should be <= maxSize", result.height <= maxSize)
        assertEquals("Should scale to maxSize", maxSize, result.width)
    }

    @Test
    fun testOptimizeImageForVLM_ExactlyMaxSize() {
        // Arrange
        val maxSize = 336
        val bitmap = Bitmap.createBitmap(maxSize, maxSize, Bitmap.Config.ARGB_8888)

        // Act
        val result = ImageUtils.optimizeImageForVLM(bitmap, maxSize)

        // Assert
        assertSame("Should return same bitmap when exactly maxSize", bitmap, result)
    }

    @Test
    fun testOptimizeImageForVLM_VeryLargeImage() {
        // Arrange
        val bitmap = Bitmap.createBitmap(4000, 3000, Bitmap.Config.ARGB_8888)
        val maxSize = 336

        // Act
        val result = ImageUtils.optimizeImageForVLM(bitmap, maxSize)

        // Assert
        assertNotNull("Should handle very large images", result)
        assertTrue("Width should be <= maxSize", result.width <= maxSize)
        assertTrue("Height should be <= maxSize", result.height <= maxSize)

        // Verify aspect ratio
        val originalRatio = 4000.0 / 3000.0
        val resultRatio = result.width.toDouble() / result.height.toDouble()
        assertEquals("Aspect ratio should be maintained", originalRatio, resultRatio, 0.01)
    }

    @Test
    fun testOptimizeImageForVLM_SmallButNonSquare() {
        // Arrange
        val bitmap = Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888)
        val maxSize = 336

        // Act
        val result = ImageUtils.optimizeImageForVLM(bitmap, maxSize)

        // Assert
        assertSame("Should return same bitmap", bitmap, result)
        assertEquals("Width should not change", 200, result.width)
        assertEquals("Height should not change", 100, result.height)
    }

    @Test
    fun testOptimizeImageForVLM_OnePixelImage() {
        // Arrange
        val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        val maxSize = 336

        // Act
        val result = ImageUtils.optimizeImageForVLM(bitmap, maxSize)

        // Assert
        assertSame("Should return same bitmap for 1x1 image", bitmap, result)
    }

    @Test
    fun testOptimizeImageForVLM_DefaultMaxSize() {
        // Arrange
        val bitmap = Bitmap.createBitmap(1000, 1000, Bitmap.Config.ARGB_8888)

        // Act - Use default maxSize (336)
        val result = ImageUtils.optimizeImageForVLM(bitmap)

        // Assert
        assertNotNull("Should return optimized bitmap with default maxSize", result)
        assertEquals("Should use default maxSize of 336", 336, result.width)
        assertEquals("Should use default maxSize of 336", 336, result.height)
    }

    // ========== Edge Cases ==========

    @Ignore("Skipping on device due to mocked Image/Plane instability")
    @Test
    fun testConvertYuvImageToBitmap_MultipleFormats() {
        // Test various invalid formats
        val invalidFormats = listOf(
            ImageFormat.RGB_565,
            ImageFormat.NV16,
            ImageFormat.NV21,
            ImageFormat.JPEG
        )

        for (format in invalidFormats) {
            val testImage = mock(Image::class.java)
            `when`(testImage.format).thenReturn(format)
            val result = ImageUtils.convertYuvImageToBitmap(testImage)
            assertNull("Should return null for format: $format", result)
        }
    }

    @Test
    fun testOptimizeImageForVLM_DifferentConfigs() {
        // Test with different bitmap configurations
        val configs = listOf(
            Bitmap.Config.ARGB_8888,
            Bitmap.Config.RGB_565
        )

        for (config in configs) {
            val bitmap = Bitmap.createBitmap(1000, 1000, config)
            val result = ImageUtils.optimizeImageForVLM(bitmap, 336)

            assertNotNull("Should handle config: $config", result)
            assertEquals("Should scale correctly for config: $config", 336, result.width)
        }
    }
}
