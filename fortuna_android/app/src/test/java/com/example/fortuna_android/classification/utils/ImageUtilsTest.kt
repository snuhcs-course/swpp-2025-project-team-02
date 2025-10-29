package com.example.fortuna_android.classification.utils

import android.graphics.Bitmap
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ImageUtilsTest {

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
    fun testBitmapToByteArray() {
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        val byteArray = ImageUtils.run { bitmap.toByteArray() }
        assertNotNull(byteArray)
        assertTrue(byteArray.isNotEmpty())
    }
}
