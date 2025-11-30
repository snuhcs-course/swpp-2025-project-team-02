package com.example.fortuna_android.classification.utils

import android.graphics.Bitmap
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for ImageUtils object
 * Tests bitmap rotation utilities
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ImageUtilsTest {

    // ========== rotateBitmap() Tests ==========

    @Test
    fun testRotateBitmap_NoRotation() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val result = ImageUtils.rotateBitmap(bitmap, 0)
        assertSame(bitmap, result)
    }

    @Test
    fun testRotateBitmap_With90Degree() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val result = ImageUtils.rotateBitmap(bitmap, 90)
        assertNotNull(result)
    }

    @Test
    fun `test rotateBitmap with 0 degrees returns same bitmap`() {
        val bitmap = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888)
        val rotated = ImageUtils.rotateBitmap(bitmap, 0)
        assertSame("0 degree rotation should return same bitmap instance", bitmap, rotated)
        assertEquals("Width should remain same", 100, rotated.width)
        assertEquals("Height should remain same", 200, rotated.height)
    }

    @Test
    fun `test rotateBitmap with 90 degrees swaps dimensions`() {
        val bitmap = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888)
        val rotated = ImageUtils.rotateBitmap(bitmap, 90)
        assertNotSame("90 degree rotation should create new bitmap", bitmap, rotated)
        assertEquals("Width should become original height", 200, rotated.width)
        assertEquals("Height should become original width", 100, rotated.height)
    }

    @Test
    fun `test rotateBitmap with 180 degrees keeps dimensions`() {
        val bitmap = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888)
        val rotated = ImageUtils.rotateBitmap(bitmap, 180)
        assertNotSame("180 degree rotation should create new bitmap", bitmap, rotated)
        assertEquals("Width should remain same", 100, rotated.width)
        assertEquals("Height should remain same", 200, rotated.height)
    }

    @Test
    fun `test rotateBitmap with 270 degrees swaps dimensions`() {
        val bitmap = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888)
        val rotated = ImageUtils.rotateBitmap(bitmap, 270)
        assertNotSame("270 degree rotation should create new bitmap", bitmap, rotated)
        assertEquals("Width should become original height", 200, rotated.width)
        assertEquals("Height should become original width", 100, rotated.height)
    }

    @Test
    fun `test rotateBitmap with 360 degrees`() {
        val bitmap = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888)
        val rotated = ImageUtils.rotateBitmap(bitmap, 360)
        // 360 degrees creates a new bitmap per implementation (not optimized to return same)
        assertNotNull("360 degree rotation should return a bitmap", rotated)
        assertEquals("Width should remain same after 360 degrees", 100, rotated.width)
        assertEquals("Height should remain same after 360 degrees", 200, rotated.height)
    }

    @Test
    fun `test rotateBitmap with negative angle`() {
        val bitmap = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888)
        val rotated = ImageUtils.rotateBitmap(bitmap, -90)
        assertNotSame("Negative rotation should create new bitmap", bitmap, rotated)
        assertEquals("Width should become original height", 200, rotated.width)
        assertEquals("Height should become original width", 100, rotated.height)
    }

    @Test
    fun `test rotateBitmap with square bitmap`() {
        val bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        val rotated90 = ImageUtils.rotateBitmap(bitmap, 90)
        val rotated180 = ImageUtils.rotateBitmap(bitmap, 180)
        assertEquals("Square bitmap width should remain same", 200, rotated90.width)
        assertEquals("Square bitmap height should remain same", 200, rotated90.height)
        assertEquals("Square bitmap width should remain same", 200, rotated180.width)
        assertEquals("Square bitmap height should remain same", 200, rotated180.height)
    }

    @Test
    fun `test rotateBitmap multiple times`() {
        val bitmap = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888)
        val rotated1 = ImageUtils.rotateBitmap(bitmap, 90)
        val rotated2 = ImageUtils.rotateBitmap(rotated1, 90)
        val rotated3 = ImageUtils.rotateBitmap(rotated2, 90)
        val rotated4 = ImageUtils.rotateBitmap(rotated3, 90)
        assertEquals("After 4x90 rotation width should match original", 100, rotated4.width)
        assertEquals("After 4x90 rotation height should match original", 200, rotated4.height)
    }

    @Test
    fun `test rotateBitmap with small bitmap`() {
        val bitmap = Bitmap.createBitmap(10, 20, Bitmap.Config.ARGB_8888)
        val rotated = ImageUtils.rotateBitmap(bitmap, 90)
        assertEquals("Small bitmap rotation should work", 20, rotated.width)
        assertEquals("Small bitmap rotation should work", 10, rotated.height)
    }

    @Test
    fun `test rotateBitmap with large bitmap`() {
        val bitmap = Bitmap.createBitmap(1920, 1080, Bitmap.Config.ARGB_8888)
        val rotated = ImageUtils.rotateBitmap(bitmap, 90)
        assertEquals("Large bitmap rotation should work", 1080, rotated.width)
        assertEquals("Large bitmap rotation should work", 1920, rotated.height)
    }

    @Test
    fun `test rotateBitmap preserves bitmap config`() {
        val configs = listOf(Bitmap.Config.ARGB_8888, Bitmap.Config.RGB_565)
        configs.forEach { config ->
            val bitmap = Bitmap.createBitmap(100, 100, config)
            val rotated = ImageUtils.rotateBitmap(bitmap, 90)
            assertEquals("Bitmap config should be preserved for $config", config, rotated.config)
        }
    }

    @Test
    fun `test rotateBitmap with odd dimensions`() {
        val bitmap = Bitmap.createBitmap(101, 201, Bitmap.Config.ARGB_8888)
        val rotated = ImageUtils.rotateBitmap(bitmap, 90)
        assertEquals("Odd dimensions should rotate correctly", 201, rotated.width)
        assertEquals("Odd dimensions should rotate correctly", 101, rotated.height)
    }

    @Test
    fun `test rotateBitmap with very wide bitmap`() {
        val bitmap = Bitmap.createBitmap(1000, 100, Bitmap.Config.ARGB_8888)
        val rotated = ImageUtils.rotateBitmap(bitmap, 90)
        assertEquals("Very wide bitmap should rotate", 100, rotated.width)
        assertEquals("Very wide bitmap should rotate", 1000, rotated.height)
    }

    @Test
    fun `test rotateBitmap with very tall bitmap`() {
        val bitmap = Bitmap.createBitmap(100, 1000, Bitmap.Config.ARGB_8888)
        val rotated = ImageUtils.rotateBitmap(bitmap, 90)
        assertEquals("Very tall bitmap should rotate", 1000, rotated.width)
        assertEquals("Very tall bitmap should rotate", 100, rotated.height)
    }

    @Test
    fun `test ImageUtils is object singleton`() {
        val instance1 = ImageUtils
        val instance2 = ImageUtils
        assertSame("ImageUtils should be singleton object", instance1, instance2)
    }

    // Note: testBitmapToByteArray moved to androidTest due to native library requirements
    // Bitmap.compress() requires real Android runtime, not Robolectric
}
