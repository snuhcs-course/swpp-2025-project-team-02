package com.example.fortuna_android.ui

import android.widget.ImageView
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Unit tests for SimpleMascotView
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SimpleMascotViewTest {

    @Test
    fun `test SimpleMascotView instantiation`() {
        val context = RuntimeEnvironment.getApplication()
        val view = SimpleMascotView(context)

        assertNotNull(view)
    }

    @Test
    fun `test SimpleMascotView is an ImageView`() {
        val context = RuntimeEnvironment.getApplication()
        val view = SimpleMascotView(context)

        assertTrue(view is ImageView)
    }

    @Test
    fun `test SimpleMascotView has FIT_CENTER scale type`() {
        val context = RuntimeEnvironment.getApplication()
        val view = SimpleMascotView(context)

        assertEquals(ImageView.ScaleType.FIT_CENTER, view.scaleType)
    }

    @Test
    fun `test SimpleMascotView with AttributeSet constructor`() {
        val context = RuntimeEnvironment.getApplication()
        val view = SimpleMascotView(context, null)

        assertNotNull(view)
        assertEquals(ImageView.ScaleType.FIT_CENTER, view.scaleType)
    }

    @Test
    fun `test SimpleMascotView with all parameters`() {
        val context = RuntimeEnvironment.getApplication()
        val view = SimpleMascotView(context, null, 0)

        assertNotNull(view)
        assertEquals(ImageView.ScaleType.FIT_CENTER, view.scaleType)
    }

    @Test
    fun `test SimpleMascotView loads fallback image when asset not found`() {
        val context = RuntimeEnvironment.getApplication()
        val view = SimpleMascotView(context)

        // The view should not crash even if mascot.png is not found
        // It should show fallback icon instead
        assertNotNull(view.drawable)
    }
}
