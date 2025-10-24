package com.example.fortuna_android.classification.utils

import android.graphics.Bitmap
import android.graphics.Color
import com.example.fortuna_android.classification.utils.ImageUtils.toByteArray
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for ImageUtils
 * Tests bitmap rotation and byte array conversion utilities
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ImageUtilsTest {

    // ========== rotateBitmap() Tests ==========

    @Test
    fun `test rotateBitmap with 0 rotation returns same bitmap`() {
        val bitmap = createTestBitmap(100, 100)

        val result = ImageUtils.rotateBitmap(bitmap, 0)

        assertSame("Should return same bitmap for 0 rotation", bitmap, result)
    }

    @Test
    fun `test rotateBitmap with 90 degrees`() {
        val bitmap = createTestBitmap(100, 50)

        val result = ImageUtils.rotateBitmap(bitmap, 90)

        assertNotNull("Rotated bitmap should not be null", result)
        assertNotSame("Should return new bitmap", bitmap, result)
        // After 90 degree rotation, dimensions swap
        assertEquals("Width should swap with height", 50, result.width)
        assertEquals("Height should swap with width", 100, result.height)
    }

    @Test
    fun `test rotateBitmap with 180 degrees`() {
        val bitmap = createTestBitmap(100, 100)

        val result = ImageUtils.rotateBitmap(bitmap, 180)

        assertNotNull("Rotated bitmap should not be null", result)
        assertNotSame("Should return new bitmap", bitmap, result)
        assertEquals("Width should remain same", 100, result.width)
        assertEquals("Height should remain same", 100, result.height)
    }

    @Test
    fun `test rotateBitmap with 270 degrees`() {
        val bitmap = createTestBitmap(100, 50)

        val result = ImageUtils.rotateBitmap(bitmap, 270)

        assertNotNull("Rotated bitmap should not be null", result)
        assertNotSame("Should return new bitmap", bitmap, result)
        // After 270 degree rotation, dimensions swap
        assertEquals("Width should swap with height", 50, result.width)
        assertEquals("Height should swap with width", 100, result.height)
    }

    @Test
    fun `test rotateBitmap with 360 degrees`() {
        val bitmap = createTestBitmap(100, 100)

        val result = ImageUtils.rotateBitmap(bitmap, 360)

        assertNotNull("Rotated bitmap should not be null", result)
        assertEquals("Width should remain same", 100, result.width)
        assertEquals("Height should remain same", 100, result.height)
    }

    @Test
    fun `test rotateBitmap with negative rotation`() {
        val bitmap = createTestBitmap(100, 50)

        val result = ImageUtils.rotateBitmap(bitmap, -90)

        assertNotNull("Rotated bitmap should not be null", result)
        // -90 is same as 270
        assertEquals("Width should swap with height", 50, result.width)
        assertEquals("Height should swap with width", 100, result.height)
    }

    @Test
    fun `test rotateBitmap with square bitmap`() {
        val bitmap = createTestBitmap(100, 100)

        val result90 = ImageUtils.rotateBitmap(bitmap, 90)
        val result180 = ImageUtils.rotateBitmap(bitmap, 180)
        val result270 = ImageUtils.rotateBitmap(bitmap, 270)

        // Square bitmaps maintain dimensions after rotation
        assertEquals("90° rotation should maintain dimensions", 100, result90.width)
        assertEquals("90° rotation should maintain dimensions", 100, result90.height)

        assertEquals("180° rotation should maintain dimensions", 100, result180.width)
        assertEquals("180° rotation should maintain dimensions", 100, result180.height)

        assertEquals("270° rotation should maintain dimensions", 100, result270.width)
        assertEquals("270° rotation should maintain dimensions", 100, result270.height)
    }

    @Test
    fun `test rotateBitmap with rectangular bitmap landscape`() {
        val bitmap = createTestBitmap(200, 100)  // Landscape

        val result = ImageUtils.rotateBitmap(bitmap, 90)

        assertNotNull("Result should not be null", result)
        assertEquals("Width becomes height", 100, result.width)
        assertEquals("Height becomes width", 200, result.height)
    }

    @Test
    fun `test rotateBitmap with rectangular bitmap portrait`() {
        val bitmap = createTestBitmap(100, 200)  // Portrait

        val result = ImageUtils.rotateBitmap(bitmap, 90)

        assertNotNull("Result should not be null", result)
        assertEquals("Width becomes height", 200, result.width)
        assertEquals("Height becomes width", 100, result.height)
    }

    @Test
    fun `test rotateBitmap with very small bitmap`() {
        val bitmap = createTestBitmap(1, 1)

        val result = ImageUtils.rotateBitmap(bitmap, 90)

        assertNotNull("Should handle 1x1 bitmap", result)
        assertEquals("Width should be 1", 1, result.width)
        assertEquals("Height should be 1", 1, result.height)
    }

    @Test
    fun `test rotateBitmap with large bitmap`() {
        val bitmap = createTestBitmap(1000, 1000)

        val result = ImageUtils.rotateBitmap(bitmap, 90)

        assertNotNull("Should handle large bitmap", result)
        assertEquals("Dimensions should be correct", 1000, result.width)
        assertEquals("Dimensions should be correct", 1000, result.height)
    }

    @Test
    fun `test rotateBitmap multiple times`() {
        val bitmap = createTestBitmap(100, 50)

        val rotated90 = ImageUtils.rotateBitmap(bitmap, 90)
        val rotated180 = ImageUtils.rotateBitmap(rotated90, 90)
        val rotated270 = ImageUtils.rotateBitmap(rotated180, 90)
        val rotated360 = ImageUtils.rotateBitmap(rotated270, 90)

        assertNotNull("Multiple rotations should work", rotated360)
        // After 4x90° = 360°, dimensions return to original
        assertEquals("After 360° width should match original", 100, rotated360.width)
        assertEquals("After 360° height should match original", 50, rotated360.height)
    }

    // ========== toByteArray() Extension Function Tests ==========

    @Test
    fun `test toByteArray converts bitmap to byte array`() {
        val bitmap = createTestBitmap(100, 100)

        val byteArray = bitmap.toByteArray()

        assertNotNull("Byte array should not be null", byteArray)
        assertTrue("Byte array should not be empty", byteArray.isNotEmpty())
    }

    @Test
    fun `test toByteArray produces non-empty array`() {
        val bitmap = createTestBitmap(100, 100)

        val byteArray = bitmap.toByteArray()

        assertTrue("Byte array should have content", byteArray.size > 0)
    }

    @Test
    fun `test toByteArray with different bitmap sizes`() {
        val small = createTestBitmap(10, 10)
        val medium = createTestBitmap(100, 100)
        val large = createTestBitmap(500, 500)

        val smallArray = small.toByteArray()
        val mediumArray = medium.toByteArray()
        val largeArray = large.toByteArray()

        assertTrue("Small bitmap should produce smallest array", smallArray.size < mediumArray.size)
        assertTrue("Medium bitmap should produce medium array", mediumArray.size < largeArray.size)
    }

    @Test
    fun `test toByteArray with different bitmap configurations`() {
        val bitmap1 = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val bitmap2 = Bitmap.createBitmap(100, 100, Bitmap.Config.RGB_565)

        val array1 = bitmap1.toByteArray()
        val array2 = bitmap2.toByteArray()

        assertNotNull("ARGB_8888 bitmap should convert", array1)
        assertNotNull("RGB_565 bitmap should convert", array2)
        assertTrue("Both should produce valid arrays", array1.isNotEmpty() && array2.isNotEmpty())
    }

    @Test
    fun `test toByteArray is consistent for same bitmap`() {
        val bitmap = createTestBitmap(100, 100)

        val array1 = bitmap.toByteArray()
        val array2 = bitmap.toByteArray()

        assertEquals("Multiple calls should produce same size", array1.size, array2.size)
    }

    @Test
    fun `test toByteArray with minimum size bitmap`() {
        val bitmap = createTestBitmap(1, 1)

        val byteArray = bitmap.toByteArray()

        assertNotNull("1x1 bitmap should convert", byteArray)
        assertTrue("Should produce valid JPEG data", byteArray.isNotEmpty())
    }

    @Test
    fun `test toByteArray with rectangular bitmaps`() {
        val landscape = createTestBitmap(200, 100)
        val portrait = createTestBitmap(100, 200)

        val landscapeArray = landscape.toByteArray()
        val portraitArray = portrait.toByteArray()

        assertNotNull("Landscape should convert", landscapeArray)
        assertNotNull("Portrait should convert", portraitArray)
        assertTrue("Both should have valid data", landscapeArray.isNotEmpty() && portraitArray.isNotEmpty())
    }

    // ========== Integration Tests ==========

    @Test
    fun `test rotate and convert to byte array workflow`() {
        val bitmap = createTestBitmap(100, 50)

        val rotated = ImageUtils.rotateBitmap(bitmap, 90)
        val byteArray = rotated.toByteArray()

        assertNotNull("Rotated bitmap should not be null", rotated)
        assertNotNull("Byte array should not be null", byteArray)
        assertTrue("Byte array should have content", byteArray.isNotEmpty())
    }

    @Test
    fun `test convert to byte array then rotate`() {
        val bitmap = createTestBitmap(100, 50)

        val byteArray = bitmap.toByteArray()
        val rotated = ImageUtils.rotateBitmap(bitmap, 90)

        assertNotNull("Byte array should not be null", byteArray)
        assertNotNull("Rotated bitmap should not be null", rotated)
        assertTrue("Original bitmap should still be usable", byteArray.isNotEmpty())
    }

    @Test
    fun `test multiple rotations and conversions`() {
        val bitmap = createTestBitmap(100, 100)

        val rotated1 = ImageUtils.rotateBitmap(bitmap, 90)
        val array1 = rotated1.toByteArray()

        val rotated2 = ImageUtils.rotateBitmap(rotated1, 90)
        val array2 = rotated2.toByteArray()

        assertNotNull("First rotation should work", rotated1)
        assertNotNull("First conversion should work", array1)
        assertNotNull("Second rotation should work", rotated2)
        assertNotNull("Second conversion should work", array2)
    }

    // ========== Edge Case Tests ==========

    @Test
    fun `test rotateBitmap with odd angles`() {
        val bitmap = createTestBitmap(100, 100)

        val rotated45 = ImageUtils.rotateBitmap(bitmap, 45)
        val rotated135 = ImageUtils.rotateBitmap(bitmap, 135)

        assertNotNull("45° rotation should work", rotated45)
        assertNotNull("135° rotation should work", rotated135)
    }

    @Test
    fun `test rotateBitmap with large angle values`() {
        val bitmap = createTestBitmap(100, 100)

        val rotated720 = ImageUtils.rotateBitmap(bitmap, 720)  // 2 full rotations
        val rotated1080 = ImageUtils.rotateBitmap(bitmap, 1080)  // 3 full rotations

        assertNotNull("720° rotation should work", rotated720)
        assertNotNull("1080° rotation should work", rotated1080)
    }

    @Test
    fun `test toByteArray produces valid JPEG header`() {
        val bitmap = createTestBitmap(100, 100)

        val byteArray = bitmap.toByteArray()

        // JPEG files start with FF D8
        assertTrue("Should have JPEG header", byteArray.size >= 2)
        assertEquals("First byte should be 0xFF", 0xFF.toByte(), byteArray[0])
        assertEquals("Second byte should be 0xD8", 0xD8.toByte(), byteArray[1])
    }

    // ========== Helper Functions ==========

    private fun createTestBitmap(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        // Fill with a simple color for testing
        bitmap.eraseColor(Color.RED)
        return bitmap
    }
}
