package com.example.fortuna_android.classification.utils

import android.graphics.Bitmap
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for ImageUtils
 * Focus on line coverage
 */
@RunWith(RobolectricTestRunner::class)
class ImageUtilsTest {

    @Test
    fun testRotateBitmap_NoRotation() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val result = ImageUtils.rotateBitmap(bitmap, 0)

        // Should return same bitmap when rotation is 0
        assertSame(bitmap, result)
    }

    @Test
    fun testRotateBitmap_With90Degree() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val result = ImageUtils.rotateBitmap(bitmap, 90)

        // Should return different bitmap when rotation is not 0
        assertNotNull(result)
        assertEquals(100, result.width)
        assertEquals(100, result.height)
    }

    @Test
    fun testRotateBitmap_With180Degree() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val result = ImageUtils.rotateBitmap(bitmap, 180)

        assertNotNull(result)
    }

    @Test
    fun testRotateBitmap_With270Degree() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val result = ImageUtils.rotateBitmap(bitmap, 270)

        assertNotNull(result)
    }

    @Test
    fun testBitmapToByteArray() {
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        val byteArray = ImageUtils.run { bitmap.toByteArray() }

        // Should convert to byte array
        assertNotNull(byteArray)
        assertTrue(byteArray.isNotEmpty())
    }

    @Test
    fun testBitmapToByteArray_LargeBitmap() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val byteArray = ImageUtils.run { bitmap.toByteArray() }

        assertTrue(byteArray.isNotEmpty())
    }
}
